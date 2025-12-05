package dev.dokky.zerojson.ktx

import dev.dokky.zerojson.TestZeroJson
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlin.test.Test
import kotlin.test.assertFailsWith

private const val prefix = "dev.dokky.zerojson.ktx.SerialNameCollisionInSealedClassesTest"

class SerialNameCollisionInSealedClassesTest {
    @Serializable
    sealed class Base {
        @Serializable
        data class Child(val type: String, @SerialName("type2") val f: String = "2") : Base()
    }

    private fun Json(discriminator: String) = TestZeroJson { classDiscriminator = discriminator }

    @Test
    fun testCollisionWithDiscriminator() {
        assertFailsWith<SerializationException> { Json("type").encodeToString(Base.serializer(), Base.Child("a")) }
        assertFailsWith<SerializationException> { Json("type2").encodeToString(Base.serializer(), Base.Child("a")) }
        Json("f").encodeToString(Base.serializer(), Base.Child("a"))
    }

    @Serializable
    sealed class BaseCollision {
        @Serializable
        class Child() : BaseCollision()

        @Serializable
        @SerialName("$prefix.BaseCollision.Child")
        class ChildCollided() : BaseCollision()
    }

    @Test
    fun testDescriptorInitializerFailure() {
        BaseCollision.Child()
        BaseCollision.ChildCollided()
        BaseCollision.ChildCollided.serializer().descriptor // Doesn't fail
        assertFailsWith<IllegalStateException> { BaseCollision.serializer().descriptor }
    }
}
