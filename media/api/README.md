# media/api

Picking, task by task. Why it has this shape: [`../README.md`](../README.md). Cropping and previews:
[`../ui/README.md`](../ui/README.md).

## Dependencies you declare

A feature that picks declares `api` (the kit adds nothing to a consumer's classpath); one that crops or
previews also declares `ui` and `coil-compose`. The module that owns the app graph declares `wiring`;
tests declare `testing`.

```kotlin
commonMain.dependencies {
    implementation(projects.core.media.api)   // the path the kit was installed under
}
```

## One image

```kotlin
val picker = rememberImagePicker { result ->
    if (result is PickResult.Picked) onImage(result.file)
}
Button(onClick = { picker.pick(ImageSource.Gallery) }) { Text("Choose a photo") }
Button(onClick = { picker.pick(ImageSource.Camera) }) { Text("Take a photo") }
```

`ImageSource.Gallery` is the system photo picker (no permission), `Camera` a new photo (asks for the camera
permission first), `Documents` the document picker limited to images. The result is a `MediaFile`: `name`,
`bytes`, `mimeType`, `size`, `extension`, and `uri` where the platform gave one.

Every image comes out decodable on both platforms: HEIC/HEIF is JPEG by now, a rotated JPEG is upright,
and SVG, RAW and the like are rejected as `UnsupportedType`.

## Several images

```kotlin
val picker = rememberImagePicker(maxItems = 10) { result ->
    if (result is PickResult.Picked) onImages(result.files)
}
```

The gallery lets the user choose up to `maxItems`; the camera and documents still give one. Files that fail
the checks are in `result.rejected` while the rest arrive; if all fail the result is `Rejected`.

## A size limit

```kotlin
rememberImagePicker(maxFileSizeBytes = 10L * 1024 * 1024) { … }
```

For images the limit applies to the final bytes (after HEIC → JPEG). An image over it is
`Rejection.TooLarge(name, limitBytes, actualBytes)`.

## Any file

```kotlin
val picker = rememberFilePicker(
    mimeTypes = listOf(MimeType.Application.PDF, MimeType.Image.ALL),
    maxFileSizeBytes = 25L * 1024 * 1024,
) { result -> … }
Button(onClick = picker::pick) { Text("Attach a file") }
```

The system document picker, offering the given types; a family (`MimeType.Image.ALL`) matches all its
members. The type and the size the platform reports are checked before the file is read. Files are not run
through the image pipeline — pick images with the image picker.

## Results

| `PickResult` | When | Typically |
|---|---|---|
| `Picked(files, rejected)` | at least one file passed | use `files`; mention `rejected` if not empty |
| `Rejected(rejections)` | everything failed the checks | say why, in the app's words |
| `Cancelled` | the user closed the picker | nothing |
| `PermissionDenied` | the camera permission was refused | explain, offer settings |
| `Unavailable` | no camera (a simulator), or nothing on screen to present from | hide the camera option |

| `Rejection` | Says |
|---|---|
| `TooLarge(name, limitBytes, actualBytes)` | the file is over the limit |
| `UnsupportedType(name, mimeType)` | not an allowed type — or, for images, not one the kit can decode |
| `Unreadable(name)` | the platform returned it but it could not be read |

## The camera permission

The image picker checks `CameraPermission` before opening the camera and asks if needed. The kit's own
implementation is minimal: it asks once and reports the answer — no rationale, no settings. **Replace it
with your app's permission code.** With permissions-kit, one file in the app:

```kotlin
object PermissionsKitCameraPermission : CameraPermission {
    @Composable
    override fun rememberCameraAccess(): CameraAccess {
        val camera by rememberUpdatedState(rememberPermissionState(AppPermission.Camera))
        return remember {
            object : CameraAccess {
                override val isGranted: Boolean
                    get() = camera.status.isGranted

                override suspend fun request(): Boolean {
                    val before = camera.status
                    if (before.isGranted || !before.canRequest) return before.isGranted
                    camera.request()
                    return snapshotFlow { camera.status }.first { it != before }.isGranted
                }
            }
        }
    }
}

@BindingContainer
@ContributesTo(AppScope::class, replaces = [MediaCameraPermissionProvidersModule::class])
interface AppCameraPermissionBindingsModule {
    companion object {
        @Provides
        fun provideCameraPermission(): CameraPermission = PermissionsKitCameraPermission
    }
}
```

`replaces` swaps the kit's binding without editing the copied `wiring`, so `kit update` never conflicts
there. The picker returns `PermissionDenied` when access is refused; the rationale and the way to settings
are then the app's (permissions-kit's `openSettings()`).

## The platform side

Only the application can declare these:

| For | Android manifest | iOS `Info.plist` |
|---|---|---|
| the camera | nothing — or `CAMERA` if the app uses the camera itself; then the kit asks for it | `NSCameraUsageDescription` |
| the gallery | nothing (Photo Picker) | nothing (PHPicker) |
| documents and files | nothing | nothing |

The kit's `impl` declares its own FileProvider for camera output; the manifests merge.

## Installing the provider

`wiring` contributes the pickers and the camera permission into the app graph's `Set<ProvidedValue<*>>`,
which the root installs once (arch-kit's `ContributedLocals` does). Without it every pick is `Cancelled`.

## Previews and tests

`FakeMediaPickerProvider(answer = { request -> … })` (`testing`) answers every pick at once and records it
in `requests`; `testImage()` is a small stand-in `MediaFile`. `FakeCameraPermission(isGranted, willGrantOnRequest)`
drives the camera path.

```kotlin
val pickers = FakeMediaPickerProvider(answer = { PickResult.Picked(listOf(testImage("avatar.jpg"))) })
CompositionLocalProvider(LocalMediaPickerProvider provides pickers) { AvatarEditor() }
// … tap "Choose a photo", then:
assertEquals(PickRequest.Image(ImageSource.Gallery, ImagePickerOptions()), pickers.requests.single())
```

`pickers.imagePicker(onResult = …)` gives the same picker without a composition.
