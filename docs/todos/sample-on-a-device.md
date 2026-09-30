# Run the sample on a device

**Status:** open

`./gradlew build` compiles and lints the Android sample and links the iOS framework; `xcodebuild` builds the
iOS sample, and it launches on the simulator to the pick screen. Nobody has tapped through it yet: several
photos from the gallery, a HEIC photo, the camera (with and without the permission), a PDF from files, an
oversized file, the cropper with each ratio, rotation and mirror, the zoomable preview. Do it once on each
platform and note what was seen here.
