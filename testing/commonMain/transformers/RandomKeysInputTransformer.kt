package dev.dokky.zerojson.framework.transformers

import dev.dokky.zerojson.framework.*
import kotlinx.serialization.json.JsonObject

object RandomKeysInputTransformer: TestInputTransformer(
	name = "Random keys",
	code = "RK",
	targets = TestTarget.decoders()
) {
	override fun transform(input: MutableTestInput) {
        val jsonElement = input.jsonElement as? JsonObject ?: return

        input.json = input.json.ignoreUnknownKeys()
		input.jsonElement = jsonElement.withRandomKeys()
	}
}