package dev.dokky.zerojson.internal

import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.StructureKind

internal inline fun anyElementExistsInInlineSubTreeTemplate(
    rootDesc: ZeroJsonDescriptor,
    serialDesc: SerialDescriptor,
    elementIndex: Int,
    childElementsOffset: Int,
    isElementPresent: (absoluteIndex: Int) -> Boolean,
    markElementPresent: (absoluteIndex: Int) -> Unit,
    callRecursive: (
        rootDesc: ZeroJsonDescriptor,
        serialDesc: SerialDescriptor,
        elementIndex: Int,
        childElementsOffset: Int,
    ) -> Boolean
): Boolean {
    val childDesc = serialDesc.getElementDescriptor(elementIndex)
    val end = childElementsOffset + if (childDesc.kind == StructureKind.CLASS) childDesc.elementsCount else 0

    for (absoluteIdx in childElementsOffset..<end) {
        if (isElementPresent(absoluteIdx)) return true

        val nextChildsOffset = rootDesc.getChildElementsOffset(absoluteIdx)
        if (nextChildsOffset <= 0) continue

        val nextElementIndex = absoluteIdx - childElementsOffset
        if (callRecursive(rootDesc, childDesc, nextElementIndex, nextChildsOffset)) {
            // put anything, the actual value does not matter as long as it's not null
            markElementPresent(absoluteIdx)
            return true
        }
    }

    return false
}