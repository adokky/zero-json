package dev.dokky.zerojson.framework

enum class TestMode {
    QUICK,
    DEFAULT,
    FULL
}

val GlobalTestMode: TestMode = getSystemProperty("test-mode")
    ?.let { TestMode.valueOf(it.uppercase()) }
    ?: TestMode.DEFAULT

val QuickTestMode: Boolean get() = GlobalTestMode == TestMode.QUICK

fun testIterations(quick: Int, normal: Int): Int = if (QuickTestMode) quick else normal

expect fun getSystemProperty(name: String): String?