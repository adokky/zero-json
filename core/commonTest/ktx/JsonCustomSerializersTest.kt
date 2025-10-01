@file:UseContextualSerialization(JsonCustomSerializersTest.B::class)
@file:Suppress("OPT_IN_USAGE")

package dev.dokky.zerojson.ktx

import dev.dokky.zerojson.Id
import dev.dokky.zerojson.ZeroJson
import kotlinx.serialization.*
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.descriptors.*
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.modules.serializersModuleOf
import kotlin.test.Test
import kotlin.test.assertEquals

class JsonCustomSerializersTest : JsonTestBase() {

    @Serializable
    private data class A(@Id(1) val b: B)

    data class B(@Id(1) val value: Int)

    private object BSerializer : KSerializer<B> {
        override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("B", PrimitiveKind.INT)
        override fun serialize(encoder: Encoder, value: B) = encoder.encodeInt(value.value)
        override fun deserialize(decoder: Decoder): B = B(decoder.decodeInt())
    }

    @Serializable
    private data class BList(@Id(1) val bs: List<B>)

    @Serializable(C.Companion::class)
    private data class C(@Id(1) val a: Int = 31, @Id(2) val b: Int = 42) {
        @Serializer(forClass = C::class)
        companion object : KSerializer<C> {
            override val descriptor: SerialDescriptor = buildClassSerialDescriptor("C") {
                element<Int>("a", annotations = listOf(Id(1)), isOptional = true)
                element<Int>("b", annotations = listOf(Id(2)), isOptional = true)
            }

            override fun serialize(encoder: Encoder, value: C) {
                val elemOutput = encoder.beginStructure(descriptor)
                elemOutput.encodeIntElement(descriptor, 1, value.b)
                if (value.a != 31) elemOutput.encodeIntElement(descriptor, 0, value.a)
                elemOutput.endStructure(descriptor)
            }
        }
    }

    @Serializable
    private data class CList1(@Id(1) val c: List<C>)

    @Serializable(CList2.Companion::class)
    private data class CList2(@Id(1) val d: Int = 5, @Id(2) val c: List<C>) {
        @Serializer(forClass = CList2::class)
        companion object : KSerializer<CList2> {
            override val descriptor: SerialDescriptor = buildClassSerialDescriptor("CList2") {
                element<Int>("d", annotations = listOf(Id(1)), isOptional = true)
                element<List<C>>("c", annotations = listOf(Id(2)))
            }

            override fun serialize(encoder: Encoder, value: CList2) {
                val elemOutput = encoder.beginStructure(descriptor)
                elemOutput.encodeSerializableElement(descriptor, 1, ListSerializer(C), value.c)
                if (value.d != 5) elemOutput.encodeIntElement(descriptor, 0, value.d)
                elemOutput.endStructure(descriptor)
            }
        }
    }

    @Serializable(CList3.Companion::class)
    private data class CList3(@Id(1) val e: List<C> = emptyList(), @Id(2) val f: Int) {
        @Serializer(forClass = CList3::class)
        companion object : KSerializer<CList3> {
            override val descriptor: SerialDescriptor = buildClassSerialDescriptor("CList3") {
                element<List<C>>("e", annotations = listOf(Id(1)), isOptional = true)
                element<Int>("f", annotations = listOf(Id(2)))
            }

            override fun serialize(encoder: Encoder, value: CList3) {
                val elemOutput = encoder.beginStructure(descriptor)
                if (value.e.isNotEmpty()) elemOutput.encodeSerializableElement(descriptor, 0, ListSerializer(C), value.e)
                elemOutput.encodeIntElement(descriptor, 1, value.f)
                elemOutput.endStructure(descriptor)
            }
        }
    }

    @Serializable(CList4.Companion::class)
    private data class CList4(@Id(1) val g: List<C> = emptyList(), @Id(2) val h: Int) {
        @Serializer(forClass = CList4::class)
        companion object : KSerializer<CList4> {
            override val descriptor: SerialDescriptor = buildClassSerialDescriptor("CList4") {
                element<List<C>>("g", annotations = listOf(Id(1)), isOptional = true)
                element<Int>("h", annotations = listOf(Id(2)))
            }

            override fun serialize(encoder: Encoder, value: CList4) {
                val elemOutput = encoder.beginStructure(descriptor)
                elemOutput.encodeIntElement(descriptor, 1, value.h)
                if (value.g.isNotEmpty()) elemOutput.encodeSerializableElement(descriptor, 0, ListSerializer(C), value.g)
                elemOutput.endStructure(descriptor)
            }
        }
    }

    @Serializable(CList5.Companion::class)
    private data class CList5(@Id(1) val g: List<Int> = emptyList(), @Id(2) val h: Int) {
        @Serializer(forClass = CList5::class)
        companion object : KSerializer<CList5> {
            override val descriptor: SerialDescriptor = buildClassSerialDescriptor("CList4") {
                element<List<Int>>("g", annotations = listOf(Id(1)), isOptional = true)
                element<Int>("h", annotations = listOf(Id(2)))
            }

            override fun serialize(encoder: Encoder, value: CList5) {
                val elemOutput = encoder.beginStructure(descriptor)
                elemOutput.encodeIntElement(descriptor, 1, value.h)
                if (value.g.isNotEmpty()) elemOutput.encodeSerializableElement(
                    descriptor, 0, ListSerializer(Int.serializer()),
                    value.g
                )
                elemOutput.endStructure(descriptor)
            }
        }
    }

    private val moduleWithB = serializersModuleOf(B::class, BSerializer)

    private fun createJsonWithB() = ZeroJson { serializersModule = moduleWithB }

    @Test
    fun testWriteCustom() = parametrizedTest {
        val a = A(B(2))
        val j = createJsonWithB()
        val s = j.encodeToStringTest(a)
        assertEquals("""{"b":2}""", s)
    }

    @Test
    fun testReadCustom() = parametrizedTest {
        val a = A(B(2))
        val j = createJsonWithB()
        val s = j.decodeFromString<A>("{b:2}")
        assertEquals(a, s)
    }

    @Test
    fun testWriteCustomList() = parametrizedTest {
        val obj = BList(listOf(B(1), B(2), B(3)))
        val j = createJsonWithB()
        val s = j.encodeToStringTest(obj)
        assertEquals("""{"bs":[1,2,3]}""", s)
    }

    @Test
    fun testReadCustomList() = parametrizedTest {
        val obj = BList(listOf(B(1), B(2), B(3)))
        val j = createJsonWithB()
        val bs = j.decodeFromString<BList>("{bs:[1,2,3]}")
        assertEquals(obj, bs)
    }

    @Test
    fun testWriteCustomListRootLevel() = parametrizedTest {
        val obj = listOf(B(1), B(2), B(3))
        val j = createJsonWithB()
        val s = j.encodeToStringTest(ListSerializer(BSerializer), obj)
        assertEquals("[1,2,3]", s)
    }

    @Test
    fun testReadCustomListRootLevel() = parametrizedTest {
        val obj = listOf(B(1), B(2), B(3))
        val j = createJsonWithB()
        val bs = j.decodeFromStringTest(ListSerializer(BSerializer), "[1,2,3]")
        assertEquals(obj, bs)
    }

    @Test
    fun testWriteCustomInvertedOrder() = parametrizedTest {
        val obj = C(1, 2)
        val s = lenient.encodeToStringTest(obj)
        assertEquals("""{"b":2,"a":1}""", s)
    }

    @Test
    fun testWriteCustomOmitDefault() = parametrizedTest {
        val obj = C(b = 2)
        val s = lenient.encodeToStringTest(obj)
        assertEquals("""{"b":2}""", s)
    }

    @Test
    fun testReadCustomInvertedOrder() = parametrizedTest {
        val obj = C(1, 2)
        val s = lenient.decodeFromString<C>("""{"b":2,"a":1}""")
        assertEquals(obj, s)
    }

    @Test
    fun testReadCustomOmitDefault() = parametrizedTest {
        val obj = C(b = 2)
        val s = lenient.decodeFromString<C>("""{"b":2}""")
        assertEquals(obj, s)
    }

    @Test
    fun testWriteListOfOptional() = parametrizedTest {
        val obj = listOf(C(a = 1), C(b = 2), C(3, 4))
        val s = lenient.encodeToStringTest(ListSerializer(C), obj)
        assertEquals("""[{"b":42,"a":1},{"b":2},{"b":4,"a":3}]""", s)
    }

    @Test
    fun testReadListOfOptional() = parametrizedTest {
        val obj = listOf(C(a = 1), C(b = 2), C(3, 4))
        val j = """[{"b":42,"a":1},{"b":2},{"b":4,"a":3}]"""
        val s = lenient.decodeFromString(ListSerializer<C>(C), j)
        assertEquals(obj, s)
    }

    @Test
    fun testWriteOptionalList1() = parametrizedTest {
        val obj = CList1(listOf(C(a = 1), C(b = 2), C(3, 4)))
        val s = lenient.encodeToStringTest(obj)
        assertEquals("""{"c":[{"b":42,"a":1},{"b":2},{"b":4,"a":3}]}""", s)
    }

    @Test
    fun testWriteOptionalList1Quoted() = parametrizedTest {
        val obj = CList1(listOf(C(a = 1), C(b = 2), C(3, 4)))
        val s = lenient.encodeToStringTest(obj)
        assertEquals("""{"c":[{"b":42,"a":1},{"b":2},{"b":4,"a":3}]}""", s)
    }

    @Test
    fun testReadOptionalList1() = parametrizedTest {
        val obj = CList1(listOf(C(a = 1), C(b = 2), C(3, 4)))
        val j = """{"c":[{"b":42,"a":1},{"b":2},{"b":4,"a":3}]}"""
        assertEquals(obj, lenient.decodeFromString(j))
    }

    @Test
    fun testWriteOptionalList2a() = parametrizedTest {
        val obj = CList2(7, listOf(C(a = 5), C(b = 6), C(7, 8)))
        val s = lenient.encodeToStringTest(obj)
        assertEquals("""{"c":[{"b":42,"a":5},{"b":6},{"b":8,"a":7}],"d":7}""", s)
    }

    @Test
    fun testReadOptionalList2a() = parametrizedTest {
        val obj = CList2(7, listOf(C(a = 5), C(b = 6), C(7, 8)))
        val j = """{"c":[{"b":42,"a":5},{"b":6},{"b":8,"a":7}],"d":7}"""
        assertEquals(obj, lenient.decodeFromString(j))
    }

    @Test
    fun testWriteOptionalList2b() = parametrizedTest {
        val obj = CList2(c = listOf(C(a = 5), C(b = 6), C(7, 8)))
        val s = lenient.encodeToStringTest(obj)
        assertEquals("""{"c":[{"b":42,"a":5},{"b":6},{"b":8,"a":7}]}""", s)
    }

    @Test
    fun testReadOptionalList2b() = parametrizedTest {
        val obj = CList2(c = listOf(C(a = 5), C(b = 6), C(7, 8)))
        val j = """{"c":[{"b":42,"a":5},{"b":6},{"b":8,"a":7}]}"""
        assertEquals(obj, lenient.decodeFromString(j))
    }

    @Test
    fun testWriteOptionalList3a() = parametrizedTest {
        val obj = CList3(listOf(C(a = 1), C(b = 2), C(3, 4)), 99)
        val s = lenient.encodeToStringTest(obj)
        assertEquals("""{"e":[{"b":42,"a":1},{"b":2},{"b":4,"a":3}],"f":99}""", s)
    }

    @Test
    fun testReadOptionalList3a() = parametrizedTest {
        val obj = CList3(listOf(C(a = 1), C(b = 2), C(3, 4)), 99)
        val j = """{"e":[{"b":42,"a":1},{"b":2},{"b":4,"a":3}],"f":99}"""
        assertEquals(obj, lenient.decodeFromString(j))
    }

    @Test
    fun testWriteOptionalList3b() = parametrizedTest {
        val obj = CList3(f = 99)
        val s = lenient.encodeToStringTest(obj)
        assertEquals("""{"f":99}""", s)
    }

    @Test
    fun testReadOptionalList3b() = parametrizedTest {
        val obj = CList3(f = 99)
        val j = """{"f":99}"""
        assertEquals(obj, lenient.decodeFromString(j))
    }

    @Test
    fun testWriteOptionalList4a() = parametrizedTest {
        val obj = CList4(listOf(C(a = 1), C(b = 2), C(3, 4)), 54)
        val s = lenient.encodeToStringTest(obj)
        assertEquals("""{"h":54,"g":[{"b":42,"a":1},{"b":2},{"b":4,"a":3}]}""", s)
    }

    @Test
    fun testReadOptionalList4a() = parametrizedTest {
        val obj = CList4(listOf(C(a = 1), C(b = 2), C(3, 4)), 54)
        val j = """{"h":54,"g":[{"b":42,"a":1},{"b":2},{"b":4,"a":3}]}"""
        assertEquals(obj, lenient.decodeFromString(j))
    }

    @Test
    fun testWriteOptionalList4b() = parametrizedTest {
        val obj = CList4(h = 97)
        val j = """{"h":97}"""
        val s = lenient.encodeToStringTest(obj)
        assertEquals(j, s)
    }

    @Test
    fun testReadOptionalList4b() = parametrizedTest {
        val obj = CList4(h = 97)
        val j = """{"h":97}"""
        assertEquals(obj, lenient.decodeFromString(j))
    }

    @Test
    fun testWriteOptionalList5a() = parametrizedTest {
        val obj = CList5(listOf(9, 8, 7, 6, 5), 5)
        val s = lenient.encodeToStringTest(obj)
        assertEquals("""{"h":5,"g":[9,8,7,6,5]}""", s)
    }

    @Test
    fun testReadOptionalList5a() = parametrizedTest {
        val obj = CList5(listOf(9, 8, 7, 6, 5), 5)
        val j = """{"h":5,"g":[9,8,7,6,5]}"""
        assertEquals(obj, lenient.decodeFromString(j))
    }

    @Test
    fun testWriteOptionalList5b() = parametrizedTest {
        val obj = CList5(h = 999)
        val s = lenient.encodeToStringTest(obj)
        assertEquals("""{"h":999}""", s)
    }

    @Test
    fun testReadOptionalList5b() = parametrizedTest {
        val obj = CList5(h = 999)
        val j = """{"h":999}"""
        assertEquals(obj, lenient.decodeFromString(j))
    }

    @Test
    fun testMapBuiltinsTest() = parametrizedTest {
        val map = mapOf(1 to "1", 2 to "2")
        val serial = MapSerializer(Int.serializer(), String.serializer())
        val s = lenient.encodeToStringTest(serial, map)
        assertEquals("""{"1":"1","2":"2"}""", s)
    }

    @Test
    fun testResolveAtRootLevel() = parametrizedTest {
        val j = createJsonWithB()
        val bs = j.decodeFromString<B>("1")
        assertEquals(B(1), bs)
    }
}
