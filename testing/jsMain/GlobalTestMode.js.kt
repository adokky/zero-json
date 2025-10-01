package dev.dokky.zerojson.framework

actual fun getSystemProperty(name: String): String? {
    val argPrefix = name.replace("-", "_").let { "--$it=" }
    val allArgs = js("__karma__.config.args")
    for (arg: String in allArgs) {
        val argValue = arg.removePrefix(argPrefix)
        if (argValue !== arg) return argValue
    }
    return null
}