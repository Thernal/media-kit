package io.thernal.mediakit.sample.shared

import androidx.compose.foundation.layout.size
import io.thernal.mediakit.media.api.domain.PickResult
import io.thernal.mediakit.media.api.domain.Rejection

internal fun PickResult.describe(): String {
    return when (this) {
        is PickResult.Picked -> buildString {
            append("Picked ${files.size}: ${files.joinToString { "${it.name} (${it.size / KILOBYTE} KB)" }}")
            if (rejected.isNotEmpty()) {
                append("\nRejected: ${rejected.joinToString { it.describe() }}")
            }
        }

        is PickResult.Rejected -> "Rejected: ${rejections.joinToString { it.describe() }}"

        PickResult.Cancelled -> "Cancelled"

        PickResult.PermissionDenied -> "Camera permission denied"

        PickResult.Unavailable -> "Not available here (no camera?)"
    }
}

private fun Rejection.describe(): String {
    return when (this) {
        is Rejection.TooLarge -> "$name is ${actualBytes / KILOBYTE} KB, over ${limitBytes / KILOBYTE} KB"
        is Rejection.UnsupportedType -> "$name is $mimeType"
        is Rejection.Unreadable -> "$name could not be read"
    }
}

internal const val KILOBYTE = 1024L
