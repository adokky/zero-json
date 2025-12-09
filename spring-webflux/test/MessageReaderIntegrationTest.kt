package dev.dokky.zerojson

import kotlinx.serialization.Serializable
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.verify
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.boot.webtestclient.autoconfigure.AutoConfigureWebTestClient
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Import
import org.springframework.http.MediaType
import org.springframework.http.codec.ServerCodecConfigurer
import org.springframework.test.context.TestConstructor
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean
import org.springframework.test.web.reactive.server.WebTestClient
import org.springframework.test.web.reactive.server.expectBody
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.reactive.config.WebFluxConfigurer
import reactor.core.publisher.Mono

@SpringBootApplication
class TestApplication

@TestConfiguration
open class TestConfig {
    @Bean
    open fun messageReader() = ZeroJsonHttpMessageReader()

    @Bean
    open fun webFluxConfigurer(reader: ZeroJsonHttpMessageReader) = object : WebFluxConfigurer {
        override fun configureHttpMessageCodecs(configurer: ServerCodecConfigurer) {
            configurer.defaultCodecs().maxInMemorySize(256 * 1024)
            configurer.customCodecs().register(reader)
        }
    }
}

@Serializable
data class TestDto(val id: Int, val name: String)

@RestController
class TestController {
    @PostMapping("/test")
    fun handle(@RequestBody dto: TestDto) = Mono.just(dto)
}

@SpringBootTest(classes = [TestApplication::class])
@Import(TestConfig::class)
@TestConstructor(autowireMode = TestConstructor.AutowireMode.ALL)
@AutoConfigureWebTestClient
@MockitoSpyBean(types = [ZeroJsonHttpMessageReader::class])
class ZeroJsonHttpMessageReaderIntegrationTest(
    val client: WebTestClient,
    val messageReader: ZeroJsonHttpMessageReader
) {
    @Test
    fun `test POST with ZeroJsonHttpMessageReader`() {
        val testDto = TestDto(1, "test")

        client.post()
            .uri("/test")
            .contentType(MediaType.APPLICATION_JSON)
            .body(Mono.just(testDto), TestDto::class.java)
            .exchange()
            .expectStatus().isOk
            .expectBody<TestDto>()
            .isEqualTo(testDto)

        verify(messageReader).readMono(any(), any(), any())
    }
}

