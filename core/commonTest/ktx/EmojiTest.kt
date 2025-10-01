package dev.dokky.zerojson.ktx

import kotlinx.serialization.builtins.serializer
import kotlin.test.Test

class EmojiTest : JsonTestBase() {
    @Test
    fun testEmojiString() {
        assertJsonFormAndRestored(
            String.serializer(),
            "\uD83C\uDF34",
            "\"\uD83C\uDF34\""
        )
    }
}
