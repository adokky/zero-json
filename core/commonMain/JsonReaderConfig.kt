package dev.dokky.zerojson

import kotlin.jvm.JvmField

/**
 * @property expectStringQuotes see [dev.dokky.zerojson.ZeroJsonConfigurationBase.isLenient]
 * @property allowComments see [dev.dokky.zerojson.ZeroJsonConfigurationBase.allowComments]
 * @property allowSpecialFloatingPointValues see [dev.dokky.zerojson.ZeroJsonConfigurationBase.allowSpecialFloatingPointValues]
 * @property stringBuilder used to create [String] instances of deserialized objects
 * @property messageBuilder separate [StringBuilder] used exclusively for composing error messages
 * @property depthLimit see [dev.dokky.zerojson.ZeroJsonConfigurationBase.maxStructureDepth]
 */
class JsonReaderConfig(
    val expectStringQuotes: Boolean = !ZeroJsonConfiguration.Default.isLenient,
    val allowComments: Boolean = ZeroJsonConfiguration.Default.allowComments,
    val allowSpecialFloatingPointValues: Boolean = ZeroJsonConfiguration.Default.allowSpecialFloatingPointValues,
    val stringBuilder: StringBuilder = StringBuilder(),
    val messageBuilder: StringBuilder = StringBuilder(),
    val depthLimit: Int = ZeroJsonConfiguration.Default.maxStructureDepth,
    val allowTrailingComma: Boolean = ZeroJsonConfiguration.Default.allowTrailingComma
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is JsonReaderConfig) return false

        if (expectStringQuotes != other.expectStringQuotes) return false
        if (allowComments != other.allowComments) return false
        if (allowSpecialFloatingPointValues != other.allowSpecialFloatingPointValues) return false
        if (depthLimit != other.depthLimit) return false
        if (stringBuilder != other.stringBuilder) return false
        if (messageBuilder != other.messageBuilder) return false

        return true
    }

    override fun hashCode(): Int {
        var result = expectStringQuotes.hashCode()
        result = 31 * result + allowComments.hashCode()
        result = 31 * result + allowSpecialFloatingPointValues.hashCode()
        result = 31 * result + depthLimit
        result = 31 * result + stringBuilder.hashCode()
        result = 31 * result + messageBuilder.hashCode()
        return result
    }

    override fun toString(): String = buildString {
        append("JsonReaderConfig(expectStringQuotes=")
        append(expectStringQuotes)
        append(", allowComments=")
        append(allowComments)
        append(", allowSpecialFloatingPointValues=")
        append(allowSpecialFloatingPointValues)
        append(", stringBuilder=")
        append(stringBuilder)
        append(", messageBuilder=")
        append(messageBuilder)
        append(", depthLimit=")
        append(depthLimit)
        append(")")
    }

    companion object {
        @JvmField
        val Default: JsonReaderConfig = JsonReaderConfig()
    }
}