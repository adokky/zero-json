package dev.dokky.zerojson.framework

import kotlin.test.Test
import kotlin.test.assertEquals

fun <T> assertSetEquals(
    expected: Collection<T>,
    actual: Collection<T>,
    itemSeparator: String = ", ",
    formatItem: StringBuilder.(T) -> Unit = { append(it) }
) {
    val diff = getDiff(expected, actual)

    if (diff.missing.isEmpty() && diff.extra.isEmpty()) return

    fun StringBuilder.appendItems(items: Collection<T>): StringBuilder {
        var first = true
        for (item in items) {
            if (first) first = false else append(itemSeparator)
            formatItem(item)
        }
        return this
    }

    fun StringBuilder.printItems(label: String, items: Collection<T>): StringBuilder {
        append(label)
        append(" (")
        append(items.size)
        append("): ")
        appendItems(items)
        appendLine()
        return this
    }

    throw AssertionError(
        buildString {
            append(when {
                diff.intersection.isEmpty() -> when {
                    diff.missing.isNotEmpty() && actual.isEmpty() -> "Set is empty\n"
                    else -> "Sets are completely different:\n"
                }
                else -> "Sets are not equal:\n"
            })

            if (diff.missing.isNotEmpty()) printItems("Missing", diff.missing)
            if (diff.extra.isNotEmpty()) printItems("Extra", diff.extra)
            if (diff.intersection.isNotEmpty()) printItems("Match", diff.intersection)
        }
    )
}

data class DiffResult<T>(val intersection: MutableList<T>, val missing: MutableList<T>, val extra: MutableList<T>)

fun <T> getDiff(
    expected: Collection<T>,
    actual: Collection<T>
): DiffResult<T> {
    val set2 = actual.toMutableList()

    val intersection = ArrayList<T>()
    val missing = ArrayList<T>()

    for (element in expected) {
        if (set2.remove(element)) {
            intersection.add(element)
        } else {
            missing.add(element)
        }
    }

    return DiffResult(intersection = intersection, missing = missing, extra = set2)
}

class SetEqualityTest {
    @Test
    fun empty() {
        getDiff<Unit>(listOf(), listOf()).also { diff ->
            assertEquals(emptyList(), diff.missing)
            assertEquals(emptyList(), diff.extra)
            assertEquals(emptyList(), diff.intersection)
        }
    }

    @Test
    fun single() {
        getDiff(listOf(1), listOf(1)).also { diff ->
            assertEquals(emptyList(), diff.missing)
            assertEquals(emptyList(), diff.extra)
            assertEquals(listOf(1), diff.intersection)
        }
    }

    @Test
    fun many() {
        getDiff(listOf(2, 4, 1), listOf(1, 2, 5)).also { diff ->
            assertEquals(listOf(4), diff.missing)
            assertEquals(listOf(5), diff.extra)
            assertEquals(setOf(1, 2), diff.intersection.toSet())
        }
    }
}