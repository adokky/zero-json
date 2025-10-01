package dev.dokky.zerojson.ktx

import dev.dokky.zerojson.ZeroJson
import dev.dokky.zerojson.framework.assertFailsWithMessage
import dev.dokky.zerojson.framework.trimMarginAndRemoveWhitespaces
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.JsonClassDiscriminator
import kotlinx.serialization.json.JsonNames
import kotlinx.serialization.json.JsonNamingStrategy
import kotlin.test.Test
import kotlin.test.assertEquals


class JsonNamingStrategyTest : JsonTestBase() {
    @Serializable
    private data class Foo(
        val simple: String = "a",
        val oneWord: String = "b",
        val already_in_snake: String = "c",
        val aLotOfWords: String = "d",
        val FirstCapitalized: String = "e",
        val hasAcronymURL: Bar = Bar.BAZ,
        val hasDigit123AndPostfix: Bar = Bar.QUX,
        val coercionTest: Bar = Bar.QUX
    )

    private enum class Bar { BAZ, QUX }

    private val jsonWithNaming = ZeroJson(default) {
        namingStrategy = JsonNamingStrategy.SnakeCase
        decodeEnumsCaseInsensitive = true // check that related feature does not break anything
    }

    @Test
    fun testJsonNamingStrategyWithAlternativeNames() = doTest(
        ZeroJson(jsonWithNaming) { useAlternativeNames = true }
    )

    @Test
    fun testJsonNamingStrategyWithoutAlternativeNames() = doTest(
        ZeroJson(jsonWithNaming) { useAlternativeNames = false }
    )

    private fun doTest(json: ZeroJson) {
        val foo = Foo()
        assertJsonFormAndRestored(
            Foo.serializer(),
            foo,
            """{"simple":"a",
                "one_word":"b",
                "already_in_snake":"c",
                "a_lot_of_words":"d",
                "first_capitalized":"e",
                "has_acronym_url":"BAZ",
                "has_digit123_and_postfix":"QUX",
                "coercion_test":"QUX"}""".trimMarginAndRemoveWhitespaces(),
            json
        )
    }

    @Test
    fun testNamingStrategyWorksWithCoercing() {
        val j = ZeroJson(jsonWithNaming) {
            coerceInputValues = true
            useAlternativeNames = false
        }
        assertEquals(
            Foo(),
            j.decodeFromString("""{
                "simple":"a",
                "one_word":"b",
                "already_in_snake":"c",
                "a_lot_of_words":"d",
                "first_capitalized":"e",
                "has_acronym_url":"baz",
                "has_digit123_and_postfix":"qux",
                "coercion_test":"invalid"}""".trimMarginAndRemoveWhitespaces())
        )
    }

    @Test
    fun testSnakeCaseStrategy() {
        fun apply(name: String) =
            JsonNamingStrategy.SnakeCase.serialNameForJson(String.serializer().descriptor, 0, name)

        val cases = mapOf<String, String>(
            "" to "",
            "_" to "_",
            "___" to "___",
            "a" to "a",
            "A" to "a",
            "_1" to "_1",
            "_a" to "_a",
            "_A" to "_a",
            "property" to "property",
            "twoWords" to "two_words",
            "threeDistinctWords" to "three_distinct_words",
            "ThreeDistinctWords" to "three_distinct_words",
            "Oneword" to "oneword",
            "camel_Case_Underscores" to "camel_case_underscores",
            "_many____underscores__" to "_many____underscores__",
            "URLmapping" to "ur_lmapping",
            "URLMapping" to "url_mapping",
            "IOStream" to "io_stream",
            "IOstream" to "i_ostream",
            "myIo2Stream" to "my_io2_stream",
            "myIO2Stream" to "my_io2_stream",
            "myIO2stream" to "my_io2stream",
            "myIO2streamMax" to "my_io2stream_max",
            "InURLBetween" to "in_url_between",
            "myHTTP2APIKey" to "my_http2_api_key",
            "myHTTP2fastApiKey" to "my_http2fast_api_key",
            "myHTTP23APIKey" to "my_http23_api_key",
            "myHttp23ApiKey" to "my_http23_api_key",
            "theWWW" to "the_www",
            "theWWW_URL_xxx" to "the_www_url_xxx",
            "hasDigit123AndPostfix" to "has_digit123_and_postfix"
        )

        cases.forEach { (input, expected) ->
            assertEquals(expected, apply(input))
        }
    }

    @Test
    fun testKebabCaseStrategy() {
        fun apply(name: String) =
            JsonNamingStrategy.KebabCase.serialNameForJson(String.serializer().descriptor, 0, name)

        val cases = mapOf<String, String>(
            "" to "",
            "_" to "_",
            "-" to "-",
            "___" to "___",
            "---" to "---",
            "a" to "a",
            "A" to "a",
            "-1" to "-1",
            "-a" to "-a",
            "-A" to "-a",
            "property" to "property",
            "twoWords" to "two-words",
            "threeDistinctWords" to "three-distinct-words",
            "ThreeDistinctWords" to "three-distinct-words",
            "Oneword" to "oneword",
            "camel-Case-WithDashes" to "camel-case-with-dashes",
            "_many----dashes--" to "_many----dashes--",
            "URLmapping" to "ur-lmapping",
            "URLMapping" to "url-mapping",
            "IOStream" to "io-stream",
            "IOstream" to "i-ostream",
            "myIo2Stream" to "my-io2-stream",
            "myIO2Stream" to "my-io2-stream",
            "myIO2stream" to "my-io2stream",
            "myIO2streamMax" to "my-io2stream-max",
            "InURLBetween" to "in-url-between",
            "myHTTP2APIKey" to "my-http2-api-key",
            "myHTTP2fastApiKey" to "my-http2fast-api-key",
            "myHTTP23APIKey" to "my-http23-api-key",
            "myHttp23ApiKey" to "my-http23-api-key",
            "theWWW" to "the-www",
            "theWWW-URL-xxx" to "the-www-url-xxx",
            "hasDigit123AndPostfix" to "has-digit123-and-postfix"
        )

        cases.forEach { (input, expected) ->
            assertEquals(expected, apply(input))
        }
    }

    @Serializable
    private data class DontUseOriginal(val testCase: String)

    @Test
    fun testNamingStrategyOverridesOriginal() {
        val json = ZeroJson(jsonWithNaming) { ignoreUnknownKeys = true }
        parametrizedTest {
            assertEquals(DontUseOriginal("a"), json.decodeFromStringTest("""{"test_case":"a","testCase":"b"}"""))
        }

        val jsonThrows = ZeroJson(jsonWithNaming) { ignoreUnknownKeys = false }
        parametrizedTest {
            assertFailsWithMessage<SerializationException>("Encountered an unknown key 'testCase'") {
                jsonThrows.decodeFromStringTest<DontUseOriginal>("""{"test_case":"a","testCase":"b"}""")
            }
        }
    }

    @Serializable
    private data class CollisionCheckPrimary(val testCase: String, val test_case: String)

    @Serializable
    private data class CollisionCheckAlternate(val testCase: String, @JsonNames("test_case") val testCase2: String)

    @Test
    fun testNamingStrategyPrioritizesOverAlternative() {
        val json = ZeroJson(jsonWithNaming) { ignoreUnknownKeys = true }
        parametrizedTest {
            assertFailsWithMessage<SerializationException>(
                "Element with name 'test_case' appeared twice in class with serial name 'dev.dokky.zerojson.ktx.JsonNamingStrategyTest.CollisionCheckPrimary'"
            ) {
                json.decodeFromStringTest<CollisionCheckPrimary>("""{"test_case":"a"}""")
            }
        }
        parametrizedTest {
            assertFailsWithMessage<SerializationException>(
                "Element with name 'test_case' appeared twice in class with serial name 'dev.dokky.zerojson.ktx.JsonNamingStrategyTest.CollisionCheckAlternate'"
            ) {
                json.decodeFromStringTest<CollisionCheckAlternate>("""{"test_case":"a"}""")
            }
        }
    }


    @Serializable
    private data class OriginalAsFallback(@JsonNames("testCase") val testCase: String)

    @Test
    fun testCanUseOriginalNameAsAlternative() {
        val json = ZeroJson(jsonWithNaming) { ignoreUnknownKeys = true }
        parametrizedTest {
            assertEquals(OriginalAsFallback("b"), json.decodeFromStringTest("""{"testCase":"b"}"""))
        }
    }

    @Serializable
    private sealed interface SealedBase {
        @Serializable
        @JsonClassDiscriminator("typeSub")
        sealed class SealedMid : SealedBase {
            @Serializable
            @SerialName("SealedSub1")
            object SealedSub1 : SealedMid()
        }

        @Serializable
        @SerialName("SealedSub2")
        data class SealedSub2(val testCase: Int = 0) : SealedBase
    }

    @Serializable
    private data class Holder(val testBase: SealedBase, val testMid: SealedBase.SealedMid)

    @Test
    fun testNamingStrategyDoesNotAffectPolymorphism() {
        val json = ZeroJson(jsonWithNaming) { classDiscriminator = "typeBase" }
        val holder = Holder(SealedBase.SealedSub2(), SealedBase.SealedMid.SealedSub1)
        assertJsonFormAndRestored(
            Holder.serializer(),
            holder,
            """{"test_base":{"typeBase":"SealedSub2","test_case":0},"test_mid":{"typeSub":"SealedSub1"}}""",
            json
        )
    }
}
