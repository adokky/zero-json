package dev.dokky.zerojson

import kotlinx.serialization.SerialInfo

/**
 * Serialized form of property marked with [JsonInline] will be inlined into parent class.
 * Applicable only for serializable elements of kinds:
 * * [kotlinx.serialization.descriptors.StructureKind.MAP]
 * * [kotlinx.serialization.descriptors.StructureKind.OBJECT]
 *
 * Example:
 *
 *    @Serializable
 *    class Person(
 *        val name: String,
 *        val age: Int,
 *        @JsonInline val location: Location,
 *        @JsonInline val extra: Map<String, String>?
 *    )
 *
 *    @Serializable
 *    class Location(
 *        val country: String,
 *        val address: String
 *    )
 *
 *    println(ZeroJson.encodeToString(
 *        Person(
 *            name = "Alex",
 *            age = 20,
 *            location = Location("France", "Paris, Bd Carnot, 37"),
 *            extra = mapOf("avatar" to "https://cdn.com/avatar23535")
 *        )
 *    ))
 *
 * prints:
 *
 * ```json
 * {
 *     "name": "Alex",
 *     "age": 20,
 *     "country":  "France",
 *     "address": "Paris, Bd Carnot, 37",
 *     "avatar": "https://cdn.com/avatar23535"
 * }
 * ```
 */
@Suppress("OPT_IN_USAGE")
@Retention(AnnotationRetention.BINARY)
@Target(AnnotationTarget.PROPERTY)
@SerialInfo
annotation class JsonInline
