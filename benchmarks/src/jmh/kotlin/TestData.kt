package dev.dokky.zerojson

import kotlinx.serialization.encodeToByteArray
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject

var TEST_DATA: Response<Person> = Response(
    data = (1..50).map { randomPerson() },
    total = 2094,
    version = 35353
)

object DiscriminatorAtStart {
    var ENCODED_DATA: ByteArray = ZeroJson.encodeToByteArray(TEST_DATA)
    var ENCODED_DATA_STRING: String = ZeroJson.encodeToString(TEST_DATA)
    var ENCODED_DATA_TREE: JsonElement = ZeroJson.encodeToJsonElement(TEST_DATA)
}

object DiscriminatorInTheMiddle {
    var ENCODED_DATA_TREE: JsonElement = DiscriminatorAtStart.ENCODED_DATA_TREE.moveDiscriminators()
    var ENCODED_DATA_STRING: String = ENCODED_DATA_TREE.toString()
    var ENCODED_DATA: ByteArray = ENCODED_DATA_STRING.encodeToByteArray()
}

val DecodersTest.TLS.inputArray: ByteArray get() = when {
    DITM -> DiscriminatorInTheMiddle.ENCODED_DATA
    else -> DiscriminatorAtStart.ENCODED_DATA
}
val DecodersTest.TLS.inputString: String get() = when {
    DITM -> DiscriminatorInTheMiddle.ENCODED_DATA_STRING
    else -> DiscriminatorAtStart.ENCODED_DATA_STRING
}

private fun JsonElement.moveDiscriminators(): JsonElement = when(this) {
    is JsonObject -> moveDiscriminators()
    is JsonArray -> JsonArray(map { it.moveDiscriminators() })
    else -> this
}

private fun JsonObject.moveDiscriminators(): JsonObject {
    val type = this["type"]
    val new = LinkedHashMap<String, JsonElement>(size)
    val insertAt = size / 2
    var i = 0

    for ((key, value) in this) {
        if (key != "type") {
            new[key] = value.moveDiscriminators()
        }
        if (type != null && i == insertAt) {
            new["type"] = type
        }
        i++
    }

    return JsonObject(new)
}