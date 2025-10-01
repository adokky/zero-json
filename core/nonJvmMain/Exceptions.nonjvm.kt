package dev.dokky.zerojson

import kotlinx.serialization.SerializationException

actual open class NoStackTraceSerializationException
actual constructor(message: String?, cause: Throwable?):
    SerializationException(message, cause)
