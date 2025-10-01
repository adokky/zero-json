[![Maven Central Version](https://img.shields.io/maven-central/v/io.github.adokky/zero-json-core)](https://mvnrepository.com/artifact/io.github.adokky/zero-json-core)
[![javadoc](https://javadoc.io/badge2/io.github.adokky/zero-json-core/javadoc.svg)](https://javadoc.io/doc/io.github.adokky/zero-json-core)
[![GitHub license](https://img.shields.io/badge/license-Apache%20License%202.0-blue.svg?style=flat)](https://www.apache.org/licenses/LICENSE-2.0)

# zero-json

Fast and powerful implementation of JSON format for [kotlinx-serialization](https://github.com/Kotlin/kotlinx.serialization).

## Features

* **Compatibility**: Can be used as a drop-in replacement for [kotlinx-serialization-json](https://github.com/Kotlin/kotlinx.serialization/tree/master) (JVM and Android only).
* **Zero-copy**: Deserialize objects directly from any byte buffer. This approach introduces a major limitation, described [here](#differences-from-kotlinx-serialization-json).
* **Zero extra allocation**: only deserialized objects are allocated. Exceptions include `Float`/`Double` types (zero allocations in most cases) and some kotlinx serializers that use `ChunkedDecoder`.
* **Map and object inlining**: mark a class property with `@JsonInline` to inline its serialized form. Only final classes and `Map` instances can be inlined.
* **Value subclasses**: Out-of-the-box support for polymorphic value subclasses:
```kotlin
@Serializable sealed interface Base
@Serializable value class Foo(val int: Int): Base
val s = ZeroJson.encodeToString<Base>(Foo(42))
println(s)
// { "type": "Foo", "value": 42 }
println(ZeroJson.decodeFromString<Base>(s))
// 42
```
* **Advanced deserializers (experimental)**: custom serializers has access to underlying parser (`JsonReader`). That allows implementing simple and efficient content-based polymorphism without extra allocations of `JsonElement`.

## Differences from `kotlinx-serialization-json`

* The input JSON string must be fully loaded into memory before decoding. This library is not suitable for deserializing large JSON files. However, this limitation is irrelevant for:
    * Typical REST APIs, where request and response sizes are limited
    * Non-blocking setups, since all existing serializers are blocking and share the same requirement of having complete data in memory before processing
* No [array polymorphism](https://kotlinlang.org/api/kotlinx.serialization/kotlinx-serialization-json/kotlinx.serialization.json/-json-builder/use-array-polymorphism.html)
* Option to serialize structured map keys as escaped strings.
* [External][external-ser] serializers and [partial][partial-ser] custom serializers are not supported because of the [bug][descriptor-bug].
* Duplicate JSON object keys are not allowed
* On JS: no `dynamic` (de)serialization 
* On JVM: There is no support for `InputStream`/`OutputStream`. Nearly all I/O libraries and frameworks provide access to underlying array or buffer abstractions. Wrapping these is the intended way to use this library. If you absolutely require `InputStream` or `OutputStream` functionality, you can wrap them around an `ArrayBuffer`. However, for any streaming workload, we recommend using the original `kotlinx-serialization-json` instead.


  [external-ser]: https://github.com/Kotlin/kotlinx.serialization/blob/master/docs/serializers.md#deriving-external-serializer-for-another-kotlin-class-experimental
  [partial-ser]: https://github.com/Kotlin/kotlinx.serialization/blob/master/formats/json-tests/commonTest/src/kotlinx/serialization/features/PartiallyCustomSerializerTest.kt
  [descriptor-bug]: https://github.com/Kotlin/kotlinx.serialization/issues/2549

### Setup

3 options: drop-in replacement, standalone library and both.

#### Standalone (`zero-json-core`)

This option allows you to use all the features specific to zero-json.

```kotlin
dependencies {
    commonMainImplementation("io.github.adokky:zero-json-core:0.1.0")
}
```

#### Drop-in replacement  (`zero-json-kotlinx`)

Use this if you only want faster `kotlinx-serialization-json` and nothing more.

```kotlin
dependencies {
    commonMainImplementation("io.github.adokky:zero-json-kotlinx:0.1.0")
}
```

#### Mixed

You can use both if you have a bunch of custom serializers tied to `kotlinx-serialization-json`, and you still want the advanced functionality of zero-json.

### @JsonInline

Quick example:

```kotlin
@Serializable
class Person(
    val name: String,
    val age: Int, 
    @JsonInline val location: Location,
    @JsonInline val extra: Map<String, String>?,
)

@Serializable
class Location(
  val country: String,
  val city: String,
  val street: String,
  val house: String
)

println(ZeroJson.encodeToString(
    Person(
        name = "Alex",
        age = 20,
        location = Location("France", "Paris", "Bd Carnot", "37"),
        extra = mapOf("avatar" to "https://cdn.com/avatar23535")
    )
))
```

```json
{
    "name": "Alex",
    "age": 20,
    "country":  "France",
    "city": "Paris",
    "street":  "Bd Carnot",
    "house": "37",
    "avatar": "https://cdn.com/avatar23535"
}
```

