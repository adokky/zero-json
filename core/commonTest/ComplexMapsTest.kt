package dev.dokky.zerojson

import dev.dokky.zerojson.framework.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlin.test.Test
import kotlin.test.assertFailsWith

class ComplexMapsTest: RandomizedJsonTest() {
    @Suppress("unused")
    @Serializable
    private data class Maps(
        @JsonInline val inlined: Map<SimpleValueClass, Maps>? = null,
        val map1: Map<SimpleValueClass, SimpleDataClass>?,
        val map2: Map<SimpleValueInteger, CompoundDataClass>?,
        val nested: Map<Boolean, Maps>? = null,
        val polyMap: Map<Int, PolyInterface>? = null
    )

    private val data = Maps(
        inlined = mapOf(SimpleValueClass("ext1") to Maps(
            map1 = mapOf(),
            map2 = mapOf(SimpleValueInteger(1111) to CompoundDataClass("СТРОКА", 0)),
            polyMap = mapOf(45 to PolyInterface.Value1(343))
        )),
        map1 = mapOf(SimpleValueClass("k1") to SimpleDataClass("a string")),
        map2 = mapOf(SimpleValueInteger(6865) to CompoundDataClass("zzz", 9786)),
        nested = mapOf(
            true to Maps(
                map1 = mapOf(),
                map2 = mapOf(),
                polyMap = mapOf(
                    42 to PolyInterface.Concrete3(
                        PolymorphicBase2.SubClass1.Concrete2("324efh", 56464),
                        PolyInterface.SubClass1.Concrete1("some string")
                    )
                ),
                nested = mapOf(
                    false to Maps(
                        map1 = null,
                        map2 = mapOf()
                    )
                )
            )
        )
    )

    @Test
    fun fixture1() {
        assertFailsWithMessage<SerializationException>("expected object, got null") {
            assertDecoded(
                """
                {"^random_key_0":null,"ext1":{"map1":{},"map2":{"1111":{"string":"СТРОКА","int":0}},"^random_key_-1":-367407613,"polyMap":{"45":{"type":"dev.dokky.zerojson.PolyInterface.Value1","value":343}}},"map1":{"k1":{"key":"a string"}},"map2":{"6865":{"string":"zzz","int":9786}},"nested":{"^random_key_0":1968207482,"true":{"^random_key_0":null,"map1":{},"map2":{},"^random_key_-1":null,"polyMap":{"42":{"type":"dev.dokky.zerojson.PolyInterface.Concrete3","base":{"type":"dev.dokky.zerojson.PolymorphicBase2.SubClass1.Concrete2","string":"324efh","int":56464},"sub1":{"type":"MyConcrete","string":"some string"}}},"^random_key_-2":0.4424768656954756,"nested":{"false":{"map1":null,"map2":{}}}},"^random_key_1":{"^random^KEY^3320631038":"Random String 1751820872","^random^KEY^3033613950":true}},"^random_key_2":null,"^random_key_1":1385727558}
            """.trimIndent(),
                data
            )
        }
    }

    private fun expectedJsonObject(topRandomKeys: Boolean = false) = jsonObject(allowRandomKeys = false) {
        if (topRandomKeys) { // having at least one random key at top level
            "random_key" eq "BUZZZZ"
        }
        "ext1" {
            "map1" noRandomKeys { }
            "map2" noRandomKeys {
                "1111" {
                    "string" eq "СТРОКА"
                    "int" eq 0
                }
            }
            "polyMap" noRandomKeys {
                "45".polymorphic<PolyInterface.Value1> {
                    "value" eq 343
                }
            }
        }
        if (topRandomKeys) potentialRandomKey()
        "map1" noRandomKeys { "k1" { "key" eq "a string" } }
        if (topRandomKeys) potentialRandomKey()
        "map2" noRandomKeys {
            "6865" {
                "string" eq "zzz"
                "int" eq 9786
            }
        }
        if (topRandomKeys) potentialRandomKey()
        "nested" {
            "true" {
                "map1" noRandomKeys {}
                "map2" noRandomKeys {}
                "polyMap" noRandomKeys {
                    "42".polymorphic<PolyInterface.Concrete3> {
                        "base".polymorphic<PolymorphicBase2.SubClass1.Concrete2> {
                            "string" eq "324efh"
                            "int" eq 56464
                        }
                        "sub1".polymorphic<PolyInterface.SubClass1.Concrete1> {
                            "string" eq "some string"
                        }
                    }
                }
                "nested" noRandomKeys {
                    "false" {
                        "map1" eq null
                        "map2" {}
                    }
                }
            }
        }
        if (topRandomKeys) potentialRandomKey()
    }

    @Test
    fun fixture2() {
        repeat(100000) {
            val obj = jsonObject(allowRandomKeys = false) {
                "random_key" eq "BUZZZZ"
                "ext1" {
                    "map1" noRandomKeys { }
                    "map2" noRandomKeys {
                        "1111" {
                            "string" eq "СТРОКА"
                            "int" eq 0
                        }
                    }
                    "polyMap" noRandomKeys {
                        "45".polymorphic<PolyInterface.Value1> {
                            "value" eq 343
                        }
                    }
                }
                "RND1" eq generateRandomJsonElement()
                "map1" noRandomKeys { "k1" { "key" eq "a string" } }
                "RND2" eq generateRandomJsonElement()
                "map2" noRandomKeys {
                    "6865" {
                        "string" eq "zzz"
                        "int" eq 9786
                    }
                }
                "RND3" eq generateRandomJsonElement()
                "nested" {
                    "true" {
                        "map1" noRandomKeys {}
                        "map2" noRandomKeys {}
                        "polyMap" noRandomKeys {
                            "42".polymorphic<PolyInterface.Concrete3> {
                                "base".polymorphic<PolymorphicBase2.SubClass1.Concrete2> {
                                    "string" eq "324efh"
                                    "int" eq 56464
                                }
                                "sub1".polymorphic<PolyInterface.SubClass1.Concrete1> {
                                    "string" eq "some string"
                                }
                            }
                        }
                        "nested" noRandomKeys {
                            "false" {
                                "map1" eq null
                                "map2" {}
                            }
                        }
                    }
                }
                "RND4" eq generateRandomJsonElement()
            }
            assertFailsWith<ZeroJsonDecodingException> {
                TestZeroJson.decodeFromJsonElement(Maps.serializer(), obj)
            }
        }
    }

    @Test
    fun object_with_top_random_keys_should_fail() {
        randomizedTest {
            domainObject(data)
            jsonElement = expectedJsonObject(topRandomKeys = true)
            excludeTargetIf { it.input == TestTarget.DataType.Domain }
            expectFailure(TestTarget.entries.filter { it.output == TestTarget.DataType.Domain })
        }
    }

    @Test
    fun all_ok() {
        randomizedTest {
            domainObject(data)
            jsonElement = expectedJsonObject(topRandomKeys = false)
        }
    }
}