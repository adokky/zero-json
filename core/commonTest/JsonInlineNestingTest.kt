package dev.dokky.zerojson

import dev.dokky.zerojson.framework.*
import dev.dokky.zerojson.framework.transformers.RandomKeysInputTransformer
import kotlinx.serialization.Serializable
import kotlin.test.Test
import kotlin.test.assertEquals

class JsonInlineNestingTest: RandomizedJsonTest() {
    @Serializable
    data class R(@JsonInline val inlined1: I1 = I1(I2("y")))
    @Serializable
    data class I1(@JsonInline val inlined2: I2 = I2("z"))
    @Serializable
    data class I2(val str: String)

    @Test
    fun deep_optional_1() {
        randomizedTest {
            domainObject(R(I1(I2("x"))))
            jsonElement { "str" eq "x" }
            excludeTargetIf { !it.isDomain() }
            disable<RandomKeysInputTransformer>()
            iterations = 100
        }
    }

    @Serializable
    data class R2(@JsonInline val inlined1: I21?)
    @Serializable
    data class I21(@JsonInline val inlined2: I22 = I22("z"), val i1Int: Int = 456)
    @Serializable
    data class I22(val str: String)

    @Test
    fun deep_optional_2_1() {
        randomizedTest {
            domainObject(R2(I21(I22("x"))))
            jsonElement { "str" eq "x" }
            excludeTargetIf { !it.isDomain() }
            disable<RandomKeysInputTransformer>()
            iterations = 100
        }
    }

    @Test
    fun deep_optional_2_2() {
        randomizedTest {
            domainObject(R2(null))
            jsonElement {}
            excludeTargetIf { !it.isDomain() }
            disable<RandomKeysInputTransformer>()
            iterations = 100
        }
    }

    @Test
    fun deep_optional_2_3() {
        randomizedTest {
            domainObject(R2(I21(i1Int = 45)))
            jsonElement { "i1Int" eq 45 }
            excludeTargetIf { !it.isDomain() }
            disable<RandomKeysInputTransformer>()
            iterations = 100
        }
    }

    @Serializable
    data class RM(val n1: String?, @JsonInline val inlined1: IM1? = null, val n2: String? = null)
    @Serializable
    data class IM1(@JsonInline val inlined2: IM2 = IM2(null, null, mapOf("x" to "y")), val n3: String?, val i1Int: Int = 456)
    @Serializable
    data class IM2(val n4: String?,  val n5: String? = null, @JsonInline val map: Map<String, String>)

    @Test
    fun deep_optional_map_text() {
        assertDecodedEquals(
            """{"xxx":"yyy"}""",
            RM(null, IM1(inlined2 = IM2(null, null, mapOf("xxx" to "yyy")), null))
        )
    }

    @Test
    fun deep_optional_map_tree() {
        assertEquals(
            RM(null, IM1(inlined2 = IM2(null, null, mapOf("xxx" to "yyy")), null)),
            ZeroJson.decodeFromJsonElement<RM>(jsonObject { "xxx" eq "yyy" })
        )
    }
}