plugins {
    alias(libs.plugins.quick.mpp)
    alias(libs.plugins.kotlinx.serialization)
}

repositories {
    google()
}

kotlin {
    compilerOptions {
        allWarningsAsErrors = false
    }

    sourceSets.configureEach {
        languageSettings {
            optIn("kotlinx.serialization.InternalSerializationApi")
            optIn("kotlinx.serialization.SealedSerializationApi")
            optIn("kotlinx.serialization.ExperimentalSerializationApi")
        }
    }
}

dependencies {
    commonMainImplementation(libs.kodec.strings.stream)
    commonMainImplementation(libs.kodec.struct)
    commonMainImplementation(libs.karamelUtils.core)
    commonMainImplementation(libs.karamelUtils.tsbits)
    commonMainImplementation(libs.androidx.collection)

    commonMainApi(libs.equalsTester)
    commonMainApi(libs.kotlinx.serialization.json)
    commonMainApi(project(":zero-json-core"))
    commonMainApi(kotlin("test"))
}

fun Project.getTestMode(): String = properties["test-mode"] as? String ?: "default"

tasks.withType<Test> {
    // example:
    // ./gradlew :testing:jvmTest -Ptest-mode=quick
    systemProperty("test-mode", project.getTestMode())
    jvmArgs = listOf("-XX:+HeapDumpOnOutOfMemoryError")
}