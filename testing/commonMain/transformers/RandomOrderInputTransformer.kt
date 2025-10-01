package dev.dokky.zerojson.framework.transformers

import dev.dokky.zerojson.framework.MutableTestInput
import dev.dokky.zerojson.framework.TestInputTransformer
import dev.dokky.zerojson.framework.TestTarget
import dev.dokky.zerojson.framework.elementsEquals
import karamel.utils.ThreadLocal
import karamel.utils.mapToArrayList
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlin.random.Random

/**
 * Guarantees at least one permutation if there is
 * at least one [JsonElement] with 2 or more child elements.
 */
open class RandomOrderInputTransformer(
	val shuffleArrays: Boolean = false
): TestInputTransformer(
	name = "Random order",
	code = "RO",
	targets = TestTarget.decoders()
) {
	private var forceShuffleCandidates = 0L

	override fun transform(input: MutableTestInput) {
		forceShuffleCandidates = 0

		val initial = input.jsonElement
		input.jsonElement = initial.shuffle()

        if (forceShuffleCandidates > 0) {
            input.jsonElement = initial.forceShuffle()
        }
    }

	private fun JsonArray.swapFirstTwoElements(): JsonArray {
		val newElements = this.toMutableList()
		val e0 = newElements[0]
		newElements[0] = newElements[1].forceShuffle()
		newElements[1] = e0.forceShuffle()
		return JsonArray(newElements)
	}

	private fun JsonObject.swapFirstTwoElements(): JsonObject {
		val newEntries = LinkedHashMap<String, JsonElement>(entries.size)
		val i = entries.iterator()

		// swap first 2 elements
		val e1 = i.next()
		val e2 = i.next()
        newEntries[e2.key] = e2.value
        newEntries[e1.key] = e1.value

        // the rest is unchanged
		while (i.hasNext()) {
			val (k, v) = i.next()
            newEntries[k] = v
        }

		return JsonObject(newEntries)
	}

	private fun forceShuffleAttempt(): Boolean =
		if (forceShuffleCandidates > 0) Random.nextLong(forceShuffleCandidates--) == 0L else false

	private fun JsonElement.forceShuffle(): JsonElement = when (this) {
        is JsonObject -> forceShuffle()
        is JsonArray -> forceShuffle()
        else -> this
    }

	private fun JsonObject.forceShuffle(): JsonObject = when {
        size > 1 && forceShuffleAttempt() -> swapFirstTwoElements()
        else -> JsonObject(mapValues { it.value.forceShuffle() })
    }

	private fun JsonArray.forceShuffle(): JsonArray = when {
        size > 1 && shuffleArrays && forceShuffleAttempt() -> swapFirstTwoElements()
        else -> JsonArray(map { it.forceShuffle() })
    }

	private fun JsonElement.shuffle(): JsonElement = when(this) {
        is JsonObject -> shuffle()
        is JsonArray -> shuffle()
        else -> this
    }

	private fun JsonObject.shuffle(): JsonObject {
		if (isEmpty()) return this

		if (size > 1) forceShuffleCandidates++

		return JsonObject(
			entries
				.mapToArrayList { it.key to it.value.shuffle() }
				.also { it.shuffle() }
				.toMap(LinkedHashMap(size))
		).also {
			if (forceShuffleCandidates >= 0 && !elementsEquals(it))
				forceShuffleCandidates = Long.MIN_VALUE
		}
	}

	private fun JsonArray.shuffle(): JsonArray {
		if (isEmpty()) return this

		if (size > 1 && shuffleArrays) forceShuffleCandidates++

		return JsonArray(
			this
				.mapToArrayList { it.shuffle() }
				.also { if (shuffleArrays) it.shuffle() }
		).also {
			if (forceShuffleCandidates >= 0 && this != it)
				forceShuffleCandidates = Long.MIN_VALUE
		}
	}

	companion object: RandomOrderInputTransformer() {
		private val tl = ThreadLocal { RandomOrderInputTransformer() }

		override fun transform(input: MutableTestInput) {
			tl.get().transform(input)
		}
	}
}

