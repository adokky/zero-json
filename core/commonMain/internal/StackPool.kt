

package dev.dokky.zerojson.internal

import kotlinx.serialization.SerializationException

// TODO 2-way intrusive linked list instead of this
internal abstract class StackPool<T: Any>(val maxDepth: Int, first: T) {
    private val items = ArrayList<T>().also { it.add(first) }
    var acquired = 0
        private set

    protected abstract fun create(previous: T): T
    protected abstract fun close(item: T)

    fun get(index: Int): T = items[index]

    fun next(): T {
        val item = if (acquired >= items.size) {
            if (acquired == maxDepth) {
                throw SerializationException("reached max structure depth $maxDepth")
            }
            create(previous = items.last()).also {
                items.add(it)
            }
        } else {
            items[acquired]
        }
        acquired++
        return item
    }

    inline fun next(init: T.() -> Unit): T = next().apply(init)

    fun release(item: T? = null) {
        val newDepth = acquired - 1
        val removed = items[newDepth]
        if (DebugMode && item != null && removed !== item) error("stack: $removed, item: $item")
        close(removed)
        acquired = newDepth // update 'depth' only after successful clear()
    }

    fun close() {
        if (acquired == 0) return
        repeat(acquired) { i -> close(items[i]) }
        acquired = 0
    }
}