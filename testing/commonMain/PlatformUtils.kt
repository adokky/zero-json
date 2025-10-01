package dev.dokky.zerojson.framework

import kotlinx.serialization.SerializationException
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

enum class Platform {
    JVM, JS, NATIVE, WASM
}

expect val currentPlatform: Platform

fun isJs(): Boolean = currentPlatform == Platform.JS
fun isJvm(): Boolean = currentPlatform == Platform.JVM
fun isNative(): Boolean = currentPlatform == Platform.NATIVE
fun isWasm(): Boolean = currentPlatform == Platform.WASM

fun SerialDescriptor.assertDescriptorEqualsTo(other: SerialDescriptor) {
    assertEquals(serialName, other.serialName)
    assertEquals(elementsCount, other.elementsCount)
    assertEquals(isNullable, other.isNullable)
    assertEquals(annotations, other.annotations)
    assertEquals(kind, other.kind)
    for (i in 0 until elementsCount) {
        getElementDescriptor(i).assertDescriptorEqualsTo(other.getElementDescriptor(i))
        val name = getElementName(i)
        val otherName = other.getElementName(i)
        assertEquals(name, otherName)
        assertEquals(getElementAnnotations(i), other.getElementAnnotations(i))
        assertEquals(name, otherName)
        assertEquals(isElementOptional(i), other.isElementOptional(i))
    }
}

inline fun noJs(test: () -> Unit) {
    if (!isJs()) test()
}

inline fun jvmOnly(test: () -> Unit) {
    if (isJvm()) test()
}

inline fun assertFailsWithMissingField(block: () -> Unit) {
    val e = assertFailsWith<SerializationException>(block = block)
    assertTrue(e.message?.contains("but it was missing") ?: false)
}