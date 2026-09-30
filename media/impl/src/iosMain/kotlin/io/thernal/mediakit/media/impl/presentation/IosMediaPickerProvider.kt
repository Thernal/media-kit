package io.thernal.mediakit.media.impl.presentation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
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
import kotlinx.coroutines.launch
import platform.PhotosUI.PHPickerConfiguration
import platform.PhotosUI.PHPickerFilter
import platform.PhotosUI.PHPickerViewController
import platform.UIKit.UIDocumentPickerViewController
import platform.UIKit.UIImagePickerController
import platform.UIKit.UIImagePickerControllerSourceType

actual fun platformMediaPickerProvider(): MediaPickerProvider {
    return IosMediaPickerProvider
}

actual fun defaultCameraPermission(): CameraPermission {
    return IosCameraPermission
}

/**
 * The gallery is PHPicker (no permission), the camera UIImagePickerController, documents and files
 * UIDocumentPickerViewController. UIKit holds delegates weakly, so each is remembered here.
 */
internal object IosMediaPickerProvider : MediaPickerProvider {
    @Composable
    override fun rememberImagePicker(
        options: ImagePickerOptions,
        onResult: (PickResult) -> Unit,
    ): ImagePicker {
        val scope = rememberCoroutineScope()
        val deliver by rememberUpdatedState(onResult)
        val currentOptions by rememberUpdatedState(options)
        val camera = LocalCameraPermission.current.rememberCameraAccess()
        val delegates = remember {
            val maxBytes = { currentOptions.maxFileSizeBytes }
            val result = { value: PickResult -> deliver(value) }
            ImageDelegates(
                gallery = GalleryDelegate(scope = scope, maxFileSizeBytes = maxBytes, deliver = result),
                camera = CameraDelegate(scope = scope, maxFileSizeBytes = maxBytes, deliver = result),
                documents = DocumentDelegate(
                    scope = scope,
                    asImages = true,
                    allowedMimeTypes = { listOf(MimeType.Image.ALL) },
                    maxFileSizeBytes = maxBytes,
                    deliver = result,
                ),
            )
        }
        return remember(camera) {
            ImagePicker { source ->
                val unavailable = { deliver(PickResult.Unavailable) }
                when (source) {
                    ImageSource.Gallery -> {
                        val configuration = PHPickerConfiguration().apply {
                            selectionLimit = currentOptions.maxItems.toLong()
                            filter = PHPickerFilter.imagesFilter()
                        }
                        val picker = PHPickerViewController(configuration = configuration)
                        picker.delegate = delegates.gallery
                        PickerPresenter.present(controller = picker, onUnavailable = unavailable)
                    }

                    ImageSource.Documents -> {
                        val picker = UIDocumentPickerViewController(
                            forOpeningContentTypes = contentTypesFor(listOf(MimeType.Image.ALL)),
                            asCopy = true,
                        )
                        picker.delegate = delegates.documents
                        PickerPresenter.present(controller = picker, onUnavailable = unavailable)
                    }

                    ImageSource.Camera -> scope.launch {
                        val cameraSource = UIImagePickerControllerSourceType.UIImagePickerControllerSourceTypeCamera
                        when {
                            !UIImagePickerController.isSourceTypeAvailable(
                                cameraSource,
                            ) -> deliver(PickResult.Unavailable)

                            !camera.isGranted && !camera.request() -> deliver(PickResult.PermissionDenied)

                            else -> {
                                val picker = UIImagePickerController().apply {
                                    sourceType = cameraSource
                                    allowsEditing = false
                                }
                                picker.delegate = delegates.camera
                                PickerPresenter.present(controller = picker, onUnavailable = unavailable)
                            }
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
        val scope = rememberCoroutineScope()
        val deliver by rememberUpdatedState(onResult)
        val currentOptions by rememberUpdatedState(options)
        val delegate = remember {
            DocumentDelegate(
                scope = scope,
                asImages = false,
                allowedMimeTypes = { currentOptions.mimeTypes },
                maxFileSizeBytes = { currentOptions.maxFileSizeBytes },
                deliver = { deliver(it) },
            )
        }
        return remember {
            FilePicker {
                val picker = UIDocumentPickerViewController(
                    forOpeningContentTypes = contentTypesFor(currentOptions.mimeTypes),
                    asCopy = true,
                )
                picker.delegate = delegate
                PickerPresenter.present(controller = picker, onUnavailable = { deliver(PickResult.Unavailable) })
            }
        }
    }
}

private class ImageDelegates(
    val gallery: GalleryDelegate,
    val camera: CameraDelegate,
    val documents: DocumentDelegate,
)
