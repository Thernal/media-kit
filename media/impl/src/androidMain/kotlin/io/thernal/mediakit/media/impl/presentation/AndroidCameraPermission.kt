package io.thernal.mediakit.media.impl.presentation

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import io.thernal.mediakit.media.api.presentation.CameraAccess
import io.thernal.mediakit.media.api.presentation.CameraPermission
import kotlinx.coroutines.CompletableDeferred

/**
 * MINIMAL PERMISSION CODE — replace it with your app's own.
 *
 * It asks once and reports the answer: no rationale, no statuses, no way to settings. An app that
 * handles permissions itself (permissions-kit, say) binds its own `CameraPermission` instead — see
 * `media/api/README.md` → The camera permission — and this file is never used.
 *
 * The camera app needs no permission of ours unless the manifest declares `CAMERA`; then Android
 * requires it granted, and this asks for it.
 */
internal object AndroidCameraPermission : CameraPermission {
    @Composable
    override fun rememberCameraAccess(): CameraAccess {
        val context = LocalContext.current
        val answers = remember { PermissionAnswers() }
        val launcher = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.RequestPermission(),
            onResult = answers::onAnswer,
        )
        return remember(key1 = context, key2 = launcher) {
            object : CameraAccess {
                override val isGranted: Boolean
                    get() = !context.declaresCamera() || context.hasCamera()

                override suspend fun request(): Boolean {
                    if (isGranted) {
                        return true
                    }
                    return answers.await { launcher.launch(Manifest.permission.CAMERA) }
                }
            }
        }
    }
}

private class PermissionAnswers {
    private var pending: CompletableDeferred<Boolean>? = null

    suspend fun await(launch: () -> Unit): Boolean {
        val answer = CompletableDeferred<Boolean>()
        pending = answer
        launch()
        return answer.await()
    }

    fun onAnswer(granted: Boolean) {
        pending?.complete(granted)
        pending = null
    }
}

private fun Context.hasCamera(): Boolean {
    return ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
}

private fun Context.declaresCamera(): Boolean {
    val info = packageManager.getPackageInfo(packageName, PackageManager.GET_PERMISSIONS)
    return info.requestedPermissions?.contains(Manifest.permission.CAMERA) == true
}
