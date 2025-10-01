package dev.dokky.zerojson.internal

internal actual fun getApplicationVariable(name: String): String? {
    return System.getProperty(name)
}