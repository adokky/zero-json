plugins {
    kotlin("jvm")
    alias(libs.plugins.kotlinx.serialization)
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
    jmh(libs.karamelUtils.core)
}

jmh {
    jmhVersion.set(libs.versions.jmh.get())
    zip64 = true
    forceGC = true
    failOnError = true
    duplicateClassesStrategy = DuplicatesStrategy.EXCLUDE
//    profilers.add("gc")
}

kotlin {
    sourceSets.configureEach {
        languageSettings {
            optIn("kotlinx.serialization.InternalSerializationApi")
            optIn("kotlinx.serialization.SealedSerializationApi")
            optIn("kotlinx.serialization.ExperimentalSerializationApi")
        }
    }
}