package dev.dokky.zerojson.framework

import kotlinx.serialization.json.*
import kotlin.random.Random

fun JsonObject.withRandomKeys(
    maxDepth: Int = 3,
    minTopLevelKeys: Int = 1,
    maxKeysPerLevel: Int = maxOf(3, minTopLevelKeys)
): JsonObject {
    require(maxDepth >= 0)
    require(minTopLevelKeys in 0..1_000_000)
    require(maxKeysPerLevel in 0..1_000_000)

    if (maxDepth == 0 || maxKeysPerLevel == 0 || isRandomKeysDisabled()) return this

    var n = Random.nextInt(minTopLevelKeys, maxKeysPerLevel + 1)
    if (n == 0) return this

    return buildJsonObject {
        putRandomKey(0, maxDepth)
        n--

        var size = n + this@withRandomKeys.size
        for ((k, v) in this@withRandomKeys) {
            size--

            put(k, if (v !is JsonObject) v else v.withRandomKeys(
                maxDepth - 1,
                minTopLevelKeys = 0,
                maxKeysPerLevel = (maxDepth - 1).coerceAtLeast(1)
            ))

            if (size > 0 && Random.nextInt(size) >= n) {
                putRandomKey(n, maxDepth)
                n--
            }
        }

        while (n > 0) {
            putRandomKey(n, maxDepth)
            n--
        }
    }
}

internal fun JsonObjectBuilder.putRandomKey(i: Int, maxDepth: Int) {
    val key = "^rnd_key_$i"
    put(key, generateRandomJsonElement(maxDepth - 1))
        ?.also { put(key, it) } // revert overwrite
}

fun JsonElement.removeNullKeys(): JsonElement = when (this) {
    is JsonObject -> JsonObject(buildMap(size) {
        for ((k, v) in this@removeNullKeys) {
            if (v != JsonNull) put(k, v.removeNullKeys())
        }
    })
    is JsonArray -> JsonArray(map { it.removeNullKeys() })
    else -> this
}