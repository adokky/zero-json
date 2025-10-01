package dev.dokky.zerojson

import kotlin.test.Test
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class SkipCommentsTest {
    private fun test(commentBlock: String) {
        val config = JsonReaderConfig(allowComments = true)

        JsonReader.startReadingFrom(commentBlock, config).apply {
            expectEof()
        }

        JsonReader.startReadingFrom("${commentBlock}~", config).apply {
            expectToken('~')
        }

        JsonReader.startReadingFrom("#${commentBlock}~", config).apply {
            expectToken('#')
            expectToken('~')
        }
    }

    @Test
    fun block1() = test("/**/")

    @Test
    fun block2() = test("/***/")

    @Test
    fun block3() = test("/****/")

    @Test
    fun block4() = test("/*/*/")

    @Test
    fun block5() = test("/*//*/")

    @Test
    fun block6() = test("/*//**/")

    @Test
    fun block7() = test("/*/**/")

    @Test
    fun block8() = test(
        "/* Lot of words" +
        "\n *Markdown* has **asterisks**.\n" +
        "Slashes '/' is also acceptable */"
    )

    @Test
    fun double_block1() = test("/**//**/")

    @Test
    fun double_block2() = test(" /*Z*/ /***/ ")

    @Test
    fun single_line_1() = test("//\n")

    @Test
    fun single_line_2() = test("///\n")

    @Test
    fun single_line_3() = test("//***/\n")

    @Test
    fun single_line_4() = test("/////\n")

    @Test
    fun single_line_5() = test("  //multi\n  //line\n  ")

    @Test
    fun combo() = test("  // single line\n  /* block */// single line\n  ")

    private fun assertFailsWithUnexpectedEof(input: String) {
        val e = assertFailsWith<ZeroJsonDecodingException> { test(input) }
        assertTrue("unexpected" in e.message)
        assertTrue("EOF" in e.message)
    }

    @Test
    fun invalid1() = assertFailsWithUnexpectedEof("/**")

    @Test
    fun invalid2() = assertFailsWithUnexpectedEof("/***")

    @Test
    fun invalid3() = assertFailsWithUnexpectedEof("/*/")

    @Test
    fun invalid4() = assertFailsWithUnexpectedEof("/*/*")

    @Test
    fun invalid5() = assertFailsWithUnexpectedEof("/")

    @Test
    fun invalid6() = assertFailsWithUnexpectedEof("/* hello")
}