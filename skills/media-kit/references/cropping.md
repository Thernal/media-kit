# Cropping and previews

## The screen

```kotlin
ImageCropScreen(
    state = rememberImageCropState(file, CropOptions(initialRatio = AspectRatio.SQUARE, isRatioLocked = true, maxOutputSize = 1024)),
    onCancel = onBack,
    onCropped = { cropped -> onIntent(Intent.AvatarCropped(cropped)) },
    onError = { onIntent(Intent.CropFailed) },
)
```

Show it as the app's own route or full-screen dialog; pass the picked `MediaFile` to it.

## The app's design system

`topBar = { done -> … }` and `bottomBar = { … }` replace the default bars; call `state.applyRatio(ratio)`,
`state.unlockRatio()`, `state.rotateLeft()`, `state.rotateRight()`, `state.mirror()`; read `state.ratios`,
`state.isLocked`, `state.aspectRatio`, `state.isProcessing`. Keep `ImageCropDefaults.TopBar/BottomBar` with
translated labels (`cancelLabel`, `doneLabel`, `freeLabel`, …) if they suffice.

Looks come from the app's design system, never from editing the kit: one app-owned file in the
design-system module, `designsystem/…/media/AppMediaStyles.kt`, `@Composable fun appMediaStyles(): MediaStyles`
— `MediaStyles(cropper = ImageCropperStyle(…), cropScreen = ImageCropScreenStyle(…, ratioChip = CropRatioChipStyle(…)),
zoom = ZoomableImageStyle(…))` from the design system's tokens — installed at the root inside the design
system's theme: `AppTheme { MediaTheme(styles = appMediaStyles()) { … } }`. No mapping yet (white on black,
the kit's neutral defaults) → write it before styling anything else; the kit's
`sample/designsystem/…/media/SampleMediaStyles.kt` shows the shape. A one-off look is `style =` at the call
site. Map looks only: `touchSlop` is behaviour.

`ImageCropper(state)` alone is the image and the frame; `state.crop()` is `Result<MediaFile>`.

## Options

`CropOptions(ratios, initialRatio, isRatioLocked, minRatio, maxRatio, maxOutputSize, jpegQuality)` — see
`media/ui/README.md` → Options. Encode product rules once, as a shared value (`val AvatarCrop = CropOptions(...)`).

## A viewer

`ZoomableImage(model = url, contentDescription = …)` — pinch, double tap, pan; at 1× a drag goes to the
parent (a `HorizontalPager` of photos works). `rememberZoomableState().reset()` when the page changes.

## Tests

`ImageCropState` is plain state: construct it, call `onLayout(containerSize, intrinsicSize)`, then the
operations, and assert `frameRect`, `aspectRatio`, `rotation`. `crop()` needs a real image and runs the
platform engine.
