package dev.dokky.zerojson

import dev.dokky.zerojson.internal.SerialKindFlags
import dev.dokky.zerojson.internal.getFlags
import kotlinx.serialization.descriptors.PolymorphicKind
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.SerialKind
import kotlinx.serialization.descriptors.StructureKind
import kotlin.test.Test
import kotlin.test.assertEquals

class SerialKindFlagsTest {
    private class Flags(
        val isClassLike: Boolean,
        val isMap: Boolean,
        val commaAfterEachElement: Boolean,
        val isQuoted: Boolean,
        val isCompound: Boolean,
        val isList: Boolean = false
    ) {
        fun check(flags: SerialKindFlags) {
            assertEquals(isClassLike,           flags.isClassLike)
            assertEquals(isMap,                 flags.isMap)
            assertEquals(commaAfterEachElement, flags.commaAfterEachElement)
            assertEquals(isQuoted,              flags.isQuoted)
            assertEquals(isCompound,            flags.isCompound)
            assertEquals(isList,                flags.isList)

            assertEquals(
                SerialKindFlags(
                    isClassLike = isClassLike,
                    isMap = isMap,
                    commaAfterEachElement = commaAfterEachElement,
                    isQuoted = isQuoted,
                    isList = isList
                ),
                flags
            )

            assertEquals(flags.isClassLike or flags.isMap, flags.isJsonObject)
            assertEquals(flags.isJsonObject or flags.isList, flags.isCompound)
        }

        fun check(vararg kind: SerialKind) = kind.forEach { check(it.getFlags()) }
    }

    @Test
    fun test() {
        Flags(isClassLike = true, isMap = false, commaAfterEachElement = true, isQuoted = false, isCompound = true).check(
            StructureKind.CLASS,
            StructureKind.OBJECT,
            PolymorphicKind.SEALED,
            PolymorphicKind.OPEN
        )

        Flags(isClassLike = false, isMap = false, commaAfterEachElement = false, isQuoted = false, isCompound = false).check(
            PrimitiveKind.BOOLEAN,
            PrimitiveKind.BYTE,
            PrimitiveKind.SHORT,
            PrimitiveKind.INT,
            PrimitiveKind.LONG,
            PrimitiveKind.FLOAT,
            PrimitiveKind.DOUBLE,
            SerialKind.ENUM,
            SerialKind.CONTEXTUAL
        )

        Flags(isClassLike = false, isMap = false, commaAfterEachElement = false, isQuoted = true, isCompound = false).check(
            PrimitiveKind.CHAR,
            PrimitiveKind.STRING
        )

        Flags(
            isClassLike = false,
            isMap = false,
            commaAfterEachElement = true,
            isQuoted = false,
            isCompound = true,
            isList = true
        ).check(
            StructureKind.LIST
        )

        Flags(isClassLike = false, isMap = true, commaAfterEachElement = false, isQuoted = false, isCompound = true).check(
            StructureKind.MAP
        )
    }
}