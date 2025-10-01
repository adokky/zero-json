package dev.dokky.zerojson.framework.transformers

import dev.dokky.zerojson.framework.MutableTestInput
import dev.dokky.zerojson.framework.TestInputTransformer
import dev.dokky.zerojson.framework.TestTarget
import dev.dokky.zerojson.framework.lenient

object UnquotedStringsInputTransformer: TestInputTransformer(
	name = "Unquoted strings",
	code = "US",
	targets = TestTarget.entries.filter { it.input == TestTarget.DataType.Text },
	deterministic = true
) {
    override fun transform(input: MutableTestInput) {
		input.json = input.json.lenient()
		input.composerConfig = input.composerConfig.copy(unquoted = true)
	}
}

