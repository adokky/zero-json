package dev.dokky.zerojson.framework

import kotlinx.serialization.KSerializer
import kotlinx.serialization.serializer

inline fun TestConfigBuilder.jsonElement(
    allowRandomKeys: Boolean = true,
    builder: DslJsonObjectBuilder.() -> Unit
) {
    jsonElement = jsonObject(allowRandomKeys = allowRandomKeys, buildJson = builder)
}

inline fun <reified T> TestConfigBuilder.domainObject(
    obj: T,
    serializer: KSerializer<T> = serializer<T>(),
    allowRandomKeys: Boolean = true,
    builder: DslJsonObjectBuilder.() -> Unit
) {
    domainObject(obj, serializer)
    jsonElement(allowRandomKeys = allowRandomKeys, builder = builder)
}

inline fun <reified T> TestConfigBuilder.domainObject(obj: T): Unit = domainObject(obj, serializer<T>())