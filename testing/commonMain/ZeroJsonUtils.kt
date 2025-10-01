package dev.dokky.zerojson.framework

import dev.dokky.zerojson.ZeroJson

fun ZeroJson.lenient(lenient: Boolean = true): ZeroJson =
	when(configuration.isLenient) {
		lenient -> this
		else -> ZeroJson(this) { this.isLenient = lenient }
	}

fun ZeroJson.ignoreUnknownKeys(ignore: Boolean = true): ZeroJson =
	when (configuration.ignoreUnknownKeys) {
		ignore -> this
		else -> ZeroJson(this) { this.ignoreUnknownKeys = ignore }
	}

fun ZeroJson.trailingComma(allowed: Boolean): ZeroJson =
    when (configuration.allowTrailingComma) {
        allowed -> this
        else -> ZeroJson(this) { this.allowTrailingComma = allowed }
    }