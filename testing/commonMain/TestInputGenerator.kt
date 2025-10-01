@file:OptIn(InternalTestingApi::class)

package dev.dokky.zerojson.framework

import io.kodec.buffers.ArrayDataBuffer
import karamel.utils.assert
import karamel.utils.unsafeCast

internal class TestInputGenerator(
    private val config: TestConfig<*>,
    private val stringBuilder: StringBuilder,
    private val sharedBuffer: ArrayDataBuffer = ArrayDataBuffer(10_000)
) {
    private var sharedBufferSize = 0

    private val defaultJsonComposer = DefaultJsonComposer(stringBuilder)

    private val initialInput = TestInputImpl(
        json = config.json,
        domainObject = config.domainObject,
        serializer = config.serializer.unsafeCast(),
        jsonElement = config.jsonElement,
        composerConfig = BaseJsonComposerConfig(),
        composer = defaultJsonComposer
    ).also {
        initState(inputIndex = -1, transformerIndex = -1, it)
    }

    private val inputStack = Array(config.transformers.size) { TestInputImpl.empty() }
    private var inputStackSize = 0

    private fun shrinkStack(newSize: Int) {
        if (inputStackSize <= newSize) return

        val oldBufferSize = sharedBufferSize
        for (i in newSize ..< inputStackSize) {
            val input = inputStack[i]
            sharedBufferSize -= input.binaryInput.size
            input.clear()
        }

        sharedBuffer.clear(sharedBufferSize, oldBufferSize)
//        if (newSize == 0) assertEquals(initialInput.binaryInput.size, sharedBufferSize)
        inputStackSize = newSize
    }

    private fun initState(inputIndex: Int, transformerIndex: Int, input: TestInputImpl) {
        // Composers can only be chained. The first one in the chain
        // is always DefaultJsonComposer which is always using this StringBuilder.
        stringBuilder.setLength(0)
        defaultJsonComposer.config = input.composerConfig
        input.composer.appendElement(input.jsonElement)

        var i = 0
        while (i < inputIndex) {
            inputStack[i++].applyTransform(stringBuilder)
        }
        input.applyTransform(stringBuilder)

        input.initState(
            transformerIndex = transformerIndex,
            jsonString = stringBuilder.toString(),
            destinationBuffer = sharedBuffer,
            destinationOffset = sharedBufferSize
        )
        sharedBufferSize += input.binaryInput.size
    }

    private val nonDeterministicTransformers = run {
        val trs = config.transformers
        SelectedTransformers(trs, trs.indices.filter { !trs[it].deterministic })
    }

    fun generate(previousSelected: SelectedTransformers, selected: SelectedTransformers): TestInputImpl? {
        val totalTrs = config.transformers.size
        val keepFrames = previousSelected.countCommonSignificantTransformers(selected, total = totalTrs)
        shrinkStack(newSize = keepFrames)

        var previous = inputStack.getOrElse(inputStackSize - 1) { initialInput }

        if (inputStackSize < selected.size) {
            selected.iterateIndices(total = totalTrs, from = previous.transformerIndex + 1) { transformerIndex ->
                // increment upfront for correct clean up
                val inputIndex = inputStackSize

                val new = inputStack[inputIndex]
                previous.copyTo(new)

                val transformer = config.transformers[transformerIndex]

                transformer.transform(new)

                if (new.equalsTo(previous) && selected.allDeterministicAfter(transformerIndex)) {
                    new.clear()
                    shrinkStack(newSize = keepFrames)
                    return null
                }

                initState(inputIndex = inputIndex, transformerIndex = transformerIndex, new)

                val prevEF = previous.expectedFailure
                val newEF = transformer.expectFailure
                new.expectedFailure = when {
                    prevEF == null -> newEF
                    newEF == null -> prevEF
                    else -> prevEF.or(newEF)
                }

                previous = new
                inputStackSize++
            }
        }

        assert { previous.isReady }

        return previous
    }

    private fun SelectedTransformers.allDeterministicAfter(transformerIndex: Int): Boolean {
        val keepLast = config.transformers.size - transformerIndex - 1
        val mask = 0.inv().shl(keepLast).inv()
        return bits and nonDeterministicTransformers.bits and mask == 0
    }

    fun reset() {
        shrinkStack(0)
    }
}