package dev.dokky.zerojson.ktx

import dev.dokky.zerojson.SampleEnum
import dev.dokky.zerojson.ZeroJson
import kotlinx.serialization.ContextualSerializer
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.contextual
import kotlin.jvm.JvmInline
import kotlin.test.Test

@Serializable
private data class SimpleContainerForUInt(val i: UInt)

@Serializable(MyUIntSerializer::class)
@JvmInline
private value class MyUInt(val m: Int)

private object MyUIntSerializer : KSerializer<MyUInt> {
    override val descriptor = UInt.serializer().descriptor
    override fun serialize(encoder: Encoder, value: MyUInt) {
        encoder.encodeInline(descriptor).encodeInt(value.m)
    }
    override fun deserialize(decoder: Decoder): MyUInt =
        MyUInt(decoder.decodeInline(descriptor).decodeInt())
}

@Serializable
private data class SimpleContainerForMyType(val i: MyUInt)

@Serializable
@JvmInline
private value class MyList<T>(val list: List<T>)

@Serializable
private data class ContainerForList<T>(val i: MyList<T>)

@Serializable
private data class UnsignedInBoxedPosition(val i: List<UInt>)

@Serializable
private data class MixedPositions(
    val int: Int,
    val intNullable: Int?,
    val uint: UInt,
    val uintNullable: UInt?,
    val boxedInt: List<Int>,
    val boxedUInt: List<UInt>,
    val boxedNullableInt: List<Int?>,
    val boxedNullableUInt: List<UInt?>
)

@Serializable
@JvmInline
private value class ResourceId(val id: String)

@Serializable
@JvmInline
private value class ResourceType(val type: String)

@Serializable
@JvmInline
private value class ResourceKind(val kind: SampleEnum)

@Serializable
private data class ResourceIdentifier(val id: ResourceId, val type: ResourceType, val type2: ValueWrapper)

@Serializable
@JvmInline
private value class ValueWrapper(val wrapped: ResourceType)

@Serializable
@JvmInline
private value class Outer(val inner: Inner)

@Serializable
private data class Inner(val n: Int)

@Serializable
private data class OuterOuter(val outer: Outer)

@Serializable
@JvmInline
private value class WithList(val value: List<Int>)

class InlineClassesTest : JsonTestBase() {
    private val precedent: UInt = Int.MAX_VALUE.toUInt() + 10.toUInt()

    @Test
    fun withList() {
        val withList = WithList(listOf(1, 2, 3))
        assertJsonFormAndRestored(WithList.serializer(), withList, """[1,2,3]""")
    }

    @Test
    fun testOuterInner() {
        val o = Outer(Inner(10))
        assertJsonFormAndRestored(Outer.serializer(), o, """{"n":10}""")
    }

    @Test
    fun testOuterOuterInner() {
        val o = OuterOuter(Outer(Inner(10)))
        assertJsonFormAndRestored(OuterOuter.serializer(), o, """{"outer":{"n":10}}""")
    }

    @Test
    fun testTopLevel() {
        assertJsonFormAndRestored(
            ResourceType.serializer(),
            ResourceType("foo"),
            """"foo"""",
        )
    }

    @Test
    fun testTopLevelOverEnum() {
        assertJsonFormAndRestored(
            ResourceKind.serializer(),
            ResourceKind(SampleEnum.OptionC),
            """"OptionC"""",
        )
    }

    @Test
    fun testTopLevelWrapper() {
        assertJsonFormAndRestored(
            ValueWrapper.serializer(),
            ValueWrapper(ResourceType("foo")),
            """"foo"""",
        )
    }

    @Test
    fun testTopLevelContextual() {
        val module = SerializersModule {
            contextual<ResourceType>(ResourceType.serializer())
        }
        assertJsonFormAndRestored(
            ContextualSerializer(ResourceType::class),
            ResourceType("foo"),
            """"foo"""",
            ZeroJson(default) { serializersModule = module }
        )
    }

    @Test
    fun testSimpleContainer() {
        assertJsonFormAndRestored(
            SimpleContainerForUInt.serializer(),
            SimpleContainerForUInt(precedent),
            """{"i":2147483657}""",
        )
    }

    @Test
    fun testSimpleContainerForMyTypeWithCustomSerializer() = assertJsonFormAndRestored(
        SimpleContainerForMyType.serializer(),
        SimpleContainerForMyType(MyUInt(precedent.toInt())),
        """{"i":2147483657}""",
    )

    @Test
    fun testSimpleContainerForList() {
        assertJsonFormAndRestored(
            ContainerForList.serializer(UInt.serializer()),
            ContainerForList(MyList(listOf(precedent))),
            """{"i":[2147483657]}""",
        )
    }

    @Test
    fun testInlineClassesWithStrings() {
        assertJsonFormAndRestored(
            ResourceIdentifier.serializer(),
            ResourceIdentifier(ResourceId("resId"), ResourceType("resType"), ValueWrapper(ResourceType("wrappedType"))),
            """{"id":"resId","type":"resType","type2":"wrappedType"}"""
        )
    }

    @Test
    fun testUnsignedInBoxedPosition() = assertJsonFormAndRestored(
        UnsignedInBoxedPosition.serializer(),
        UnsignedInBoxedPosition(listOf(precedent)),
        """{"i":[2147483657]}""",
    )

    @Test
    fun testMixedPositions() {
        assertJsonFormAndRestored(
            MixedPositions.serializer(),
            MixedPositions(
                int = precedent.toInt(),
                intNullable = precedent.toInt(),
                uint = precedent,
                uintNullable = precedent,
                boxedInt = listOf(precedent.toInt()),
                boxedUInt = listOf(precedent),
                boxedNullableInt = listOf(null, precedent.toInt(), null),
                boxedNullableUInt = listOf(null, precedent, null)
            ),
            """{"int":-2147483639,"intNullable":-2147483639,"uint":2147483657,"uintNullable":2147483657,"boxedInt":[-2147483639],"boxedUInt":[2147483657],"boxedNullableInt":[null,-2147483639,null],"boxedNullableUInt":[null,2147483657,null]}""",
        )
    }
}
