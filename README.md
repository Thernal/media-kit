# media-kit

Picking and cropping images and picking files in a Compose Multiplatform app (android, iosArm64,
iosSimulatorArm64): the system photo picker (several at once), the camera, the document picker; HEIC
converted and orientation fixed at pick time; type and size checks as typed results; an image cropper
with ratios, zoom, rotation and mirror; a zoomable preview. No upload — what to do with the bytes is the
app's.

```kotlin
val picker = rememberImagePicker(maxItems = 5, maxFileSizeBytes = 10 * MB) { result ->
    when (result) {
        is PickResult.Picked -> onImages(result.files)            // MediaFile: name, bytes, mimeType…
        is PickResult.Rejected -> showError(result.rejections)    // TooLarge, UnsupportedType, Unreadable
        PickResult.PermissionDenied -> showCameraSettingsHint()
        PickResult.Cancelled, PickResult.Unavailable -> Unit
    }
}
Button(onClick = { picker.pick(ImageSource.Gallery) }) { Text("Choose photos") }
```

```kotlin
ImageCropScreen(
    state = rememberImageCropState(file, CropOptions(initialRatio = AspectRatio.SQUARE, maxOutputSize = 1080)),
    onCancel = navigator::pop,
    onCropped = { cropped -> upload(cropped.bytes) },
    onError = ::showError,
)
```

## Documentation

| Read | For |
|---|---|
| this file | what is here and how it is built |
| [`media/api/README.md`](media/api/README.md) | picking, task by task: images, several, camera and its permission, files, results, previews, tests |
| [`media/ui/README.md`](media/ui/README.md) | cropping and previewing, task by task: the screen, your own bars, options, the zoomable image |
| [`media/README.md`](media/README.md) | why each part has its shape, and what changed from the app it came from |
| [`skills/media-kit`](skills/media-kit/SKILL.md) | the same for an agent working in an app that uses the kit |

## For AI agents

An application takes the kit by copy: `skillctl.sh kit install media-kit --package <its package> --module
<its module path> --alias <its plugin alias>` copies the modules below renamed, installs the `media-kit`
skill, and records both in `kits.lock`. `kit.yml` lists what the copied modules expect, including the
manifest and `Info.plist` entries only the application can declare.

## Layout

| Module | Holds | Depends on |
|---|---|---|
| `media/api` | `MediaFile`, `MimeType`, `PickResult`/`Rejection`, `ImageSource`, `CropOptions`, `AspectRatio`; `MediaPickerProvider`, `rememberImagePicker`, `rememberFilePicker`; `CameraPermission` | Compose runtime |
| `media/impl` | the Android pickers (Photo Picker, camera through the kit's FileProvider, Storage Access Framework), the iOS pickers (PHPicker, UIImagePickerController, UIDocumentPicker), HEIC and orientation, the minimal camera permission | api, activity-compose, core, exifinterface (Android) |
| `media/wiring` | the pickers and the camera permission as `ProvidedValue`s in the app graph | api, impl, Metro |
| `media/ui` | the crop engine (Android, iOS), `ImageCropState`, `ImageCropper`, `ImageCropScreen`, `ZoomableImage` | api, Coil 3 |
| `media/testing` | `FakeMediaPickerProvider`, `FakeCameraPermission`, `testImage()` | api |
| `sample/shared`, `sample/android`, `sample/ios` | pick, crop, preview on both platforms | the kit — never copied |

A feature that only picks needs `api`; one that crops or previews adds `ui`.

## Building

```sh
./gradlew build
```

Every target, tests on the JVM host and the iOS simulator — the pick checks and the image pipeline, the
crop state and geometry, the zoom, the crop engine on real images on iOS, the fakes, and the camera
permission replaced through Metro — Detekt, which fails on any finding (`-PdetektAutoCorrect=true` fixes
formatting first), Android lint on the sample, and the sample's iOS framework link. The Gradle daemon runs
on JDK 21 (Metro).

The iOS sample is an Xcode project (`sample/ios/Sample.xcodeproj`, generated from `project.yml` by
XcodeGen); its build phase compiles the framework with Gradle:

```sh
xcodebuild -project sample/ios/Sample.xcodeproj -scheme Sample -sdk iphonesimulator \
  -destination 'generic/platform=iOS Simulator' build
```
