package dev.dokky.zerojson.internal

import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.JsonUnquotedLiteral

internal fun JsonReaderImpl.readJsonPrimitive(strict: Boolean): JsonPrimitive {
    if (trySkipNull()) {
        return JsonNull
    }

    tryReadBoolean()?.let {
        return JsonPrimitive(it)
    }

    return if (strict) readJsonPrimitiveStrict() else readJsonPrimitiveLenient()
}

private fun JsonReaderImpl.readJsonPrimitiveStrict(): JsonPrimitive {
    tryReadNumberAsString(config.allowSpecialFloatingPointValues)?.let {
        return JsonUnquotedLiteral(it)
    }

    if (!nextIs('"') && (config.expectStringQuotes || JsonCharClasses.isStringTerminator(nextCodePoint))) {
        fail("expected JSON element")
    }

    return JsonPrimitive(readString())
}

private fun JsonReaderImpl.readJsonPrimitiveLenient(): JsonPrimitive {
    val quotes = nextIs('"')
    if (!quotes && JsonCharClasses.isStringTerminator(nextCodePoint)) {
        fail("expected JSON element")
    }

    val content = readString(requireQuotes = false)
    return if (quotes) JsonPrimitive(content) else JsonUnquotedLiteral(content)
}