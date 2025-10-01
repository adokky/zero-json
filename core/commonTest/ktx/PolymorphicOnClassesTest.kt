package dev.dokky.zerojson.ktx

import dev.dokky.zerojson.framework.isNative
import dev.dokky.zerojson.framework.isWasm
import kotlinx.serialization.*
import kotlinx.serialization.descriptors.*
import kotlinx.serialization.internal.AbstractPolymorphicSerializer
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic
import kotlinx.serialization.modules.subclass
import kotlin.reflect.KClass
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class PolymorphicOnClassesTest {

    // this has implicit @Polymorphic
    private interface IMessage {
        val body: String
    }

    // and this class too has implicit @Polymorphic
    @Serializable
    private abstract class Message : IMessage {
        abstract override val body: String
    }

    @Polymorphic
    @Serializable
    @SerialName("SimpleMessage") // to cut out package prefix
    private open class SimpleMessage : Message() {
        override var body: String = "Simple"
    }

    @Serializable
    @SerialName("DoubleSimpleMessage")
    private class DoubleSimpleMessage(val body2: String) : SimpleMessage()

    @Serializable
    @SerialName("MessageWithId")
    private open class MessageWithId(val id: Int, override val body: String) : Message()

    @Serializable
    private class Holder(
        val iMessage: IMessage,
        val iMessageList: List<IMessage>,
        val message: Message,
        val msgSet: Set<Message>,
        val simple: SimpleMessage,
        // all above should be polymorphic
        val withId: MessageWithId // but this not
    )

    private fun genTestData(): Holder {
        var cnt = -1
        fun gen(): MessageWithId {
            cnt++
            return MessageWithId(cnt, "Message #$cnt")
        }

        return Holder(gen(), listOf(gen(), gen()), gen(), setOf(SimpleMessage()), DoubleSimpleMessage("DoubleSimple"), gen())
    }

    @Suppress("UNCHECKED_CAST")
    private val testModule = SerializersModule {
        listOf(Message::class, IMessage::class, SimpleMessage::class).forEach { clz ->
            polymorphic(clz as KClass<IMessage>) {
                subclass(SimpleMessage.serializer())
                subclass(DoubleSimpleMessage.serializer())
                subclass(MessageWithId.serializer())
            }
        }
    }

    @Test
    fun testDescriptor() {
        val polyDesc = Holder.serializer().descriptor.elementDescriptors.first()
        assertEquals(PolymorphicSerializer(IMessage::class).descriptor, polyDesc)
        assertEquals(2, polyDesc.elementsCount)
        assertEquals(PrimitiveKind.STRING, polyDesc.getElementDescriptor(0).kind)
    }

    private fun SerialDescriptor.inContext(module: SerializersModule): List<SerialDescriptor> = when (kind) {
        PolymorphicKind.OPEN -> module.getPolymorphicDescriptors(this)
        else -> error("Expected this function to be called on OPEN descriptor")
    }

    @Test
    fun testResolvePolymorphicDescriptor() {
        val polyDesc = Holder.serializer().descriptor.elementDescriptors.first() // iMessage: IMessage

        assertEquals(PolymorphicKind.OPEN, polyDesc.kind)

        val inheritors = polyDesc.inContext(testModule)
        val names = listOf("SimpleMessage", "DoubleSimpleMessage", "MessageWithId").toSet()
        assertEquals(names, inheritors.map(SerialDescriptor::serialName).toSet(), "Expected correct inheritor names")
        assertTrue(inheritors.all { it.kind == StructureKind.CLASS }, "Expected all inheritors to be CLASS")
    }

    @Test
    fun testDocSampleWithAllDistinct() {
        fun allDistinctNames(descriptor: SerialDescriptor, module: SerializersModule) = when (descriptor.kind) {
            is PolymorphicKind.OPEN -> module.getPolymorphicDescriptors(descriptor)
                .map { it.elementNames.toList() }.flatten().toSet()
            is SerialKind.CONTEXTUAL -> module.getContextualDescriptor(descriptor)?.elementNames?.toList().orEmpty().toSet()
            else -> descriptor.elementNames.toSet()
        }

        val polyDesc = Holder.serializer().descriptor.elementDescriptors.first() // iMessage: IMessage
        assertEquals(setOf("id", "body", "body2"), allDistinctNames(polyDesc, testModule))
        assertEquals(setOf("id", "body"), allDistinctNames(MessageWithId.serializer().descriptor, testModule))
    }

    @Test
    fun testSerializerLookupForInterface() {
        // On JVM and JS IR it can be supported via reflection/runtime hacks
        // on Native, unfortunately, only with intrinsics.
        if (isNative() || isWasm()) return
        val msgSer = serializer<IMessage>()
        assertEquals(IMessage::class, (msgSer as AbstractPolymorphicSerializer).baseClass)
    }

    @Test
    fun testSerializerLookupForAbstractClass() {
        val absSer = serializer<Message>()
        assertEquals(Message::class, (absSer as AbstractPolymorphicSerializer).baseClass)
    }
}
