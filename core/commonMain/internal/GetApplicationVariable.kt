package dev.dokky.zerojson.internal

internal expect fun getApplicationVariable(name: String): String?

internal val DebugMode: Boolean = getApplicationVariable("zero-json-debug")?.toBooleanStrictOrNull() ?: false

internal inline fun debugAssert(body: () -> Boolean) {
    if (DebugMode) {
        if (!body()) throw AssertionError()
    }
}