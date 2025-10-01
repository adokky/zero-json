package dev.dokky.zerojson

import dev.dokky.zerojson.framework.AbstractDecoderTest
import kotlin.test.Test

class ImplicitNullTest: AbstractDecoderTest() {
    @Test
    fun optionalFields() {
        assertDecodedEquals(
            json = """
            {
                "${ComplexClass::`поле 1`.name}": "test",
                "${ComplexClass::int.name}": 12e5,
                "${ComplexClass::long.name}": 12e4,
                "${ComplexClass::nestedSimple.name}": { "key": "foo" }
            }
            """,
            expected = ComplexClass(
                "test",
                int = 1200000,
                long = 120000,
                nestedSimple = SimpleDataClass("foo"),
                nullableInt = null,
                nullableString = null,
                nullableLong = null,
                selfNested = null
            )
        )
    }
}