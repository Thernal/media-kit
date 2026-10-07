package io.thernal.mediakit.media.api.domain

import androidx.compose.runtime.Immutable

/** Why one picked file was not accepted. The app words it; the kit carries no strings. */
@Immutable
sealed interface Rejection {
    val name: String

    data class TooLarge(
        override val name: String,
        val limitBytes: Long,
        val actualBytes: Long,
    ) : Rejection

    data class UnsupportedType(
        override val name: String,
        val mimeType: String,
    ) : Rejection

    /** The platform returned the file but it could not be read or decoded. */
    data class Unreadable(override val name: String) : Rejection
}
