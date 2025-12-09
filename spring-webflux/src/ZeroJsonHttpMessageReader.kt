package dev.dokky.zerojson

import io.kodec.java.asBuffer
import kotlinx.serialization.serializer
import org.springframework.core.ResolvableType
import org.springframework.http.MediaType
import org.springframework.http.ReactiveHttpInputMessage
import org.springframework.http.codec.HttpMessageReader
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

open class ZeroJsonHttpMessageReader(
    private val json: ZeroJson = ZeroJson { ignoreUnknownKeys = true }
) : HttpMessageReader<Any> {
    override fun getReadableMediaTypes(): List<MediaType> =
        listOf(MediaType.APPLICATION_JSON)

    override fun canRead(elementType: ResolvableType, mediaType: MediaType?): Boolean {
        mediaType ?: return false
        if (!mediaType.includes(MediaType.APPLICATION_JSON)) return false
        val annotations = elementType.resolve()?.annotations ?: return false
        return annotations.any { it is kotlinx.serialization.Serializable }
    }

    override fun read(
        elementType: ResolvableType,
        message: ReactiveHttpInputMessage,
        hints: Map<String, Any>
    ): Flux<Any> {
        val serializer = serializer(elementType.type)
        return message.body.map { dataBuffer ->
            json.decode(serializer, dataBuffer.asByteBuffer().asBuffer())
        }
    }

    override fun readMono(
        elementType: ResolvableType,
        message: ReactiveHttpInputMessage,
        hints: Map<String, Any>
    ): Mono<Any> {
        return read(elementType, message, hints).single()
    }
}
