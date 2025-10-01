@file:OptIn(InternalSerializationApi::class)

package dev.dokky.zerojson.ktx

import dev.dokky.zerojson.Id
import dev.dokky.zerojson.framework.assertDescriptorEqualsTo
import kotlinx.serialization.InternalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.*
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlin.test.Test
import kotlin.test.assertEquals

class EnumSerializationTest : JsonTestBase() {
    @Suppress("unused")
    @Serializable
    private enum class RegularEnum { VALUE }

    @Serializable
    private data class Regular(val a: RegularEnum)

    @Serializable
    private data class RegularNullable(val a: RegularEnum?)

    @Suppress("unused")
    @Serializable
    @SerialName("custom_enum")
    private enum class CustomEnum {
        @SerialName("foo_a") FooA,
        @SerialName("foo_b") @Id(10) FooB
    }

    @Serializable
    private data class WithCustomEnum(val c: CustomEnum)

    @Serializable(CustomEnumSerializer::class)
    private enum class WithCustom {
        @SerialName("1") ONE,
        @SerialName("2") TWO
    }

    private class CustomEnumSerializer : KSerializer<WithCustom> {
        override val descriptor: SerialDescriptor = buildSerialDescriptor("WithCustom", SerialKind.ENUM) {
            element("1", buildSerialDescriptor("WithCustom.1", StructureKind.OBJECT))
            element("2", buildSerialDescriptor("WithCustom.2", StructureKind.OBJECT))
        }

        override fun serialize(encoder: Encoder, value: WithCustom) {
            encoder.encodeInt(value.ordinal + 1)
        }

        override fun deserialize(decoder: Decoder): WithCustom =
            WithCustom.entries[decoder.decodeInt() - 1]
    }

    @Serializable
    private data class CustomInside(val inside: WithCustom)

    @Test
    fun testEnumSerialization() =
        assertJsonFormAndRestored(
            WithCustomEnum.serializer(),
            WithCustomEnum(CustomEnum.FooB),
            """{"c":"foo_b"}""",
            default
        )

    @Test
    fun testEnumWithCustomSerializers() =
        assertJsonFormAndRestored(
            CustomInside.serializer(),
            CustomInside(WithCustom.TWO), """{"inside":2}"""
        )

    @Test
    fun testHasMeaningfulToString() {
        val regular = Regular.serializer().descriptor.toString()
        assertEquals(
            "dev.dokky.zerojson.ktx.EnumSerializationTest.Regular(a: dev.dokky.zerojson.ktx.EnumSerializationTest.RegularEnum)",
            regular
        )
        val regularNullable = RegularNullable.serializer().descriptor.toString()
        assertEquals(
            "dev.dokky.zerojson.ktx.EnumSerializationTest.RegularNullable(a: dev.dokky.zerojson.ktx.EnumSerializationTest.RegularEnum?)",
            regularNullable
        )
        // slightly differs from previous one
        val regularNullableJoined = RegularNullable.serializer().descriptor.elementDescriptors.joinToString()
        assertEquals("dev.dokky.zerojson.ktx.EnumSerializationTest.RegularEnum(VALUE)?", regularNullableJoined)

        val regularEnum = RegularEnum.serializer().descriptor.toString()
        assertEquals("dev.dokky.zerojson.ktx.EnumSerializationTest.RegularEnum(VALUE)", regularEnum)
    }

    @Test
    fun testHasMeaningfulHashCode() {
        val a = Regular.serializer().descriptor.hashCode()
        val b = RegularNullable.serializer().descriptor.hashCode()
        val c = RegularEnum.serializer().descriptor.hashCode()
        assertEquals(setOf(a, b, c).size, 3, ".hashCode must give different result for different descriptors")
    }

    @Suppress("unused")
    private enum class MyEnum { A, B, C; }

    @Suppress("unused")
    @Serializable
    @SerialName("dev.dokky.zerojson.ktx.EnumSerializationTest.MyEnum")
    private enum class MyEnum2 { A, B, C; }

    @Suppress("unused")
    @Serializable
    private class Wrapper(val a: MyEnum)

    @Test
    fun testStructurallyEqualDescriptors() {
        val libraryGenerated = Wrapper.serializer().descriptor.getElementDescriptor(0)
        val codeGenerated = MyEnum2.serializer().descriptor
        assertEquals(libraryGenerated::class, codeGenerated::class)
        libraryGenerated.assertDescriptorEqualsTo(codeGenerated)
    }
}
