package dev.dokky.zerojson.ktx

import kotlinx.serialization.Serializable
import kotlin.test.Test
import kotlin.test.assertEquals

class JsonUnionEnumTest : JsonTestBase() {

    enum class SomeEnum { ALPHA, BETA, GAMMA }

    @Serializable
    data class WithUnions(
        val s: String,
        val e: SomeEnum = SomeEnum.ALPHA,
        val i: Int = 42
    )

    @Test
    fun testEnum() = parametrizedTest {
        val data = WithUnions("foo", SomeEnum.BETA)
        val json = default.encodeToString(WithUnions.serializer(), data)
        val restored = default.decodeFromString(WithUnions.serializer(), json)
        assertEquals(data, restored)
    }
}
