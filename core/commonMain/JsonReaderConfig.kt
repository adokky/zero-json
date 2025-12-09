package dev.dokky.zerojson

import dev.dokky.zerojson.internal.StringBuilderWrapper

/**
 * @property expectStringQuotes see [dev.dokky.zerojson.ZeroJsonConfigurationBase.isLenient]
 * @property allowComments see [dev.dokky.zerojson.ZeroJsonConfigurationBase.allowComments]
 * @property allowSpecialFloatingPointValues see [dev.dokky.zerojson.ZeroJsonConfigurationBase.allowSpecialFloatingPointValues]
 * @property depthLimit see [dev.dokky.zerojson.ZeroJsonConfigurationBase.maxStructureDepth]
 * @property maxStringLength see [dev.dokky.zerojson.ZeroJsonConfigurationBase.maxStringLength]
 * @property allowTrailingComma see [dev.dokky.zerojson.ZeroJsonConfigurationBase.allowTrailingComma]
 */
class JsonReaderConfig internal constructor(
    val expectStringQuotes: Boolean,
    val allowComments: Boolean,
    val allowSpecialFloatingPointValues: Boolean,
    internal val stringBuilder: StringBuilderWrapper,
    internal val messageBuilder: StringBuilderWrapper,
    val depthLimit: Int,
    val allowTrailingComma: Boolean,
    val maxStringLength: Int
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
        allowTrailingComma: Boolean = ZeroJsonConfiguration.Default.allowTrailingComma,
        maxStringLength: Int = ZeroJsonConfiguration.Default.maxStringLength,
    ): this(
        expectStringQuotes = expectStringQuotes,
        allowComments = allowComments,
        allowSpecialFloatingPointValues = allowSpecialFloatingPointValues,
        stringBuilder = StringBuilderWrapper(stringBuilder),
        messageBuilder = StringBuilderWrapper(messageBuilder),
        depthLimit = depthLimit,
        allowTrailingComma = allowTrailingComma,
        maxStringLength = maxStringLength
    )

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is JsonReaderConfig) return false

        if (expectStringQuotes != other.expectStringQuotes) return false
        if (allowComments != other.allowComments) return false
        if (allowSpecialFloatingPointValues != other.allowSpecialFloatingPointValues) return false
        if (stringBuilder !== other.stringBuilder) return false
        if (messageBuilder !== other.messageBuilder) return false
        if (depthLimit != other.depthLimit) return false
        if (allowTrailingComma != other.allowTrailingComma) return false
        if (maxStringLength != other.maxStringLength) return false

        return true
    }

    override fun hashCode(): Int {
        var result = expectStringQuotes.hashCode()
        result = 31 * result + allowComments.hashCode()
        result = 31 * result + allowSpecialFloatingPointValues.hashCode()
        result = 31 * result + depthLimit
        result = 31 * result + allowTrailingComma.hashCode()
        result = 31 * result + maxStringLength
        return result
    }

    override fun toString(): String =
        "JsonReaderConfig(expectStringQuotes=$expectStringQuotes, " +
        "allowComments=$allowComments, " +
        "allowSpecialFloatingPointValues=$allowSpecialFloatingPointValues, " +
        "depthLimit=$depthLimit, " +
        "allowTrailingComma=$allowTrailingComma, " +
        "maxStringLength=$maxStringLength)"
}