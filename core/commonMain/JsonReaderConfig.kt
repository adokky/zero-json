package dev.dokky.zerojson

import dev.dokky.zerojson.internal.StringBuilderWrapper
import kotlin.jvm.JvmField

/**
 * @property expectStringQuotes see [dev.dokky.zerojson.ZeroJsonConfigurationBase.isLenient]
 * @property allowComments see [dev.dokky.zerojson.ZeroJsonConfigurationBase.allowComments]
 * @property allowSpecialFloatingPointValues see [dev.dokky.zerojson.ZeroJsonConfigurationBase.allowSpecialFloatingPointValues]
 * @property depthLimit see [dev.dokky.zerojson.ZeroJsonConfigurationBase.maxStructureDepth]
 */
class JsonReaderConfig internal constructor(
    val expectStringQuotes: Boolean,
    val allowComments: Boolean,
    val allowSpecialFloatingPointValues: Boolean,
    internal val stringBuilder: StringBuilderWrapper,
    internal val messageBuilder: StringBuilderWrapper,
    val depthLimit: Int,
    val allowTrailingComma: Boolean
) {
    /**
     * @param stringBuilder used to create [String] instances of deserialized objects
     * @param messageBuilder separate [StringBuilder] used exclusively for composing error messages
     */
    constructor(
        expectStringQuotes: Boolean = !ZeroJsonConfiguration.Default.isLenient,
        allowComments: Boolean = ZeroJsonConfiguration.Default.allowComments,
        allowSpecialFloatingPointValues: Boolean = ZeroJsonConfiguration.Default.allowSpecialFloatingPointValues,
        stringBuilder: StringBuilder = StringBuilder(),
        messageBuilder: StringBuilder = StringBuilder(),
        depthLimit: Int = ZeroJsonConfiguration.Default.maxStructureDepth,
        allowTrailingComma: Boolean = ZeroJsonConfiguration.Default.allowTrailingComma
    ): this(
        expectStringQuotes,
        allowComments,
        allowSpecialFloatingPointValues,
        StringBuilderWrapper(stringBuilder),
        StringBuilderWrapper(messageBuilder),
        depthLimit,
        allowTrailingComma
    )

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is JsonReaderConfig) return false

        if (expectStringQuotes != other.expectStringQuotes) return false
        if (allowComments != other.allowComments) return false
        if (allowSpecialFloatingPointValues != other.allowSpecialFloatingPointValues) return false
        if (depthLimit != other.depthLimit) return false
        if (stringBuilder !== other.stringBuilder) return false
        if (messageBuilder !== other.messageBuilder) return false

        return true
    }

    override fun hashCode(): Int {
        var result = expectStringQuotes.hashCode()
        result = 31 * result + allowComments.hashCode()
        result = 31 * result + allowSpecialFloatingPointValues.hashCode()
        result = 31 * result + depthLimit
        return result
    }

    override fun toString(): String {
        return "JsonReaderConfig(" +
                "expectStringQuotes=$expectStringQuotes, " +
                "allowComments=$allowComments, " +
                "allowSpecialFloatingPointValues=$allowSpecialFloatingPointValues, " +
                "depthLimit=$depthLimit, " +
                "allowTrailingComma=$allowTrailingComma)"
    }
}