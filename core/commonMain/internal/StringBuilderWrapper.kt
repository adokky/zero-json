package dev.dokky.zerojson.internal

internal class StringBuilderWrapper(
    private val initialCapacity: Int = 16,
    private val maxCapacity: Int = 100_000,
    initialBuilder: StringBuilder = StringBuilder(initialCapacity)
) {
    constructor(initialBuilder: StringBuilder, maxCapacity: Int = 100_000): this(
        initialCapacity = initialBuilder.length,
        maxCapacity = maxCapacity,
        initialBuilder = initialBuilder
    )

    internal var builder: StringBuilder = initialBuilder
        private set

    private var currentCapacity: Int = initialCapacity

    fun updateCapacity() {
        if (builder.length > currentCapacity) currentCapacity = builder.length
    }

    fun setLength(newLength: Int) {
        updateCapacity()
        builder.setLength(newLength)
        if (newLength > currentCapacity) currentCapacity = newLength
    }

    fun clearAndShrink() {
        if (currentCapacity > maxCapacity) {
            builder = StringBuilder(initialCapacity)
            currentCapacity = initialCapacity
        } else {
            builder.setLength(0)
        }
    }

    inline fun buildString(body: StringBuilder.() -> Unit): String {
//        setLengthRaw(0)
//        builder.body()
//        updateCapacity()
//        return builder.toString().also {
//            setLengthRaw(0)
//        }
        val oldLength = builder.length
        builder.body()
        updateCapacity()
        val result = builder.substring(startIndex = oldLength)
        // Kotlin bug:
        // The accessed declaration 'fun setLength(p0: Int): Unit' is declared in 'public/*package*/'
        // class 'AbstractStringBuilder', but is accessed from 'internal' inline declaration
        setLengthRaw(oldLength)
        return result
    }

    fun setLengthRaw(l: Int) = builder.setLength(l)

    fun clear() {
        updateCapacity()
        builder.setLength(0)
    }

    val length: Int get() = builder.length

    override fun toString(): String = builder.toString()

    fun removeSubstring(startIndex: Int): String {
        val result = builder.substring(startIndex)
        builder.setLength(startIndex)
        return result
    }

    fun substring(startIndex: Int): String = builder.substring(startIndex)

    fun substring(startIndex: Int, endIndex: Int): String = builder.substring(startIndex, endIndex)
}