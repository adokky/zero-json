package dev.dokky.zerojson.framework

import dev.dokky.zerojson.TestZeroJson
import dev.dokky.zerojson.framework.transformers.RandomOrderInputTransformer
import karamel.utils.unsafeCast
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.*
import kotlin.test.*

class RandomOrderInputTransformerTest {
    private val input = TestInputImpl(
        TestZeroJson,
        Unit,
        Unit.serializer().unsafeCast(),
        JsonNull,
        BaseJsonComposerConfig(),
        DefaultJsonComposer(StringBuilder())
    )

    private val objects: List<JsonObject> = listOf(
        jsonObject {
            "key" {
                "x1" {
                    "y1" eq "z1"
                    "y2" eq "z2"
                }
                "x2" eq "y2"
            }
        },
        jsonObject {
            "key" { }
            "arr" array {
                add(1)
                add(2)
                add(3)
                jsonObject {
                    "x1" {
                        "y1" {
                            "z1" {
                                "y1" eq 1
                                "y2" charArray emptyList()
                            }
                        }
                    }
                }
            }
        },
    )

    @Test
    fun ok() {
        repeat(100_000) {
            val initial = objects.random()

            input.clear()
            input.jsonElement = initial

            RandomOrderInputTransformer.transform(input)

            val new = input.jsonElement
            assertEquals(initial, new)
            assertNotSame(initial, new)
            assertFalse(initial.orderSensitiveEquals(new))
        }
    }

    @Test
    fun ok2() {
        val arr = jsonObject {
            "k1" eq buildJsonArray {
                add(1)
                add(2)
                add(3)
                add(jsonObject {
                    "k1" eq 1
                    "k2" eq 2
                })
            }
        }

        repeat(10_000) {
            input.clear()
            input.jsonElement = arr

            RandomOrderInputTransformer.transform(input)

            val new = input.jsonElement
            assertEquals(arr, new)
            assertFalse(arr.orderSensitiveEquals(new), new.toString())
        }
    }

    @Test
    fun failure() {
        repeat(100_000) {
            val initial = jsonObject {
                "k" {
                    "k" array {
                        jsonObject { "k" eq "v" }
                        jsonObject {  }
                    }
                }
            }

            input.clear()
            input.jsonElement = initial

            RandomOrderInputTransformer.transform(input)

            val new = input.jsonElement
            assertEquals(initial, new)
            assertTrue(initial.orderSensitiveEquals(new))
        }
    }

    @Test
    fun primitives() {
        for (p in listOf<JsonElement>(JsonNull, JsonPrimitive(1), JsonPrimitive("string"))) {
            repeat(100) {
                input.clear()
                input.jsonElement = p

                RandomOrderInputTransformer.transform(input)

                val new = input.jsonElement
                assertEquals(p, new)
                assertTrue(p.orderSensitiveEquals(new))
            }
        }
    }

    @Test
    fun array_quality() {
        val a1 = buildJsonArray {
            add(1)
            add(2)
            add(3)
        }
        assertTrue(a1.orderSensitiveEquals(buildJsonArray {
            add(1)
            add(2)
            add(3)
        }))
        assertFalse(a1.orderSensitiveEquals(buildJsonArray {
            add(2)
            add(1)
            add(3)
        }))
    }

    @Test
    fun array_is_untouched() {
        val a1 = buildJsonArray {
            add(1)
            add(2)
            add(3)
        }

        val a2 = jsonObject {
            "k" eq buildJsonArray {
                add(1)
                add(2)
                add(3)
            }
        }

        val a3 = jsonObject {
            "k1" eq buildJsonArray {
                add(1)
                add(2)
                add(3)
                add(jsonObject { "k1" eq 1 })
            }
        }

        val a4 = buildJsonArray {
            add(buildJsonArray { add(1); add(2); add(3) })
            add(buildJsonArray { add(4); add(5); add(6) })
            add(buildJsonArray { add(7); add(8); add(9) })
        }

        for (arr in listOf(a1, a2, a3, a4)) repeat(10_000) {
            input.clear()
            input.jsonElement = arr

            RandomOrderInputTransformer.transform(input)

            val new = input.jsonElement
            assertEquals(arr, new)
            assertTrue(arr.orderSensitiveEquals(new), new.toString())
        }
    }
}