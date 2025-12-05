package dev.dokky.zerojson

import dev.dokky.zerojson.framework.*
import karamel.utils.unsafeCast
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerializationException
import kotlinx.serialization.decodeFromString
import kotlin.test.Test
import kotlin.test.assertFailsWith


class PolymorphismTest: RandomizedJsonTest() {
    @Test
    fun concrete1() {
        val expected = PolymorphicBase1.SubClass1.Concrete1("Hello world!")
        randomizedTest<PolymorphicBase1>(expected) {
            discriminator<PolymorphicBase1.SubClass1.Concrete1>()
            PolymorphicBase1.SubClass1.Concrete1::string eq expected.string
        }
    }

    @Test
    fun concrete2() {
        val expected = PolymorphicBase1.SubClass1.Concrete2(128)
        randomizedTest<PolymorphicBase1>(expected) {
            discriminator<PolymorphicBase1.SubClass1.Concrete2>()
            PolymorphicBase1.SubClass1.Concrete2::int eq 128
        }
    }

    @Test
    fun concrete1_null() {
        assertDecoded<PolymorphicBase1?>("null", null)
    }

    @Test
    fun concrete1_nullable() {
        val expected = PolymorphicBase1.SubClass1.Concrete2(128)
        val obj = jsonObject {
            discriminator<PolymorphicBase1.SubClass1.Concrete2>()
            PolymorphicBase1.SubClass1.Concrete2::int eq 128
        }
        assertDecoded<PolymorphicBase1?>(obj.toString(), expected)
    }

    @Test
    fun pair_with_kotlinx() {
        encodeDecode<PolymorphicBase2>(PolymorphicBase2.SubClass1.Concrete2(
            "dev.dokky.zerojson.PolymorphicBase2.SubClass1.Concrete2", -128))
    }

    @Test
    fun concrete3() {
        val obj =  PolymorphicBase2.Concrete3(
            base = PolymorphicBase2.Concrete3(
                base = PolymorphicBase2.SubClass1.Concrete1("Привет, Мир!"),
                sub1 = PolymorphicBase2.SubClass1.Concrete1("Наследник Base.SubClass1")
            ),
            sub1 = PolymorphicBase2.SubClass1.Concrete2("framework", Int.MIN_VALUE)
        )
        randomizedTest<PolymorphicBase2>(obj) {
            discriminator<PolymorphicBase2.Concrete3>()
            "base".polymorphic<PolymorphicBase2.Concrete3> {
                "base".polymorphic<PolymorphicBase2.SubClass1.Concrete1> {
                    "string" eq "Привет, Мир!"
                }
                "sub1".polymorphic<PolymorphicBase2.SubClass1.Concrete1> {
                    "string" eq "Наследник Base.SubClass1"
                }
            }
            "sub1".polymorphic<PolymorphicBase2.SubClass1.Concrete2> {
                "string" eq "framework"
                "int" eq Int.MIN_VALUE
            }
        }
    }

    @Test
    fun sub_value_int() {
        val obj = PolyInterface.Value1(868)
        randomizedTest<PolyInterface>(obj) {
            discriminator<PolyInterface.Value1>()
            "value" eq obj.int
        }
    }

    @Test
    fun sub_value_float() {
        val obj = PolyInterface.SubInterface.Value2(235.0f)
        randomizedTest<PolyInterface>(obj) {
            discriminator<PolyInterface.SubInterface.Value2>()
            "value" eq obj.float
        }
    }

    @Test
    fun fixture() {
        assertFailsWith<SerializationException> {
            println(TestZeroJson.decodeFromString<PolyInterface>(
                "{type:dev.dokky.zerojson.PolyInterface.SubInterface.Value2,value:235.ꈣ0}"
            ))
        }
    }

    @Test
    fun nested_polymorphic() {
        val obj = PolyInterface.SubPoly(
            PolymorphicBase1.Concrete3(
                base = PolymorphicBase1.SubClass1.Concrete2(9334),
                sub1 = PolymorphicBase1.SubClass1.Concrete1("YYY"),
            )
        )
        randomizedTest<PolyInterface>(obj) {
            discriminator<PolyInterface.SubPoly>()
            "value".polymorphic<PolymorphicBase1.Concrete3> {
                "base".polymorphic<PolymorphicBase1.SubClass1.Concrete2> {
                    "int" eq 9334
                }
                "sub1".polymorphic<PolymorphicBase1.SubClass1.Concrete1> {
                    "string" eq "YYY"
                }
            }
        }
    }

    @Test
    fun sub_value_nested_int() {
        val obj = PolyInterface.NestedValue(SimpleValueInteger(763))
        randomizedTest<PolyInterface>(obj) {
            discriminator<PolyInterface.NestedValue>()
            "value" eq 763
        }
    }

    @Test
    fun value_subclass_wrapping_data_class() {
        val obj = PolyInterface.CompoundSubValues.DataClassWrapped(CompoundDataClass("a string", 987654))
        randomizedTest<PolyInterface>(obj) {
            discriminator<PolyInterface.CompoundSubValues.DataClassWrapped>()
            "string" eq obj.someData.string
            "int" eq obj.someData.int
        }
    }

    @Test
    fun value_subclass_wrapping_list() {
        val obj = PolyInterface.CompoundSubValues.ListWrapped(listOf("a", "b", "x"))
        randomizedTest<PolyInterface>(obj) {
            discriminator<PolyInterface.CompoundSubValues.ListWrapped>()
            "value" stringArray obj.list
        }
    }

    @Test
    fun value_subclass_wrapping_map() {
        val obj = PolyInterface.CompoundSubValues.MapWrapped(mapOf(1 to 10L, 2 to 20L, 3 to 30L))
        randomizedTest<PolyInterface>(obj) {
            discriminator<PolyInterface.CompoundSubValues.MapWrapped>()
            "value" noRandomKeys {
                "1" eq 10L
                "2" eq 20L
                "3" eq 30L
            }
        }
    }

    private fun <T: Any> contextualTest(
        concreteValue: T,
        concreteSerializer: KSerializer<T>,
        concreteValueBuilder: DslJsonObjectBuilder.(T) -> Unit
    ) {
        val contextualJson = ZeroJson {
            serializersModule {
                contextual(Any::class, concreteSerializer.unsafeCast<KSerializer<Any>>())
            }
        }

        randomizedTestSuite {
            test("Explicit null value") {
                domainObject<PolyInterface>(PolyInterface.ContextualNullableValue(null))
                json = contextualJson
                iterations = 5
                jsonElement {
                    discriminator<PolyInterface.ContextualNullableValue>()
                    "value" eq null
                }
            }
            test("Missing null value") {
                domainObject<PolyInterface>(PolyInterface.ContextualNullableValue(null))
                json = contextualJson
                iterations = 5
                jsonElement {
                    discriminator<PolyInterface.ContextualNullableValue>()
                }
            }
            test("Missing null value (explicitNulls = true)") {
                domainObject<PolyInterface>(PolyInterface.ContextualNullableValue(null))
                json = ZeroJson(contextualJson) { explicitNulls = true }
                expectFailureIfTarget { it.output == TestTarget.DataType.Domain }
                // with explicitNulls=true it will not match the input
                excludeTargetIf { it.input == TestTarget.DataType.Domain }
                iterations = 5
                jsonElement {
                    discriminator<PolyInterface.ContextualNullableValue>()
                }
            }
            test("Normal value") {
                domainObject<PolyInterface>(PolyInterface.ContextualValue(concreteValue))
                json = ZeroJson(contextualJson) { explicitNulls = true }
                jsonElement {
                    discriminator<PolyInterface.ContextualValue>()
                    concreteValueBuilder(concreteValue)
                }
            }
        }
    }

    private inline fun <reified T: Any> contextualTest(
        concreteValue: T,
        noinline concreteValueBuilder: DslJsonObjectBuilder.(T) -> Unit
    ) {
        contextualTest(
            concreteValue = concreteValue,
            concreteSerializer = kotlinx.serialization.serializer<T>(),
            concreteValueBuilder = concreteValueBuilder,
        )
    }

    @Test
    fun contextual_int() {
        contextualTest(123) {
            "value" eq it
        }
    }

    @Test
    fun contextual_value() {
        contextualTest(SimpleValueInteger(9646)) {
            "value" eq it.value
        }
    }

    @Test
    fun contextual_data_class() {
        contextualTest(SimpleDataClass("{[-]}")) {
            "key" eq it.key
        }
    }

    @Test
    fun contextual_value_wrapping_data_class() {
        contextualTest(ComplexValue(CompoundDataClass("Строковое поле", 111))) {
            "string" eq it.data.string
            "int" eq it.data.int
        }
    }

    @Test
    fun contextual_polymorphic() {
        contextualTest<PolyInterface>(PolyInterface.SubClass1.Concrete2("ZZZ", 456)) {
            "value".polymorphic<PolyInterface.SubClass1.Concrete2> {
                "string" eq "ZZZ"
                "int" eq 456
            }
        }
    }

    @Test
    fun contextual_polymorphic_value() {
        contextualTest<PolyInterface>(PolyInterface.Value1(73)) {
            "value".polymorphic<PolyInterface.Value1> {
                "value" eq 73
            }
        }
    }

    @Test
    fun contextual_double_nested_value() {
        contextualTest(PolyInterface.NestedValue(SimpleValueInteger(871120))) {
            "value" eq it.v.value
        }
    }
}
