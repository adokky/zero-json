package dev.dokky.zerojson

import dev.dokky.zerojson.internal.nameForErrorMessage
import kotlinx.serialization.SerializationException

fun JsonWriter.writeValue(v: ZeroJsonElement?) {
    when(v) {
        is Map<*, *> -> writeObject(v)
        is Collection<*> -> writeArray(v)
        is Byte -> writeNumber(v)
        is Short -> writeNumber(v)
        is Int -> writeNumber(v)
        is Long -> writeNumber(v)
        is Float -> writeNumber(v)
        is Double -> writeNumber(v)
        is Boolean -> writeBoolean(v)
        null -> writeNull()
        else -> throw SerializationException("can not serialize value of type ${v::class.nameForErrorMessage()}")
    }
}

private fun JsonWriter.writeArray(v: Collection<*>) {
    beginArray()

    val lastIndex = v.size - 1
    for ((i, e) in v.withIndex()) {
        writeValue(e)
        if (i < lastIndex) comma()
    }

    endArray()
}

private fun JsonWriter.writeObject(v: Map<*, *>) {
    beginObject()

    if (v.isNotEmpty()) {
        @Suppress("UNCHECKED_CAST")
        v as Map<String, ZeroJsonElement>
        val i = v.entries.iterator()
        while (true) {
            val e = i.next()
            writeString(e.key)
            colon()
            writeValue(e.value)
            if (i.hasNext()) comma() else break
        }
    }

    endObject()
}
