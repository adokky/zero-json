package dev.dokky.zerojson

import dev.dokky.zerojson.internal.JsonReaderImpl
import dev.dokky.zerojson.internal.skipString
import dev.dokky.zerojson.internal.trySkipArrayComma
import dev.dokky.zerojson.internal.trySkipObjectComma
import karamel.utils.unsafeCast
import kotlin.jvm.JvmInline

@JvmInline
value class JsonObjectReader @PublishedApi internal constructor(
    @PublishedApi internal val reader: JsonReaderImpl
) {
    constructor(reader: JsonReader): this(reader.unsafeCast())

    fun skipKey() {
        reader.skipString()
        reader.expectColon()
    }

    fun skipValue() {
        reader.skipElement()
    }

    fun skipEntry() {
        skipKey()
        skipValue()
    }

    fun readKey(): String = reader.readString()
        .also { reader.expectColon() }

    fun hasMoreKeys(): Boolean = reader.nextCodePoint != '}'.code
}

inline fun <R> JsonObjectReader.readValue(read: JsonReader.() -> R): R {
    val result = reader.read()
    reader.trySkipObjectComma()
    return result
}

inline fun <R> JsonReader.readObject(body: JsonObjectReader.() -> R): R {
    expectBeginObject()
    val reader = JsonObjectReader(this)
    return reader.body().also {
        while (reader.hasMoreKeys()) reader.skipEntry()
        expectEndObject()
    }
}

@JvmInline
value class JsonArrayReader @PublishedApi internal constructor(
    @PublishedApi internal val reader: JsonReaderImpl
) {
    constructor(reader: JsonReader): this(reader.unsafeCast())

    fun skipItem() {
        reader.skipElement()
        reader.trySkipArrayComma()
    }

    inline fun <R> readItem(read: JsonReader.() -> R): R {
        val result = reader.read()
        reader.trySkipArrayComma()
        return result
    }

    fun hasMoreItems(): Boolean = reader.nextCodePoint != ']'.code
}

inline fun <R> JsonReader.readArray(body: JsonArrayReader.() -> R): R {
    expectBeginArray()
    val reader = JsonArrayReader(this)
    return reader.body().also {
        while (reader.hasMoreItems()) reader.skipItem()
        expectEndArray()
    }
}