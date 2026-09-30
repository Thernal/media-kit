# media/ui

Cropping and previewing images, task by task. Why it has this shape: [`../README.md`](../README.md).

Declare `media/ui` and `coil-compose` in the module that shows them. Coil loads the image; the app's own
Coil setup (disk cache, network fetcher) applies.

## A crop screen

```kotlin
val state = rememberImageCropState(file = picked, options = CropOptions(initialRatio = AspectRatio.SQUARE))
ImageCropScreen(
    state = state,
    onCancel = { navigator.pop() },
    onCropped = { cropped -> viewModel.onAvatar(cropped) },   // a JPEG MediaFile with width and height
    onError = { error -> showMessage(error) },                 // a source the platform could not decode
)
```

Show it as a route or a full-screen dialog — the kit names no navigator. Pass a picked `MediaFile` (the
pickers guarantee it decodes); any JPEG, PNG, GIF or WebP works. One finger on a corner resizes, inside the
frame moves it, outside pans a zoomed image; two fingers zoom.

## Your own bars

`ImageCropScreen` takes `topBar` and `bottomBar`. Build them from the app's design system and call the
state's operations; the defaults (`ImageCropDefaults.TopBar`, `BottomBar`) take their labels as parameters.

```kotlin
ImageCropScreen(
    state = state,
    onCancel = onCancel,
    onCropped = onCropped,
    onError = onError,
    topBar = { done -> AppTopBar(title = stringResource(Res.string.crop), onBack = onCancel, action = done) },
    bottomBar = {
        AppChips(state.ratios, selected = state.aspectRatio, onSelect = state::applyRatio, onFree = state::unlockRatio)
        AppIconButton(Icons.RotateLeft, onClick = state::rotateLeft)
        AppIconButton(Icons.Flip, onClick = state::mirror)
    },
)
```

Or skip the screen: `ImageCropper(state, modifier)` is just the image and the frame, and
`scope.launch { state.crop() }` returns `Result<MediaFile>`.

## Options

| `CropOptions` | Default | Does |
|---|---|---|
| `ratios` | 1:1, 4:5, 3:4, 4:3, 16:9 | the ratios the bottom bar offers |
| `initialRatio` | `null` — the whole image, free | the frame's ratio when the cropper opens, locked |
| `isRatioLocked` | `false` | keep `initialRatio` (square without one), offer no other |
| `minRatio`, `maxRatio` | none | bounds for a free frame (width / height) |
| `maxOutputSize` | none — full resolution | the result's longest side in pixels |
| `jpegQuality` | 90 | 0–100 |

An avatar: `CropOptions(initialRatio = AspectRatio.SQUARE, isRatioLocked = true, maxOutputSize = 1024)`.
Posts between 9:16 and 16:9: `CropOptions(minRatio = AspectRatio.R9_16.value, maxRatio = AspectRatio.R16_9.value)`.

## A zoomable preview

```kotlin
ZoomableImage(model = imageUrl, contentDescription = null, modifier = Modifier.fillMaxSize())
```

Pinch 1–4×, double tap steps 1× → 2.5× → 4× → 1× around the tapped point, one finger pans a zoomed image.
At 1× a one-finger drag is left to the parent (a pager, a swipe to dismiss). `model` is anything Coil
loads — a URL (with the app's network fetcher), `file.bytes`, a path. `rememberZoomableState()` gives
`scale`, `offset` and `reset()`; `Modifier.zoomGestures(state)` puts the same gestures on other content.
