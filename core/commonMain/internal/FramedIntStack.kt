package dev.dokky.zerojson.internal

internal class FramedIntStack(initialCapacity: Int = DEFAULT_FRAME_CAPACITY * 2) {
    private var data = IntArray(initialCapacity.coerceAtLeast(MIN_INITIAL_CAPACITY))
    private var dataSize = 0

    fun ensureCapacity(requiredFreeSpace: Int) {
        val requiredCapacity = dataSize + requiredFreeSpace
        if (data.size < requiredCapacity) {
            resize(requiredCapacity)
        }
    }

    private fun resize(requiredCapacity: Int = 0) {
        data = data.copyOf(newSize = (data.size * 1.6).toInt().coerceAtLeast(requiredCapacity))
    }

    fun enter() {
        ensureCapacity(DEFAULT_FRAME_CAPACITY)
        data[dataSize] = 0
        dataSize++
    }

    fun leave(clear: Boolean = true) {
        val frameSize = data[dataSize - 1] + 1
        val frameStart = dataSize - frameSize
        if (clear) data.fill(0, fromIndex = frameStart, toIndex = dataSize)
        dataSize = frameStart
    }

    fun removeLast(): Int {
        val newSize = dataSize - 1
        if (newSize < 0) error("stack is empty")

        val frameSize = data[newSize]
        if (frameSize == 0) throw IllegalStateException("array is empty")
        data[newSize] = 0

        val result = data[newSize - 1]
        data[newSize - 1] = frameSize - 1

        dataSize = newSize
        return result
    }

    fun add(int: Int) {
        ensureCapacity(1)

        val sizeIndex = dataSize - 1
        if (sizeIndex < 0) error("stack is empty")

        val data = data
        val frameSize = data[sizeIndex]
        data[sizeIndex] = int
        data[dataSize] = frameSize + 1
        dataSize++
    }

    val size: Int get() {
        val idx = dataSize - 1
        if (idx < 0) error("stack is empty")
        return data[idx]
    }

    fun isEmpty(): Boolean = size == 0

    fun clear() {
        data.fill(0, 0, dataSize)
        dataSize = 0
    }

    private companion object {
        const val MIN_INITIAL_CAPACITY = 16
        const val DEFAULT_FRAME_CAPACITY = 16
    }
}