package dev.dokky.zerojson.internal

import kotlinx.serialization.descriptors.SerialDescriptor

internal actual fun createSharedCache(): MutableMap<SerialDescriptor, ZeroJsonDescriptor>? = HashMap()