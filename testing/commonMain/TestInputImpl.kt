package dev.dokky.zerojson.framework

import dev.dokky.zerojson.TestZeroJson
import dev.dokky.zerojson.ZeroJson
import io.kodec.buffers.Buffer
import io.kodec.buffers.MutableDataBuffer
import io.kodec.buffers.SubBufferWrapper
import karamel.utils.unsafeCast
import kotlinx.serialization.KSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlin.random.Random
import kotlin.test.fail

internal class TestInputImpl(
    json: ZeroJson,
    override var domainObject: Any?,
    override var serializer: KSerializer<Any?>,
    jsonElement: JsonElement,
    override var composerConfig: BaseJsonComposerConfig,
    override var composer: JsonComposer
): TestInput, MutableTestInput {
    var stackTracesEnabled = true

    private var originalJson = json
    private var _noStackTracesJson: ZeroJson? = null

    internal var expectedFailure: TestConfig.ExpectedFailure? = null

    private var _jsonElementWithoutNullKeys: JsonElement? = null
    val jsonElementWithoutNullKeys: JsonElement
        get() = _jsonElementWithoutNullKeys
            ?: jsonElement.removeNullKeys().also { _jsonElementWithoutNullKeys = it }

    override var jsonElement: JsonElement = jsonElement
        set(value) {
            if (value !== field) _jsonElementWithoutNullKeys = null
            field = value
        }

    override var json: ZeroJson
        get() = if (stackTracesEnabled) originalJson else noStackTracesJson
        set(value) {
            _noStackTracesJson = null
            originalJson = value
        }

    val noStackTracesJson: ZeroJson get() {
        var result = _noStackTracesJson
        if (result == null) {
            result = ZeroJson(originalJson) { fullStackTraces = false }
            _noStackTracesJson = result
        }
        return result
    }

    var textTransformer: ((StringBuilder) -> Unit)? = null
        private set

    var transformerIndex = -1
        private set
    override var stringInput = ""
        private set
    override var binaryOffset: Int = 0
        private set
    var isReady = false
        private set

    private val buffer = SubBufferWrapper(Buffer.Empty)
    override val binaryInput: Buffer get() = buffer

    fun clear() {
        stringInput = ""
        transformerIndex = -1
        binaryOffset = 0
        buffer.setSource(Buffer.Empty)
        empty.copyTo(this)
        textTransformer = null
        expectedFailure = null
        stackTracesEnabled = true
        isReady = false
    }

    fun initState(
        transformerIndex: Int,
        jsonString: String,
        destinationBuffer: MutableDataBuffer,
        destinationOffset: Int
    ) {
        this.binaryOffset = Random.nextInt(7)
        this.transformerIndex = transformerIndex
        this.stringInput = jsonString

        val encodedStringSize = try {
            destinationBuffer.putStringUtf8(destinationOffset + binaryOffset, jsonString)
        } catch (e: IndexOutOfBoundsException) {
            fail("probably too big input (" +
                    "buffer.size=${destinationBuffer.size}, " +
                    "offset=${destinationOffset + binaryOffset}, " +
                    "transformerIndex=$transformerIndex" +
                    "):\n$jsonString", e)
        }
        buffer.setSource(destinationBuffer,
            start = destinationOffset,
            endExclusive = destinationOffset + binaryOffset + encodedStringSize
        )
        isReady = true
    }

    override fun transformTextInput(transform: (StringBuilder) -> Unit) {
        if (textTransformer != null) error("text input transformer is already set")
        textTransformer = transform
    }

    internal fun applyTransform(input: StringBuilder) {
        textTransformer?.let { it(input) }
    }

    internal fun copyTo(destination: TestInputImpl) {
        destination.json                = json
        destination.domainObject        = domainObject
        destination.serializer          = serializer
        destination.jsonElement         = jsonElement
        destination.composerConfig      = composerConfig
        destination.composer            = composer
    }

    fun equalsTo(other: TestInputImpl): Boolean {
        if (this === other) return true

        if (domainObject != other.domainObject) return false
        if (serializer != other.serializer) return false
        if (composerConfig != other.composerConfig) return false
        if (composer != other.composer) return false
        if (!jsonElement.orderSensitiveEquals(other.jsonElement)) return false
        if (json != other.json) return false
        if (textTransformer != other.textTransformer) return false

        return true
    }

    override fun toString(): String = buildString {
        append("TestInputImpl(originalJson=").append(originalJson)
        append(", expectedFailure=").append(expectedFailure)
        append(", jsonElement=").append(jsonElement)
        append(", json=").append(json)
        append(", transformerIndex=").append(transformerIndex)
        append(", stringInput='").append(stringInput)
        append("', binaryOffset=").append(binaryOffset)
        append(", buffer=").append(buffer)
        append(", binaryInput=").append(binaryInput)
        append(", textTransformer=").append(textTransformer)
        append(")")
    }


    internal companion object {
        private val empty = empty()

        fun empty() = TestInputImpl(
            json = TestZeroJson,
            domainObject = null,
            serializer = Unit.serializer().unsafeCast(),
            jsonElement = JsonNull,
            composerConfig = BaseJsonComposerConfig(),
            composer = DefaultJsonComposer(StringBuilder(0))
        )
    }
}