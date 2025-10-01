package dev.dokky.zerojson.ktx

import dev.dokky.zerojson.ZeroJsonCompat
import dev.dokky.zerojson.framework.assertStringFormAndRestored
import kotlinx.serialization.Polymorphic
import kotlinx.serialization.PolymorphicSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic
import kotlinx.serialization.modules.subclass
import kotlin.test.Test

class SealedPolymorphismTest {
    @Serializable
    private data class FooHolder(
        val someMetadata: Int,
        val payload: List<@Polymorphic Foo>
    )

    @Serializable
    @SerialName("Foo")
    private sealed class Foo {
        @Serializable
        @SerialName("Bar")
        data class Bar(val bar: Int) : Foo()
        @Serializable
        @SerialName("Baz")
        data class Baz(val baz: String) : Foo()
    }

    private val sealedModule = SerializersModule {
        polymorphic(Foo::class) {
            subclass(Foo.Bar.serializer())
            subclass(Foo.Baz.serializer())
        }
    }

    private val json = ZeroJsonCompat { serializersModule = sealedModule }

    @Test
    fun testSaveSealedClassesList() {
        assertStringFormAndRestored(
            """{"someMetadata":42,"payload":[
            |{"type":"Bar","bar":1},
            |{"type":"Baz","baz":"2"}]}""".trimMargin().replace("\n", ""),
            FooHolder(42, listOf(Foo.Bar(1), Foo.Baz("2"))),
            FooHolder.serializer(),
            json
        )
    }

    @Test
    fun testCanSerializeSealedClassPolymorphicallyOnTopLevel() {
        assertStringFormAndRestored(
            """{"type":"Bar","bar":1}""",
            Foo.Bar(1),
            PolymorphicSerializer(Foo::class),
            json
        )
    }
}
