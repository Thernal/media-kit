# Video

**Status:** open

The pickers take images and any file; picking video from the gallery (`PickVisualMedia.VideoOnly`,
`PHPickerFilter.videosFilter`), recording one (`CaptureVideo`, `UIImagePickerController` with
`kUTTypeMovie`), and a size check before the bytes are read (the file picker already does this) belong
together. Video should not be read into memory: `MediaFile` holds bytes, so video needs a file-backed
variant — see `large-files.md`. Decide when an app needs it (D55).
