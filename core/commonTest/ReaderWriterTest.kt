package dev.dokky.zerojson

import dev.dokky.zerojson.internal.JsonReaderImpl
import dev.dokky.zerojson.internal.JsonTextWriter
import dev.dokky.zerojson.internal.ZeroUtf8TextReader
import io.kodec.buffers.ArrayBuffer
import io.kodec.text.BufferTextWriter
import karamel.utils.MapEntry
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ReaderWriterTest {
    private val buffer = ArrayBuffer(100)
    private val input = ZeroUtf8TextReader().also { it.startReadingFrom(buffer) }
    private val reader = JsonReaderImpl(input, config = JsonReaderConfig())

    private val writerBuffer = ArrayBuffer(1000)
    private val writer = JsonTextWriter(BufferTextWriter(writerBuffer))

    @Test
    fun test() {
        val obj = object {
            val e1 = MapEntry("Тестовый ключ 1", "Тестовое значение")
            val e2 = MapEntry("Тестовый ключ 2", -123)
            val e3 = MapEntry("! Тестовый ключ 3", floatArrayOf(-0.12f, 2.0f))
        }

        writer.run {
            beginObject()
                writeString(obj.e1.key)
                colon()
                writeString(obj.e1.value)
                comma()

                writeString(obj.e2.key)
                colon()
                writeNumber(obj.e2.value)
                comma()

                writeString(obj.e3.key)
                colon()
                beginArray()
                    writeNumber(obj.e3.value[0])
                    comma()
                    writeNumber(obj.e3.value[1])
                endArray()
            endObject()
        }

        buffer.setArray(writerBuffer.toByteArray())
        input.startReadingFrom(buffer)

        reader.run {
            expectBeginObject()

                assertEquals(obj.e1.key, readString())
                expectColon()
                assertEquals(obj.e1.value, readString())
                expectComma()

                assertEquals(obj.e2.key, readString())
                expectColon()
                assertEquals(obj.e2.value, readInt())
                expectComma()

                assertEquals(obj.e3.key, readString())
                expectColon()
                expectBeginArray()
                    assertTrue(obj.e3.value[0].equals(readFloat()))
                    expectComma()
                    assertTrue(obj.e3.value[1].equals(readFloat()))
                expectEndArray()

            expectEndObject()
        }

        buffer.setArray(writerBuffer.toByteArray())
        input.startReadingFrom(buffer)

        reader.readObject {
            assertEquals(obj.e1.key, readKey())
            assertEquals(obj.e1.value, readValue { readString() })

            assertEquals(obj.e2.key, readKey())
            assertEquals(obj.e2.value, readValue { readInt() })

            assertEquals(obj.e3.key, readKey())
            assertContentEquals(obj.e3.value, readValue {
                FloatArray(2).also { array ->
                    expectBeginArray()
                    array[0] = readFloat()
                    expectComma()
                    array[1] = readFloat()
                    expectEndArray()
                }
            })
        }
    }
}