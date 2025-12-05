@file:Suppress("OPT_IN_USAGE")

package dev.dokky.zerojson.ktx

import dev.dokky.zerojson.Box
import dev.dokky.zerojson.ZeroJson
import dev.dokky.zerojson.framework.assertFailsWithMessage
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.JsonNames
import kotlin.test.Test
import kotlin.test.assertEquals

@Suppress("EnumEntryName")
class JsonEnumsCaseInsensitiveTest: JsonTestBase() {
    @Serializable
    private data class Foo(
        val one: Bar = Bar.BAZ,
        val two: Bar = Bar.QUX,
        val three: Bar = Bar.QUX
    )

    private enum class Bar { BAZ, QUX }

    // It seems that we no longer report a warning that @Serializable is required for enums with @SerialName.
    // It is still required for them to work at top-level.
    @Serializable
    private enum class Cases {
        ALL_CAPS,
        MiXed,
        all_lower,

        @JsonNames("AltName")
        hasAltNames,

        @SerialName("SERIAL_NAME")
        hasSerialName
    }

    @Serializable
    private data class EnumCases(val cases: List<Cases>)

    private val json = ZeroJson(default) { decodeEnumsCaseInsensitive = true }

    @Test
    fun testCases() = parametrizedTest { 
        val input =
            """{"cases":["ALL_CAPS","all_caps","mixed","MIXED","miXed","all_lower","ALL_LOWER","all_Lower","hasAltNames","HASALTNAMES","altname","ALTNAME","AltName","SERIAL_NAME","serial_name"]}"""
        val target = listOf(
            Cases.ALL_CAPS,
            Cases.ALL_CAPS,
            Cases.MiXed,
            Cases.MiXed,
            Cases.MiXed,
            Cases.all_lower,
            Cases.all_lower,
            Cases.all_lower,
            Cases.hasAltNames,
            Cases.hasAltNames,
            Cases.hasAltNames,
            Cases.hasAltNames,
            Cases.hasAltNames,
            Cases.hasSerialName,
            Cases.hasSerialName
        )
        val decoded = json.decodeFromStringTest<EnumCases>(input)
        assertEquals(EnumCases(target), decoded)
        val encoded = json.encodeToStringTest(decoded)
        assertEquals(
            """{"cases":["ALL_CAPS","ALL_CAPS","MiXed","MiXed","MiXed","all_lower","all_lower","all_lower","hasAltNames","hasAltNames","hasAltNames","hasAltNames","hasAltNames","SERIAL_NAME","SERIAL_NAME"]}""",
            encoded
        )
    }

    @Test
    fun testTopLevelList() = parametrizedTest { 
        val input = """["all_caps","serial_name"]"""
        val decoded = json.decodeFromStringTest<List<Cases>>(input)
        assertEquals(listOf(Cases.ALL_CAPS, Cases.hasSerialName), decoded)
        assertEquals("""["ALL_CAPS","SERIAL_NAME"]""", json.encodeToStringTest(decoded))
    }

    @Test
    fun testTopLevelEnum() = parametrizedTest { 
        val input = """"altName""""
        val decoded = json.decodeFromStringTest<Cases>(input)
        assertEquals(Cases.hasAltNames, decoded)
        assertEquals(""""hasAltNames"""", json.encodeToStringTest(decoded))
    }

    @Test
    fun testSimpleCase() = parametrizedTest { 
        val input = """{"one":"baz","two":"Qux","three":"QUX"}"""
        val decoded = json.decodeFromStringTest<Foo>(input)
        assertEquals(Foo(), decoded)
        assertEquals("""{"one":"BAZ","two":"QUX","three":"QUX"}""", json.encodeToStringTest(decoded))
    }

    enum class E { VALUE_A, @JsonNames("ALTERNATIVE") VALUE_B }

    @Test
    fun testDocSample() {
        val j = ZeroJson { decodeEnumsCaseInsensitive = true }
        @Serializable
        data class Outer(val enums: List<E>)
        assertEquals(
            Outer(listOf(E.VALUE_A, E.VALUE_B)),
            j.decodeFromString<Outer>("""{"enums":["value_A", "alternative"]}""")
        )
    }

    private val withCoercing = ZeroJson(json) { coerceInputValues = true }

    @Test
    fun testCoercingStillWorks() = parametrizedTest {
        val input = """{"one":"baz","two":"unknown","three":"Que"}"""
        assertEquals(Foo(),  withCoercing.decodeFromStringTest<Foo>(input))
    }

    @Test
    fun testCaseInsensitivePriorityOverCoercing() = parametrizedTest {
        val input = """{"one":"QuX","two":"Baz","three":"Que"}"""
        assertEquals(Foo(Bar.QUX, Bar.BAZ, Bar.QUX), withCoercing.decodeFromStringTest<Foo>(input))
    }

    @Test
    fun testCoercingStillWorksWithNulls() = parametrizedTest {
        val input = """{"one":"baz","two":"null","three":null}"""
        assertEquals(Foo(),  withCoercing.decodeFromStringTest<Foo>(input))
    }

    @Test
    fun testFeatureDisablesProperly() = parametrizedTest { 
        val disabled = ZeroJson(json) {
            coerceInputValues = true
            decodeEnumsCaseInsensitive = false
        }
        val input = """{"one":"BAZ","two":"BAz","three":"baz"}""" // two and three should be coerced to QUX
        assertEquals(Foo(), disabled.decodeFromStringTest<Foo>(input))
    }

    @Test
    fun testFeatureDisabledThrowsWithoutCoercing() = parametrizedTest { 
        val disabled = ZeroJson(json) {
            coerceInputValues = false
            decodeEnumsCaseInsensitive = false
        }
        val input = """{"one":"BAZ","two":"BAz","three":"baz"}"""
        assertFailsWithMessage<SerializationException>("An unknown entry 'BAz'") {
            disabled.decodeFromStringTest<Foo>(input)
        }
    }

    @Serializable enum class BadEnum { Bad, BAD }

    @Serializable data class ListBadEnum(val l: List<BadEnum>)

    @Test
    fun testLowercaseClashThrowsException() = parametrizedTest {
        val expectedMessage = """Element with name 'BAD' appeared twice in class with serial name 'dev.dokky.zerojson.ktx.JsonEnumsCaseInsensitiveTest.BadEnum'"""
        assertFailsWithMessage<SerializationException>(expectedMessage) {
            json.decodeFromStringTest<Box<BadEnum>>("""{"boxed":"bad"}""")
        }
        assertFailsWithMessage<SerializationException>(expectedMessage) {
            json.decodeFromStringTest<Box<BadEnum>>("""{"boxed":"unrelated"}""")
        }
    }

    @Test
    fun testLowercaseClashHandledWithoutFeature() = parametrizedTest { 
        val disabled = ZeroJson(json) {
            coerceInputValues = false
            decodeEnumsCaseInsensitive = false
        }
        assertEquals(ListBadEnum(listOf(BadEnum.Bad, BadEnum.BAD)), disabled.decodeFromStringTest("""{"l":["Bad","BAD"]}"""))
    }
}
