package dev.dokky.zerojson

import kotlinx.serialization.encodeToByteArray
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.*
import java.io.File
import java.time.LocalDateTime

var TEST_DATA: Response<Person> = Json.decodeFromStream(Response::class.java.getResourceAsStream("/test_data_x3.json")!!)

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

fun main() {
    val testData = Response(
        data = (1..10).map { randomPerson() },
        total = 2094,
        version = 35353
    )

    val file = File("benchmarks/src/jmh/resources/test_data_${LocalDateTime.now()}.json")
    file.createNewFile()
    file.outputStream().use { out ->
        Json.encodeToStream(testData, out)
    }
}