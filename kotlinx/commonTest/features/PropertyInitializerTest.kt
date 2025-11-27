@file:Suppress("MayBeConstant")

package kotlinx.serialization.features

import kotlinx.serialization.Serializable
import kotlinx.serialization.Serializer
import kotlinx.serialization.Transient
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlin.test.Ignore
import kotlin.test.Test
import kotlin.test.assertEquals

internal val globalVar: Int = 4

internal fun globalFun(): Int = 7

internal val PROPERTY_INITIALIZER_JSON = buildJsonObject {
    put("valProperty", 1)
    put("varProperty", 2)
    put("literalConst", 3)
    put("globalVarRef", 4)
    put("computed", 5)
    put("doubleRef", 6)
    put("globalFun", 7)
    put("globalFunExpr", 8)
    put("itExpr", 9)
    put("transientRefFromProp" ,10)
    put("bodyProp", 11)
    put("dependBodyProp" ,12)
    put("getterDepend", 13)
}.toString()

@Ignore // https://github.com/Kotlin/kotlinx.serialization/issues/2549
@Suppress("MemberVisibilityCanBePrivate", "unused", "ComplexRedundantLet")
class PropertyInitializerTest {
    @Serializable
    data class InternalClass(
        val valProperty: Int,
        var varProperty: Int,
        val literalConst: Int = 3,
        val globalVarRef: Int = globalVar,
        val computed: Int = valProperty + varProperty + 2,
        val doubleRef: Int = literalConst + literalConst,
        var globalFun: Int = globalFun(),
        var globalFunExpr: Int = globalFun() + 1,
        val itExpr: Int = literalConst.let { it + 6 },
        @Transient val constTransient: Int = 6,
        @Transient val serializedRefTransient: Int = varProperty + 1,
        @Transient val refTransient: Int = serializedRefTransient,
        val transientRefFromProp: Int = constTransient + 4,
    ) {
        val valGetter: Int get() { return 5 }
        var bodyProp: Int = 11
        var dependBodyProp: Int = bodyProp + 1
        var getterDepend: Int = valGetter + 8
    }

    private val format = Json { encodeDefaults = true }

    data class ExternalClass(
        val valProperty: Int,
        var varProperty: Int,
        val literalConst: Int = 3,
        val globalVarRef: Int = globalVar,
        val computed: Int = valProperty + varProperty + 2,
        val doubleRef: Int = literalConst + literalConst,
        var globalFun: Int = globalFun(),
        var globalFunExpr: Int = globalFun() + 1,
        val itExpr: Int = literalConst.let { it + 6 },
        @Transient val constTransient: Int = 6,
        @Transient val serializedRefTransient: Int = varProperty + 1,
        @Transient val refTransient: Int = serializedRefTransient,
        val transientRefFromProp: Int = constTransient + 4,
    ) {
        val valGetter: Int get() { return 5 }
        var bodyProp: Int = 11
        var dependBodyProp: Int = bodyProp + 1
        var getterDepend: Int = valGetter + 8
    }

    @Serializer(ExternalClass::class)
    object ExternalSerializer

    @Test
    fun testInternalSerializeDefault() {
        val encoded = format.encodeToString(InternalClass(1, 2))
        assertEquals(PROPERTY_INITIALIZER_JSON, encoded)
    }

    @Test
    fun testInternalDeserializeDefault() {
        val decoded = format.decodeFromString<InternalClass>("""{"valProperty": 5, "varProperty": 6}""")
        assertEquals(InternalClass(5, 6), decoded)
    }

    @Test
    fun testExternalSerializeDefault() {
        val encoded = format.encodeToString(ExternalSerializer, ExternalClass(1, 2))
        assertEquals(PROPERTY_INITIALIZER_JSON, encoded)
    }

    @Test
    fun testExternalDeserializeDefault() {
        val decoded = format.decodeFromString(ExternalSerializer,"""{"valProperty": 5, "varProperty": 6}""")
        assertEquals(ExternalClass(5, 6), decoded)
    }
}
