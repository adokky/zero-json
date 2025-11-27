package kotlinx.serialization.features

import kotlinx.serialization.SerialInfo
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonNamingStrategy
import kotlinx.serialization.json.JsonTestBase
import kotlin.test.Ignore
import kotlin.test.Test

/**
 * From [JsonNamingStrategy] description:
 * ```text
 * If the original serial name is present in the Json input but transformed is not, MissingFieldException still would be thrown.
 * ```
 *
 * [JsonNamingStrategyExclusionTest.E.SECOND_E] is tranformed to `second_e` and [kotlinx.serialization.json.JsonBuilder.coerceInputValues] is `false`.
 *
 * Both tests should fail because of the input `"enum_bar_two":"SECOND_E"`.
 *
 * Correct version of this test can be found in `core/commonTest/ktx`.
 */
@Ignore // kotlinx bug
class JsonNamingStrategyExclusionTest : JsonTestBase() {
    @SerialInfo
    @Target(AnnotationTarget.CLASS, AnnotationTarget.PROPERTY)
    annotation class OriginalSerialName

    private fun List<Annotation>.hasOriginal() = filterIsInstance<OriginalSerialName>().isNotEmpty()

    private val myStrategy = JsonNamingStrategy { descriptor, index, serialName ->
        if (descriptor.annotations.hasOriginal() || descriptor.getElementAnnotations(index).hasOriginal()) serialName
        else JsonNamingStrategy.SnakeCase.serialNameForJson(descriptor, index, serialName)
    }

    @Serializable
    @OriginalSerialName
    data class Foo(val firstArg: String = "a", val secondArg: String = "b")

    enum class E {
        @OriginalSerialName
        FIRST_E,
        SECOND_E
    }

    @Serializable
    data class Bar(
        val firstBar: String = "a",
        @OriginalSerialName val secondBar: String = "b",
        val fooBar: Foo = Foo(),
        val enumBarOne: E = E.FIRST_E,
        val enumBarTwo: E = E.SECOND_E
    )

    private fun doTest(json: Json) {
        val j = Json(json) {
            namingStrategy = myStrategy
        }
        val bar = Bar()
        assertJsonFormAndRestored(
            Bar.serializer(),
            bar,
            """{"first_bar":"a","secondBar":"b","foo_bar":{"firstArg":"a","secondArg":"b"},"enum_bar_one":"FIRST_E","enum_bar_two":"SECOND_E"}""",
            j
        )
    }

    @Test
    fun testJsonNamingStrategyWithAlternativeNames() = doTest(Json(default) {
        useAlternativeNames = true
    })

    @Test
    fun testJsonNamingStrategyWithoutAlternativeNames() = doTest(Json(default) {
        useAlternativeNames = false
    })
}
