package dev.dokky.zerojson

import kotlinx.serialization.Serializable
import org.junit.jupiter.api.Test
import org.springframework.core.ResolvableType
import org.springframework.core.io.buffer.DataBuffer
import org.springframework.core.io.buffer.DefaultDataBufferFactory
import org.springframework.http.MediaType
import org.springframework.http.ReactiveHttpInputMessage
import reactor.core.publisher.Flux
import java.nio.charset.StandardCharsets
import kotlin.test.assertEquals

class MessageReaderTest {
    @Serializable
    data class TestDto(val id: Int, val name: String)

    @Test
    fun `test read with valid json`() {
        val reader = ZeroJsonHttpMessageReader()
        val json = """{"id": 1, "name": "test"}"""
        val bytes = json.toByteArray(StandardCharsets.UTF_8)
        val buffer = DefaultDataBufferFactory().wrap(bytes)
        val type = ResolvableType.forClass(TestDto::class.java)
        val message = object : ReactiveHttpInputMessage {
            override fun getHeaders() = throw NotImplementedError()
            override fun getBody(): Flux<DataBuffer> = Flux.just(buffer)
        }

        val result = reader.readMono(type, message, emptyMap())
            .block()

        assertEquals(TestDto(1, "test"), result)
    }
    
    @Test
    fun `test canRead with serializable annotation`() {
        val reader = ZeroJsonHttpMessageReader()
        val type = ResolvableType.forClass(TestDto::class.java)
        val mediaType = MediaType.APPLICATION_JSON
        
        val canRead = reader.canRead(type, mediaType)
        assertEquals(true, canRead)
    }
    
    @Test
    fun `test canRead without serializable annotation`() {
        val reader = ZeroJsonHttpMessageReader()
        val type = ResolvableType.forClass(String::class.java)
        val mediaType = MediaType.APPLICATION_JSON
        
        val canRead = reader.canRead(type, mediaType)
        assertEquals(false, canRead)
    }
}