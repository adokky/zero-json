package dev.dokky.zerojson.ktx

import kotlinx.serialization.Serializable
import kotlin.test.Test
import kotlin.time.Duration
import kotlin.time.DurationUnit
import kotlin.time.toDuration

class DurationTest : JsonTestBase() {
    @Serializable
    private data class DurationHolder(val duration: Duration)
    @Test
    fun testDuration() {
        assertJsonFormAndRestored(
            DurationHolder.serializer(),
            DurationHolder(1000.toDuration(DurationUnit.SECONDS)),
            """{"duration":"PT16M40S"}"""
        )
    }
}