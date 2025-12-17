package dev.dokky.zerojson.framework

actual fun getSystemProperty(name: String): String? {
    if (name == "test-mode") return "quick"
    return null
}