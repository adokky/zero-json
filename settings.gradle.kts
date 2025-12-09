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

include(":zero-json-spring-webflux")
project(":zero-json-spring-webflux").projectDir = file("./spring-webflux")

include(":benchmarks")
include(":testing")

// https://docs.gradle.org/8.11.1/userguide/configuration_cache.html#config_cache:stable
enableFeaturePreview("STABLE_CONFIGURATION_CACHE")