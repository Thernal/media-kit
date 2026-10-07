package io.thernal.mediakit.media.impl.presentation

import io.thernal.mediakit.media.api.domain.MediaFile
import io.thernal.mediakit.media.api.domain.MimeType
import io.thernal.mediakit.media.api.domain.PickResult
import io.thernal.mediakit.media.api.domain.Rejection
import io.thernal.mediakit.media.impl.domain.PickedItem
import io.thernal.mediakit.media.impl.domain.prepareImage
import io.thernal.mediakit.media.impl.domain.toPickResult
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import platform.PhotosUI.PHPickerResult
import platform.PhotosUI.PHPickerViewController
import platform.PhotosUI.PHPickerViewControllerDelegateProtocol
import platform.UniformTypeIdentifiers.UTTypeImage
import platform.darwin.NSObject
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

/** The photo library (PHPicker): no permission, up to the picker's selection limit. */
internal class GalleryDelegate(
    private val scope: CoroutineScope,
    private val maxFileSizeBytes: () -> Long?,
    private val deliver: (PickResult) -> Unit,
) : NSObject(), PHPickerViewControllerDelegateProtocol {
    override fun picker(
        picker: PHPickerViewController,
        didFinishPicking: List<*>,
    ) {
        picker.dismissViewControllerAnimated(true, completion = null)
        val results = didFinishPicking.filterIsInstance<PHPickerResult>()
        if (results.isEmpty()) {
            deliver(PickResult.Cancelled)
            return
        }
        scope.launch {
            val items = results.mapIndexed { index, result ->
                when (val loaded = result.load(fallbackName = "image_${index + 1}")) {
                    is PickedItem.Accepted -> prepareImage(loaded.file, maxFileSizeBytes(), IosImageCodec)
                    is PickedItem.Refused -> loaded
                }
            }
            deliver(items.toPickResult())
        }
    }
}

/** The original bytes in the first image type the item offers — HEIC stays HEIC until prepareImage. */
private suspend fun PHPickerResult.load(fallbackName: String): PickedItem {
    val provider = itemProvider
    val identifier = provider.registeredTypeIdentifiers
        .filterIsInstance<String>()
        .firstOrNull {
            provider.hasItemConformingToTypeIdentifier(
                it,
            ) && mimeTypeForIdentifier(it)?.startsWith("image/") == true
        }
        ?: UTTypeImage.identifier
    val mimeType = mimeTypeForIdentifier(identifier) ?: MimeType.Image.JPEG
    val extension = mimeType.substringAfter('/').replace("jpeg", "jpg")
    val name = "${provider.suggestedName ?: fallbackName}.$extension"
    val bytes = suspendCoroutine<ByteArray?> { continuation ->
        provider.loadDataRepresentationForTypeIdentifier(identifier) { data, _ ->
            continuation.resume(data?.toByteArray())
        }
    } ?: return PickedItem.Refused(Rejection.Unreadable(name))
    return PickedItem.Accepted(MediaFile(name = name, bytes = bytes, mimeType = mimeType))
}
