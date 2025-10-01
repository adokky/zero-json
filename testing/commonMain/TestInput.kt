package dev.dokky.zerojson.framework

import dev.dokky.zerojson.ZeroJson
import io.kodec.buffers.Buffer
import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.JsonElement

internal interface TestInput {
    val json: ZeroJson
    val domainObject: Any?
    val serializer: KSerializer<Any?>
    val jsonElement: JsonElement
    val composerConfig: BaseJsonComposerConfig
    val stringInput: String
    val binaryInput: Buffer
    val binaryOffset: Int
}