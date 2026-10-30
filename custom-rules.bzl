load("@aspect_rules_lint//lint:checkstyle.bzl", "lint_checkstyle_aspect")
load("@aspect_rules_lint//lint:lint_test.bzl", "lint_test")
load("@aspect_rules_lint//lint:spotbugs.bzl", "lint_spotbugs_aspect")
load("@rules_java//java/common:java_common.bzl", "java_common")
load("@rules_pkg//pkg:providers.bzl", "PackageFilesInfo")

checkstyle = lint_checkstyle_aspect(
    binary = Label("//:checkstyle_bin"),
    config = Label("//:checkstyle-configuration.xml"),
    rule_kinds = ["java_library", "java_test"],
)

spotbugs = lint_spotbugs_aspect(
    binary = Label("//:spotbugs_bin"),
    exclude_filter = Label("//:spotbugs-configuration.xml"),
)

checkstyle_test = lint_test(aspect = checkstyle)
spotbugs_test = lint_test(aspect = spotbugs)

def _java_runtime_files_impl(ctx):
    runtime = ctx.attr.runtime[java_common.JavaRuntimeInfo]
    execution_runtime = ctx.toolchains["@bazel_tools//tools/jdk:toolchain_type"].java.java_runtime
    if runtime.java_home != execution_runtime.java_home:
        fail("Image packaging requires the same native Linux build and runtime JDK")
    prefix = runtime.java_home + "/"
    mapping = {}
    for file in runtime.files.to_list():
        if not file.path.startswith(prefix):
            fail("Runtime file is outside java_home: " + file.path)
        mapping["opt/java/" + file.path[len(prefix):]] = file
    if "opt/java/legal/java.base/LICENSE" not in mapping:
        fail("The selected Java runtime must include its redistribution notices")
    return [
        DefaultInfo(files = runtime.files),
        PackageFilesInfo(dest_src_map = mapping, attributes = {})
    ]

java_runtime_files = rule(
    implementation = _java_runtime_files_impl,
    attrs = {
        "runtime": attr.label(
            default = Label("@rules_java//toolchains:current_java_runtime"),
            providers = [java_common.JavaRuntimeInfo]
        )
    },
    toolchains = ["@bazel_tools//tools/jdk:toolchain_type"]
)
