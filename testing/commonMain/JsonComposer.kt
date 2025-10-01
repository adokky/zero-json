package dev.dokky.zerojson.framework

import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

sealed interface JsonComposer: Appendable {
	fun appendObject(value: JsonObject)
	fun appendArray(value: JsonArray)
	fun appendPrimitive(value: JsonPrimitive)

	fun appendElement(element: JsonElement) {
		when(element) {
			is JsonArray -> appendArray(element)
			is JsonObject -> appendObject(element)
			is JsonPrimitive -> appendPrimitive(element)
		}
	}

	abstract class Delegate(private val output: JsonComposer): JsonComposer by output
}