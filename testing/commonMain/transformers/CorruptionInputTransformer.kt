package dev.dokky.zerojson.framework.transformers

import dev.dokky.zerojson.framework.*
import io.kodec.text.DefaultCharClasses
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlin.random.Random
import kotlin.random.nextInt

object CorruptionInputTransformer: TestInputTransformer(
	name = "Corruption",
	code = "COR",
	targets = TestTarget.entries.filter { it.input == TestTarget.DataType.Text },
	incompatibleWith = listOf(RandomKeysInputTransformer::class),
	expectFailure = TestConfig.ExpectedFailure {
		it is SerializationException || it is TestTargetFailure
	}
) {
	private val lenientJson = Json { isLenient = true }

	override fun transform(input: MutableTestInput) {
		input.json = input.json.ignoreUnknownKeys(false)

		if (input.json.configuration.isLenient) {
			input.composerConfig = input.composerConfig.copy(corruptionInvertedProb = 5, allowCorruptQuotes = false)
			input.transformTextInput { it.corrupt(lenientJson, allowCorruptingQuotes = false) }
		} else {
			input.composerConfig = input.composerConfig.copy(corruptionInvertedProb = 5)
			input.transformTextInput { it.corrupt(Json) }
		}
	}
}

private fun StringBuilder.corrupt(
	json: Json,
	allowTrailingComma: Boolean = false,
	allowCorruptingQuotes: Boolean = true
) {
    val beforeCorruption = json.tryParseJsonElement(this)

	var remaining = Random.nextInt(1, 4)

	while (remaining > 0) {

		while (remaining > 0) {
            val deleted = Random.nextBoolean() && deleteRandom(quotes = allowCorruptingQuotes)
            val inserted = Random.nextBoolean() && insertRandom(
                allowTrailingComma = allowTrailingComma,
                allowQuotes = allowCorruptingQuotes
            )

            if (deleted || inserted) remaining--
		}

        val afterCorruption = json.tryParseJsonElement(this)

        if (
            // the original JSON was malformed, and we accidentally made it valid
            (beforeCorruption == null && afterCorruption != null) ||
            // resulting JSON become the same as before the corruption
            ((afterCorruption?.let { beforeCorruption?.orderSensitiveEquals(it) } == true))
        ) {
			// add another corruption iteration
			remaining++
		}
	}
}

private fun StringBuilder.insertRandom(allowTrailingComma: Boolean, allowQuotes: Boolean): Boolean {
	val maxPos = indexOfLast { !DefaultCharClasses.isWhitespace(it.code) }.coerceAtLeast(0)
	val pos = Random.nextInt(maxPos + 1) // do not insert at the end

	val fragment = randomFragment(allowQuotes)

	if (!allowTrailingComma && fragment.isComma()) {
        val i = skipWhiteSpace(pos)
        val nextTokenAfterComma = getOrElse(i) { 'x' }
		if (nextTokenAfterComma.let { it == ']' || it == '}' }) return false
	}

    // escaping non-special character gives the same character
    if (fragment.singleOrNull() == '\\' &&
        getOrNull(pos)?.let { it in specialEscapeChars } == false)
    {
        return false
    }

	insert(pos, fragment)
	return true
}

private fun StringBuilder.skipWhiteSpace(pos: Int): Int {
    var i = pos
    while (i < length && DefaultCharClasses.isWhitespace(get(i).code)) i++
    return i
}

private val specialEscapeChars = charArrayOf('r', 't', 'n', '\\')

private fun Char.isNotAllowedToRemove(quotes: Boolean): Boolean =
	DefaultCharClasses.isWhitespace(code) || (!quotes && this == '"')

private fun StringBuilder.deleteRandom(attempts: Int = 4, quotes: Boolean = true): Boolean {
	if (isEmpty()) return false

	var attempt = 0
	retry@ while (++attempt <= attempts) {
		var idx = Random.nextInt(indices)
		while (get(idx).isNotAllowedToRemove(quotes)) {
			idx++
			if (idx >= length) continue@retry
		}
		deleteAt(idx)
		return true
	}

	return false
}

private fun Json.tryParseJsonElement(input: StringBuilder): JsonElement? =
	input.runCatching { parseToJsonElement(input.toString()) }.getOrNull()

private fun String.isComma(): Boolean {
	var comma = 0
	var whitespaces = true
	for (c in this) {
		if (c == ',') comma++ else {
			whitespaces = whitespaces && DefaultCharClasses.isWhitespace(c.code)
		}
	}
	return comma == 1 && whitespaces
}

private fun randomFragment(allowQuotes: Boolean): String {
	var fragment: String
	do {
		val size = Random.nextInt(1, 4)
		fragment = buildString(size) {
			repeat(size) {
				append(randomChar(allowQuotes))
			}
		}
	} while (fragment.all { DefaultCharClasses.isWhitespace(it.code) } || fragment.singleOrNull() == '\\')
	return fragment
}

private val jsonTokens = arrayOf('{', '}', '[', ']', ':', ',', '\\', '"').also {
	check(it.last() == '"')
}

private fun randomChar(allowQuotes: Boolean): Char {
	if (Random.nextInt(3) == 0) return when {
        allowQuotes -> jsonTokens.random()
        else -> jsonTokens[Random.nextInt(jsonTokens.size - 1)]
    }

	var char: Char
	do {
		char = Random.nextInt(0x20, Char.MIN_SURROGATE.code).toChar()
	} while (char == '0')
	return char
}