package dev.dokky.zerojson.internal

import karamel.utils.assertionsEnabled
import kotlinx.serialization.descriptors.SerialDescriptor

internal fun checkEqualSerialNames(name1: String, name2: String) {
    if (assertionsEnabled) {
        doCheckSerialNamesEqual(name1, name2)
    }
}

private fun doCheckSerialNamesEqual(name1: String, name2: String) {
    if (name1.removeSuffix("?") != name2.removeSuffix("?")) {
        throw AssertionError(
            "serial descriptor name mismatch:\n" +
                    "    - '$name1'\n" +
                    "    - '$name2'"
        )
    }
}

internal fun ZeroJsonDescriptor.check(descriptor: SerialDescriptor): Boolean =
    descriptor.kind.isCollection() ||
    descriptor.serialName.trimEnd('?') == serialName.trimEnd('?')