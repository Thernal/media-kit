package io.thernal.mediakit.media.impl.presentation

import io.thernal.mediakit.media.api.domain.MimeType
import io.thernal.mediakit.media.api.domain.PickResult
import io.thernal.mediakit.media.impl.domain.PickedItem
import io.thernal.mediakit.media.impl.domain.prepareImage
import io.thernal.mediakit.media.impl.domain.toPickResult
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import platform.Foundation.NSURL
import platform.UIKit.UIDocumentPickerDelegateProtocol
import platform.UIKit.UIDocumentPickerViewController
import platform.darwin.NSObject

/**
 * The document picker. [asImages] routes what it returns through the image pipeline (the image
 * picker's documents source); otherwise files are checked against [allowedMimeTypes] only.
 */
internal class DocumentDelegate(
    private val scope: CoroutineScope,
    private val asImages: Boolean,
    private val allowedMimeTypes: () -> List<String>,
    private val maxFileSizeBytes: () -> Long?,
    private val deliver: (PickResult) -> Unit,
) : NSObject(), UIDocumentPickerDelegateProtocol {
    override fun documentPicker(
        controller: UIDocumentPickerViewController,
        didPickDocumentsAtURLs: List<*>,
    ) {
        val urls = didPickDocumentsAtURLs.filterIsInstance<NSURL>()
        if (urls.isEmpty()) {
            deliver(PickResult.Cancelled)
            return
        }
        scope.launch { deliver(urls.map { read(it) }.toPickResult()) }
    }

    private suspend fun read(url: NSURL): PickedItem {
        if (!asImages) {
            return readFileUrl(url, allowedMimeTypes(), maxFileSizeBytes())
        }
        return when (val read = readFileUrl(url, listOf(MimeType.Image.ALL), null)) {
            is PickedItem.Accepted -> prepareImage(read.file, maxFileSizeBytes(), IosImageCodec)
            is PickedItem.Refused -> read
        }
    }

    override fun documentPickerWasCancelled(controller: UIDocumentPickerViewController) {
        deliver(PickResult.Cancelled)
    }
}
