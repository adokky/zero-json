package dev.dokky.zerojson.internal

import kotlinx.serialization.descriptors.StructureKind

internal fun treeDecoderJsonPath(treeDecoderStack: AutoCloseableStack<JsonTreeDecoder>, output: StringBuilder) {
    output.append('$')

    for (i in 1 until treeDecoderStack.acquired) {
        val decoder = treeDecoderStack.get(i)
        if (decoder.elementIndex < 0) break

        when (decoder.descriptor.kind) {
            StructureKind.MAP -> decoder.currentKey?.let { output.appendSegment(it) } ?: break
            StructureKind.LIST -> output.append('[').append(decoder.elementIndex).append(']')
            StructureKind.CLASS -> {
                if (decoder.elementIndex >= decoder.descriptor.elementsCount) break
                if (decoder.descriptor.isElementJsonInline(decoder.elementIndex)) continue
                val key = decoder.currentKey
                if (key == null) output.append("[null]") else output.appendSegment(key)
            }
            else -> break
        }
    }
}

private fun StringBuilder.appendSegment(key: String) {
    if (key.jsonPathSegmentRequireEscaping(segmentStart = 0)) {
        append("['")
        appendJsonPathSegment(key, segmentStart = 0, segmentEnd = key.length)
        append("']")
    } else {
        append('.').append(key)
    }
}