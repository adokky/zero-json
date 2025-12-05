package dev.dokky.zerojson.framework.transformers

import dev.dokky.zerojson.framework.MutableTestInput
import dev.dokky.zerojson.framework.TestInputTransformer
import dev.dokky.zerojson.framework.TestTarget

open class RandomSpaceInputTransformer(
	val maxRandomSpaces: Int = 3
): TestInputTransformer(
	name = "Randomized space",
	code = "RS",
	targets = TestTarget.entries.filter { it.input == TestTarget.DataType.Text }
) {
	init {
	    require(maxRandomSpaces > 0)
	}

	override fun transform(input: MutableTestInput) {
		input.composerConfig = input.composerConfig.copy(maxRandomSpaces = maxRandomSpaces)
	}

    companion object Default: RandomSpaceInputTransformer()
}