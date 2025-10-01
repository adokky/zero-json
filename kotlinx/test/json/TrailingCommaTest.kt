/*
 * Copyright 2017-2023 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package kotlinx.serialization.json

import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.test.assertFailsWithMessage
import kotlinx.serialization.test.checkSerializationException
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals

class TrailingCommaTest : JsonTestBase() {
    val tj = Json { allowTrailingComma = true }

    @Serializable
    data class Optional(val data: String = "")

    @Serializable
    data class MultipleFields(val a: String, val b: String, val c: String)

    private val multipleFields = MultipleFields("1", "2", "3")

    @Serializable
    data class WithMap(val m: Map<String, String>)

    private val withMap = WithMap(mapOf("a" to "1", "b" to "2", "c" to "3"))

    @Serializable
    data class WithList(val l: List<Int>)

    private val withList = WithList(listOf(1, 2, 3))

    @Test
    fun basic() = parametrizedTest { mode ->
        val sd = """{"data":"str",}"""
        assertEquals(Optional("str"), tj.decodeFromString<Optional>(sd, mode))
    }

    @Test
    fun trailingCommaNotAllowedByDefaultForObjects() = parametrizedTest { mode ->
        val sd = """{"data":"str",}"""
        checkSerializationException({
            default.decodeFromString<Optional>(sd, mode)
        }, { message ->
            assertContains(
                message,
                """trailing comma"""
            )
        })
    }

    @Test
    fun trailingCommaNotAllowedByDefaultForLists() = parametrizedTest { mode ->
        val sd = """{"l":[1,]}"""
        checkSerializationException({
            default.decodeFromString<WithList>(sd, mode)
        }, { message ->
            assertContains(
                message,
                """trailing comma"""
            )
        })
    }

    @Test
    fun trailingCommaNotAllowedByDefaultForMaps() = parametrizedTest { mode ->
        val sd = """{"m":{"a": "b",}}"""
        checkSerializationException({
            default.decodeFromString<WithMap>(sd, mode)
        }, { message ->
            assertContains(
                message,
                """trailing comma"""
            )
        })
    }

    @Test
    fun emptyObjectNotAllowed() = parametrizedTest { mode ->
        assertFailsWithMessage<SerializationException>("expected string") {
            tj.decodeFromString<Optional>("""{,}""", mode)
        }
    }

    @Test
    fun emptyListNotAllowed() = parametrizedTest { mode ->
        assertFailsWithMessage<SerializationException>("expected") {
            tj.decodeFromString<WithList>("""{"l":[,]}""", mode)
        }
    }

    @Test
    fun emptyMapNotAllowed() = parametrizedTest { mode ->
        assertFailsWithMessage<SerializationException>("expected string") {
            tj.decodeFromString<WithMap>("""{"m":{,}}""", mode)
        }
    }

    @Test
    fun testMultipleFields() = parametrizedTest { mode ->
        val input = """{"a":"1","b":"2","c":"3", }"""
        assertEquals(multipleFields, tj.decodeFromString(input, mode))
    }

    @Test
    fun testWithMap() = parametrizedTest { mode ->
        val input = """{"m":{"a":"1","b":"2","c":"3", }}"""

        assertEquals(withMap, tj.decodeFromString(input, mode))
    }

    @Test
    fun testWithList() = parametrizedTest { mode ->
        val input = """{"l":[1, 2, 3, ]}"""
        assertEquals(withList, tj.decodeFromString(input, mode))
    }

    @Serializable
    data class Mixed(val mf: MultipleFields, val wm: WithMap, val wl: WithList)

    @Test
    fun testMixed() = parametrizedTest { mode ->
        //language=JSON5
        val input = """{"mf":{"a":"1","b":"2","c":"3",},
            "wm":{"m":{"a":"1","b":"2","c":"3",},},
            "wl":{"l":[1, 2, 3,],},}"""
        assertEquals(Mixed(multipleFields, withMap, withList), tj.decodeFromString(input, mode))
    }
}
