package dev.dokky.zerojson.internal

import dev.dokky.zerojson.ZeroJsonConfiguration
import kotlinx.serialization.InternalSerializationApi

internal abstract class AutoCloseableStack<T: AutoCloseable>(maxDepth: Int, first: T): StackPool<T>(maxDepth, first) {
    override fun close(item: T) { item.close() }
}

@OptIn(InternalSerializationApi::class)
internal inline fun <T: AutoCloseable> AutoCloseableStack(
    config: ZeroJsonConfiguration,
    first: T,
    crossinline create: (previous: T) -> T
): AutoCloseableStack<T> = object :  AutoCloseableStack<T>(config.maxStructureDepth, first) {
    override fun create(previous: T): T = create(previous)
}