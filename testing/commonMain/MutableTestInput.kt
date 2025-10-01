package dev.dokky.zerojson.framework

import dev.dokky.zerojson.ZeroJson
import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.JsonElement

interface MutableTestInput {
    var json: ZeroJson
    var domainObject: Any?
    var serializer: KSerializer<Any?>
    var jsonElement: JsonElement
    var composerConfig: BaseJsonComposerConfig
    var composer: JsonComposer

    fun transformTextInput(transform: (StringBuilder) -> Unit)
}