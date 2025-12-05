@file:OptIn(InternalSerializationApi::class)

package dev.dokky.zerojson.framework

import dev.dokky.zerojson.DefaultTestConfiguration
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.InternalSerializationApi
import kotlinx.serialization.descriptors.serialDescriptor
import kotlinx.serialization.json.*
import kotlin.jvm.JvmInline
import kotlin.random.Random
import kotlin.reflect.KProperty1

inline fun jsonObject(allowRandomKeys: Boolean = true, buildJson: DslJsonObjectBuilder.() -> Unit): JsonObject =
    buildJsonObject { DslJsonObjectBuilder(this).buildJson() }.let { obj ->
        if (allowRandomKeys) obj else JsonObject(MarkedMap(NoRandomKeysMarker, LinkedHashMap(obj)))
    }

@Suppress("EqualsOrHashCode")
@PublishedApi
internal class MarkedMap<K: Any, V: Any>(val marker: Any, val map: Map<K, V>): Map<K, V> by map {
    override fun equals(other: Any?): Boolean = (other === marker) || map == other
}

@PublishedApi
internal object NoRandomKeysMarker

fun JsonObject.isRandomKeysDisabled(): Boolean = this.equals(NoRandomKeysMarker)

@JvmInline
@Suppress("NOTHING_TO_INLINE")
@OptIn(ExperimentalSerializationApi::class)
value class DslJsonObjectBuilder(@PublishedApi internal val builder: JsonObjectBuilder) {
    infix fun String.eq(value: JsonElement) { builder.put(this, value) }
    infix fun String.eq(value: String?) { builder.put(this, value) }
    infix fun String.eq(value: Number?) { builder.put(this, value) }
    infix fun String.eq(value: Boolean?) { builder.put(this, value) }
    infix fun String.eq(value: Char?) { builder.put(this, value?.toString()) }
    infix fun String.eq(value: Enum<*>?) { builder.put(this, value?.name) }
    infix fun String.eq(value: Nothing?) { builder.put(this, value) }

    infix fun String.eq(value: UByte?) { builder.put(this, value?.let(::JsonPrimitive) ?: JsonNull) }
    infix fun String.eq(value: UShort?) { builder.put(this, value?.let(::JsonPrimitive) ?: JsonNull) }
    infix fun String.eq(value: UInt?) { builder.put(this, value?.let(::JsonPrimitive) ?: JsonNull) }
    infix fun String.eq(value: ULong?) { builder.put(this, value?.let(::JsonPrimitive) ?: JsonNull) }

    // inline to not break KProperty.name optimization
    inline infix fun KProperty1<*, JsonElement>.eq(value: JsonElement) { builder.put(name, value) }
    inline infix fun KProperty1<*, String>.eq(value: String?) { builder.put(name, value) }
    inline infix fun KProperty1<*, Number>.eq(value: Number?) { builder.put(name, value) }
    inline infix fun KProperty1<*, Boolean>.eq(value: Boolean?) { builder.put(name, value) }
    inline infix fun KProperty1<*, Char>.eq(value: Char?) { builder.put(name, value?.toString()) }
    inline infix fun KProperty1<*, Enum<*>>.eq(value: Enum<*>?) { builder.put(name, value?.name) }
    inline infix fun KProperty1<*, Any?>.eq(value: Nothing?) { builder.put(name, value) }

    inline infix fun String.noRandomKeys(crossinline inner: DslJsonObjectBuilder.() -> Unit): Unit =
        eqObject(allowRandomKeys = false, inner = inner)

    inline operator fun String.invoke(crossinline inner: DslJsonObjectBuilder.() -> Unit): Unit =
        eqObject(inner = inner)

    inline fun <reified T> String.polymorphic(crossinline inner: DslJsonObjectBuilder.() -> Unit) {
        builder.putJsonObject(this) {
            DslJsonObjectBuilder(this).run {
                discriminator<T>()
                inner()
            }
        }
    }

    private inline fun <T> String.array(values: Array<T>, mapper: (T) -> JsonElement) {
        builder.put(this, JsonArray(values.map(mapper)))
    }

    private inline fun <T> String.array(values: Collection<T>, mapper: (T) -> JsonElement) {
        builder.put(this, JsonArray(values.map(mapper)))
    }

    operator fun String.invoke(vararg values: JsonElement?) {
        builder.put(this, JsonArray(values.map { it ?: JsonNull }))
    }

    operator fun String.invoke(vararg values: String?): Unit = array(values) { JsonPrimitive(it) }
    operator fun String.invoke(vararg values: Number?): Unit = array(values) { JsonPrimitive(it) }
    operator fun String.invoke(vararg values: Boolean?): Unit = array(values) { JsonPrimitive(it) }
    operator fun String.invoke(vararg values: Char?): Unit = array(values) { JsonPrimitive(it?.toString()) }
    operator fun String.invoke(vararg values: Enum<*>?): Unit = array(values) { JsonPrimitive(it?.name) }

    infix fun String.stringArray(values: Collection<String?>): Unit = array(values) { JsonPrimitive(it) }
    infix fun String.numberArray(values: Collection<Number?>): Unit = array(values) { JsonPrimitive(it) }
    infix fun String.booleanArray(values: Collection<Boolean?>): Unit = array(values) { JsonPrimitive(it) }
    infix fun String.charArray(values: Collection<Char?>): Unit = array(values) { JsonPrimitive(it?.toString()) }
    infix fun String.enumArray(values: Collection<Enum<*>?>): Unit = array(values) { JsonPrimitive(it?.name) }
    infix fun String.elementArray(values: Collection<JsonElement?>): Unit = array(values) { it ?: JsonNull }

    infix fun String.array(inner: JsonArrayBuilder.() -> Unit) {
        builder.putJsonArray(this, inner)
    }

    @PublishedApi
    internal inline fun String.eqObject(
        allowRandomKeys: Boolean = true,
        crossinline inner:  DslJsonObjectBuilder.() -> Unit
    ) {
        builder.put(this, jsonObject(allowRandomKeys = allowRandomKeys) { inner() })
    }

    inline fun <reified T> discriminator(name: String = DefaultTestConfiguration.classDiscriminator) {
        builder.put(name, serialDescriptor<T>().serialName)
    }

    fun randomKey(value: JsonElement = generateRandomJsonElement()) {
        builder.put("%random_key_${Random.nextLong()}", value)
    }

    fun potentialRandomKey(n: Int = 2, value: JsonElement = generateRandomJsonElement()) {
        require(n >= 2)
        if (Random.nextInt(n) == 0) randomKey(value)
    }

    fun randomString(key: String, size: Int = 6) {
        builder.put(key, buildString(size.coerceAtLeast(16)) {
            repeat(6) {
                append(Random.nextInt(0, Char.MIN_SURROGATE.code).toChar())
            }
        })
    }
}