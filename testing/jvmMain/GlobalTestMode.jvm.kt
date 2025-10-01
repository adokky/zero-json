package dev.dokky.zerojson.framework

actual fun getSystemProperty(name: String): String? = System.getProperty(name)