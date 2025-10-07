package dev.dokky.zerojson.internal

import kotlinx.serialization.descriptors.SerialDescriptor
import java.util.concurrent.ConcurrentHashMap

internal actual fun createSharedCache(): MutableMap<SerialDescriptor, ZeroJsonDescriptor>? =
    ConcurrentHashMap(256)