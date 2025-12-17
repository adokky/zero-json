package dev.dokky.zerojson.framework

actual fun getSystemProperty(name: String): String? {
    val argPrefix = name.replace("-", "_").let { "--$it=" }
    val allArgs = runCatching { js("__karma__.config.args") }.getOrElse {
        if (name == "test-mode") return "quick"
        return null
    }
    for (arg: String in allArgs) {
        val argValue = arg.removePrefix(argPrefix)
        if (argValue !== arg) return argValue
    }
    return null
}