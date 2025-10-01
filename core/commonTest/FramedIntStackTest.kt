package dev.dokky.zerojson

import dev.dokky.zerojson.framework.assertFailsWithMessage
import dev.dokky.zerojson.internal.FramedIntStack
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class FramedIntStackTest {
    private val STACK_IS_EMPTY = "stack is empty"
    private val ARRAY_IS_EMPTY = "array is empty"

    @Test
    fun simple() {
        val stack = FramedIntStack()

        assertFailsWithMessage<IllegalStateException>(STACK_IS_EMPTY) { stack.size }
        assertFailsWithMessage<IllegalStateException>(STACK_IS_EMPTY) { stack.add(43) }
        assertFailsWithMessage<IllegalStateException>(STACK_IS_EMPTY) { stack.removeLast() }

        stack.enter()

        assertEquals(0, stack.size)
        assertTrue(stack.isEmpty())

        stack.add(67)
        assertEquals(1, stack.size)
        assertFalse(stack.isEmpty())

        assertEquals(67, stack.removeLast())
        assertEquals(0, stack.size)

        stack.add(98)
        assertEquals(1, stack.size)

        stack.enter()

        stack.add(11)
        stack.add(22)
        assertEquals(2, stack.size)

        assertEquals(22, stack.removeLast())
        assertEquals(11, stack.removeLast())

        assertFailsWithMessage<IllegalStateException>(ARRAY_IS_EMPTY) { stack.removeLast() }
        assertEquals(0, stack.size)

        stack.leave()
        assertEquals(1, stack.size)

        assertEquals(98, stack.removeLast())
        assertEquals(0, stack.size)

        stack.leave()
        assertFailsWithMessage<IllegalStateException>(STACK_IS_EMPTY) { stack.size }
    }

    @Test
    fun many_elements() {
        val stack = FramedIntStack()

        fun assertArrayIsEmpty() {
            assertEquals(0, stack.size)
            assertFailsWithMessage<IllegalStateException>(ARRAY_IS_EMPTY) { stack.removeLast() }
        }

        fun addElements(count: Int) {
            assertArrayIsEmpty()
            repeat(count) { stack.add(it) }
            assertEquals(count, stack.size)
        }

        fun removeElements(count: Int) {
            assertEquals(count, stack.size)
            for (it in count-1 downTo 0) {
                assertEquals(it, stack.removeLast())
            }
            assertArrayIsEmpty()
        }

        stack.enter()
        addElements(1000)

        stack.enter()
        addElements(400)

        stack.enter()
        addElements(1500)
        stack.leave()

        removeElements(400)

        stack.leave()
        removeElements(1000)

        stack.leave()
        assertFailsWithMessage<IllegalStateException>(STACK_IS_EMPTY) { stack.removeLast() }
    }
}