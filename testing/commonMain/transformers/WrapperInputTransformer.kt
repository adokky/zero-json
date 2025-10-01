package dev.dokky.zerojson.framework.transformers

import dev.dokky.zerojson.framework.MutableTestInput
import dev.dokky.zerojson.framework.TestInputTransformer
import dev.dokky.zerojson.framework.TestTarget
import karamel.utils.unsafeCast
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.*

object WrapperInputTransformer: TestInputTransformer(
	name = "Value wrapping",
	code = "WR",
	targets = TestTarget.entries,
	deterministic = true
) {
	override fun transform(input: MutableTestInput) {
		input.serializer = Wrapper.serializer(input.serializer).unsafeCast()
		input.domainObject = Wrapper(input.domainObject)
		input.jsonElement = input.jsonElement.wrap()
	}

	@Serializable
	private data class CompoundDataClass(val string: String, val int: Int)

	// todo additionally wrap data in list/map
	@Serializable
	private data class Wrapper<T>(val data1: T, val someData: CompoundDataClass, val data2: T?) {
		constructor(data: T): this(
			data,
			CompoundDataClass(SEPARATOR_OBJECT["string"]!!.jsonPrimitive.content, Int.MAX_VALUE),
			data
		)
	}

	private fun JsonElement.wrap(): JsonObject = buildJsonObject {
		put("data1", this@wrap)
		put("someData", SEPARATOR_OBJECT)
		put("data2", this@wrap)
	}

	private val SEPARATOR_OBJECT = buildJsonObject {
		put("string", "[{:Тестовая Строка:}]")
		put("int", Int.MAX_VALUE)
	}
}