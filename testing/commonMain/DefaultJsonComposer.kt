package dev.dokky.zerojson.framework

import io.kodec.text.DefaultCharClasses
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlin.random.Random

data class BaseJsonComposerConfig(
	/** Maximum number of space characters between JSON tokens. */
	val maxRandomSpaces: Int = 0,
	/** Emit all strings without quotes. */
	val unquoted: Boolean = false,
	/**
	 * Probability to skip JSON token.
	 * Calculated as: `1 / corruptionInvertedProb`.
	 * If `0` - corruption disabled.
	 */
	val corruptionInvertedProb: Int = 0,
	val allowCorruptQuotes: Boolean = false,
    val trailingComma: Boolean = false
)

internal class DefaultJsonComposer(
	output: Appendable,
	config: BaseJsonComposerConfig = BaseJsonComposerConfig()
): JsonComposer, Appendable by output
{
	var config: BaseJsonComposerConfig = config
		set(value) {
			depth = 0
			field = value
		}

	private var depth = 0

	override fun appendElement(element: JsonElement) {
		depth++
		if (!shouldCorrupt(div = 2 + 8 / depth)) { // the deeper, the more chances to corrupt
			when (element) {
				is JsonArray -> appendArray(element)
				is JsonObject -> appendObject(element)
				is JsonPrimitive -> appendPrimitive(element)
			}
		}
		depth--
	}

	private fun shouldCorrupt(div: Int = 1): Boolean =
		config.corruptionInvertedProb > 0 && Random.nextInt(config.corruptionInvertedProb * div) == 0

	private fun shouldNotCorrupt(): Boolean = !shouldCorrupt()

	private fun appendToken(token: Char) {
		if (shouldNotCorrupt()) append(token)
	}

	override fun appendObject(value: JsonObject) {
		appendToken('{')

		var first = true
		for (entry in value) {
			if (first) first = false else appendToken(',')

			appendSpace()
			if (!shouldCorrupt(div = 2)) appendJsonString(entry.key)
			appendSpace()

			appendToken(':')

			appendSpace()
			val value = entry.value
			appendElement(value)
			appendSpace()
		}

        structureEnd(value.entries, '}')
	}

	private fun appendSpace() = appendRandomSpace(config.maxRandomSpaces)

	override fun appendArray(value: JsonArray) {
		appendToken('[')

		var first = true
		for (v in value) {
			if (first) first = false else appendToken(',')

			appendSpace()
			appendElement(v)
			appendSpace()
		}

        structureEnd(value, ']')
	}

    private fun structureEnd(collection: Collection<*>, closingBracket: Char) {
        if (collection.isEmpty()) {
            appendSpace()
        } else if (config.trailingComma) {
            appendToken(',')
            appendSpace()
        }

        appendToken(closingBracket)
    }

	override fun appendPrimitive(value: JsonPrimitive) {
        if (value.isString) appendJsonString(value.content) else append(value.content)
	}

	private fun appendRandomSpace(max: Int) {
		if (max == 0) return
		repeat(Random.nextInt(max + 1)) { append(spaceChars.random()) }
	}

	private fun appendJsonString(string: String) {
		val escaped = string.jsonEscape()
		val quotes = !config.unquoted ||
				escaped !== escaped ||
				escaped.isEmpty() ||
				escaped.any { DefaultCharClasses.isJsonToken(it.code) }
		if (quotes) appendQuotes()
		append(escaped)
		if (quotes) appendQuotes()
	}

	private fun appendQuotes() {
		if (!config.allowCorruptQuotes || shouldNotCorrupt()) append('"')
	}

	private companion object {
		val spaceChars = arrayOf('\n', '\t', ' ')
	}
}

fun JsonElement.format(
	maxRandomSpaces: Int = 0,
	unquoted: Boolean = false,
	corruptionInvertedProb: Int = 0,
	allowCorruptQuotes: Boolean = false
): String {
	return buildString {
		val composer = DefaultJsonComposer(this, BaseJsonComposerConfig(
			maxRandomSpaces = maxRandomSpaces,
			unquoted = unquoted,
			corruptionInvertedProb = corruptionInvertedProb,
			allowCorruptQuotes = allowCorruptQuotes
		))
		composer.appendElement(this@format)
	}
}