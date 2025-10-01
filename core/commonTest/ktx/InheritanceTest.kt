@file:Suppress("EqualsOrHashCode")

package dev.dokky.zerojson.ktx

import dev.dokky.zerojson.ZeroJsonCompat
import dev.dokky.zerojson.framework.assertFailsWithMissingField
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import kotlin.test.Test
import kotlin.test.assertEquals

@Serializable
private abstract class AbstractSerializable {
    abstract val rootState: String // no backing field

    val publicState: String = "A"
}

@Serializable
private open class SerializableBase: AbstractSerializable() {
    private val privateState: String = "B" // still should be serialized

    @Transient
    private val privateTransientState = "C" // not serialized: explicitly transient

    val notAState: String // not serialized: no backing field
        get() = "D"

    override val rootState: String
        get() = "E" // still not serializable

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is SerializableBase) return false

        if (privateState != other.privateState) return false
        if (privateTransientState != other.privateTransientState) return false

        return true
    }
}

@Serializable
private class Derived(val derivedState: Int): SerializableBase() {
    override val rootState: String = "foo" // serializable!

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Derived) return false
        if (!super.equals(other)) return false

        if (derivedState != other.derivedState) return false
        if (rootState != other.rootState) return false

        return true
    }
}

@Serializable
private open class Base1(open var state1: String) {
    override fun toString(): String {
        return "Base1(state1='$state1')"
    }
}

@Serializable
private class Derived2(@SerialName("state2") override var state1: String): Base1(state1) {
    override fun toString(): String {
        return "Derived2(state1='$state1')"
    }
}

class InheritanceTest {
    private val json = ZeroJsonCompat { encodeDefaults = true }

    @Test
    fun canBeSerializedAsDerived() {
        val derived = Derived(42)
        val msg = json.encodeToString(Derived.serializer(), derived)
        assertEquals("""{"publicState":"A","privateState":"B","derivedState":42,"rootState":"foo"}""", msg)
        val d2 = json.decodeFromString(Derived.serializer(), msg)
        assertEquals(derived, d2)
    }

    @Test
    fun canBeSerializedAsParent() {
        val derived = Derived(42)
        val msg = json.encodeToString(SerializableBase.serializer(), derived)
        assertEquals("""{"publicState":"A","privateState":"B"}""", msg)
        val d2 = json.decodeFromString(SerializableBase.serializer(), msg)
        assertEquals(SerializableBase(), d2)
        // no derivedState
        assertFailsWithMissingField { json.decodeFromString(Derived.serializer(), msg) }
    }

    @Test
    fun testWithOpenProperty() {
        val d = Derived2("foo")
        val msgFull = json.encodeToString(Derived2.serializer(), d)
        assertEquals("""{"state1":"foo","state2":"foo"}""", msgFull)
        assertEquals("""{"state1":"foo"}""", json.encodeToString(Base1.serializer(), d))
        val restored = json.decodeFromString(Derived2.serializer(), msgFull)
        val restored2 = json.decodeFromString(Derived2.serializer(), """{"state1":"bar","state2":"foo"}""") // state1 is ignored anyway
        assertEquals("""Derived2(state1='foo')""", restored.toString())
        assertEquals("""Derived2(state1='foo')""", restored2.toString())
    }
}




