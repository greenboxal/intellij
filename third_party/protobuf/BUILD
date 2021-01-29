load("//intellij_platform_sdk:build_defs.bzl", "select_for_plugin_api")

package(default_visibility = ["//visibility:public"])

licenses(["notice"])

java_library(
    name = "protobuf_internal",
    visibility = ["//visibility:private"],
    exports = select_for_plugin_api({
        "intellij-2020.1": ["@protobuf_2020_1//jar"],
        "intellij-ue-2020.1": ["@protobuf_2020_1//jar"],
        "intellij-2020.2": ["@protobuf_2020_2//jar"],
        "intellij-ue-2020.2": ["@protobuf_2020_2//jar"],
        "default": [],
    }),
)

java_library(
    name = "protobuf_for_tests",
    testonly = 1,
    exports = [":protobuf_internal"],
)

java_library(
    name = "protobuf",
    neverlink = 1,
    exports = [":protobuf_internal"],
)
