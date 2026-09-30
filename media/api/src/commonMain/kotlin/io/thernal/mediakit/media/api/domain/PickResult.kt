package io.thernal.mediakit.media.api.domain

import androidx.compose.runtime.Immutable

/** What a picker returns. Every outcome is a value; none is thrown. */
@Immutable
sealed interface PickResult {
    /**
     * At least one file was picked and passed the checks. Files that did not are in [rejected] — when
     * several were picked, the good ones still arrive.
     */
    data class Picked(
        val files: List<MediaFile>,
        val rejected: List<Rejection> = emptyList(),
    ) : PickResult {
        /** The first file — the only one, unless the picker allowed several. */
        val file: MediaFile
            get() = files.first()
    }

    /** Everything picked failed the checks. */
    data class Rejected(val rejections: List<Rejection>) : PickResult

    /** The user closed the picker without choosing. */
    data object Cancelled : PickResult

    /** The camera was asked for and the permission refused. */
    data object PermissionDenied : PickResult

    /** The source cannot open here: no camera (a simulator), or nothing on screen to present from. */
    data object Unavailable : PickResult
}

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
