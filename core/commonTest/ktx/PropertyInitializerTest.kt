@file:Suppress("MayBeConstant")

package dev.dokky.zerojson.ktx

import dev.dokky.zerojson.ZeroJson
import kotlinx.serialization.*
import kotlin.test.Test
import kotlin.test.assertEquals

internal val globalVar: Int = 4

internal fun globalFun(): Int = 7

internal val PROPERTY_INITIALIZER_JSON = """{
    "valProperty": 1,
    "varProperty": 2,
    "literalConst": 3,
    "globalVarRef": 4,
    "computed": 5,
    "doubleRef": 6,
    "globalFun": 7,
    "globalFunExpr": 8,
    "itExpr": 9,
    "transientRefFromProp": 10,
    "bodyProp": 11,
    "dependBodyProp": 12,
    "getterDepend": 13
}""".filterNot { it.isWhitespace() }

@Suppress("MemberVisibilityCanBePrivate", "unused", "ComplexRedundantLet")
class PropertyInitializerTest {
    @Serializable
    private data class InternalClass(
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

    private val format = ZeroJson { encodeDefaults = true }

    private data class ExternalClass(
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
    private object ExternalSerializer

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
}
