package dev.dokky.zerojson.framework

import kotlinx.serialization.json.*

internal fun JsonElement.orderSensitiveEquals(other: JsonElement): Boolean {
    return when(this) {
        is JsonObject -> if (other !is JsonObject) false else orderSensitiveEquals(other)
        is JsonArray -> if (other !is JsonArray) false else orderSensitiveEquals(other)
        is JsonPrimitive -> {
            if (other !is JsonPrimitive) return false

            if (!isString && content.first().let { it == '-' || it.isDigit() }) {
                if (other.isString) return false
                if (content == other.content) return true
                if (intOrNull?.let { it == other.intOrNull } == true) return true
                if (floatOrNull?.let { it == other.floatOrNull } == true) return true
                return false
            }

            this == other
        }
    }
}

private fun JsonArray.orderSensitiveEquals(other: JsonArray): Boolean {
    if (size != other.size) return false
    for (i in indices) {
        if (!this[i].orderSensitiveEquals(other[i])) return false
    }
    return true
}

internal fun JsonObject.orderSensitiveEquals(other: JsonObject): Boolean {
    if (size != other.size) return false
    val i1 = iterator()
    val i2 = other.iterator()
    while (i1.hasNext()) {
        val (k1, v1) = i1.next()
        val (k2, v2) = i2.next()
        if (k1 != k2) return false
        if (!v1.orderSensitiveEquals(v2)) return false
    }
    return true
}

internal fun JsonObject.elementsEquals(other: JsonObject): Boolean {
    if (size != other.size) return false
    val i1 = iterator()
    val i2 = other.iterator()
    while (i1.hasNext()) {
        if (i1.next() != i2.next()) return false
    }
    return true
}