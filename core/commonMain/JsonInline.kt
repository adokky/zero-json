package dev.dokky.zerojson

import kotlinx.serialization.SerialInfo

/**
 * Serialized form of property marked with [JsonInline] will be inlined into parent class.
 * Applicable only for serializable elements of kinds:
 * * [kotlinx.serialization.descriptors.StructureKind.MAP]
 * * [kotlinx.serialization.descriptors.StructureKind.CLASS]
 *
 * Example:
 *
 *     @Serializable
 *     class Person(
 *         val name: String,
 *         val age: Int,
 *         @JsonInline val location: Location,
 *         @JsonInline val extra: Map<String, String>?
 *     )
 *
 *     @Serializable
 *     class Location(@JsonInline val country: Country, val city: String)
 *
 *     @Serializable
 *     class Country(
 *         @JsonNames("countryName") val name: String,
 *         @JsonNames("countryCode") val code: Int
 *     )
 *
 *     println(ZeroJson.encodeToString(
 *         Person(
 *             name = "Alex",
 *             age = 44,
 *             location = Location(
 *                 country = Country("Dreamland", 1234),
 *                 city = "SimCity"
 *             ),
 *             extra = mapOf("avatar" to "https://cdn.example/picture23535")
 *         )
 *     ))
 *
 * prints:
 *
 * ```json
 * {
 *     "name": "Alex",
 *     "age": 44,
 *     "countryName":  "Dreamland",
 *     "countryCode":  1234,
 *     "city": "SimCity",
 *     "avatar": "https://cdn.example/picture23535"
 * }
 * ```
 */
@Suppress("OPT_IN_USAGE")
@Retention(AnnotationRetention.BINARY)
@Target(AnnotationTarget.PROPERTY)
@SerialInfo
annotation class JsonInline
