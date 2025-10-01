package dev.dokky.zerojson.framework

import kotlinx.serialization.json.*
import kotlin.random.Random

fun generateRandomJsonElement(maxDepth: Int = 2): JsonElement {
    val type = Random.nextInt(if (maxDepth > 0) 7 else 5)
    return when(type) {
        0 -> JsonNull
        1 -> JsonPrimitive(Random.nextInt())
        2 -> JsonPrimitive(Random.nextDouble())
        3 -> JsonPrimitive("Random String ${Random.nextInt()}")
        4 -> JsonPrimitive(Random.nextBoolean())
        5 -> generateRandomJsonArray(maxDepth)
        else -> generateRandomJsonObject(maxDepth)
    }
}

fun generateRandomJsonObject(maxDepth: Int): JsonObject = buildJsonObject {
    repeat(Random.nextInt(3)) { i ->
        putRandomKey(i, maxDepth = maxDepth - 1)
    }
}

fun generateRandomJsonArray(maxDepth: Int): JsonArray = buildJsonArray {
    repeat(Random.nextInt(3)) {
        add(generateRandomJsonElement(maxDepth - 1))
    }
}