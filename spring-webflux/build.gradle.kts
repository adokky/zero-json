plugins {
    alias(libs.plugins.quick.jvm)
    alias(libs.plugins.quick.publish)
    alias(libs.plugins.kotlinx.serialization)
}

mavenPublishing {
    pom {
        description = "Spring WebFlux integration for zero-json"
        inceptionYear = "2025"
    }
}

repositories {
    google()
}

dependencies {
    api(project(":zero-json-core"))
    implementation(libs.kodec.javaIo)
    implementation(libs.kotlinx.serialization.json)
    implementation("org.springframework:spring-core:5.3.39")
    implementation("org.springframework:spring-web:5.3.39")
    implementation("io.projectreactor:reactor-core:3.8.0")

    testImplementation("org.springframework.boot:spring-boot-starter-webflux:4.0.0")
    testImplementation("org.springframework.boot:spring-boot-webtestclient:4.0.0")
    testImplementation("org.springframework.boot:spring-boot-starter-test:4.0.0")
    testImplementation("io.projectreactor:reactor-test:3.8.0")
    testImplementation("org.mockito:mockito-core:5.20.0")
    testImplementation("org.mockito.kotlin:mockito-kotlin:6.1.0")
    testImplementation("org.jetbrains.kotlin:kotlin-reflect")
    testImplementation(libs.kotlinx.coroutines.jdk8)
    testImplementation(libs.kotlinx.coroutines.reactor)
}