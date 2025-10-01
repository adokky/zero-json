package dev.dokky.zerojson

import kotlinx.serialization.Contextual
import kotlinx.serialization.SerialInfo
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlin.js.JsName
import kotlin.jvm.JvmInline
import kotlin.jvm.JvmName

@SerialInfo
@Target(AnnotationTarget.PROPERTY)
annotation class Id(val id: Int)

@Suppress("EnumEntryName")
@Serializable
enum class TestEnum {
    entry1,
    entry2,
    `entry 3`
}

enum class SampleEnum { OptionA, @Suppress("unused") OptionB, OptionC }

@Serializable
data class StringData(val data: String)

@Serializable
data class SimpleDataClass(val key: String)

@Serializable
data class CompoundDataClass(val string: String, val int: Int)

@Serializable
@JvmInline
value class SimpleValueClass(val key: String)

@Serializable
@JvmInline
value class SimpleValueInteger(val value: Int)

@Serializable
@JvmInline
value class SimpleValueClassWrapper(val key: SimpleValueClass)

@Serializable
@JvmInline
value class NullableValueClassWrapper(val key: SimpleValueClass?)

@Serializable
@JvmInline
value class NullableValueClassDoubleWrapper(val key: NullableValueClassWrapper?)

@Serializable
@JvmInline
value class ComplexValue(val data: CompoundDataClass)

@Serializable
data class MultiValueDataClass(val cv1: ComplexValue?, val cv2: ComplexValue?)

@Serializable
data class Box<T>(val value: T)

@JvmInline
@Serializable
value class InlineBox<T>(val value: T)

@Serializable
sealed class SimpleSealed {
    @Serializable
    data class SubSealedA(val s: String) : SimpleSealed()

    @Serializable
    data class SubSealedB(val i: Int) : SimpleSealed()
}

@Serializable
class ValueFieldsContainer(
    @Suppress("unused") val w1: NullableValueClassDoubleWrapper?,
    @Suppress("unused") val w2: SimpleValueInteger?,
    @Suppress("unused") val w3: SimpleValueClassWrapper
)

@Suppress("TestFunctionName")
@JvmName("SimpleValueClass_Wrapper")
fun SimpleValueClassWrapper(key: String): SimpleValueClassWrapper = SimpleValueClassWrapper(SimpleValueClass(key))

@Serializable
data class ComplexClass(
    @Suppress("PropertyName", "NonAsciiCharacters")
    @JsName("field1")
    val `поле 1`: String,
    val int: Int,
    val long: Long,
    val nullableString: String?,
    val nullableInt: Int?,
    val nullableLong: Long?,
    val nestedSimple: SimpleDataClass?,
    val selfNested: ComplexClass?
)

@Serializable
sealed class SealedParent(val i: Int)

@Serializable
@SerialName("first child")
data class SealedChild(val j: Int) : SealedParent(1)

@Serializable
data class UnsignedNumbers(
    val uByte: UByte,
    val uInt: UInt,
    val nuInt: UInt?,
    val uLong: ULong,
    val nuByte: UByte?,
    val uShort: UShort,
    val nuShort: UShort?,
    val nuLong: ULong?
)

@Serializable
data class Maps(
    val map1: Map<Int, Int>,
    val map2: Map<String, Maps> = emptyMap(),
    val inner: Maps? = null
)

@Serializable
class ArraysAndLists(
    val array: Array<Int>,
    val intArray: IntArray,
    val intList: List<Int>
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false

        other as ArraysAndLists

        if (!array.contentEquals(other.array)) return false
        if (!intArray.contentEquals(other.intArray)) return false
        if (intList != other.intList) return false

        return true
    }

    override fun hashCode(): Int {
        var result = array.contentHashCode()
        result = 31 * result + intArray.contentHashCode()
        result = 31 * result + intList.hashCode()
        return result
    }

    override fun toString(): String =
        "ArraysAndLists(array=${array.contentToString()}, intArray=${intArray.contentToString()}, intList=$intList)"
}

@Serializable sealed class PolymorphicBase1 {
    @Serializable sealed class SubClass1: PolymorphicBase1() {
        @Serializable data class Concrete1(val string: String): SubClass1()
        @SerialName("MyConcrete2")
        @Serializable data class Concrete2(val int: Int): SubClass1()
    }
    @Serializable data class Concrete3(val base: PolymorphicBase1, val sub1: SubClass1): PolymorphicBase1()
}

@Serializable sealed class PolymorphicBase2 {
    @Serializable sealed class SubClass1: PolymorphicBase2() {
        @Serializable
        @SerialName("MyConcrete")
        data class Concrete1(val string: String): SubClass1()
        @Serializable data class Concrete2(val string: String, val int: Int): SubClass1()
    }
    @Serializable data class Concrete3(val base: PolymorphicBase2, val sub1: SubClass1): PolymorphicBase2()
}

@Serializable sealed interface PolyInterface {
    @JvmInline
    @Serializable
    value class Value1(val int: Int): PolyInterface

    @JvmInline
    @Serializable
    value class NestedValue(val v: SimpleValueInteger): PolyInterface

    @Serializable
    sealed interface SubInterface: PolyInterface {
        val float: Float

        @JvmInline
        @Serializable
        value class Value2(override val float: Float): SubInterface
    }

    @Serializable sealed class SubClass1: PolyInterface {
        @Serializable
        @SerialName("MyConcrete")
        data class Concrete1(val string: String): SubClass1()

        @Serializable data class Concrete2(val string: String, val int: Int): SubClass1()
    }

    @Serializable data class Concrete3(val base: PolymorphicBase2, val sub1: SubClass1): PolyInterface

    @JvmInline
    @Serializable
    @SerialName("SubPoly")
    value class SubPoly(val base: PolymorphicBase1): PolyInterface

    @Serializable
    sealed interface CompoundSubValues: PolyInterface {
        @JvmInline
        @Serializable
        value class DataClassWrapped(val someData: CompoundDataClass): CompoundSubValues

        @JvmInline
        @Serializable
        value class ListWrapped(val list: List<String>): CompoundSubValues

        @JvmInline
        @Serializable
        value class MapWrapped(val map: Map<Int, Long>): CompoundSubValues
    }

    @JvmInline
    @Serializable
    value class ContextualValue(@Contextual val v: Any): CompoundSubValues

    @JvmInline
    @Serializable
    value class ContextualNullableValue(@Contextual val v: Any?): CompoundSubValues
}