package dev.dokky.zerojson.framework

import dev.dokky.zerojson.ZeroJson
import dev.dokky.zerojson.framework.TestConfig.ExpectedFailureForMode
import karamel.utils.Box
import karamel.utils.filterToArrayList
import karamel.utils.unsafeCast
import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.JsonElement

class TestConfigBuilder @PublishedApi internal constructor() {
    private var domainObject: Box<Any?>? = null
    private var serializer: KSerializer<*>? = null

    private var _jsonElement: JsonElement? = null
    var jsonElement: JsonElement
        get() = _jsonElement ?: uninitializedJsonElement()
        set(value) { _jsonElement = value }

    var json: ZeroJson = TestConfig.DefaultPrototype.json
    var name: String? = TestConfig.DefaultPrototype.name
    var iterations: Int = TestConfig.DefaultPrototype.iterations
    var modesPerIteration: Int = TestConfig.DefaultPrototype.modesPerIteration

    var transformers: MutableList<TestInputTransformer> = TestConfig.DefaultTransformers.toMutableList()

    /**
     * Compare deserialized object and [domainObject] by calling
     * [toString][Any.toString] instead of [equals][Any.equals].
     * Useful with absent or buggy [equals][Any.equals] implementation.
     */
    var compareToString: Boolean = TestConfig.DefaultPrototype.compareToString

    private val exclude = HashSet<TestTarget>()
    private val expectFailureForTargets = HashMap<TestTarget, TestConfig.ExpectedFailure>()
    private val expectFailureForModes = ArrayList<ExpectedFailureForMode>()

    private val disabledTransformerFilters = ArrayList<(TestInputTransformer) -> Boolean>()

    fun disableTransformerIf(predicate: (TestInputTransformer) -> Boolean) {
        transformers.removeAll(predicate)
        disabledTransformerFilters += predicate
    }

    fun exclude(targets: Collection<TestTarget>) {
        exclude += targets
    }

    fun includeOnly(targets: Collection<TestTarget>) {
        exclude += (TestTarget.entries - targets.toSet())
    }

    fun expectFailure(failure: TestConfig.ExpectedFailure, targets: Iterable<TestTarget>) {
        for (t in targets) {
            require(t !in exclude) {
                "ExpectedFailure was configured for excluded target '$t'"
            }

            expectFailureForTargets[t] = failure
        }
    }

    fun expectFailureIfMode(failure: TestConfig.ExpectedFailure, match: (JsonTestMode) -> Boolean) {
        expectFailureForModes.add(ExpectedFailureForMode(match, failure))
    }

    fun <T> domainObject(obj: T, serializer: KSerializer<T>) {
        this.domainObject = Box(obj)
        this.serializer = serializer
    }

    fun <T> toConfig(): TestConfig<T> = TestConfig(
        domainObject = (domainObject ?: uninitializedDomainObject()).value.unsafeCast(),
        jsonElement = jsonElement,
        serializer = (serializer ?: uninitializedDomainObject()).unsafeCast(),
        json = json,
        compareToString = compareToString,
        exclude = exclude,
        expectTargetFailure = expectFailureForTargets,
        expectModeFailure = expectFailureForModes,
        name = name,
        iterations = iterations,
        modesPerIteration = modesPerIteration,
        transformers = this.transformers
            .filterToArrayList { tr -> !disabledTransformerFilters.any { f -> f(tr) } }
            .ifEmpty { error("'transformers' is empty") }
    )

    private fun uninitializedJsonElement(): Nothing = error("'jsonElement' is not initialized")

    private fun uninitializedDomainObject(): Nothing = error("'domainObject' is not initialized")
}

inline fun TestConfig(builder: TestConfigBuilder.() -> Unit): TestConfig<*> =
    TestConfigBuilder().apply(builder).toConfig<Any?>()

