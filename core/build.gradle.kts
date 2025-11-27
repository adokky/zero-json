plugins {
    alias(libs.plugins.kotlinx.serialization)
    alias(libs.plugins.quick.mpp)
    alias(libs.plugins.quick.publish)
}

repositories {
    google()
}

mavenPublishing {
    pom {
        description = "Fast and powerful implementation of JSON format for kotlinx-serialization"
        inceptionYear = "2025"
    }
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
    commonMainCompileOnly(libs.kotlinx.serialization.json)

    // using compileOnly dependencies in these targets is not supported
    nativeMainApi(libs.kotlinx.serialization.json)
    jsCommonMainApi(libs.kotlinx.serialization.json)

    commonMainImplementation(libs.androidx.collection)
    commonMainImplementation(libs.kodec.struct)
    commonMainImplementation(libs.kodec.strings.stream)
    commonMainImplementation(libs.kodec.strings.common)
    commonMainImplementation(libs.kodec.strings.utf)
    commonMainImplementation(libs.bitvector)
    commonMainImplementation(libs.objectPool)
    commonMainImplementation(libs.karamelUtils.core)
    commonMainImplementation(libs.karamelUtils.tsbits)

    commonTestImplementation(project(":testing"))
}

mavenPublishing {
    coordinates(artifactId = "zero-json-core")
    pom {
        name = "zero-json-core"
        description = "Fast and powerful implementation of JSON format for kotlinx.serialization"
        inceptionYear = "2025"
    }
}

tasks.withType<Test> {
    // example:
    // ./gradlew :core:jvmTest -Ptest-mode=quick
    systemProperty("test-mode", properties["test-mode"] as? String ?: "default")
    systemProperty("zero-json-debug", properties["zero-json-debug"] as? String ?: "true")
    jvmArgs = listOf("-XX:+HeapDumpOnOutOfMemoryError")
}

val generateKarmaConfig by project.tasks.registering {
    group = "JS test setup"
    description = "Generates a Karma configuration that passes test arguments."

    val karmaConfigFile = layout.projectDirectory.file("karma.config.d/custom-args.js")
    outputs.file(karmaConfigFile)

    val testMode = properties["test-mode"] as? String ?: "default"

    doFirst {
        // language=javascript
        karmaConfigFile.asFile.writeText("""            
            // Passing test-mode parameter
            // https://karma-runner.github.io/5.0/config/configuration-file.html#clientargs
            config.set({
                client: {
                    args: ["--test_mode=$testMode"]
                }
            });
        """.trimIndent())
    }
}

tasks.withType<org.jetbrains.kotlin.gradle.targets.js.testing.KotlinJsTest> {
    dependsOn(generateKarmaConfig)
}

kover.reports {
    verify.rule {
        minBound(50)
    }
}