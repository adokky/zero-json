[![Maven Central Version](https://img.shields.io/maven-central/v/io.github.adokky/zero-json-core)](https://mvnrepository.com/artifact/io.github.adokky/zero-json-core)
[![javadoc](https://javadoc.io/badge2/io.github.adokky/zero-json-core/javadoc.svg)](https://javadoc.io/doc/io.github.adokky/zero-json-core)
[![GitHub license](https://img.shields.io/badge/license-Apache%20License%202.0-blue.svg?style=flat)](https://www.apache.org/licenses/LICENSE-2.0)

# zero-json

Fast and powerful implementation of JSON format for [kotlinx-serialization](https://github.com/Kotlin/kotlinx.serialization).

* **Compatibility**: Can be used as a drop-in replacement for [kotlinx-serialization-json](https://github.com/Kotlin/kotlinx.serialization/tree/master).
* **Zero extra allocation**: only deserialized objects are allocated. Exceptions include `Float`/`Double` types (zero allocations in most cases) and some kotlinx serializers that use `ChunkedDecoder`.
* **Zero-copy**: deserialize objects without intermediate copies by wrapping any byte buffer. Buffer wrapping is done through a simple `Buffer` interface that requires only `size` property and `get` method to be implemented.
* **Map and object inlining**: mark a class property with `@JsonInline` to inline its serialized form. Only final classes and `Map` instances can be inlined.
* **Value subclasses**: zero-json provides out-of-the-box support for polymorphic value subclasses, automatically serializing them with a type and value field:
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

* The input JSON string must be fully loaded into memory before decoding. This library is not suitable for deserializing large JSON files. This limitation is irrelevant for typical REST APIs, where request and response sizes are limited
* No [array polymorphism](https://kotlinlang.org/api/kotlinx.serialization/kotlinx-serialization-json/kotlinx.serialization.json/-json-builder/use-array-polymorphism.html)
* Option to serialize structured map keys as escaped strings.
* [External][external-ser] serializers and [partial][partial-ser] custom serializers are not supported because of the [bug][descriptor-bug].
* Duplicate JSON object keys are not allowed
* On JS: no `dynamic` support
* On JVM: There is no support for `InputStream`/`OutputStream`. Nearly all I/O libraries and frameworks provide access to underlying array or buffer abstractions. Wrapping these is the intended way to use this library. If you absolutely require `InputStream` or `OutputStream`, you can wrap them around an `ArrayBuffer`. However, for any streaming workload, we recommend using the original `kotlinx-serialization-json` instead.


  [external-ser]: https://github.com/Kotlin/kotlinx.serialization/blob/master/docs/serializers.md#deriving-external-serializer-for-another-kotlin-class-experimental
  [partial-ser]: https://github.com/Kotlin/kotlinx.serialization/blob/master/formats/json-tests/commonTest/src/kotlinx/serialization/features/PartiallyCustomSerializerTest.kt
  [descriptor-bug]: https://github.com/Kotlin/kotlinx.serialization/issues/2549

## Setup

Setup Google repository (zero-json uses `androidx.collection:collection` under the hood):

```kotlin
repositories {
    google()
}
```

### Standalone (`zero-json-core`)

This option allows you to use all the features specific to zero-json.

```kotlin
implementation("io.github.adokky:zero-json-core:0.2.0")
```

### Drop-in replacement  (`zero-json-kotlinx`)

Use this if you only want faster `kotlinx-serialization-json` and nothing more.

```kotlin
implementation("io.github.adokky:zero-json-kotlinx:0.2.0")
```

If you have transitive `kotlinx-serialization-json` somewhere in dependency graph, setup capability resolution:

```kotlin
configurations.all {
    resolutionStrategy.capabilitiesResolution.withCapability("org.jetbrains.kotlinx:kotlinx-serialization-json") {
        select(candidates.single { (it.id as? ModuleComponentIdentifier)?.group == "io.github.adokky" })
    }
}
```

Both `zero-json-core` and `zero-json-kotlinx` can be used simultaneously.

## @JsonInline

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
class Location(val country: String, val city: String)

println(ZeroJson.encodeToString(
    Person(
        name = "Alex",
        age = 20,
        location = Location("France", "Paris"),
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
    "avatar": "https://cdn.com/avatar23535"
}
```

