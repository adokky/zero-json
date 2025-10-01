package dev.dokky.zerojson.ktx

import dev.dokky.zerojson.ZeroJsonCompat
import kotlinx.serialization.EncodeDefault
import kotlinx.serialization.EncodeDefault.Mode.ALWAYS
import kotlinx.serialization.EncodeDefault.Mode.NEVER
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlin.test.Test
import kotlin.test.assertEquals

class SkipDefaultsTest {
    private val jsonDropDefaults = ZeroJsonCompat { encodeDefaults = false }
    private val jsonEncodeDefaults = ZeroJsonCompat { encodeDefaults = true }

    @Suppress("unused")
    @Serializable
    private data class Data(val bar: String, val foo: Int = 42) {
        var list: List<Int> = emptyList()
        val listWithSomething: List<Int> = listOf(1, 2, 3)
    }

    @Serializable
    private data class DifferentModes(
        val a: String = "a",
        @EncodeDefault val b: String = "b",
        @EncodeDefault(ALWAYS) val c: String = "c",
        @EncodeDefault(NEVER) val d: String = "d"
    )

    @Test
    fun serializeCorrectlyDefaults() {
        val d = Data("bar")
        assertEquals(
            """{"bar":"bar","foo":42,"list":[],"listWithSomething":[1,2,3]}""",
            jsonEncodeDefaults.encodeToString(Data.serializer(), d)
        )
    }

    @Test
    fun serializeCorrectly() {
        val d = Data("bar", 100).apply { list = listOf(1, 2, 3) }
        assertEquals(
            """{"bar":"bar","foo":100,"list":[1,2,3]}""",
            jsonDropDefaults.encodeToString(Data.serializer(), d)
        )
    }

    @Test
    fun serializeCorrectlyAndDropBody() {
        val d = Data("bar", 43)
        assertEquals("""{"bar":"bar","foo":43}""", jsonDropDefaults.encodeToString(Data.serializer(), d))
    }

    @Test
    fun serializeCorrectlyAndDropAll() {
        val d = Data("bar")
        assertEquals("""{"bar":"bar"}""", jsonDropDefaults.encodeToString(Data.serializer(), d))
    }

    @Test
    fun encodeDefaultsAnnotationWithFlag() {
        val data = DifferentModes()
        assertEquals("""{"a":"a","b":"b","c":"c"}""", jsonEncodeDefaults.encodeToString(data))
        assertEquals("""{"b":"b","c":"c"}""", jsonDropDefaults.encodeToString(data))
    }

}
