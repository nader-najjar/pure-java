# pure-java

Java best practices.

&nbsp;

## 1. Local Development Machine Prerequisites

* Install Bazelisk as per <https://bazel.build/install/bazelisk>
  * Bazelisk reads `.bazelversion`; Bazel downloads the project's Java toolchain

&nbsp;

## 2. Main Build Tasks

### `bazel build //... //:app_bin_deploy.jar`

* Compiles the code under `src`
* Builds the application deploy JAR

### `bazel test //...`

* Runs unit tests
* Runs Checkstyle
* Runs SpotBugs

### `bazel coverage //:all_tests --combined_report=lcov`

* Runs coverage

### `bazel build //:container_tarball`

* Builds the OCI archive separately from the normal build

### `bazel clean`

* Removes build output directories

### `bazel help`

* Invokes Bazel's help command, showing basic functionality

### `bazel query //...`

* Shows available targets

&nbsp;

## 3. Workflows

### Adding A Java Dependency

1) Find the coordinates on Maven Central (<https://central.sonatype.com>) or the library’s docs.
2) Add the dependency in `MODULE.bazel`, with an explicit version unless a declared BOM supplies it.
3) Update the Maven lockfile using the workflow below.
4) Add the dependency to the appropriate target in `BUILD.bazel` and run the main build tasks above.

### Updating Maven Lockfile

1) Run `REPIN=1 bazel run @maven//:pin`

### Upgrading Bazel Version

1) Update `.bazelversion`

### Upgrading Java Version

1) Update the Java toolchain flags in `.bazelrc` and the Java runtime base image digest in `MODULE.bazel` together

&nbsp;

## 4. IDE Setup

### IntelliJ IDEA Ultimate

Follow the instructions here: <https://app.notion.com/p/f4c0a2b7875583b79fa5015d58412fea>
    * Use the command `bazel run //tools/ide:print_java_home` to get the JDK home selected by Bazel

### Visual Studio Code

Follow the instructions here: <https://app.notion.com/p/4fd0a2b787558204a6c001d0b034f698>
    * Use the command `bazel run //tools/ide:print_java_home` to get the JDK home selected by Bazel

Skip Maven/Gradle-specific steps.

&nbsp;

## 5. References

### Maven

* Maven Central Search: <https://central.sonatype.com>

### Jackson

* Jackson Databind Javadoc: <https://javadoc.io/doc/com.fasterxml.jackson.core/jackson-databind/latest/index.html>
* Jackson Databind GitHub README Tutorial: <https://github.com/FasterXML/jackson-databind>
* Jackson Databind Wiki (Including Databind-Specific Annotations): <https://github.com/FasterXML/jackson-databind/wiki>
* Jackson Annotations Javadoc: <https://javadoc.io/doc/com.fasterxml.jackson.core/jackson-annotations/latest/com.fasterxml.jackson.annotation/com/fasterxml/jackson/annotation/package-summary.html>
* Jackson Annotations GitHub README Tutorial: <https://github.com/FasterXML/jackson-annotations>
* Jackson Annotations Wiki: <https://github.com/FasterXML/jackson-annotations/wiki>

### Hibernate

* Main Documentation Page: <https://docs.hibernate.org/validator/9.1/reference/en-US/html_single/>
* Anchor To Available Annotations: <https://docs.hibernate.org/validator/9.1/reference/en-US/html_single/#section-builtin-constraints>

## Concepts

- [Build and OCI Image Packaging](https://app.notion.com/p/3f90a2b787558157a5c2cf7a9b7e92cc?pvs=204)
- [Product Release Pipelines Across Repositories](https://app.notion.com/p/3f90a2b7875581ef953bca201941e0dd?pvs=204)
- [Deployment Testing and Promotion](https://app.notion.com/p/3f90a2b7875581de812ffaddc1e44b83?pvs=204)
- [Shutdown Concepts](https://app.notion.com/p/4080a2b78755818a93b6c7ffa13a809d)

## License

All rights reserved. See [LICENSE](LICENSE).
