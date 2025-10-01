@file:JvmName("ZeroJsonExceptionsKt")

package dev.dokky.zerojson

import karamel.utils.appendTrimmed
import kotlinx.serialization.SerializationException
import kotlin.jvm.JvmName

expect open class NoStackTraceSerializationException(
    message: String? = null,
    cause: Throwable? = null
): SerializationException

interface DecodingException {
    val message: String?
    val path: String?
}

open class ZeroJsonDecodingException(
    message: String,
    position: Int = -1,
    path: String? = null,
    cause: Throwable? = null
): NoStackTraceSerializationException(message, cause), DecodingException {
    var position: Int = position
        internal set
    override var path: String? = path
        internal set

    override val message: String get() {
        val original = super.message ?: ""
        if (path == null && position < 0) return original
        return buildString(original.length) {
            path?.let {
                append("path=")
                append(it)
            }
            if (position >= 0) {
                if (isNotEmpty()) append(", ")
                append("position=")
                append(position)
            }
            if (isNotEmpty()) append(": ")
            appendTrimmed(original)
        }
    }
}

class JsonNumberIsOutOfRange(
    min: Number,
    max: Number,
    position: Int = -1,
    path: String? = null
): ZeroJsonDecodingException("number is out of range $min..$max", position, path)

class JsonMaxDepthReachedException():
    ZeroJsonDecodingException("JSON structure nesting limit reached", -1)