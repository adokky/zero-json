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

    js().browser {
        testTask {
            useMocha {
                timeout = (60 * 60 * 1000).toString()
            }
        }
    }

    @Suppress("OPT_IN_USAGE")
    wasmJs().browser {
        testTask {
            useKarma {
                useFirefox()
                useConfigDirectory(rootDir.resolve("karma.config.d"))
            }
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
    jvmMainImplementation(libs.kodec.javaIo)
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

kover.reports {
    verify.rule {
        minBound(50)
    }
}