# template-pure-java

A small Java service template built with Bazel. Bazel owns Java compilation, dependency resolution, tests, static analysis, coverage, and OCI image construction.

## Prerequisites

Install [Bazelisk](https://bazel.build/install/bazelisk). It reads `.bazelversion` and selects the pinned Bazel release. Bazel downloads the pinned Java 25 toolchains; a separate system JDK is not needed for project compilation or tests.

## Standard commands

```sh
bazel build //:app_bin_deploy.jar //:lambda_deployment_package
bazel test --build_tests_only //...
bazel coverage //:all_tests --combined_report=lcov
```

`bazel test --build_tests_only //...` runs the JUnit suite, Checkstyle, and SpotBugs. Aspect ruleset telemetry is disabled in `.bazelrc`. `bazel coverage` writes the combined LCOV report under Bazel's output tree. Image packaging runs separately from the application build; `bazel cquery --output=files //image:oci_image_tarball` prints the OCI archive path after the fleet artifact is extracted.

For local lint output, use:

```sh
bazel build --config=lint //:app //:composition //:entrypoint //:lambda_entrypoint //:all_tests
```

## Release Pipeline

`.github/workflows/pipeline.yml` runs CI on pull requests and pushes to `main`. On a successful main push, the same pipeline run continues to OCI Image Packaging. The jobs are defined in separate reusable workflow files.

CI builds, tests, and measures coverage on native Linux/ARM64 (`ubuntu-24.04-arm`), using the same commands as local development. Bazel targets the native host platform. It stages the deploy JAR and image-build definitions, then uploads `release-artifact-<commit SHA>`.

CD downloads that artifact without checking out the repository or recompiling Java. The archive contains `image/executable/app_bin_deploy.jar`, `image/BUILD.bazel`, `MODULE.bazel`, `MODULE.bazel.lock`, `.bazelrc`, `.bazelversion`, and `maven_install.json`. In this standalone workspace, `bazel build //image:oci_image_tarball` assembles a Linux/ARM64 OCI archive, uploaded as `oci-image-<commit SHA>`. Bazel image rules perform packaging without Docker.

Local builds provide development feedback; their outputs are never release inputs. Use the explicit application targets above for local development: `bazel build //...` includes image targets that require the staged fleet JAR. Deployment infrastructure must select Linux/ARM64. Registry publication, deployment, and runtime testing belong to the consuming project's pipeline.

Concepts: [Build and OCI Image Packaging](https://app.notion.com/p/3f90a2b787558157a5c2cf7a9b7e92cc?pvs=204).

## IDE setup

Use your usual [IntelliJ IDEA setup guide](https://nader-najjar.notion.site/JetBrains-IDE-Setup-Usage-Guide-d9b0a2b78755822f9d03819f5f02feb2) or [VS Code setup guide](https://nader-najjar.notion.site/Visual-Studio-Code-Setup-Usage-Guide-f4e0a2b7875583be9293817d26459034). The multi-repository workspace and editor readability instructions still apply.

### Get the JDK path selected by Bazel

Run this from the repository root:

```sh
bazel run //tools/ide:print_java_home
```

It prints the JDK home reported by the Java runtime selected for this Bazel target. The configured compiler toolchain and target runtime both use `remotejdk_25`, so this is also the JDK behind this project's Java 25 compilation. Use the printed path as the IntelliJ Project SDK and as VS Code's Java home. Re-run the command after changing the Java toolchain or clearing Bazel's downloaded repositories.

`bazel info java-home` reports the JDK running Bazel itself, which can differ from the project's selected JDK. Bazel's `JavaRuntimeInfo.java_home` identifies the selected runtime inside Bazel's execution root, but on macOS that repository root is a wrapper around the IDE-ready `Contents/Home` directory. Running this tiny target lets the selected JDK report its actual home directly, without depending on Bazel cache layout or platform-specific path rewriting.

### IntelliJ IDEA Ultimate

In the guide's **Project Settings → Project** step, choose **SDK → Add JDK from disk** and use the path printed above. Set **Language Level** to **SDK Default**. Keep the guide's Multi-Project Workspace and per-repository terminal setup. The Gradle JVM step does not apply because this repository builds with Bazel.

### Visual Studio Code

Keep the guide's saved multi-root `.code-workspace`. In the Java folder settings, use the printed path for `java.jdt.ls.java.home` and for the `path` entry in `java.configuration.runtimes` (with `name` set to `JavaSE-25` and `default` set to `true`). This points both the Java language server and project Java runtime at Bazel's JDK. The Maven/Gradle-specific Java-home settings do not apply here.

For example, replace the placeholder with the command's output:

```json
{
  "java.jdt.ls.java.home": "/path/printed/by/command",
  "java.configuration.runtimes": [
    { "name": "JavaSE-25", "path": "/path/printed/by/command", "default": true }
  ],
  "java.test.config": {
    "javaExec": "/path/printed/by/command/bin/java"
  }
}
```

`java.test.config.javaExec` expects the Java executable, so its path ends in `bin/java` on macOS/Linux (or `\bin\java.exe` on Windows). The other two settings expect the JDK home directory.

## Application configuration

The template requires `STAGE`, `AWS_REGION`, `CALCULATION_RESULTS_TABLE_NAME`, `EXAMPLE_STRING_VAR`, `EXAMPLE_INT_VAR`, and `EXAMPLE_BOOLEAN_VAR`. `STAGE` must be exactly `ALPHA`, `BETA`, `GAMMA`, or `PRODUCTION`. `AWS_REGION` must exactly match a region ID known to AWS SDK 2.55.11, such as `us-east-1`. Boolean environment values must be lowercase `true` or `false`. Missing, blank, and invalid values fail validation.

JSON input ignores unknown properties and rejects duplicate keys. All five declared input properties must be present; string, timestamp, and list values must be non-null, and primitive values reject nulls.

AWS SDK versions are managed by the BOM in `MODULE.bazel`; the application uses Regions and the DynamoDB client. The DynamoDB table must exist with a string partition key named `id`, and the deployment needs permission to write items. AWS credentials use the SDK default provider chain. The calculator and persistence are dummy examples; repeated identical calculations overwrite the same item.

## Project layout

- `src/main/java` — sample application and business logic.
- `src/main/resources` — runtime resources.
- `src/test/java` — JUnit 5 tests and test support.
- `tools/lint` — hermetic Checkstyle and SpotBugs launchers and lint aspects.
- `MODULE.bazel` — pinned Bazel modules, Maven coordinates, and Java container base image digest.
- `maven_install.json` — resolved Maven dependency versions, URLs, and SHA-256 checksums.

## Application boundaries

`Main` and `LambdaHandler` stay at the package root. `composition` holds Guice modules, `environment` groups environment parsing, errors, and models, and `execution` holds the per-input workflow. `shared` retains stateless utilities.

| Target | Responsibility |
| --- | --- |
| `//:app` | Execution, business logic, environment configuration, and shared utilities |
| `//:composition` | Strict Guice wiring and environment/AWS client providers |
| `//:entrypoint` | Process entry point and shutdown hook |
| `//:lambda_entrypoint` | AWS Lambda stream handler |
| `//:logging` | Shared JSON logging configuration and runtime |
| `//:app_bin` | Runnable process and deploy JAR |
| `//:lambda_bin` | Handler deploy JAR with runtime dependencies |

Checkstyle and SpotBugs cover every production Java source and the tests. Coverage measures `//:app`, excluding both entrypoints and injection wiring to match the legacy scope. The report measures line coverage and currently has no enforced minimum. The composition test constructs strict wiring with substituted environment and AWS clients; entrypoint and shutdown integration tests are deferred.

## Dependency updates

1. Change Maven coordinates or BOM versions in `MODULE.bazel`. Use explicit artifact versions unless a declared BOM supplies them.
2. Run `REPIN=1 bazel run @maven//:pin` to update `maven_install.json`.
3. Review both files together and build the affected targets.

Update Bazel in `.bazelversion`. Update the Java toolchain flags in `.bazelrc` and the Java runtime base image digest in `MODULE.bazel` together when changing the Java major version.

## Shutdown

Shutdown pattern (implemented in `Main`):

1. SIGTERM (or SIGINT/SIGHUP) fires the shutdown hook, which interrupts the main thread and waits up to `SHUTDOWN_GRACE_PERIOD_SECONDS` seconds for main to finish. Keep that grace period below the platform's SIGTERM-to-SIGKILL timeout (ECS and Kubernetes default to 30s)
2. Main clears the interrupt, performs all resource cleanup in its own `finally` block, then releases the hook
3. The JVM exits after the hook thread returns. On a normal exit the hook returns at once. After a signal the process exits with 128 + the signal number (143 for SIGTERM) regardless of main's exit code

Work must respond to that interrupt:

* The interrupt aborts interruptible waits (sleep, queue and latch waits, `Future.get`, SDK retry backoff) by throwing an exception, which should propagate up to `Main`
* Classic `java.net` socket I/O, `synchronized` and `Lock.lock()` ignore the interrupt, so a call already in flight runs until it returns or times out; normal calls finish well within `SHUTDOWN_GRACE_PERIOD_SECONDS`
* Never swallow `InterruptedException`: declare it, or call `Thread.currentThread().interrupt()` and rethrow it wrapped
* Only `Main` calls `System.exit`; elsewhere throw so the exception reaches `Main`
* CPU-bound loops check `Thread.currentThread().isInterrupted()` between iterations
* Work on other threads is not interrupted; main must stop it in its `finally` block

Correctness is guaranteed by idempotency: dying at any point (including SIGKILL, which bypasses hooks entirely) leaves the system in a consistent state. Graceful shutdown only improves efficiency - it is never required for correctness.


## Outputs

The template currently supports two deployment paths: OCI/container through `Main`, and AWS Lambda through `LambdaHandler`. Keep only the path your application uses and remove the other using the instructions below. Both paths share execution and business logic. AWS integrations are examples; other cloud providers can be added.

### OCI / Container (`Main`)

* After extracting the fleet release artifact, build `//image:oci_image_tarball` for a Linux/ARM64 OCI archive. This process can also run locally with `bazel run //:app_bin -- '<event-json>'`.
* The process launcher defaults to `-XX:MaxRAMPercentage=75.0`. The OCI images set the same option through `JAVA_TOOL_OPTIONS`, since their base image runs the deploy JAR directly rather than the Bazel launcher.

To keep only Lambda, remove:

* `src/main/java/io/template/Main.java`, its exclusion from the `app` source glob, and `entrypoint` and `app_bin` in `BUILD.bazel`
* The `entrypoint` references in both lint targets and the `app_bin_deploy.jar` build target in CI
* `image/`, `.github/scripts/stage-release`, `.github/workflows/cd.yml`, the `cd` job in `pipeline.yml`, and CI's image-artifact staging and upload steps
* The `rules_oci` dependency, `distroless` extension, and its `use_repo` block in `MODULE.bazel`
* The Shutdown section and `closeResources` in `composition/AWSClientsModule.java`, including the unused `Injector` import

### AWS Lambda (`LambdaHandler`)

* Handler: `io.template.LambdaHandler::handleRequest`, on the `java25` runtime.
* The event JSON reaches `Executor` as its single input argument. The injector is reused across invocations, and logs include the AWS request ID.
* Build `//:lambda_deployment_package` to produce `bazel-bin/lambda-deployment-package.zip`. The ZIP contains `lib/lambda_bin_deploy.jar`, a Bazel deploy JAR bundling the handler, logging configuration, and runtime dependencies. ZIP timestamps are fixed and build stamping is disabled.
* Deploy that ZIP with your infrastructure tool. Lambda clients live for the execution environment's lifetime; the process Shutdown section does not apply.

To keep only OCI/container deployment, remove:

* `src/main/java/io/template/LambdaHandler.java`, its exclusion from the `app` source glob, and `lambda_entrypoint`, `lambda_bin`, and `lambda_deployment_package` in `BUILD.bazel`
* The `lambda_entrypoint` references in both lint targets, the `pkg_zip` load, and the Lambda build command and CI step
* `com.amazonaws:aws-lambda-java-core` from `MODULE.bazel`

After removing a deployment path, update this Outputs section and regenerate the Maven lockfile using the workflow above. Keep shared environment, execution, composition, logging, and the DynamoDB sample unless you also choose to remove that sample.
