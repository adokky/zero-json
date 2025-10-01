package dev.dokky.zerojson.framework

import kotlin.random.Random

object StringsDataSet {
    const val SINGLE_SURROGATE_PAIR: String = "\uD801\uDC37" // '𐐷'

    val utfTestString: String = buildString {
        val chars = charArrayOf('a', 'ф', '﷽')
        for (c1 in chars)
            for (c2 in chars)
                for (c3 in chars) {
                    append(c1)
                    append(c2)
                    append(c3)
                }

        append("\uD801\uDC37") // surrogate pair for '𐐷'
    }

    fun getAsciiData(): Sequence<String> = sequenceOf(
        "",
        Char(0).toString(),
        Char(127).toString(),
        "hello",
        "hello world!",
        "Just a long string to test",
        "Modern, concise and safe programming language\n" +
        "Easy to pick up, so you can create powerful applications immediately. "
    )

    fun getUtfData(excludeChar: Char? = null, random: Random = Random): Sequence<String> =
        sequenceOf(
            "test тест",
            "Привет, Мир!",
            "!П",
            "\u2705, \u10cb",
            SINGLE_SURROGATE_PAIR,
            "﷽ WHAT is that? \uD809\uDC2B\uD808\uDE19⸻ and finally ꧅",
            utfTestString
        )
        .plus(getAsciiData())
        .plus(getRandomThreeChars(count = 10_000, random))
        .plus(getSurrogatePairs(count = 100, random))
        .let { strings ->
            if (excludeChar != null) {
                val replacement = if (excludeChar == Char.MIN_VALUE) (Char.MIN_VALUE + 1) else (excludeChar - 1)
                strings.map { it.replace(excludeChar, replacement) }
            } else {
                strings
            }
        }

    fun getRandomThreeChars(count: Int, random: Random = Random): Sequence<String> = sequence {
        fun char() = random.nextInt(0, Char.MIN_SURROGATE.code).toChar()
        repeat(count) {
            yield("${char()}${char()}${char()}")
        }
    }

    fun getSurrogatePairs(count: Int, random: Random = Random): Sequence<String> = sequence {
        yield(charArrayOf(Char.MIN_HIGH_SURROGATE, Char.MIN_LOW_SURROGATE).concatToString())
        yield(charArrayOf(Char.MAX_HIGH_SURROGATE, Char.MIN_LOW_SURROGATE).concatToString())
        yield(charArrayOf(Char.MAX_HIGH_SURROGATE, Char.MAX_LOW_SURROGATE).concatToString())
        yield(charArrayOf(Char.MIN_HIGH_SURROGATE, Char.MAX_LOW_SURROGATE).concatToString())

        val hs = random.nextInt(Char.MIN_HIGH_SURROGATE.code, Char.MAX_HIGH_SURROGATE.code + 1).toChar()
        val ls = random.nextInt(Char.MIN_LOW_SURROGATE.code, Char.MAX_LOW_SURROGATE.code + 1).toChar()
        repeat(count) {
            yield(charArrayOf(hs, ls).concatToString())
        }
    }
}