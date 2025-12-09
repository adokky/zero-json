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
        val dir = classesDir.orNull ?: return@CommandLineArgumentProvider emptyList()
        listOf("--patch-module", "kotlinx.serialization.json=${dir.asFile.absolutePath}")
    })
}

dependencies {
    commonMainApi(libs.kotlinx.serialization.core)
    commonMainImplementation(project(":zero-json-core")) {
        val c = libs.kotlinx.serialization.json.get()
        exclude(c.group, c.name)
    }
    jvmTestImplementation(libs.kotlinx.coroutines.jdk8)
}

configurations.all {
    val c = libs.kotlinx.serialization.json.get()
    outgoing {
        capability("io.github.adokky:zero-json-kotlinx:$version")
        capability("${c.group}:${c.name}:${c.version}")
    }
}