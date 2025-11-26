import org.jetbrains.kotlin.gradle.dsl.ExplicitApiMode

plugins {
    alias(libs.plugins.quick.mpp)
    alias(libs.plugins.kotlinx.serialization)
    alias(libs.plugins.quick.publish)
}

repositories {
    google()
}

mavenPublishing {
    pom {
        description = "Drop-in replacement of kotlinx-serialization-json with zero-copy capabilities and better performance"
        inceptionYear = "2025"
    }
}

kotlin {
    explicitApi = ExplicitApiMode.Strict
    sourceSets.configureEach {
        languageSettings {
            optIn("kotlinx.serialization.InternalSerializationApi")
            optIn("kotlinx.serialization.SealedSerializationApi")
            optIn("kotlinx.serialization.ExperimentalSerializationApi")
            optIn("kotlinx.serialization.internal.CoreFriendModuleApi")
        }
    }
    compilerOptions {
        freeCompilerArgs.add("-Xwarning-level=REDUNDANT_VISIBILITY_MODIFIER:disabled")
    }
}

tasks.withType<Test> {
    // example:
    // ./gradlew :kotlinx:test -Pzero-json-debug=false
    systemProperty("zero-json-debug", properties["zero-json-debug"] as? String ?: "true")
}

tasks.withType<JavaCompile>().configureEach {
    val classesDir = kotlin.sourceSets.getByName("jvmMain").kotlin.classesDirectory
    options.compilerArgumentProviders.add(CommandLineArgumentProvider {
        listOf("--patch-module", "kotlinx.serialization.json=${classesDir.get().asFile.absolutePath}")
    })
}

dependencies {
    commonMainImplementation(project(":zero-json-core"))
    commonMainApi(libs.kotlinx.serialization.core)
}

configurations.all {
    outgoing {
        capability("io.github.adokky:zero-json-kotlinx:$version")
        libs.kotlinx.serialization.json.get().also { c ->
            capability("${c.group}:${c.name}:${c.version}")
        }
    }
}