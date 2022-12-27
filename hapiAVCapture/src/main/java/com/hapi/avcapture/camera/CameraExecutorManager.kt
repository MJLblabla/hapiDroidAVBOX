
package com.hapi.avcapture.camera

import android.Manifest
import android.hardware.camera2.CameraCaptureSession
import android.hardware.camera2.CameraDevice
import android.hardware.camera2.CameraManager
import android.hardware.camera2.CaptureRequest
import android.hardware.camera2.params.OutputConfiguration
import android.hardware.camera2.params.SessionConfiguration
import android.os.Build
import android.view.Surface
import androidx.annotation.RequiresApi
import androidx.annotation.RequiresPermission
import java.util.concurrent.Executors

/**
 * A [ICameraThreadManager] that manages camera API >= 28.
 */
class CameraExecutorManager : ICameraThreadManager {
    private val cameraExecutor = Executors.newSingleThreadExecutor()

    @RequiresApi(Build.VERSION_CODES.P)
    @RequiresPermission(Manifest.permission.CAMERA)
    override fun openCamera(
        manager: CameraManager,
        cameraId: String,
        callback: CameraDevice.StateCallback
    ) {
        manager.openCamera(cameraId, cameraExecutor, callback)
    }

    @RequiresApi(Build.VERSION_CODES.P)
    override fun createCaptureSession(
        camera: CameraDevice,
        targets: List<Surface>,
        callback: CameraCaptureSession.StateCallback
    ) {
        val outputs = mutableListOf<OutputConfiguration>()
        targets.forEach { outputs.add(OutputConfiguration(it)) }
        SessionConfiguration(
            SessionConfiguration.SESSION_REGULAR,
            outputs,
            cameraExecutor,
            callback
        ).also { sessionConfig ->
            camera.createCaptureSession(sessionConfig)
        }
    }

    @RequiresApi(Build.VERSION_CODES.P)
    override fun setRepeatingSingleRequest(
        captureSession: CameraCaptureSession,
        captureRequest: CaptureRequest,
        callback: CameraCaptureSession.CaptureCallback
    ): Int {
        return captureSession.setSingleRepeatingRequest(captureRequest, cameraExecutor, callback)
    }

    @RequiresApi(Build.VERSION_CODES.P)
    override fun captureBurstRequests(
        captureSession: CameraCaptureSession,
        captureRequests: List<CaptureRequest>,
        callback: CameraCaptureSession.CaptureCallback
    ): Int {
        return captureSession.captureBurstRequests(captureRequests, cameraExecutor, callback)
    }

    override fun release() {
    }
}