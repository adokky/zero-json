package dev.dokky.zerojson.framework

import dev.dokky.zerojson.TestZeroJson
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerializationException
import kotlinx.serialization.StringFormat
import kotlin.test.assertEquals
import kotlin.test.assertFails
import kotlin.test.assertFailsWith
import kotlin.test.fail

inline fun <reified T : Any> assertStringFormAndRestored(
    expected: String,
    original: T,
    serializer: KSerializer<T>,
    format: StringFormat = TestZeroJson,
    printResult: Boolean = false
) {
    val string = format.encodeToString(serializer, original)
    if (printResult) println("[Serialized form] $string")
    assertEquals(expected, string)
    val restored = format.decodeFromString(serializer, string)
    if (printResult) println("[Restored form] $restored")
    assertEquals(original, restored)
}

fun <T : Any> assertSerializedAndRestored(
    original: T,
    serializer: KSerializer<T>,
    format: StringFormat = TestZeroJson,
    printResult: Boolean = false
) {
    if (printResult) println("[Input] $original")
    val string = format.encodeToString(serializer, original)
    if (printResult) println("[Serialized form] $string")
    val restored = format.decodeFromString(serializer, string)
    if (printResult) println("[Restored form] $restored")
    assertEquals(original, restored)
}

fun assertFailsWith(
    exceptionName: String,
    unwrap: Boolean = false,
    block: () -> Unit
) {
    var exception: Throwable = assertFails(block)
    if (unwrap) {
        while (exception is AssertionError) {
            exception = exception.cause ?: break
        }
    }
    assertEquals(
        exceptionName,
        exception::class.simpleName,
        "Expected exception with type '${exceptionName}' but got '${exception::class.simpleName}'"
    )
}

fun assertFailsWithSerialMessage(
    exceptionName: String,
    message: String,
    assertionMessage: String? = null,
    block: () -> Unit
) {
    val exception = assertFailsWith(SerializationException::class, assertionMessage, block)
    assertEquals(
        exceptionName,
        exception::class.simpleName,
        "Expected exception type '$exceptionName' but actual is '${exception::class.simpleName}'"
    )
    if (message !in exception.message!!) {
        exception.printStackTrace()
        fail("expected:<$message> but was:<${exception.message}>")
    }
}

fun assertFailsWithSerialMessage(
    message: String,
    assertionMessage: String? = null,
    block: () -> Unit
) {
    assertFailsWithMessage<SerializationException>(message, assertionMessage, block)
}

inline fun <reified T : Throwable> assertFailsWithMessage(
    message: String,
    assertionMessage: String? = null,
    block: () -> Unit
) {
    val exception = assertFailsWith(T::class, assertionMessage, block)

    if (message !in exception.message!!) {
        exception.printStackTrace()
        fail("expected:<$message> but was:<${exception.message}>")
    }
}