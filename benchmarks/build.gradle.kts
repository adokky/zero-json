plugins {
    kotlin("jvm")
    alias(libs.plugins.kotlinx.serialization)
    java
    application
    alias(libs.plugins.jmh)
}

repositories {
    mavenCentral()
    google()
}

dependencies {
    jmh(libs.jmh.processor)
    jmh(project(":zero-json-core"))
    jmh(libs.kotlinx.serialization.json)
    jmh(libs.kodec.buffers.core)
}

jmh {
    jmhVersion.set(libs.versions.jmh.get())
    zip64 = true
    forceGC = true
    failOnError = true
    duplicateClassesStrategy = DuplicatesStrategy.EXCLUDE
}