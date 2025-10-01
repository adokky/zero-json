package dev.dokky.zerojson

import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.serializer
import kotlin.test.Test
import kotlin.test.assertEquals

class ClassInfoTest {
    @Serializable
    class TestClass<T>(
        val int: Int,
        val str: String,
        val list: List<T>,
        val g1: GenericClass<String>,
        val g2: GenericClass<GenericClass<T>>,
        val g3: GenericClass<T>,
    )

    @Serializable
    data class GenericClass<T>(val data: T)

    @Test
    fun descriptor_specialization() {
        val root = TestClass.serializer(String.serializer()).descriptor
        assertEquals(ListSerializer(String.serializer()).descriptor, root.getElementDescriptor(2))
        assertEquals(GenericClass.serializer(GenericClass.serializer(String.serializer())).descriptor, root.getElementDescriptor(4))
    }
}