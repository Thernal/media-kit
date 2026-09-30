package io.thernal.mediakit.media.impl.domain

import io.thernal.mediakit.media.api.domain.MediaFile
import io.thernal.mediakit.media.api.domain.PickResult
import io.thernal.mediakit.media.api.domain.Rejection

/** One item a platform picker returned, read or refused. */
internal sealed interface PickedItem {
    data class Accepted(val file: MediaFile) : PickedItem

    data class Refused(val rejection: Rejection) : PickedItem
}

/** What the caller receives for everything one pick returned. Nothing returned is a cancel. */
internal fun List<PickedItem>.toPickResult(): PickResult {
    val files = filterIsInstance<PickedItem.Accepted>().map { it.file }
    val rejections = filterIsInstance<PickedItem.Refused>().map { it.rejection }
    return when {
        files.isNotEmpty() -> PickResult.Picked(files = files, rejected = rejections)
        rejections.isNotEmpty() -> PickResult.Rejected(rejections)
        else -> PickResult.Cancelled
    }
}
