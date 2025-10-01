package dev.dokky.zerojson.framework

import dev.dokky.zerojson.NoStackTraceSerializationException
import kotlinx.serialization.KSerializer
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

internal abstract class TestException(message: String): NoStackTraceSerializationException(message)
internal class TestEncoderException: TestException("test encoding error")
internal class TestDecoderException: TestException("test decoding error")

internal class BuggySerializer(
    val encoderFailAt: Int = 0,
    val decoderFailAt: Int = 0,
    val decoderException: Throwable = TestDecoderException(),
    val encoderException: Throwable = TestEncoderException()
): KSerializer<SimpleDataClass> {
    var serializeCalled = 0
    var deserializeCalled = 0

    override val descriptor = SimpleDataClass.serializer().descriptor

    override fun serialize(encoder: Encoder, value: SimpleDataClass) {
        if (serializeCalled == encoderFailAt) throw encoderException
        serializeCalled++
        SimpleDataClass.serializer().serialize(encoder, value)
    }

    override fun deserialize(decoder: Decoder): SimpleDataClass {
        if (deserializeCalled == decoderFailAt) throw decoderException
        deserializeCalled++
        return SimpleDataClass.serializer().deserialize(decoder)
    }
}