package io.thernal.mediakit.media.impl.presentation

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.ActivityResultLauncher
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
import io.thernal.mediakit.media.api.presentation.CameraAccess
import io.thernal.mediakit.media.api.presentation.FilePicker
import io.thernal.mediakit.media.api.presentation.FilePickerOptions
import io.thernal.mediakit.media.api.presentation.ImagePicker
import io.thernal.mediakit.media.api.presentation.ImagePickerOptions
import io.thernal.mediakit.media.api.presentation.LocalCameraPermission
import io.thernal.mediakit.media.api.presentation.MediaPickerProvider
import io.thernal.mediakit.media.impl.domain.PickedItem
import io.thernal.mediakit.media.impl.domain.prepareImage
import io.thernal.mediakit.media.impl.domain.toPickResult
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

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
        val receive: (List<Uri>) -> Unit = { uris ->
            scope.launch { deliver(context.readImages(uris = uris, maxFileSizeBytes = options.maxFileSizeBytes)) }
        }
        val openGallery = rememberGallery(maxItems = options.maxItems, receive = receive)
        val documents = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.OpenDocument(),
            onResult = { uri -> receive(listOfNotNull(uri)) },
        )
        val openCamera = rememberCamera(
            camera = camera,
            scope = scope,
            receive = receive,
            deliver = { result -> deliver(result) },
        )
        return remember(key1 = options, key2 = camera) {
            ImagePicker { source ->
                when (source) {
                    ImageSource.Gallery -> openGallery()
                    ImageSource.Documents -> documents.launch(arrayOf(MimeType.Image.ALL))
                    ImageSource.Camera -> openCamera()
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

/** The Photo Picker: one image, or up to [maxItems]. */
@Composable
private fun rememberGallery(
    maxItems: Int,
    receive: (List<Uri>) -> Unit,
): () -> Unit {
    val single = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
        onResult = { uri -> receive(listOfNotNull(uri)) },
    )
    val multipleContract = remember(maxItems) {
        ActivityResultContracts.PickMultipleVisualMedia(maxOf(a = MIN_MULTIPLE_PICK, b = maxItems))
    }
    val multiple = rememberLauncherForActivityResult(contract = multipleContract, onResult = receive)
    return {
        val request = PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
        if (maxItems > 1) {
            multiple.launch(request)
        } else {
            single.launch(request)
        }
    }
}

/**
 * The system camera app, writing into the kit's FileProvider, once [camera] is granted; the target
 * survives a recreation.
 */
@Composable
private fun rememberCamera(
    camera: CameraAccess,
    scope: CoroutineScope,
    receive: (List<Uri>) -> Unit,
    deliver: (PickResult) -> Unit,
): () -> Unit {
    val context = LocalContext.current
    var pendingCapture by rememberSaveable { mutableStateOf<Uri?>(null) }
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
    val start = {
        val result = context.launchCapture(capture = capture, onTarget = { pendingCapture = it })
        if (result != null) {
            pendingCapture = null
            deliver(result)
        }
    }
    return {
        scope.launch {
            if (camera.isGranted || camera.request()) {
                start()
            } else {
                deliver(PickResult.PermissionDenied)
            }
        }
    }
}

/** Starts the capture into a new file; returns the outcome when it could not start. */
private fun Context.launchCapture(
    capture: ActivityResultLauncher<Uri>,
    onTarget: (Uri) -> Unit,
): PickResult? {
    if (!packageManager.hasSystemFeature(PackageManager.FEATURE_CAMERA_ANY)) {
        return PickResult.Unavailable
    }
    val uri = newCaptureUri()
    onTarget(uri)
    return try {
        capture.launch(uri)
        null
    } catch (_: ActivityNotFoundException) {
        PickResult.Unavailable
    } catch (_: SecurityException) {
        // The manifest declares CAMERA and it is not granted.
        PickResult.PermissionDenied
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

// PickMultipleVisualMedia refuses a limit below two; a single pick uses PickVisualMedia instead.
private const val MIN_MULTIPLE_PICK = 2
