# Picking

## An image

```kotlin
val picker = rememberImagePicker(maxFileSizeBytes = 10L * 1024 * 1024) { result ->
    when (result) {
        is PickResult.Picked -> onIntent(Intent.ImagePicked(result.file))
        is PickResult.Rejected -> onIntent(Intent.ImageRejected(result.rejections.first()))
        PickResult.PermissionDenied -> onIntent(Intent.CameraDenied)
        PickResult.Cancelled, PickResult.Unavailable -> Unit
    }
}
```

`picker.pick(ImageSource.Gallery | Camera | Documents)`. Offer the camera only where it exists; on the iOS
simulator it returns `Unavailable`.

## Several

`rememberImagePicker(maxItems = 10) { }` — the gallery allows up to 10; `result.files` holds those that
passed, `result.rejected` the others.

## A file

```kotlin
val picker = rememberFilePicker(mimeTypes = listOf(MimeType.Application.PDF), maxFileSizeBytes = 25L * 1024 * 1024) { }
```

Type and reported size are checked before reading. Files skip the image pipeline.

## Rejections in the app's words

```kotlin
fun Rejection.message(): UiString = when (this) {
    is Rejection.TooLarge -> UiString.Resource(Res.string.file_too_large, listOf(limitBytes / MB))
    is Rejection.UnsupportedType -> UiString.Resource(Res.string.file_type_not_supported)
    is Rejection.Unreadable -> UiString.Resource(Res.string.file_unreadable)
}
```

## From a ViewModel

The picker is UI: the screen owns it and posts the result as an intent. Never pass a picker or a
`PickResult` callback into a ViewModel; pass the `MediaFile` (or better, its upload) onward.

## Tests

```kotlin
val pickers = FakeMediaPickerProvider(answer = { request ->
    when (request) {
        is PickRequest.Image -> PickResult.Picked(listOf(testImage()))
        is PickRequest.File -> PickResult.Rejected(listOf(Rejection.TooLarge("a.pdf", 1, 2)))
    }
})
CompositionLocalProvider(LocalMediaPickerProvider provides pickers) { Screen() }
```

`pickers.requests` records every pick; `FakeCameraPermission` drives the camera path. Without a composition,
`pickers.imagePicker(onResult = …).pick(ImageSource.Gallery)`.
