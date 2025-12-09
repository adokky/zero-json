package dev.dokky.zerojson

import dev.dokky.zerojson.internal.*
import io.kodec.text.RandomAccessTextReader
import io.kodec.text.ReadNumberResult
import karamel.utils.unsafeCast

typealias ZeroJsonElement = Any
typealias ZeroJsonObject = Map<String, ZeroJsonElement>
typealias ZeroJsonArray = List<ZeroJsonElement?>

fun JsonReader.readValue(): ZeroJsonElement? = when(nextCodePoint) {
    '['.code -> readArray()
    '{'.code -> readObject()
    else -> readPrimitive()
}

fun JsonReader.readObject(): ZeroJsonObject = buildMap {
    readObject {
        while (hasMoreKeys()) {
            val key = readKey()
            val value = readValue { readValue() }
            if (value != null) this@buildMap[key] = value
        }
    }
}

fun JsonReader.readArray(): ZeroJsonArray = buildList {
    readArray {
        while (hasMoreItems()) {
            readItem { add(readValue()) }
        }
    }
}

fun JsonReader.readPrimitive(expectQuotes: Boolean = config.expectStringQuotes): ZeroJsonElement? {
    val reader = this.unsafeCast<JsonReaderImpl>()
    return reader.input.readPrimitive(
        stringBuilder = config.stringBuilder,
        expectStringQuotes = expectQuotes,
        tempResultBuffer = reader.readNumberResult,
        allowSpecialFloatingPointValues = config.allowSpecialFloatingPointValues
    ).also {
        skipWhitespace()
    }
}

internal fun RandomAccessTextReader.readPrimitive(
    stringBuilder: StringBuilderWrapper,
    tempResultBuffer: ReadNumberResult,
    expectStringQuotes: Boolean,
    allowSpecialFloatingPointValues: Boolean,
    maxStringLength: Int = ZeroJsonConfiguration.Default.maxStringLength
): ZeroJsonElement? {
    if (trySkipJsonNull()) return null

    tryReadJsonBoolean()?.let { return it }

    tryReadJsonNumber(
        resultBuffer = tempResultBuffer,
        allowSpecialFloatingPointValues = allowSpecialFloatingPointValues,
    )?.let { return it }

    if (!nextIs('"') && JsonCharClasses.isStringTerminator(nextCodePoint)) throwExpectedJsonElement()

    return stringBuilder.buildString {
        readJsonString(this, requireQuotes = expectStringQuotes)
    }
}