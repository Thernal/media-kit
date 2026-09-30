package io.thernal.mediakit.media.impl.presentation

import io.thernal.mediakit.media.api.domain.MediaFile
import io.thernal.mediakit.media.api.domain.MimeType
import io.thernal.mediakit.media.api.domain.Rejection
import io.thernal.mediakit.media.impl.domain.PickedItem
import io.thernal.mediakit.media.impl.domain.checkFile
import io.thernal.mediakit.media.impl.domain.mimeTypeForExtension
import kotlinx.cinterop.ExperimentalForeignApi
import platform.Foundation.NSData
import platform.Foundation.NSFileManager
import platform.Foundation.NSFileSize
import platform.Foundation.NSNumber
import platform.Foundation.NSURL
import platform.Foundation.dataWithContentsOfURL
import platform.UniformTypeIdentifiers.UTType
import platform.UniformTypeIdentifiers.UTTypeAudio
import platform.UniformTypeIdentifiers.UTTypeImage
import platform.UniformTypeIdentifiers.UTTypeItem
import platform.UniformTypeIdentifiers.UTTypeMovie
import platform.UniformTypeIdentifiers.UTTypeText

/** The document picker's content types for MIME types and families. */
internal fun contentTypesFor(mimeTypes: List<String>): List<UTType> {
    if (mimeTypes.isEmpty() || MimeType.ALL in mimeTypes) {
        return listOf(UTTypeItem)
    }
    return mimeTypes.mapNotNull { mimeType ->
        when (mimeType) {
            MimeType.Image.ALL -> UTTypeImage
            MimeType.Video.ALL -> UTTypeMovie
            "audio/*" -> UTTypeAudio
            "text/*" -> UTTypeText
            else -> UTType.typeWithMIMEType(mimeType)
        }
    }.ifEmpty { listOf(UTTypeItem) }
}

/** The MIME type of a type identifier (`public.heic` → `image/heic`). */
internal fun mimeTypeForIdentifier(identifier: String): String? {
    return UTType.typeWithIdentifier(identifier)?.preferredMIMEType
}

/** Reads a file URL the document picker returned (a copy in the app's sandbox). */
@OptIn(ExperimentalForeignApi::class)
internal fun readFileUrl(
    url: NSURL,
    allowedMimeTypes: List<String>,
    maxFileSizeBytes: Long?,
): PickedItem {
    val name = url.lastPathComponent ?: "file"
    val extension = url.pathExtension.orEmpty()
    val mimeType = UTType.typeWithFilenameExtension(extension)?.preferredMIMEType ?: mimeTypeForExtension(extension)
    val accessed = url.startAccessingSecurityScopedResource()
    try {
        val reportedSize = url.path?.let { path ->
            (
                NSFileManager.defaultManager.attributesOfItemAtPath(
                    path,
                    null,
                )?.get(NSFileSize) as? NSNumber
                )?.longLongValue
        }
        checkFile(
            name = name,
            mimeType = mimeType,
            size = reportedSize,
            allowedMimeTypes = allowedMimeTypes,
            maxFileSizeBytes = maxFileSizeBytes,
        )?.let { return PickedItem.Refused(it) }
        val data = NSData.dataWithContentsOfURL(url) ?: return PickedItem.Refused(Rejection.Unreadable(name))
        val file = MediaFile(name = name, bytes = data.toByteArray(), mimeType = mimeType, uri = url.absoluteString)
        return checkFile(
            name = name,
            mimeType = mimeType,
            size = file.size,
            allowedMimeTypes = allowedMimeTypes,
            maxFileSizeBytes = maxFileSizeBytes,
        )?.let(PickedItem::Refused) ?: PickedItem.Accepted(file)
    } finally {
        if (accessed) {
            url.stopAccessingSecurityScopedResource()
        }
    }
}
