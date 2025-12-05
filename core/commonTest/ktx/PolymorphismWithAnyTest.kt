package dev.dokky.zerojson.ktx

import dev.dokky.zerojson.IntData
import dev.dokky.zerojson.ZeroJson
import kotlinx.serialization.Polymorphic
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.plus
import kotlinx.serialization.modules.polymorphic
import kotlinx.serialization.modules.subclass
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class PolymorphismWithAnyTest: JsonTestBase() {
    @Serializable
    private data class MyPolyData(val data: Map<String, @Polymorphic Any>)

    @Serializable
    private data class MyPolyDataWithPolyBase(
        val data: Map<String, @Polymorphic Any>,
        @Polymorphic val polyBase: PolyBase
    )

    // KClass.toString() on JS prints simple name, not FQ one
    @Suppress("NAME_SHADOWING")
    private fun checkNotRegisteredMessage(className: String, scopeName: String, exception: SerializationException) {
        val expectedText = "Serializer for subclass '$className' is not found in the polymorphic scope of '$scopeName'"
        assertTrue(exception.message!!.startsWith(expectedText),
            "Found $exception, but expected to start with: $expectedText")
    }

    @Test
    fun testFailWithoutModulesWithCustomClass() = parametrizedTest {
        checkNotRegisteredMessage(
            "dev.dokky.zerojson.IntData", "Any",
            assertFailsWith<SerializationException>("not registered") {
                ZeroJson.encodeToString(
                    MyPolyData.serializer(),
                    MyPolyData(mapOf("a" to IntData(42)))
                )
            }
        )
    }

    @Test
    fun testWithModules() {
        val json = ZeroJson {
            serializersModule = SerializersModule { polymorphic(Any::class) { subclass(IntData.serializer()) } }
        }
        assertJsonFormAndRestored(
            expected = """{"data":{"a":{"type":"dev.dokky.zerojson.IntData","intV":42}}}""",
            data = MyPolyData(mapOf("a" to IntData(42))),
            serializer = MyPolyData.serializer(),
            json = json
        )
    }

    /**
     * This test should fail because PolyDerived registered in the scope of PolyBase, not kotlin.Any
     */
    @Test
    fun testFailWithModulesNotInAnyScope() = parametrizedTest {
        val json = ZeroJson { serializersModule = BaseAndDerivedModule }
        checkNotRegisteredMessage(
            "dev.dokky.zerojson.ktx.PolyDerived", "Any",
            assertFailsWith<SerializationException> {
                json.encodeToString(
                    MyPolyData.serializer(),
                    MyPolyData(mapOf("a" to PolyDerived("foo")))
                )
            }
        )
    }

    private val baseAndDerivedModuleAtAny = SerializersModule {
        polymorphic(Any::class) {
            subclass(PolyDerived.serializer())
        }
    }

    @Test
    fun testRebindModules() {
        val json = ZeroJson { serializersModule = baseAndDerivedModuleAtAny }
        assertJsonFormAndRestored(
            expected = """{"data":{"a":{"type":"dev.dokky.zerojson.ktx.PolyDerived","id":1,"s":"foo"}}}""",
            data = MyPolyData(mapOf("a" to PolyDerived("foo"))),
            serializer = MyPolyData.serializer(),
            json = json
        )
    }

    /**
     * This test should fail because PolyDerived registered in the scope of kotlin.Any, not PolyBase
     */
    @Test
    fun testFailWithModulesNotInParticularScope() = parametrizedTest {
        val json = ZeroJson { serializersModule = baseAndDerivedModuleAtAny }
        checkNotRegisteredMessage(
            "dev.dokky.zerojson.ktx.PolyDerived", "dev.dokky.zerojson.ktx.PolyBase",
            assertFailsWith {
                json.encodeToString(
                    MyPolyDataWithPolyBase.serializer(),
                    MyPolyDataWithPolyBase(
                        mapOf("a" to PolyDerived("foo")),
                        PolyDerived("foo")
                    )
                )
            }
        )
    }

    @Test
    fun testBindModules() {
        val json = ZeroJson { serializersModule = (baseAndDerivedModuleAtAny + BaseAndDerivedModule) }
        assertJsonFormAndRestored(
            expected = """{"data":{"a":{"type":"dev.dokky.zerojson.ktx.PolyDerived","id":1,"s":"foo"}},
                |"polyBase":{"type":"dev.dokky.zerojson.ktx.PolyDerived","id":1,"s":"foo"}}""".trimMargin().lines().joinToString(
                ""
            ),
            data = MyPolyDataWithPolyBase(
                mapOf("a" to PolyDerived("foo")),
                PolyDerived("foo")
            ),
            serializer = MyPolyDataWithPolyBase.serializer(),
            json = json
        )
    }

    @Test
    fun testTypeKeyLastInInput() = parametrizedTest {
        val json = ZeroJson { serializersModule = (baseAndDerivedModuleAtAny + BaseAndDerivedModule) }
        val input = """{"data":{"a":{"id":1,"s":"foo","type":"dev.dokky.zerojson.ktx.PolyDerived"}},
                |"polyBase":{"id":1,"s":"foo","type":"dev.dokky.zerojson.ktx.PolyDerived"}}""".trimMargin().lines().joinToString(
            "")
        val data = MyPolyDataWithPolyBase(
            mapOf("a" to PolyDerived("foo")),
            PolyDerived("foo")
        )
        assertEquals(data, json.decodeFromString(MyPolyDataWithPolyBase.serializer(), input))
    }
}
