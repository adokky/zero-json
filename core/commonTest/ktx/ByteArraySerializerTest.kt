package dev.dokky.zerojson.ktx

import dev.dokky.zerojson.Id
import dev.dokky.zerojson.TestZeroJson
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ByteArraySerializer
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ByteArraySerializerTest {
    @Serializable
    class ByteArrayCarrier(@Id(2) val data: ByteArray) {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other == null || this::class != other::class) return false

            other as ByteArrayCarrier

            return data.contentEquals(other.data)
        }
        override fun hashCode(): Int = data.contentHashCode()
        override fun toString(): String = "ByteArrayCarrier(data=${data.contentToString()})"
    }

    @Test
    fun testByteArrayJson() {
        val bytes = byteArrayOf(42, 43, 44, 45)
        val s = TestZeroJson.encodeToString(ByteArraySerializer(), bytes)
        assertEquals(s, """[42,43,44,45]""")
        val bytes2 = TestZeroJson.decodeFromString(ByteArraySerializer(), s)
        assertTrue(bytes.contentEquals(bytes2))
    }

    @Test
    fun testWrappedByteArrayJson() {
        val obj = ByteArrayCarrier(byteArrayOf(42, 100))
        val s = TestZeroJson.encodeToString(ByteArrayCarrier.serializer(), obj)
        assertEquals("""{"data":[42,100]}""", s)
        val obj2 = TestZeroJson.decodeFromString(ByteArrayCarrier.serializer(), s)
        assertEquals(obj, obj2)
    }
}
