/*
 * Copyright 2017-2022 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package kotlinx.serialization.test

import kotlinx.serialization.SerializationException
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlin.random.Random
import kotlin.random.nextInt
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

fun toHexChar(i: Int) : Char {
    val d = i and 0xf
    return if (d < 10) (d + '0'.code).toChar()
    else (d - 10 + 'a'.code).toChar()
}

val ESCAPE_STRINGS: Array<String?> = arrayOfNulls<String>(93).apply {
    for (c in 0..0x1f) {
        val c1 = toHexChar(c shr 12)
        val c2 = toHexChar(c shr 8)
        val c3 = toHexChar(c shr 4)
        val c4 = toHexChar(c)
        this[c] = "\\u$c1$c2$c3$c4"
    }
    this['"'.code] = "\\\""
    this['\\'.code] = "\\\\"
    this['\t'.code] = "\\t"
    this['\b'.code] = "\\b"
    this['\n'.code] = "\\n"
    this['\r'.code] = "\\r"
    this[0x0c] = "\\f"
}

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

fun generateRandomUnicodeString(size: Int): String {
    return buildString(size) {
        repeat(size) {
            val pickEscape = Random.nextBoolean()
            if (pickEscape) {
                // Definitely an escape symbol
                append(ESCAPE_STRINGS.random().takeIf { it != null } ?: 'N')
            } else {
                // Any symbol, including escaping one
                append(Char(Random.nextInt(Char.MIN_VALUE.code..Char.MAX_VALUE.code)).takeIf { it.isDefined() && !it.isSurrogate()} ?: 'U')
            }
        }
    }
}
