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
import platform.Foundation.NSUUID
import platform.UIKit.UIImage
import platform.UIKit.UIImagePickerController
import platform.UIKit.UIImagePickerControllerDelegateProtocol
import platform.UIKit.UIImagePickerControllerOriginalImage
import platform.UIKit.UINavigationControllerDelegateProtocol
import platform.darwin.NSObject

/** The camera (UIImagePickerController). The photo arrives as a UIImage and leaves as an upright JPEG. */
internal class CameraDelegate(
    private val scope: CoroutineScope,
    private val maxFileSizeBytes: () -> Long?,
    private val deliver: (PickResult) -> Unit,
) : NSObject(), UIImagePickerControllerDelegateProtocol, UINavigationControllerDelegateProtocol {
    override fun imagePickerControllerDidCancel(picker: UIImagePickerController) {
        picker.dismissViewControllerAnimated(true, completion = null)
        deliver(PickResult.Cancelled)
    }

    override fun imagePickerController(
        picker: UIImagePickerController,
        didFinishPickingMediaWithInfo: Map<Any?, *>,
    ) {
        picker.dismissViewControllerAnimated(true, completion = null)
        val image = didFinishPickingMediaWithInfo[UIImagePickerControllerOriginalImage] as? UIImage
        if (image == null) {
            deliver(PickResult.Cancelled)
            return
        }
        scope.launch {
            val name = "photo_${NSUUID().UUIDString}.jpg"
            val bytes = image.uprightJpeg()
            val item = if (bytes == null) {
                PickedItem.Refused(Rejection.Unreadable(name))
            } else {
                prepareImage(
                    MediaFile(name = name, bytes = bytes, mimeType = MimeType.Image.JPEG),
                    maxFileSizeBytes(),
                    IosImageCodec,
                )
            }
            deliver(listOf(item).toPickResult())
        }
    }
}
