/*
 * Copyright 2017-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package kotlinx.serialization.test

enum class Platform {
    JVM, JS, NATIVE, WASM
}

fun isJs(): Boolean = false
fun isJvm(): Boolean = true
fun isNative(): Boolean = false
fun isWasm(): Boolean = false