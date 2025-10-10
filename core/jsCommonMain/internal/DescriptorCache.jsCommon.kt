package dev.dokky.zerojson.internal

private val simpleCaches = SimpleSharedCaches()

internal actual fun createSharedCaches(): SharedDescriptorCaches? = simpleCaches