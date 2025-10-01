package dev.dokky.zerojson.framework

import kotlinx.serialization.json.*
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class JsonObjectKeyOrderTest {
    @Test
    fun primitives() {
        assertFalse(JsonPrimitive("ab").orderSensitiveEquals(JsonPrimitive("ac")))
        assertFalse(JsonPrimitive(123).orderSensitiveEquals(JsonPrimitive(124)))
        assertFalse(JsonPrimitive(123).orderSensitiveEquals(JsonPrimitive("123")))
        assertTrue(JsonPrimitive(123.0).orderSensitiveEquals(JsonUnquotedLiteral("123.00")))
    }

    @Test
    fun equal_maps() {
        val o1 = jsonObject {
            repeat(100) { it.toString() eq it }
        }
        val o2 = JsonObject(buildMap {
            repeat(100) { put(it.toString(), JsonPrimitive(it)) }
        })

        val e1: JsonElement = o1
        val e2: JsonElement = o2

        assertEquals(o1, o2)
        assertTrue(o1.orderSensitiveEquals(o2))
        assertTrue(e1.orderSensitiveEquals(e2))
        assertTrue(o1.elementsEquals(o2))
    }

    @Test
    fun different_order() {
        val o1 = jsonObject {
            for(i in 100 downTo 1) { i.toString() eq i }
        }
        val o2 = JsonObject(buildMap {
            for (i in 1 .. 100) {
                put(i.toString(), JsonPrimitive(i))
            }
        })

        val e1: JsonElement = o1
        val e2: JsonElement = o2

        assertEquals(o1, o2)
        assertFalse(o1.orderSensitiveEquals(o2))
        assertFalse(e1.orderSensitiveEquals(e2))
        assertFalse(o1.elementsEquals(o2))
    }

    @Test
    fun different_order_wrapped() {
        val o1 = jsonObject {
            for(i in 100 downTo 1) { i.toString() eq i }
        }
        val o2 = JsonObject(buildMap {
            for (i in 1 .. 100) {
                put(i.toString(), JsonPrimitive(i))
            }
        })

        fun JsonElement.wrap(): JsonElement = jsonObject {
            "k1" eq "v1"
            "k2" {
                "k" array {
                    add(JsonPrimitive(1))
                    add(JsonPrimitive(2))
                    add(JsonArray(listOf(this@wrap)))
                    add(JsonPrimitive(4))
                }
            }
        }

        val e1 = o1.wrap()
        val e2 = o2.wrap()

        assertEquals(e1, e2)
        assertFalse(e1.orderSensitiveEquals(e2))
        assertFalse(e1.jsonObject.orderSensitiveEquals(e2.jsonObject))
        assertFalse(o1.elementsEquals(o2))
    }
}