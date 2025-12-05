package dev.dokky.zerojson

import kotlinx.serialization.encodeToString
import kotlin.test.assertEquals

abstract class EncoderTest(protected val json: ZeroJson = TestZeroJson) {
    protected inline fun <reified T> test(expectedJson: String, value: T) {
        assertEquals(expectedJson, json.encodeToString<T>(value))
    }
}