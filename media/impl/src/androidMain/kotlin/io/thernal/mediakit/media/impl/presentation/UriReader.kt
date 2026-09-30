package io.thernal.mediakit.media.impl.presentation

import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.provider.OpenableColumns
import io.thernal.mediakit.media.api.domain.MediaFile
import io.thernal.mediakit.media.api.domain.MimeType
import io.thernal.mediakit.media.api.domain.Rejection
import io.thernal.mediakit.media.impl.domain.PickedItem
import io.thernal.mediakit.media.impl.domain.checkFile
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException

/**
 * Reads a content URI a picker returned. Type and reported size are checked before the bytes are
 * read, so an oversized video is refused without loading it; the size is checked again on the bytes.
 */
internal suspend fun Context.readUri(
    uri: Uri,
    allowedMimeTypes: List<String>,
    maxFileSizeBytes: Long?,
    fallbackName: String,
    dispatcher: CoroutineDispatcher = Dispatchers.IO,
): PickedItem {
    return withContext(dispatcher) {
        val metadata = queryMetadata(uri)
        val name = metadata.name?.takeIf(String::isNotBlank) ?: fallbackName
        val mimeType = contentResolver.getType(uri) ?: MimeType.Application.OCTET_STREAM
        checkFile(
            name = name,
            mimeType = mimeType,
            size = metadata.size,
            allowedMimeTypes = allowedMimeTypes,
            maxFileSizeBytes = maxFileSizeBytes,
        )?.let { return@withContext PickedItem.Refused(it) }
        val bytes = try {
            contentResolver.openInputStream(uri)?.use { it.readBytes() }
        } catch (_: IOException) {
            null
        } catch (_: SecurityException) {
            null
        } ?: return@withContext PickedItem.Refused(Rejection.Unreadable(name))
        val file = MediaFile(name = name, bytes = bytes, mimeType = mimeType, uri = uri.toString())
        checkFile(
            name = name,
            mimeType = mimeType,
            size = file.size,
            allowedMimeTypes = allowedMimeTypes,
            maxFileSizeBytes = maxFileSizeBytes,
        )?.let(PickedItem::Refused) ?: PickedItem.Accepted(file)
    }
}

private class UriMetadata(
    val name: String?,
    val size: Long?,
)

private fun Cursor.stringOrNull(index: Int): String? {
    return takeIf { index >= 0 && !isNull(index) }?.getString(index)
}

private fun Cursor.longOrNull(index: Int): Long? {
    return takeIf { index >= 0 && !isNull(index) }?.getLong(index)
}

private fun Context.queryMetadata(uri: Uri): UriMetadata {
    val projection = arrayOf(OpenableColumns.DISPLAY_NAME, OpenableColumns.SIZE)
    return contentResolver.query(uri, projection, null, null, null)?.use { cursor ->
        if (!cursor.moveToFirst()) {
            return@use null
        }
        val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
        val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
        UriMetadata(
            name = cursor.stringOrNull(nameIndex),
            size = cursor.longOrNull(sizeIndex),
        )
    } ?: UriMetadata(name = null, size = null)
}
