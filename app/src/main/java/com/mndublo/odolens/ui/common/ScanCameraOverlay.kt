package com.mndublo.odolens.ui.common

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.mndublo.odolens.ui.camera.CameraView
import kotlinx.coroutines.launch
import java.io.File

/**
 * Full-screen camera overlay shared by the scan screens.
 *
 * Shows [CameraView] while [visible]. Once a photo is captured it hides the
 * camera (via [onClose]), decodes the captured file, invokes [onBitmap] with the
 * result (or [onError] when decoding fails) and cleans up the temp file.
 */
@Composable
fun ScanCameraOverlay(
    visible: Boolean,
    onBitmap: suspend (Bitmap) -> Unit,
    onError: (String) -> Unit,
    onClose: () -> Unit
) {
    if (!visible) return

    // System back closes the camera and returns to the app screen instead of
    // finishing the activity (which would quit the app). Only composed while
    // the overlay is visible, so normal back navigation is unaffected otherwise.
    BackHandler { onClose() }

    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val loadErrorMessage = stringResource(com.mndublo.odolens.R.string.camera_failed_to_load_image)

    CameraView(
        onImageCaptured = { path ->
            onClose()
            scope.launch {
                val bitmap = BitmapFactory.decodeFile(path)
                if (bitmap != null) {
                    onBitmap(bitmap)
                } else {
                    onError(loadErrorMessage)
                }
                try {
                    File(path).delete()
                } catch (e: Exception) {
                }
            }
        },
        onClose = onClose
    )
}
