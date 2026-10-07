---
name: media-kit
description: Writes, reviews and debugs image and file picking, image cropping and zoomable image previews in Compose Multiplatform apps that use media-kit (packages io.thernal.mediakit.media.*; rememberImagePicker, rememberFilePicker, ImageSource, PickResult, Rejection, MediaFile, MimeType, CameraPermission, MediaCameraPermissionProvidersModule, ImageCropScreen, ImageCropper, rememberImageCropState, CropOptions, AspectRatio, ZoomableImage, MediaTheme, MediaStyles, FakeMediaPickerProvider). Use it for any such work in such a project, even when the kit is not named - an avatar or photo upload, choosing several photos, taking a photo, attaching a PDF, file size or type limits, HEIC photos, rotated photos, cropping to a ratio, a full-screen photo viewer with pinch zoom, styling the cropper from the design system, the camera permission, and tests of picker-driven screens. Not for projects without media-kit.
---

# media-kit

media-kit picks images and files and crops and previews images in Compose on Android and iOS. Guide:
https://github.com/Thernal/media-kit — `media/api/README.md` (picking), `media/ui/README.md` (cropping,
preview), `media/README.md` (why).

## 1. Orient first

```sh
grep -rn --include=*.kt -e "rememberImagePicker(" -e "rememberFilePicker(" . | head   # existing pickers — copy their shape
grep -rn --include=*.kt -e "ImageCropScreen(" -e "ImageCropper(" -e "ZoomableImage(" . | head
grep -rn --include=*.kt -e "MediaCameraPermissionProvidersModule" .    # is the camera permission replaced?
grep -rn --include=*.kt -e "MediaStyles(" -e "MediaTheme(" .           # token mapping
grep -rn --include=*.kt -e "MediaProvidersModule" -e "compositionLocals" .
grep -rn -e "NSCameraUsageDescription" --include=Info.plist .
```

Nothing installed → [references/setup.md](references/setup.md). **Taken as a kit?** A `kits.lock` naming
`media-kit` means the modules were copied renamed with skill-manager — this skill too. `skillctl.sh kit
status media-kit` shows what moved upstream; offer `kit update media-kit` rather than hand edits.

## 2. The model

- Pick: `rememberImagePicker(maxItems, maxFileSizeBytes) { result -> }` then `picker.pick(ImageSource.X)`;
  `rememberFilePicker(mimeTypes, maxFileSizeBytes) { }` then `picker.pick()`.
- The result is a `PickResult`: `Picked(files, rejected)`, `Rejected`, `Cancelled`, `PermissionDenied`,
  `Unavailable`. Rejections are typed; the app words them. Nothing throws.
- Picked images are always decodable, upright JPEG/PNG/GIF/WebP — HEIC converted, SVG refused.
- Crop: `ImageCropScreen(rememberImageCropState(file, CropOptions(...)), onCancel, onCropped, onError)`, or
  `ImageCropper(state)` with the app's own controls. Preview: `ZoomableImage(model, …)`.
- The camera permission goes through `CameraPermission`; the kit's is minimal and meant to be replaced.
- Looks come from `ImageCropperStyle`, `ImageCropScreenStyle`, `ZoomableImageStyle`, defaulting to
  `MediaTheme.styles`; the app maps its design system's tokens once, in one file of its design-system module
  (`appMediaStyles()`), installed at the root. Never restyle by editing kit code.

## 3. Tasks

| Task | Read |
|---|---|
| install, the root, manifest and `Info.plist`, replacing the camera permission (permissions-kit adapter) | [setup.md](references/setup.md) |
| pick images, several, the camera, files; results and rejections; previews and tests | [picking.md](references/picking.md) |
| a crop screen, custom bars, crop options, the cropper alone, a zoomable viewer | [cropping.md](references/cropping.md) |

## 4. Rules

- Never launch `ActivityResultContracts.GetContent`/`PickVisualMedia` or present `PHPickerViewController`
  from a feature — the kit's pickers are the one path, with its checks.
- Never ask for `READ_MEDIA_IMAGES` or photo library access to pick photos: the system photo pickers need none.
- Handle every `PickResult` branch; show `rejected` files, don't drop them silently.
- Put user-facing words for rejections in the app's strings; the kit has none.
- Replace the camera permission with `@ContributesTo(AppScope::class, replaces = [MediaCameraPermissionProvidersModule::class])`,
  never by editing the copied `wiring`.
- Don't hold `MediaFile.bytes` in a ViewModel's state longer than needed — they are the whole file.
- Upload is the app's: the kit hands over bytes and a MIME type.

## 5. Verify

```sh
./gradlew build
```
