package dev.dokky.zerojson

import dev.dokky.zerojson.framework.*
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonPrimitive
import kotlin.test.Test

class NullTest: RandomizedJsonTest() {
    @Test
    fun umbrella_object() {
        val obj = umbrellaInstance.copy(
            unitN = null,
            booleanN = null,
            byteN = null,
            shortN = null,
            intN = null,
            longN = null,
            floatN = null,
            doubleN = null,
            charN = null,
            stringN = null,
            enumN = null,
            intDataN = null,
            listNInt = null,
            listIntN = listOf(null, null, 789, null),
            listNIntN = null,
            listListEnumN = listOf(listOf(null, Attitude.NEGATIVE, null), listOf(), listOf(null, Attitude.POSITIVE, null)),
            listIntData = emptyList(),
            listIntDataN = mutableListOf(null, null, IntData(334), null),
            mapIntStringN = emptyMap(),
            mapStringInt = emptyMap(),
            tree = Tree("", left = null, right = null),
            arrays = null
        )

        randomizedTest {
            domainObject(obj)
            jsonElement {
                "unit" {}
                "boolean" eq obj.boolean
                "byte" eq obj.byte
                "short" eq obj.short
                "int" eq obj.int
                "long" eq obj.long
                "float" eq obj.float
                "double" eq obj.double
                "char" eq obj.char
                "string" eq obj.string
                "enum" eq obj.enum
                "intData" { "intV" eq obj.intData.intV }
                "unitN" eq null
                "booleanN" eq null
                "byteN" eq null
                "shortN" eq null
                "intN" eq null
                "longN" eq null
                "floatN" eq null
                "doubleN" eq null
                "charN" eq null
                "stringN" eq null
                "enumN" eq null
                "intDataN" eq null
                "listInt".numberArray(obj.listInt)
                "listIntN".numberArray(obj.listIntN)
                "listNInt" eq null
                "listNIntN" eq null
                "listListEnumN"(
                    JsonArray(listOf(JsonNull, JsonPrimitive("NEGATIVE"), JsonNull)),
                    JsonArray(emptyList()),
                    JsonArray(listOf(JsonNull, JsonPrimitive("POSITIVE"), JsonNull))
                )
                "listIntData".numberArray(emptyList())
                "listIntDataN"(null, null, jsonObject { "intV" eq obj.listIntDataN[2]?.intV }, null)
                "tree" {
                    "name" eq ""
                    "left" eq null
                    "right" eq null
                }
                "mapStringInt" noRandomKeys {}
                "mapIntStringN" noRandomKeys {}
                "arrays" eq null
            }
        }
    }
}