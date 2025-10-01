package dev.dokky.zerojson.framework

import karamel.utils.asInt

object NumbersDataSet {
    val ints8: ByteArray =
        listOf(0, 1, 2, 3, 7, 8, 9, 63, 64, 65)
            .flatMap { listOf(it.toByte(), (-it).toByte()) }
            .plus(listOf(Byte.MIN_VALUE, Byte.MAX_VALUE))
            .toByteArray()

    val ints16: ShortArray =
        listOf(
            128, 129,
            0x191, -0x191,
            0xe13, -0xe13,
            0x1107, -0x1107,
            Short.MAX_VALUE,
            Short.MIN_VALUE,
            (Short.MAX_VALUE - 1).toShort(),
            (Short.MIN_VALUE + 1).toShort())
            .plus(ints8.map { it.asInt().toShort() })
            .toShortArray()

    val ints32: IntArray = intArrayOf(
        0x23_11_07, -0x23_11_07,
        0xFF_FF_01, -0xFF_FF_01,
        0xFF_FF_7F, -0xFF_FF_7F,
        0xFF_FF_FE, -0xFF_FF_FE,
        0xFF_FF_FF, -0xFF_FF_FF,
        0x59_23_11_07, -0x59_23_11_07,
        Int.MAX_VALUE,
        Int.MIN_VALUE,
        Int.MAX_VALUE - 1,
        Int.MIN_VALUE + 1
    ) + ints16.map { it.toInt() }

    val ints64: LongArray =
        longArrayOf(
            0x04_59_23_11_07L,
            0x04_89_59_23_11_07L,
            0x04_e1_89_59_23_11_07L,
            0x04_e1_a6_89_59_23_11_07L,
            0xFF_FF_FF_FFL,
            0xFF_FF_FF_FF_FFL,
            0xFF_FF_FF_FF_FF_FFL,
            0xFF_FF_FF_FF_FF_FF_FFL,
            0L.inv()
        ).flatMap { listOf(it, -it, 1 - it, it - 1, 3 - it, it - 3) }.toLongArray() +
                longArrayOf(
                    Long.MIN_VALUE,
                    Long.MIN_VALUE + 1,
                    Long.MAX_VALUE,
                    Long.MAX_VALUE - 1
                ) + ints32.map { it.toLong() }

    // partial ints

    val ints24: IntArray  = ints32.map { (it shl 8) shr  8 }.distinct().toIntArray()
    val uints24: IntArray = ints32.map { (it shl 8) ushr 8 }.distinct().toIntArray()

    private fun smallLongs(bits: Int): LongArray {
        val tail = 64 - bits
        return ints64.map { (it shl tail) shr tail }.distinct().toLongArray()
    }

    private fun ulongs(bits: Int): LongArray {
        val tail = 64 - bits
        return ints64.map { (it shl tail) ushr tail }.distinct().toLongArray()
    }

    val ints40: LongArray = smallLongs(40)
    val ints48: LongArray = smallLongs(48)
    val ints56: LongArray = smallLongs(56)

    val uints40: LongArray = ulongs(40)
    val uints48: LongArray = ulongs(48)
    val uints56: LongArray = ulongs(56)

    // floats

    val floats32: FloatArray = getFloat32()
    val floats64: DoubleArray = getFloat64()

    fun getFloat32(
        max: Int = 1000,
        maxDiv: Int = 1000,
        special: Boolean = true
    ): FloatArray = buildList(max * maxDiv + 100) {
        add(0f)
        add(Float.MAX_VALUE)
        add(Float.MIN_VALUE)
        if (special) {
            add(Float.NEGATIVE_INFINITY)
            add(Float.POSITIVE_INFINITY)
            add(Float.NaN)
        }
        for (int in 1..max) {
            for (div in 1..maxDiv) {
                val f = int.toFloat() / div
                add(f)
                add(-f)
            }
        }
    }.toFloatArray()

    fun getFloat64(
        max: Int = 1000,
        maxDiv: Int = 1000,
        special: Boolean = true
    ): DoubleArray = buildList(max * maxDiv + 100) {
        add(0.0)
        add(Double.MAX_VALUE)
        add(Double.MIN_VALUE)
        if (special) {
            add(Double.NEGATIVE_INFINITY)
            add(Double.POSITIVE_INFINITY)
            add(Double.NaN)
        }
        for (int in 1..max) {
            for (div in 1..maxDiv) {
                val f = int.toDouble() / div
                add(f)
                add(-f)
            }
        }
    }.toDoubleArray()
}