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
}

jmh {
    zip64 = true
    jmhVersion.set(libs.versions.jmh.get())
    forceGC = true
    failOnError = true // Should JMH fail immediately if any benchmark had experienced the unrecoverable error?
//    forceGC = false // Should JMH force GC between iterations?
    duplicateClassesStrategy = DuplicatesStrategy.EXCLUDE
}