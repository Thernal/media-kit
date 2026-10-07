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

## Installing

An application takes the kit **by copy**, not as a dependency: the code is copied into the app, renamed to the app's own package, and belongs to the app from then on. Nothing is published to a Maven repository.

### With skill-manager

If you have access to the author's knowledge repository (`github.com/Thernal/knowledge`), its **skill-manager** skill does all of it — copy, rename, the skill, and later updates:

```sh
skillctl.sh kit install media-kit --package com.example.app --module :core:media --alias app
```

It copies the `code` parts of [`kit.yml`](kit.yml) renamed, installs the `media-kit` skill and records the copy in `kits.lock`. `kit status` then shows what changed upstream and what the app edited; `kit update` merges the kit's changes three ways, keeping the app's edits. The install prints what the app must provide (`requires`).

### Without it

The same by hand, from a clone of this repository.

1. **Copy** the paths listed under `code` in [`kit.yml`](kit.yml) into the app, under the module path the app gives them: `media/…` → `core/media/…`. Note the commit you copied (`git rev-parse HEAD`) — updates start from it.
2. **Rename** in everything copied:

   | In the kit | Becomes | Where |
   |---|---|---|
   | `io.thernal.mediakit` | the app's package, e.g. `com.example.app` | sources, build files; and the directories `io/thernal/mediakit` |
   | `:media:` and `":media"`, `projects.media.` | the module path, e.g. `:core:media:`, `projects.core.media.` | build files |
   | `libs.plugins.mediakit.` | the app's catalog alias, e.g. `libs.plugins.app.` | build files |

   ```sh
   # in the app, after copying — perl, so it runs the same on macOS and Linux
   grep -rlI -e io.thernal.mediakit -e io/thernal/mediakit -e :media -e plugins.mediakit. core/media \
     | xargs perl -pi -e 's/\Qio.thernal.mediakit\E/com.example.app/g; s{\Qio/thernal/mediakit\E}{com/example/app}g; s/\Q:media:\E/:core:media:/g; s/"\Q:media\E"/":core:media"/g; s/projects\.\Qmedia\E\./projects.core.media./g; s/libs\.plugins\.\Qmediakit\E\./libs.plugins.app./g'
   find core/media -depth -type d -path '*/io/thernal/mediakit' | while read -r d; do
     mkdir -p "${d%/io/thernal/mediakit}/com/example" && mv "$d" "${d%/io/thernal/mediakit}/com/example/app"
   done
   find core/media -depth -type d -empty -delete
   ```

3. **Provide** what the copy expects — the `requires` list in [`kit.yml`](kit.yml): convention plugins (build-kit's, or the ones in this repository's `build-logic/convention`), catalog entries, settings — and, where listed, platform setup.
4. **The skill** (optional): copy [`skills/media-kit`](skills/media-kit) into the app's skills directory (`.claude/skills/` for Claude Code), with the same renames, so an agent working in the app knows the kit.
5. **Updates** are yours to carry: `git diff <the commit you copied> <a newer one> -- <the code paths>` in the kit shows what changed; apply what you want, renamed the same way.

## Layout

| Module | Holds | Depends on |
|---|---|---|
| `media/api` | `MediaFile`, `MimeType`, `PickResult`/`Rejection`, `ImageSource`, `CropOptions`, `AspectRatio`; `MediaPickerProvider`, `rememberImagePicker`, `rememberFilePicker`; `CameraPermission` | Compose runtime |
| `media/impl` | the Android pickers (Photo Picker, camera through the kit's FileProvider, Storage Access Framework), the iOS pickers (PHPicker, UIImagePickerController, UIDocumentPicker), HEIC and orientation, the minimal camera permission | api, activity-compose, core, exifinterface (Android) |
| `media/wiring` | the pickers and the camera permission as `ProvidedValue`s in the app graph | api, impl, Metro |
| `media/ui` | the crop engine (Android, iOS), `ImageCropState`, `ImageCropper`, `ImageCropScreen`, `ZoomableImage`; their styles and `MediaTheme` | api, Coil 3 |
| `media/testing` | `FakeMediaPickerProvider`, `FakeCameraPermission`, `testImage()` | api |
| `sample/designsystem` | a small design system and its mapping onto `MediaStyles` — how an app styles the kit | the kit — never copied |
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
