package dev.dokky.zerojson

import dev.dokky.zerojson.framework.jsonObject
import kotlin.test.Test

class PolymorphicEncodingTest: EncoderTest() {
    @Test
    fun polymorphic() {
        val expected = """{"type":"dev.dokky.zerojson.PolymorphicBase1.SubClass1.Concrete1","string":"hello"}"""

        test<PolymorphicBase1>(expected, PolymorphicBase1.SubClass1.Concrete1("hello"))
        test<PolymorphicBase1.SubClass1>(expected, PolymorphicBase1.SubClass1.Concrete1("hello"))
    }

    @Test
    fun polymorphic_nested() {
        val expectedObject = jsonObject {
            discriminator<PolymorphicBase1.Concrete3>()
            "base".polymorphic<PolymorphicBase1.Concrete3> {
                "base".polymorphic<PolymorphicBase1.SubClass1.Concrete2> {
                    "int" eq 2
                }
                "sub1".polymorphic<PolymorphicBase1.SubClass1.Concrete1> {
                    "string" eq "3"
                }
            }
            "sub1".polymorphic<PolymorphicBase1.SubClass1.Concrete2> {
                "int" eq 4
            }
        }

        test<PolymorphicBase1>(expectedObject.toString(),
            PolymorphicBase1.Concrete3(
                PolymorphicBase1.Concrete3(
                    PolymorphicBase1.SubClass1.Concrete2(2),
                    PolymorphicBase1.SubClass1.Concrete1("3")
                ),
                PolymorphicBase1.SubClass1.Concrete2(4)
            )
        )
    }

    @Test
    fun polymorphic_interface() {
        val expected = """{"type":"MyConcrete","string":"hello"}"""

        test<PolyInterface>(expected, PolyInterface.SubClass1.Concrete1("hello"))
        test<PolyInterface.SubClass1>(expected, PolyInterface.SubClass1.Concrete1("hello"))
    }

    @Test
    fun polymorphic_value_subclasses_1() = test<PolyInterface>(
        """{"type":"dev.dokky.zerojson.PolyInterface.Value1","value":6556}""",
        PolyInterface.Value1(6556)
    )

    @Test
    fun polymorphic_value_subclasses_2() {
        test<PolyInterface>(
            """{"type":"dev.dokky.zerojson.PolyInterface.SubInterface.Value2","value":6556.0}""",
            PolyInterface.SubInterface.Value2(6556f)
        )
        test<PolyInterface.SubInterface>(
            """{"type":"dev.dokky.zerojson.PolyInterface.SubInterface.Value2","value":6556.0}""",
            PolyInterface.SubInterface.Value2(6556f)
        )
    }
}