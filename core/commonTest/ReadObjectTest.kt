package dev.dokky.zerojson

import dev.dokky.zerojson.internal.JsonReaderImpl
import kotlinx.serialization.SerializationException
import kotlin.test.*

class ReadObjectTest: AbstractJsonReaderTest() {
    @Test
    fun simple() {
        test("""
            {
              "key": "value",
              "number": 123,
              "bool1": true,
              "bool2": false,
              "nullable": null
            }
        """)
        {
            readObject {
                assertEquals("key", readKey())
                assertEquals("value", readValue { readString() })

                assertEquals("number", readKey())
                assertEquals(123, readValue { readInt() })

                assertEquals("bool1", readKey())
                assertTrue(readValue { readBoolean() })

                assertEquals("bool2", readKey())
                assertFalse(readValue { readBoolean() })

                assertEquals("nullable", readKey())
                readValue { readNull() }
            }
        }
    }

    @Test
    fun unquoted() {
        testWithInput(
            """
            {
              "a key": value, // single line comment
              "a number": 123,
              /*
                  Multi
                  Line
                  Comment
              */
              bool1: true,
              bool2: false,
              some.nullable: null
            }
            """
        ) { input ->
            val reader = JsonReaderImpl(input, JsonReaderConfig(expectStringQuotes = false, allowComments = true))

            reader.readObject {
                assertEquals("a key", readKey())
                assertEquals("value", readValue { readString() })

                assertEquals("a number", readKey())
                assertEquals(123, readValue { readInt() })

                assertEquals("bool1", readKey())
                assertTrue(readValue { readBoolean() })

                assertEquals("bool2", readKey())
                assertFalse(readValue { readBoolean() })

                assertEquals("some.nullable", readKey())
                readValue { readNull() }
            }
        }
    }

    @Test
    fun malformed() {
        testWithInput(
            """
            {
              "a key": [}
            }
            """
        ) {
            assertFailsWith<SerializationException> {
                reader.readObject {
                    assertEquals("a key", readKey())
                    readValue {
                        readArray {
                            readItem { readString() }
                        }
                    }
                }
            }
        }
    }

    @Test
    fun empty_array() {
        testWithInput("[ ]") {
            reader.readArray {
                assertFalse(hasMoreItems())
            }
        }
    }

    @Test
    fun array() {
        testWithInput("[1, 2, 3]") {
            val list = buildList<Int> {
                reader.readArray {
                    while (hasMoreItems()) {
                        add(readItem { readInt() })
                    }
                }
            }

            assertEquals(listOf(1, 2, 3), list)
        }
    }
}