pluginManagement {
    repositories {
        gradlePluginPortal()
        mavenCentral()
    }
}

rootProject.name = "zero-json"

include(":zero-json-core")
project(":zero-json-core").projectDir = file("./core")

include(":zero-json-kotlinx")
project(":zero-json-kotlinx").projectDir = file("./kotlinx")

include(":benchmarks")
include(":testing")

// https://docs.gradle.org/8.11.1/userguide/configuration_cache.html#config_cache:stable
enableFeaturePreview("STABLE_CONFIGURATION_CACHE")