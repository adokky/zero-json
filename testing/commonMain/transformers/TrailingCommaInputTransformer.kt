package dev.dokky.zerojson.framework.transformers

import dev.dokky.zerojson.framework.MutableTestInput
import dev.dokky.zerojson.framework.TestInputTransformer
import dev.dokky.zerojson.framework.TestTarget
import dev.dokky.zerojson.framework.trailingComma

object TrailingCommaInputTransformer: TestInputTransformer(
    name = "TrailingComma",
    code = "TC",
    targets = TestTarget.entries.filter { it.input == TestTarget.DataType.Text },
    // technically compatible, but does not make sense
    incompatibleWith = listOf(
        CorruptionInputTransformer::class,
        UnquotedStringsInputTransformer::class
    ),
) {
    override fun transform(input: MutableTestInput) {
        input.json = input.json.trailingComma(true)
        input.composerConfig = input.composerConfig.copy(trailingComma = true)
    }
}