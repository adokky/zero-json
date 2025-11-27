/*
 * Copyright 2017-2022 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package kotlinx.serialization

import dev.dokky.zerojson.DecodingException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonTestBase
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs

class JsonPathTest : JsonTestBase() {

    @Serializable
    class Outer(val a: Int, val i: Inner)

    @Serializable
    class Inner(val a: Int, val b: String, val c: List<String>, val d: Map<Int, Box>)

    @Serializable
    class Box(val s: String)

    @Test
    fun testBasicError() {
        expectPath("$.a") { Json.decodeFromString<Outer>("""{"a":foo}""") }
        expectPath("$.i") { Json.decodeFromString<Outer>("""{"a":42, "i":[]}""") }
        expectPath("$.i.b") { Json.decodeFromString<Outer>("""{"a":42, "i":{"a":43, "b":42}""") }
        expectPath("$.i.b") { Json.decodeFromString<Outer>("""{"a":42, "i":{"b":42}""") }
    }

    @Test
    fun testMissingKey() {
        val ex = assertFailsWith<SerializationException> {
            Json.decodeFromString<Outer>("""{"a":42, "i":{"d":{1:{}}""")
        }
        assertIs<MissingFieldException>(ex)
        assertContains(ex.message!!, "$.i.d['1']")
    }

    @Test
    fun testUnknownKeyIsProperlyReported() {
        expectPath("$.i.foo") { Json.decodeFromString<Outer>("""{"a":42, "i":{"foo":42}""") }
        expectPath("$.x") { Json.decodeFromString<Outer>("""{"x":{}, "a": 42}""") }
        // The only place we have misattribution in
        // Json.decodeFromString<Outer>("""{"a":42, "x":{}}""")
    }

    @Test
    fun testMalformedRootObject() {
        expectPath("$") { Json.decodeFromString<Outer>("""{{""") }
    }

    @Test
    fun testArrayIndex() {
        expectPath("$.i.c[1]") { Json.decodeFromString<Outer>("""{"a":42, "i":{ "c": ["a", 2]}""") }
        expectPath("$[2]") { Json.decodeFromString<List<String>>("""["a", "2", 3]""") }
    }

    @Test
    fun testArrayIndexMalformedArray() {
        // Also zeroes as we cannot distinguish what exactly wen wrong is such cases
        expectPath("$.i.c[0]") { Json.decodeFromString<Outer>("""{"a":42, "i":{ "c": [[""") }
        expectPath("$[0]") { Json.decodeFromString<List<String>>("""[[""") }
        // But we can here
        expectPath("$.i.c") { Json.decodeFromString<Outer>("""{"a":42, "i":{ "c": {}}}""") }
        expectPath("$") { Json.decodeFromString<List<String>>("""{""") }
    }

    @Test
    fun testMapKey() {
        expectPath("$.i.d") { Json.decodeFromString<Outer>("""{"a":42, "i":{ "d": {"foo": {}}""") }
        expectPath("$.i.d") { Json.decodeFromString<Outer>("""{"a":42, "i":{ "d": {42: {"s":"s"}, 42.1:{}}""") }
        expectPath("$") { Json.decodeFromString<Map<Int, String>>("""{"foo":"bar"}""") }
        expectPath("$") { Json.decodeFromString<Map<Int, String>>("""{42:"bar", "foo":"bar"}""") }
        expectPath("$['42'].foo") { Json.decodeFromString<Map<Int, Map<String, Int>>>("""{42: {"foo":"bar"}""") }
    }

    @Test
    fun testMalformedMap() {
        expectPath("$.i.d") { Json.decodeFromString<Outer>("""{"a":42, "i":{ "d": []""") }
        expectPath("$") { Json.decodeFromString<Map<Int, String>>("""[]""") }
    }

    @Test
    fun testMapValue() {
        expectPath("$.i.d['42'].xx") { Json.decodeFromString<Outer>("""{"a":42, "i":{ "d": {42: {"xx":"bar"}}""") }
        expectPath("$.i.d['43'].xx") { Json.decodeFromString<Outer>("""{"a":42, "i":{ "d": {42: {"s":"s"}, 43: {"xx":"bar"}}}""") }
        expectPath("$['239']") { Json.decodeFromString<Map<Int, String>>("""{239:bar}""") }
    }

    @Serializable
    class Fp(val d: Double)

    @Test
    fun testInvalidFp() {
        expectPath("$.d") { Json.decodeFromString<Fp>("""{"d": NaN}""") }
    }

    @Serializable
    class EH(val e: E)
    enum class E

    @Test
    fun testUnknownEnum() {
        expectPath("$.e") { Json.decodeFromString<EH>("""{"e": "foo"}""") }
    }

    @Serializable
    data class SimpleNested(val n: SimpleNested? = null, val t: DataObject? = null)

    @Serializable
    data object DataObject

    @Test
    fun testMalformedDataObjectInDeeplyNestedStructure() {
        var outer = SimpleNested(t = DataObject)
        repeat(20) {
            outer = SimpleNested(n = outer)
        }
        val str = Json.encodeToString(SimpleNested.serializer(), outer)
        // throw-away data
        Json.decodeFromString(SimpleNested.serializer(), str)

        val malformed = str.replace("{}", "42")
        val expectedPath = "$" + ".n".repeat(20) + ".t"
        expectPath(expectedPath) { Json.decodeFromString(SimpleNested.serializer(), malformed) }
    }

    private inline fun expectPath(path: String, block: () -> Unit) {
        val ex = assertFailsWith<SerializationException> { block() }
        assertIs<DecodingException>(ex)
        assertEquals(path, ex.path, message = ex.stackTraceToString())
    }
}
