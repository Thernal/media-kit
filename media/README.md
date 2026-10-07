# media — design

Why the kit has the shape it has. It is Act2Act's `features/media` (CMP) without its upload API, its
navigation routes and its design system, with the decisions recorded as epic decisions D50–D55. ArenaGo
has no media code.

## Pickers behind a provider

A feature calls `rememberImagePicker { }` from `api` and never sees a launcher or a view controller: the
pickers are a `MediaPickerProvider` behind a composition local, installed by `wiring` (the shape of
permissions-kit, D45/D50). Without `wiring` every pick is `Cancelled` and nothing opens, so previews
compose; a test installs `FakeMediaPickerProvider` and answers each pick itself. Act2Act's `expect fun`
tied every caller to the system pickers.

## Results are values, rejections are typed

Every outcome is a `PickResult`; nothing is thrown. Act2Act returned a `ValidationError(UiString)` with its
own strings; a kit copied into apps with other languages cannot carry strings, so a rejection says what
happened — `TooLarge(limit, actual)`, `UnsupportedType(mimeType)`, `Unreadable` — and the app words it.
With several images, the good ones still arrive (`Picked.files`) beside the refused ones (`rejected`).

## One image pipeline

Whatever the source, a picked image passes `prepareImage`: HEIC/HEIF becomes JPEG (Android 9+ through
`ImageDecoder`, iOS through `UIImage`), a JPEG whose EXIF orientation is not upright is redrawn, anything
the crop engine and Coil cannot decode on both platforms — SVG, RAW — is refused *now* rather than failing
on the crop screen, and the size limit applies to the final bytes. The platform half is a two-method
`ImageCodec`; the rules are common code, tested once.

A file picker checks the type and the size the platform reports before reading the bytes, so an oversized
video is refused without loading it, and checks the size again on the bytes.

## The gallery is the system photo picker

Android uses the Photo Picker (`PickVisualMedia`), iOS `PHPickerViewController`: no permission on either,
and the user sees only what they choose. Act2Act used `ACTION_PICK` on MediaStore. Several images are
`rememberImagePicker(maxItems = n)`: Android's multiple-selection contract takes its limit when it is
registered, not when it is launched, so the limit is an option of the picker rather than of each pick.

## The camera and its permission

The camera needs the permission on iOS always, and on Android only when the app's manifest declares
`CAMERA`. The image picker asks through `CameraPermission` (`isGranted`, `request()`), a contract in `api`.
The kit's own implementation is deliberately minimal — ask once, report the answer, no rationale, no
settings — and marked in code as the part to replace (D51): an app with its own permission handling
contributes a Metro container that `replaces` `MediaCameraPermissionProvidersModule`, and never edits the copied
file. With permissions-kit that container is a one-file adapter (`media/api/README.md`). media-kit does
not import permissions-kit (D3).

## Android camera output

`TakePicture` needs a content URI the camera app may write to. The kit declares its own FileProvider — a
subclass, so it never merges with one the app declares — with the authority
`${applicationId}.media.captures` over a cache directory. That needs Android resources in a KMP
library module, which AGP 9 allows (`androidResources.enable`).

## The crop UI: state, cropper, screen

`ImageCropState` holds everything the cropper shows — frame, ratio, zoom, offset, rotation, mirror — and
the operations (`applyRatio`, `rotateLeft`, `mirror`, `crop()`); it is Act2Act's crop ViewModel as a plain
state holder, because a kit cannot bring arch-kit or a navigator (D3). `ImageCropper` draws it on
foundation only; `ImageCropScreen` adds two bars as slots, with plain defaults whose labels are
parameters. An app with a design system passes its own bars and keeps the cropper (D53).

`CropOptions` carries what Act2Act hard-coded as its post rules (9:16–16:9, a fixed ratio list) as
optional bounds; its rules are one options value (D54). A frame chosen with a ratio is fitted inside the
image — Act2Act spread it to the container's width, beyond a portrait image's sides.

## Looks belong to the app's design system

The kit owns behaviour — the gestures, the frame's rules, the crop — and none of the look. Every colour,
shape, padding, text style and duration the cropper, the crop screen's default bars and `ZoomableImage`
draw is a field of a style read from `MediaTheme.styles`, so one file in the app's design system decides
them all and a kit update never changes how an app looks. The defaults (white on black) are there because
previews and tests need something, not because they are a design. A design system shipped in the kit's
`code` would be merged into every app's own on update, so the kit's example lives in
`sample/designsystem`, which nothing copies.

## The crop engine

The frame on screen is mapped back to the source's pixels in common code (`toNormalizedCropRect`: offset,
zoom, a counter-clockwise rotation undone, a mirror reflected), then each platform cuts, turns and encodes:
Android with `Bitmap` and a `Matrix`, iOS with `CGImage` and a `UIImageOrientation`. Both apply the
source's EXIF orientation first, as Coil does when it draws it, so what is cut is what was shown. The
result can be scaled down to `maxOutputSize`. The iOS engine is tested on the simulator with real
images.

## Coil draws the images

The cropper and `ZoomableImage` load through Coil 3 (D52): it downsamples a 50 MP photo to the screen,
which is not code to own. The pickers do not use it — `api`, `impl` and `wiring` have no Coil dependency.

## What changed from Act2Act

- No `FileRepository`, `FileApi` or navigation routes; no design-system sheets (`ImageSourceSelectSheet`,
  `ImageChangeSheet`) — the app's.
- `FileEntity` → `MediaFile` (size and extension derived); `FilePickerResult` → `PickResult` with typed
  rejections and several files.
- The gallery is the Photo Picker; several images; the camera permission is a replaceable contract.
- The crop ViewModel → `ImageCropState`; the post rules → `CropOptions`; frames fit inside the image;
  `maxOutputSize`; no SVG decoding (refused at pick time instead).
- The image preview's `PreviewState` → `ZoomableState` with the same gestures.
