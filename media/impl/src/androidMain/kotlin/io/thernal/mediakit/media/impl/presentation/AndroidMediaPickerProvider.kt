package io.thernal.mediakit.media.impl.presentation

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import io.thernal.mediakit.media.api.domain.ImageSource
import io.thernal.mediakit.media.api.domain.MimeType
import io.thernal.mediakit.media.api.domain.PickResult
import io.thernal.mediakit.media.api.presentation.CameraPermission
import io.thernal.mediakit.media.api.presentation.FilePicker
import io.thernal.mediakit.media.api.presentation.FilePickerOptions
import io.thernal.mediakit.media.api.presentation.ImagePicker
import io.thernal.mediakit.media.api.presentation.ImagePickerOptions
import io.thernal.mediakit.media.api.presentation.LocalCameraPermission
import io.thernal.mediakit.media.api.presentation.MediaPickerProvider
import io.thernal.mediakit.media.impl.domain.PickedItem
import io.thernal.mediakit.media.impl.domain.prepareImage
import io.thernal.mediakit.media.impl.domain.toPickResult
import kotlinx.coroutines.launch

actual fun platformMediaPickerProvider(): MediaPickerProvider {
    return AndroidMediaPickerProvider
}

actual fun defaultCameraPermission(): CameraPermission {
    return AndroidCameraPermission
}

// PickMultipleVisualMedia refuses a limit below two; a single pick uses PickVisualMedia instead.
private const val MIN_MULTIPLE_PICK = 2

/**
 * The gallery is the system Photo Picker (no permission), the camera is the system camera app writing
 * into the kit's FileProvider, documents and files are the Storage Access Framework.
 */
internal object AndroidMediaPickerProvider : MediaPickerProvider {
    @Composable
    override fun rememberImagePicker(
        options: ImagePickerOptions,
        onResult: (PickResult) -> Unit,
    ): ImagePicker {
        val context = LocalContext.current
        val scope = rememberCoroutineScope()
        val deliver by rememberUpdatedState(onResult)
        val camera = LocalCameraPermission.current.rememberCameraAccess()
        var pendingCapture by rememberSaveable { mutableStateOf<Uri?>(null) }

        fun receive(uris: List<Uri>) {
            scope.launch { deliver(context.readImages(uris = uris, maxFileSizeBytes = options.maxFileSizeBytes)) }
        }

        val gallery = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.PickVisualMedia(),
            onResult = { uri -> receive(listOfNotNull(uri)) },
        )
        val multipleContract = remember(options.maxItems) {
            ActivityResultContracts.PickMultipleVisualMedia(maxOf(a = MIN_MULTIPLE_PICK, b = options.maxItems))
        }
        val galleryMultiple = rememberLauncherForActivityResult(contract = multipleContract, onResult = ::receive)
        val documents = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.OpenDocument(),
            onResult = { uri -> receive(listOfNotNull(uri)) },
        )
        val capture = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.TakePicture(),
            onResult = { saved ->
                val uri = pendingCapture
                pendingCapture = null
                if (saved && uri != null) {
                    receive(listOf(uri))
                } else {
                    deliver(PickResult.Cancelled)
                }
            },
        )

        fun openCamera() {
            if (!context.packageManager.hasSystemFeature(PackageManager.FEATURE_CAMERA_ANY)) {
                deliver(PickResult.Unavailable)
                return
            }
            val uri = context.newCaptureUri()
            pendingCapture = uri
            try {
                capture.launch(uri)
            } catch (_: ActivityNotFoundException) {
                pendingCapture = null
                deliver(PickResult.Unavailable)
            } catch (_: SecurityException) {
                // The manifest declares CAMERA and it is not granted.
                pendingCapture = null
                deliver(PickResult.PermissionDenied)
            }
        }

        return remember(key1 = options, key2 = camera) {
            ImagePicker { source ->
                when (source) {
                    ImageSource.Gallery -> {
                        val request = PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                        if (options.maxItems > 1) {
                            galleryMultiple.launch(request)
                        } else {
                            gallery.launch(request)
                        }
                    }

                    ImageSource.Documents -> documents.launch(arrayOf(MimeType.Image.ALL))

                    ImageSource.Camera -> scope.launch {
                        if (camera.isGranted || camera.request()) {
                            openCamera()
                        } else {
                            deliver(PickResult.PermissionDenied)
                        }
                    }
                }
            }
        }
    }

    @Composable
    override fun rememberFilePicker(
        options: FilePickerOptions,
        onResult: (PickResult) -> Unit,
    ): FilePicker {
        val context = LocalContext.current
        val scope = rememberCoroutineScope()
        val deliver by rememberUpdatedState(onResult)
        val documents = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.OpenDocument(),
            onResult = { uri ->
                if (uri == null) {
                    deliver(PickResult.Cancelled)
                } else {
                    scope.launch {
                        val item = context.readUri(
                            uri = uri,
                            allowedMimeTypes = options.mimeTypes,
                            maxFileSizeBytes = options.maxFileSizeBytes,
                            fallbackName = "file",
                        )
                        deliver(listOf(item).toPickResult())
                    }
                }
            },
        )
        return remember(options) {
            FilePicker { documents.launch(options.mimeTypes.toTypedArray()) }
        }
    }
}

private suspend fun Context.readImages(
    uris: List<Uri>,
    maxFileSizeBytes: Long?,
): PickResult {
    val items = uris.mapIndexed { index, uri ->
        // Any image type is read; prepareImage refuses what the kit cannot decode, and checks the
        // size on the final bytes (a HEIC shrinks or grows as JPEG).
        val read = readUri(
            uri = uri,
            allowedMimeTypes = listOf(MimeType.Image.ALL),
            maxFileSizeBytes = null,
            fallbackName = "image_${index + 1}.jpg",
        )
        when (read) {
            is PickedItem.Accepted -> prepareImage(
                file = read.file,
                maxFileSizeBytes = maxFileSizeBytes,
                codec = AndroidImageCodec(),
            )

            is PickedItem.Refused -> read
        }
    }
    return items.toPickResult()
}
