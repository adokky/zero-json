package dev.dokky.zerojson.ktx

import dev.dokky.zerojson.ZeroJson
import dev.dokky.zerojson.internal.JsonContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic
import kotlinx.serialization.modules.subclass
import kotlin.test.Test
import kotlin.test.assertFailsWith

private const val prefix = "dev.dokky.zerojson.ktx.SerialNameCollisionTest"

class SerialNameCollisionTest {
    interface IBase

    @Serializable
    abstract class Base : IBase

    @Serializable
    data class Derived(val type: String, val type2: String) : Base()

    @Serializable
    data class DerivedCustomized(
        @SerialName("type") val t: String, @SerialName("type2") val t2: String, val t3: String
    ) : Base()

    @Serializable
    @SerialName("$prefix.Derived")
    data class DerivedRenamed(val type: String, val type2: String) : Base()

    private fun Json(discriminator: String, context: SerializersModule): ZeroJson {
        val json = ZeroJson {
            classDiscriminator = discriminator
            serializersModule = context
        }
        JsonContext.useThreadLocal(json) {
            // we only need to trigger JsonContext validation
        }
        return json
    }

    @Test
    fun testCollisionWithDiscriminator() {
        val module = SerializersModule {
            polymorphic(Base::class) {
                subclass(Derived.serializer())
            }
        }

        assertFailsWith<IllegalArgumentException> { Json("type", module) }
        assertFailsWith<IllegalArgumentException> { Json("type2", module) }
        Json("type3", module) // OK
    }

    @Test
    fun testCollisionWithDiscriminatorViaSerialNames() {
        val module = SerializersModule {
            polymorphic(Base::class) {
                subclass(DerivedCustomized.serializer())
            }
        }

        assertFailsWith<IllegalArgumentException> { Json("type", module) }
        assertFailsWith<IllegalArgumentException> { Json("type2", module) }
        assertFailsWith<IllegalArgumentException> { Json("t3", module) }
        Json("t4", module) // OK
    }
}
