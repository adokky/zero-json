package dev.dokky.zerojson.framework

import kotlin.jvm.JvmStatic

enum class TestTarget(val input: DataType, val output: DataType) {
    TextToObject   (DataType.Text,     DataType.Domain),
    BinaryToObject (DataType.Text,     DataType.Domain),
    TextToTree     (DataType.Text,     DataType.JsonTree),
    BinaryToTree   (DataType.Text,     DataType.JsonTree),
    TreeToObject   (DataType.JsonTree, DataType.Domain),
    ObjectToText   (DataType.Domain,   DataType.Text),
    ObjectToBinary (DataType.Domain,   DataType.Text),
    ObjectToTree   (DataType.Domain,   DataType.JsonTree);

    enum class DataType { Text, JsonTree, Domain }

    companion object {
        @JvmStatic fun decoders(): List<TestTarget> = entries.decoders()
        @JvmStatic fun encoders(): List<TestTarget> = entries.encoders()
    }
}

fun TestTarget.hasDataType(type: TestTarget.DataType): Boolean = input == type || output == type

fun TestTarget.isText(): Boolean = hasDataType(TestTarget.DataType.Text)
fun TestTarget.isDomain(): Boolean = hasDataType(TestTarget.DataType.Domain)
fun TestTarget.isTree(): Boolean = hasDataType(TestTarget.DataType.JsonTree)

fun TestTarget.isDecoder(): Boolean = input  != TestTarget.DataType.Domain
fun TestTarget.isEncoder(): Boolean = output != TestTarget.DataType.Domain

fun Iterable<TestTarget>.decoders(): List<TestTarget> = filter { it.isDecoder() }
fun Iterable<TestTarget>.encoders(): List<TestTarget> = filter { it.isEncoder() }