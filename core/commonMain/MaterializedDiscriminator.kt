package dev.dokky.zerojson

import kotlinx.serialization.SerialInfo

/**
 * Allows discriminator to physically present in marked class.
 *
 * Has no effect on non-sealed classes.
 *
 * If base class is marked then all **direct** subclasses are also allowed to have discriminator property.
 */
@Suppress("OPT_IN_USAGE")
@SerialInfo
@Retention(AnnotationRetention.BINARY)
@Target(AnnotationTarget.CLASS)
annotation class MaterializedDiscriminator