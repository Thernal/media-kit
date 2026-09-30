# Setup

## Modules

`skillctl.sh kit install media-kit --package <app package> --module <path, e.g. :core:media> --alias <plugin alias>`
copies `api`, `impl`, `wiring`, `ui`, `testing` under that path. They apply the application's `kmp.library`,
`compose` and `injection` conventions (build-kit provides them) and need these catalog entries:
`compose-runtime`, `compose-foundation`, `compose-ui`, `coil-compose`, `androidx-activity-compose`,
`androidx-core`, `androidx-exifinterface`, `kotlinx-coroutines-core`, `metro-runtime` and the Metro plugin.

| Module | Who depends on it |
|---|---|
| `api` | every feature that picks |
| `ui` | features that crop or preview (with `coil-compose`) |
| `impl` | only `wiring` |
| `wiring` | the module that owns the app graph |
| `testing` | `commonTest` of features with pickers |

`impl` enables Android resources (its FileProvider's paths); nothing to do in the app.

## The root

`MediaWiring` contributes `LocalMediaPickerProvider` and `LocalCameraPermission` into
`Set<ProvidedValue<*>>`. The app graph exposes the set and the root installs it once:

```kotlin
@DependencyGraph(AppScope::class)
interface AppGraph {
    val compositionLocals: Set<ProvidedValue<*>>
}

CompositionLocalProvider(*graph.compositionLocals.toTypedArray()) { AppContent() }
```

With arch-kit, `ComponentLocals(…)` installs the same set. On Android the root must be inside a
`ComponentActivity` (the launchers need an activity-result registry).

## Manifest and Info.plist

- iOS: `NSCameraUsageDescription` if the camera is offered. The photo and document pickers need nothing.
- Android: nothing for the pickers. If the app declares `CAMERA` (for its own camera screen), Android
  requires it granted for the system camera too, and the kit asks for it; add
  `<uses-feature android:name="android.hardware.camera" android:required="false" />`.

## Replacing the camera permission

The kit's `CameraPermission` asks once, with no rationale and no settings. An app with its own permission
code replaces it — with permissions-kit, one file (full code in `media/api/README.md` → The camera
permission):

```kotlin
object PermissionsKitCameraPermission : CameraPermission { … rememberPermissionState(AppPermission.Camera) … }

@BindingContainer
@ContributesTo(AppScope::class, replaces = [MediaCameraPermissionWiring::class])
interface AppCameraPermissionWiring {
    companion object {
        @Provides
        fun provideCameraPermission(): CameraPermission = PermissionsKitCameraPermission
    }
}
```

## Checks

- Every pick returns `Cancelled` at once → the provider is not installed at the root.
- The camera crashes the iOS app → `NSCameraUsageDescription` is missing.
- The camera returns `Unavailable` → no camera (the iOS simulator has none).
- The camera returns `PermissionDenied` without a prompt → refused earlier; send the user to settings.
