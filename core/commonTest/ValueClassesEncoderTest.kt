package dev.dokky.zerojson

import kotlin.test.Test

class ValueClassesEncoderTest: EncoderTest() {
    @Test
    fun values_classes_simple() {
        test("\"строка?\"", SimpleValueClass("строка?"))
        test("142", SimpleValueInteger(142))
        test<SimpleValueInteger?>("142", SimpleValueInteger(142))
        test<SimpleValueInteger?>("null", null)
    }

    @Test
    fun values_classes_simple_wrapper() {
        test("\"строка!\"", SimpleValueClassWrapper("строка!"))
    }

    @Test
    fun values_classes_in_map() {
        test("""{"key1":"value1"}""",
            mapOf(
                SimpleValueClass("key1") to SimpleValueClass("value1")
            )
        )

        test("""{"key1":"value1","key2":"value2"}""",
            mapOf(
                SimpleValueClass("key1") to SimpleValueClass("value1"),
                SimpleValueClass("key2") to SimpleValueClass("value2"),
            )
        )
    }

    @Test
    fun values_classes_in_list() {
        test("""["v1"]""",
            listOf(
                SimpleValueClass("v1")
            )
        )

        test("""["v1","v2"]""",
            listOf(
                SimpleValueClass("v1"), SimpleValueClass("v2")
            )
        )
    }

    @Test
    fun values_class_wrappers_in_nullable_list() {
        test("""["v1"]""",
            listOf<SimpleValueClassWrapper?>(
                SimpleValueClassWrapper("v1")
            )
        )

        test("""[null]""",
            listOf<SimpleValueClassWrapper?>(null)
        )

        test("""["v1",null,"v2"]""",
            listOf(
                SimpleValueClassWrapper("v1"), null, SimpleValueClassWrapper("v2")
            )
        )
    }

    @Test
    fun values_classes_nullable_wrapper() {
        test<NullableValueClassWrapper?>("\"строка!\"",
            NullableValueClassWrapper(SimpleValueClass("строка!"))
        )
        test<NullableValueClassWrapper?>("null",
            NullableValueClassWrapper(null)
        )
        test<NullableValueClassWrapper?>("null",
            null
        )
    }

    @Test
    fun values_classes_double_wrapper() {
        test<NullableValueClassDoubleWrapper?>("\"строка!\"",
            NullableValueClassDoubleWrapper(
                NullableValueClassWrapper(
                    SimpleValueClass("строка!")
                )
            )
        )
        test<NullableValueClassDoubleWrapper?>("null",
            NullableValueClassDoubleWrapper(
                NullableValueClassWrapper(null)
            )
        )
        test<NullableValueClassDoubleWrapper?>("null",
            NullableValueClassDoubleWrapper(null)
        )
        test<NullableValueClassDoubleWrapper?>("null",
            null
        )
    }

    @Test
    fun values_classes_double_wrapper_inside_map() {
        fun wrapper(s: String) = NullableValueClassDoubleWrapper(
            NullableValueClassWrapper(
                SimpleValueClass(s)
            )
        )

        test("""{"key1":"value1","key2":"value2"}""",
            mapOf(
                wrapper("key1") to wrapper("value1"),
                wrapper("key2") to wrapper("value2"),
            )
        )
    }

    @Test
    fun values_classes_inside_regular_class() {
        test("""{"w1":"abc","w2":-5669,"w3":"xyz"}""",
            ValueFieldsContainer(
                w1 = NullableValueClassDoubleWrapper(
                    NullableValueClassWrapper(
                        SimpleValueClass("abc")
                    )
                ),
                w2 = SimpleValueInteger(-5669),
                w3 = SimpleValueClassWrapper("xyz")
            )
        )

        test("""{"w3":"xyz"}""",
            ValueFieldsContainer(
                w1 = NullableValueClassDoubleWrapper(
                    NullableValueClassWrapper(null)
                ),
                w2 = null,
                w3 = SimpleValueClassWrapper("xyz")
            )
        )
    }

    @Test
    fun values_classes_with_compound_data() {
        test<ComplexValue?>("null", null)
        test<ComplexValue?>("""{"string":"hello","int":78656}""",
            ComplexValue(CompoundDataClass("hello", 78656))
        )
    }

    @Test
    fun values_classes_with_boxed_compound_data() {
        test<Box<ComplexValue?>>("{}", Box(null))
        test<Box<ComplexValue?>>("""{"value":{"string":"hello","int":78656}}""",
            Box(ComplexValue(CompoundDataClass("hello", 78656)))
        )
    }

    @Test
    fun multiple_values_classes_with_compound_data_inside_regular_class() {
        test<MultiValueDataClass>(
            """{"cv1":{"string":"hello","int":12344},"cv2":{"string":"world","int":7568}}""",
            MultiValueDataClass(
                ComplexValue(CompoundDataClass("hello", 12344)),
                ComplexValue(CompoundDataClass("world", 7568))
            )
        )

        test<MultiValueDataClass>(
            """{"cv2":{"string":"world","int":7568}}""",
            MultiValueDataClass(
                null,
                ComplexValue(CompoundDataClass("world", 7568))
            )
        )

        test<MultiValueDataClass>(
            """{"cv1":{"string":"hello","int":12344}}""",
            MultiValueDataClass(
                ComplexValue(CompoundDataClass("hello", 12344)),
                null
            )
        )
    }
}