/*
 * Copyright 2017-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package kotlinx.serialization.test

enum class Platform {
    JVM, JS, NATIVE, WASM
}

expect fun isJs(): Boolean
expect fun isJvm(): Boolean
expect fun isNative(): Boolean
expect fun isWasm(): Boolean