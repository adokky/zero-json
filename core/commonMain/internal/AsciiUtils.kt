package dev.dokky.zerojson.internal

/*
[ 1011011
{ 1111011
] 1011101
} 1111101
 */
internal fun Int.isOpeningBracket(): Boolean = this or 0b100000 == 0b1111011
internal fun Int.isClosingBracket(): Boolean = this or 0b100000 == 0b1111101

internal fun Int.toClosingBracket(): Int = this + 2
internal fun Int.toOpeningBracket(): Int = this - 2

internal fun Char.toOpeningBracket(): Char = this - 2