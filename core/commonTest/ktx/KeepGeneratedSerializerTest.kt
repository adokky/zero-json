@file:Suppress("OPT_IN_USAGE")

package dev.dokky.zerojson.ktx

import dev.dokky.zerojson.TestZeroJson
import kotlinx.serialization.*
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlin.jvm.JvmInline
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertSame
import kotlin.test.assertTrue

class KeepGeneratedSerializerTest {
    @Serializable(with = ValueSerializer::class)
    @KeepGeneratedSerializer
    @JvmInline
    private value class Value(val i: Int)

    private object ValueSerializer: KSerializer<Value> {
        override val descriptor = PrimitiveSerialDescriptor("ValueSerializer", PrimitiveKind.INT)
        override fun deserialize(decoder: Decoder): Value {
            val value = decoder.decodeInt()
            return Value(value - 42)
        }
        override fun serialize(encoder: Encoder, value: Value) {
            encoder.encodeInt(value.i + 42)
        }
    }

    @Test
    fun testValueClass() {
        test(Value(1), "43", "1", Value.serializer(), Value.generatedSerializer())
    }

    @Serializable(with = DataSerializer::class)
    @KeepGeneratedSerializer
    private data class Data(val i: Int)

    private object DataSerializer: KSerializer<Data> {
        override val descriptor = PrimitiveSerialDescriptor("DataSerializer", PrimitiveKind.INT)
        override fun deserialize(decoder: Decoder): Data = Data(decoder.decodeInt())
        override fun serialize(encoder: Encoder, value: Data) { encoder.encodeInt(value.i) }
    }

    @Test
    fun testDataClass() {
        test(Data(2), "2", "{\"i\":2}", Data.serializer(), Data.generatedSerializer())
    }

    @Serializable(with = ParentSerializer::class)
    @KeepGeneratedSerializer
    private open class Parent(val p: Int) {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is Parent) return false
            if (p != other.p) return false
            return true
        }

        override fun hashCode(): Int = p
    }

    private object ParentSerializer: KSerializer<Parent> {
        override val descriptor = PrimitiveSerialDescriptor("ParentSerializer", PrimitiveKind.INT)
        override fun deserialize(decoder: Decoder): Parent = Parent(decoder.decodeInt() - 1)
        override fun serialize(encoder: Encoder, value: Parent) { encoder.encodeInt(value.p + 1) }
    }

    @Serializable
    private data class Child(val c: Int): Parent(0)

    @Serializable(with = ChildSerializer::class)
    @KeepGeneratedSerializer
    private data class ChildWithCustom(val c: Int): Parent(0)

    private object ChildSerializer: KSerializer<ChildWithCustom> {
        override val descriptor = PrimitiveSerialDescriptor("ChildSerializer", PrimitiveKind.INT)
        override fun deserialize(decoder: Decoder): ChildWithCustom {
            val value = decoder.decodeInt()
            return ChildWithCustom(value - 2)
        }

        override fun serialize(encoder: Encoder, value: ChildWithCustom) {
            encoder.encodeInt(value.c + 2)
        }
    }

    @Test
    fun testInheritance() {
        test(Parent(3), "4", "{\"p\":3}", Parent.serializer(), Parent.generatedSerializer())
        test(Child(4), "{\"p\":0,\"c\":4}", "", Child.serializer(), null)
        test(ChildWithCustom(5), "7", "{\"p\":0,\"c\":5}", ChildWithCustom.serializer(), ChildWithCustom.generatedSerializer())
    }


    @Suppress("unused")
    @Serializable(with = MyEnumSerializer::class)
    @KeepGeneratedSerializer
    private enum class MyEnum {
        A,
        B,
        FALLBACK
    }

    @Suppress("unused")
    @Serializable
    private data class EnumHolder(val e: MyEnum)

    private object MyEnumSerializer: KSerializer<MyEnum> {
        val defaultSerializer = MyEnum.generatedSerializer()

        override val descriptor = PrimitiveSerialDescriptor("MyEnumSerializer", PrimitiveKind.INT)

        override fun deserialize(decoder: Decoder): MyEnum {
            decoder.decodeString()
            return MyEnum.A
        }

        override fun serialize(encoder: Encoder, value: MyEnum) {
            // always encode FALLBACK entry by generated serializer
            defaultSerializer.serialize(encoder, MyEnum.FALLBACK)
        }
    }

    @Test
    fun testEnum() {
        test(MyEnum.A, "\"FALLBACK\"", "\"A\"", MyEnum.serializer(), MyEnum.generatedSerializer())
        assertTrue(serializer<MyEnum>() is MyEnumSerializer, "serializer<MyEnum> illegal = " + serializer<MyEnum>())
        assertTrue(MyEnum.serializer() is MyEnumSerializer, "MyEnum.serializer() illegal = " + MyEnum.serializer())
        assertEquals("kotlinx.serialization.internal.EnumSerializer<dev.dokky.zerojson.ktx.KeepGeneratedSerializerTest.MyEnum>", MyEnum.generatedSerializer().toString(), "MyEnum.generatedSerializer() illegal")
        assertSame(MyEnum.generatedSerializer(), MyEnum.generatedSerializer(), "MyEnum.generatedSerializer() instance differs")
    }


    @Serializable(with = ParametrizedSerializer::class)
    @KeepGeneratedSerializer
    private data class ParametrizedData<T>(val t: T)

    private class ParametrizedSerializer(val serializer: KSerializer<Any>): KSerializer<ParametrizedData<Any>> {
        override val descriptor = PrimitiveSerialDescriptor("ParametrizedSerializer", PrimitiveKind.INT)

        override fun deserialize(decoder: Decoder): ParametrizedData<Any> {
            val value = serializer.deserialize(decoder)
            return ParametrizedData(value)
        }

        override fun serialize(encoder: Encoder, value: ParametrizedData<Any>) {
            serializer.serialize(encoder, value.t)
        }
    }

    @Test
    fun testParametrized() {
        test(
            ParametrizedData<Data>(Data(6)), "6", "{\"t\":6}", ParametrizedData.serializer(Data.serializer()), ParametrizedData.generatedSerializer(
                Data.serializer()))
    }


    @Serializable(WithCompanion.Companion::class)
    @KeepGeneratedSerializer
    private data class WithCompanion(val value: Int) {
        @Suppress("EXTERNAL_SERIALIZER_USELESS")
        @Serializer(WithCompanion::class)
        companion object {
            override val descriptor = PrimitiveSerialDescriptor("WithCompanionDesc", PrimitiveKind.INT)
            override fun deserialize(decoder: Decoder): WithCompanion {
                val value = decoder.decodeInt()
                return WithCompanion(value)
            }

            override fun serialize(encoder: Encoder, value: WithCompanion) {
                encoder.encodeInt(value.value)
            }
        }
    }

    @Test
    fun testCompanion() {
        test(WithCompanion(7), "7", "{\"value\":7}", WithCompanion.serializer(), WithCompanion.generatedSerializer())
    }


    @Serializable(with = ObjectSerializer::class)
    @KeepGeneratedSerializer
    private object Object

    private object ObjectSerializer: KSerializer<Object> {
        override val descriptor = PrimitiveSerialDescriptor("ObjectSerializer", PrimitiveKind.INT)

        override fun deserialize(decoder: Decoder): Object {
            decoder.decodeInt()
            return Object
        }
        override fun serialize(encoder: Encoder, value: Object) {
            encoder.encodeInt(8)
        }
    }

    @Test
    fun testObject() {
        test(Object, "8", "{}", Object.serializer(), Object.generatedSerializer())
        assertEquals("dev.dokky.zerojson.ktx.KeepGeneratedSerializerTest.Object()", Object.generatedSerializer().descriptor.toString(), "Object.generatedSerializer() illegal")
        assertSame(Object.generatedSerializer(), Object.generatedSerializer(), "Object.generatedSerializer() instance differs")
    }

    private inline fun <reified T : Any> test(
        value: T,
        customJson: String,
        keepJson: String,
        serializer: KSerializer<T>,
        generatedSerializer: KSerializer<T>?
    ) {
        val implicitJson = TestZeroJson.encodeToString(value)
        assertEquals(customJson, implicitJson, "TestZeroJson.encodeToString(value: ${T::class.simpleName})")
        val implicitDecoded = TestZeroJson.decodeFromString<T>(implicitJson)
        assertEquals(value, implicitDecoded, "TestZeroJson.decodeFromString(json): ${T::class.simpleName}")

        val explicitJson = TestZeroJson.encodeToString(serializer, value)
        assertEquals(customJson, explicitJson, "TestZeroJson.encodeToString(${T::class.simpleName}.serializer(), value)")
        val explicitDecoded = TestZeroJson.decodeFromString(serializer, explicitJson)
        assertEquals(value, explicitDecoded, "TestZeroJson.decodeFromString(${T::class.simpleName}.serializer(), json)")

        if (generatedSerializer == null) return
        val keep = TestZeroJson.encodeToString(generatedSerializer, value)
        assertEquals(keepJson, keep, "TestZeroJson.encodeToString(${T::class.simpleName}.generatedSerializer(), value)")
        val keepDecoded = TestZeroJson.decodeFromString(generatedSerializer, keep)
        assertEquals(value, keepDecoded, "TestZeroJson.decodeFromString(${T::class.simpleName}.generatedSerializer(), json)")
    }
}