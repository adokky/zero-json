package dev.dokky.zerojson

import dev.dokky.zerojson.framework.*
import dev.dokky.zerojson.framework.transformers.RandomOrderInputTransformer
import dev.dokky.zerojson.framework.transformers.WrapperInputTransformer
import karamel.utils.unsafeCast
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.*
import kotlin.test.Test

class JsonElementTest: RandomizedJsonTest() {
    @Serializable
    private data class DataWithJsonElements(
        @Serializable(with = JsonObjectSerializer::class) val e1: JsonObject,
        @Serializable(with = JsonArraySerializer::class) val e2: JsonArray,
        @Serializable(with = JsonElementSerializer::class) val e3: JsonElement,
        @Serializable(with = JsonElementSerializer::class) val e4: JsonElement,
        @Serializable(with = JsonElementSerializer::class) val e5: JsonElement,
        @Serializable(with = JsonElementSerializer::class) val e6: JsonElement,
        @Serializable(with = JsonElementSerializer::class) val e7: JsonElement,
        val elementMap1: Map<
            @Serializable(with = JsonElementSerializer::class) JsonElement,
            @Serializable(with = JsonElementSerializer::class) JsonElement
        >,
        val list1: List<@Serializable(with = JsonElementSerializer::class) JsonElement>,
        val box1: Box<@Serializable(with = JsonElementSerializer::class) JsonElement?>?,
        val box2: InlineBox<@Serializable(with = JsonElementSerializer::class) JsonElement?>?
    )

    private val explicitNullsJson = TestZeroJson { explicitNulls = true }

    private inline fun test(builder: TestConfigBuilder.() -> Unit) {
        randomizedTest {
            json = explicitNullsJson
            transformers = mutableListOf(WrapperInputTransformer, RandomOrderInputTransformer)
            iterations = 1
            builder()
        }
    }

    @Test
    fun inside_object() {
        val data = DataWithJsonElements(
            jsonObject { "key" eq "value" },
            buildJsonArray { add(1); add(2); add(3) },
            JsonPrimitive(123),
            JsonPrimitive("Hello, world!"),
            JsonNull,
            JsonPrimitive(true),
            JsonPrimitive(-344.16),
            elementMap1 = mapOf(JsonPrimitive("1") to JsonPrimitive("2"), JsonPrimitive("3") to JsonPrimitive(4)),
            list1 = listOf(
                jsonObject { "key" eq "value" },
                buildJsonArray { add(1); add(2); add(3) },
                JsonPrimitive(123),
                JsonPrimitive("Hello, world!"),
                JsonNull,
                JsonPrimitive(true),
                JsonPrimitive(-344.16),
            ),
            box1 = Box(jsonObject { "k" eq 456 }),
            box2 = null
        )
        test {
            json = explicitNullsJson
            domainObject(data)
            jsonElement {
                "e1" eq data.e1
                "e2" eq data.e2
                "e3" eq data.e3
                "e4" eq data.e4
                "e5" eq data.e5
                "e6" eq data.e6
                "e7" eq data.e7
                "elementMap1" {
                    "1" eq "2"
                    "3" eq 4
                }
                "box1" { "value" eq data.box1!!.value!! }
                "box2" eq null
                "list1" elementArray data.list1
            }
        }
    }

    private inline fun <reified T: JsonElement> test(
        element: T,
        customize: TestConfigBuilder.() -> Unit = {}
    ) {
        test {
            name = "concrete serializer"
            val ser = when(T::class) {
                JsonArray::class -> JsonArraySerializer
                JsonObject::class -> JsonObjectSerializer
                else -> JsonPrimitiveSerializer
            }
            domainObject(element, ser.unsafeCast())
            jsonElement = element
            customize()
        }
        test {
            name = "base serializer"
            domainObject(element, JsonElementSerializer)
            jsonElement = element
            customize()
        }
    }

    @Test
    fun null_primitive() = test<JsonPrimitive>(JsonNull) {
        // with 'null' input, `JsonNull?` always decoded as Kotlin `null`, not `JsonNull`
        compareToString  = true
    }

    @Test
    fun integer() = test(JsonPrimitive(123))

    @Test
    fun bool() = test(JsonPrimitive(true))

    @Test
    fun string() = test(JsonPrimitive("two words"))

    @Test
    fun json_object() = test(jsonObject { "key" eq "value" })

    @Test
    fun json_array() {
        listOf(
            listOf(),
            listOf(1),
            listOf(-1, 2, -3)
        )
        .forEach { data ->
            test(JsonArray(data.map { JsonPrimitive(it) }))
        }
    }
}