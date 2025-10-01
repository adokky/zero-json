package dev.dokky.zerojson

import dev.dokky.zerojson.framework.AbstractDecoderTest
import kotlin.test.Test

class CollectionsAndClassesMixTest: AbstractDecoderTest() {
    @Test
    fun list_of_classes() {
        encodeDecode(
            listOf(
                CompoundDataClass("c1", 443),
                CompoundDataClass("c2", 767)
            )
        )
    }

    @Test
    fun boxed_list_of_values_classes() {
        encodeDecode(
            Box(
                listOf(
                    ComplexValue(CompoundDataClass("1", 2)),
                    ComplexValue(CompoundDataClass("3", 4)),
                )
            )
        )
    }

    @Test
    fun boxed_list_of_class_with_values_classes() {
        encodeDecode(
            Box(
                listOf(
                    MultiValueDataClass(
                        ComplexValue(CompoundDataClass("1", 2)),
                        ComplexValue(CompoundDataClass("3", 4)),
                    ),
                    MultiValueDataClass(
                        ComplexValue(CompoundDataClass("5", 6)),
                        ComplexValue(CompoundDataClass("7", 8)),
                    ),
                )
            )
        )
    }

    @Test
    fun list_of_maps_with_multi_values_classes() {
        encodeDecode(
            listOf(
                mapOf(
                    "1" to MultiValueDataClass(
                        ComplexValue(CompoundDataClass("1", 2)),
                        ComplexValue(CompoundDataClass("3", 4)),
                    )
                ),
                mapOf(
                    "2" to MultiValueDataClass(
                        ComplexValue(CompoundDataClass("a", 45)),
                        ComplexValue(CompoundDataClass("b", 65)),
                    )
                )
            )
        )
    }
}