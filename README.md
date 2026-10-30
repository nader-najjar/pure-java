# pure-java

> Java best practices for a standalone process.

### 1. Prerequisites

---

Install [Bazelisk](https://bazel.build/install/bazelisk). It reads `.bazelversion`; Bazel supplies the Java toolchain. Run commands from the repository root.

### 2. Build and Test

---

#### Everyday Commands

| Goal | Command |
| --- | --- |
| Compile production code | `bazel build //:entrypoint` |
| Unit tests only | `bazel test //:all_tests` |
| Checkstyle and SpotBugs | `bazel test //:checkstyle //:spotbugs` |
| Coverage | `bazel coverage //:all_tests --combined_report=lcov` |
| Package without checks | `bazel build //:app_bin_deploy.jar` |
| Full verification | `bazel build //... //:app_bin_deploy.jar && bazel test //... && bazel coverage //:all_tests --combined_report=lcov` |
| Clean outputs | `bazel clean` |
| List targets | `bazel query //...` |

Tests and packaging compile their prerequisites automatically. Coverage measures `//:app`, excluding the entrypoint and composition; no minimum is enforced.

Run one test class, or one method:

```sh
bazel test //:all_tests --test_arg='--include-classname=io\.template\.samplebusinesslayer\.CalculatorTest'
bazel test //:all_tests --test_arg='--include-classname=io\.template\.samplebusinesslayer\.CalculatorTest' --test_arg='--include-methodname=.*throwsOnDivisionByZero'
```

#### Outputs

- Application JAR: `bazel-bin/app_bin_deploy.jar`.
- Test logs: `bazel-testlogs/<target>/test.log`.
- Combined coverage: `$(bazel info output_path)/_coverage/_coverage_report.dat`.

### 3. Development Workflows

---

#### Dependency Management

- Add/remove: edit Maven coordinates in `MODULE.bazel` and the consuming target's `deps` in `BUILD.bazel`.
- Upgrade/pin: change the explicit artifact version or its declared BOM version in `MODULE.bazel`.
- Refresh locks: `REPIN=1 bazel run @maven//:pin`; review `maven_install.json` with the declarations, then build and test.
- Inspect resolved versions/conflicts: check `artifacts`, `dependencies`, and `conflict_resolution` in `maven_install.json`. Trace a target's dependency path with:

```sh
bazel query 'somepath(//:app, @maven//:org_slf4j_slf4j_api)'
```

#### Toolchain Changes

- Bazel: `.bazelversion`; Java language/runtime versions: `.bazelrc`.
- Bazel rules: `MODULE.bazel`. Refresh `MODULE.bazel.lock` with `bazel mod deps --lockfile_mode=update`, then run full verification.

### 4. IDE Setup

---

Print the project's JDK home:

```sh
echo "$(bazel info output_base)/$(bazel cquery @rules_java//toolchains:current_java_runtime --output=starlark --starlark:expr='providers(target)["TemplateVariableInfo"].variables["JAVABASE"]')"
```

Follow the [IntelliJ IDEA](https://app.notion.com/p/f4c0a2b7875583b79fa5015d58412fea) or [VS Code](https://app.notion.com/p/4fd0a2b787558204a6c001d0b034f698) setup guide, skipping Maven/Gradle-specific steps.

- IntelliJ: use that path as the Project SDK and **SDK Default** language level.
- VS Code: use it for `java.jdt.ls.java.home` and the `JavaSE-25` entry in `java.configuration.runtimes`; use `<JDK home>/bin/java` for `java.test.config.javaExec`.

Re-run after toolchain changes or cache cleanup.

### 5. Concepts

---

- [Shutdown Concepts](https://app.notion.com/p/4080a2b78755818a93b6c7ffa13a809d)
- [Bazel Java dependencies](https://github.com/bazel-contrib/rules_jvm_external)
- [JUnit test selection](https://docs.junit.org/5.13.4/user-guide/index.html#running-tests-console-launcher)

### 6. Appendix

---

#### Java Dependency Attributes

| `BUILD.bazel` attribute | Use |
| --- | --- |
| `deps` | Compile-time dependencies; also available at runtime |
| `runtime_deps` | Runtime-only dependencies |
| `exports` | Dependencies exposed to consumers of a library |
| `plugins` | Annotation processors |
| `neverlink = True` | Compile against a library without including it at runtime |

### License

---

All rights reserved. See [LICENSE](LICENSE).
