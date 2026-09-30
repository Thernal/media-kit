package io.thernal.mediakit.media.impl.presentation

import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import java.io.File
import java.util.UUID

/** The kit's own FileProvider for camera output; see the module's AndroidManifest. */
internal class CaptureFileProvider : FileProvider()

private const val CAPTURES_DIRECTORY = "mediakit-captures"

/** A new, empty file the camera app may write into, as a content URI it can be granted. */
internal fun Context.newCaptureUri(): Uri {
    val directory = File(cacheDir, CAPTURES_DIRECTORY).apply { mkdirs() }
    val file = File(directory, "${UUID.randomUUID()}.jpg")
    return FileProvider.getUriForFile(this, "$packageName.mediakit.captures", file)
}
