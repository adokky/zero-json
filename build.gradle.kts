plugins {
    // failed to apply kotlin plugin in subprojects without applying it in root first
    kotlin("multiplatform") version libs.versions.kotlin apply false
    alias(libs.plugins.kotlinx.serialization) apply false
}

group = "io.github.adokky"
version = "0.1.1"

subprojects {
    group = rootProject.group
}

interface Injected {
    @get:Inject val fs: FileSystemOperations
}

// https://github.com/gradle/gradle/issues/15367
// https://github.com/apache/lucene-solr/pull/1767/files
allprojects {
    plugins.withType<JavaPlugin> {
        val tempDirs = ArrayList<Any>()

        val cleanTaskTmp by tasks.registering {
            val objects = project.objects
            val injected = objects.newInstance<Injected>()
            doLast {
                for (tempDir in tempDirs) {
                    injected.fs.delete {
                        val tempFiles = objects.fileTree()
                            .from(tempDir)
                            .matching { include("jar_extract*") }
                        delete(tempFiles)
                    }
                }
            }
        }

        tasks.withType<Test> {
            finalizedBy(cleanTaskTmp)
            @Suppress("UNNECESSARY_SAFE_CALL")
            temporaryDir?.let { tempDirs += it }
        }
    }

    version = rootProject.version
}