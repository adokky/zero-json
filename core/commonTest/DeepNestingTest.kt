@file:Suppress("PropertyName", "ClassName")

package dev.dokky.zerojson

import dev.dokky.zerojson.framework.*
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.*
import kotlin.random.Random
import kotlin.test.Test

@ExperimentalSerializationApi
class DeepNestingTest: RandomizedJsonTest() {
    @Serializable
    private data class Node(
        val data: Int,
        val left: Node?,
        val right: Node?
    )

    private fun buildNodeOrNull(depth: Int = 0): Node? =
        when {
            depth + 1 >= ZeroJsonConfiguration.Default.maxStructureDepth || Random.nextInt(depth) != 0 -> null
            else -> buildNode(depth)
        }

    private fun buildNode(depth: Int): Node = Node(
        data = depth,
        left = buildNodeOrNull(depth + 1),
        right = buildNodeOrNull(depth + 1)
    )

    private fun Node?.toJsonElement(): JsonElement =
        this?.toJsonObject() ?: JsonNull

    private fun Node.toJsonObject(): JsonObject = buildJsonObject {
        put("data", data)
        put("left", left.toJsonElement())
        put("right", right.toJsonElement())
    }

    @Test
    fun randomized1() {
        repeat(100) { i ->
            val root = buildNode(0)
            randomizedTest {
                name = "Deep nesting (attempt ${i + 1})"
                domainObject(root)
                jsonElement = root.toJsonObject()
                iterations = 1
            }
        }
    }

    @Test
    fun randomized2() {
        repeat(10) { i ->
            val root = buildNode(0)
            randomizedTest {
                name = "Deep nesting (attempt ${i+1})"
                domainObject(root)
                jsonElement = root.toJsonObject()
                iterations = if (GlobalTestMode == TestMode.QUICK) 20 else 100
            }
        }
    }
}