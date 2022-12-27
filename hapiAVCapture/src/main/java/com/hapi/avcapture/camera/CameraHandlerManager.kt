
package com.hapi.avcapture.camera

import android.Manifest
import android.hardware.camera2.CameraCaptureSession
import android.hardware.camera2.CameraDevice
import android.hardware.camera2.CameraManager
import android.hardware.camera2.CaptureRequest
import android.os.Handler
import android.os.HandlerThread
import android.view.Surface
import androidx.annotation.RequiresPermission

/**
 * As camera API that support a [Handler] are deprecated since API >= 30.
 * Is is a [ICameraThreadManager] that manages camera API < 30.
 */
class CameraHandlerManager : ICameraThreadManager {
    private var cameraThread = HandlerThread("CameraThread").apply { start() }
    private var cameraHandler = Handler(cameraThread.looper)

    @RequiresPermission(Manifest.permission.CAMERA)
    override fun openCamera(
        manager: CameraManager,
        cameraId: String,
        callback: CameraDevice.StateCallback
    ) {
        manager.openCamera(cameraId, callback, cameraHandler)
    }

    override fun createCaptureSession(
        camera: CameraDevice,
        targets: List<Surface>,
        callback: CameraCaptureSession.StateCallback
    ) {
        @Suppress("deprecation")
        camera.createCaptureSession(targets, callback, cameraHandler)
    }

    override fun setRepeatingSingleRequest(
        captureSession: CameraCaptureSession,
        captureRequest: CaptureRequest,
        callback: CameraCaptureSession.CaptureCallback
    ): Int {
        return captureSession.setRepeatingRequest(captureRequest, callback, cameraHandler)
    }

    override fun captureBurstRequests(
        captureSession: CameraCaptureSession,
        captureRequests: List<CaptureRequest>,
        callback: CameraCaptureSession.CaptureCallback
    ): Int {
        return captureSession.captureBurst(captureRequests, callback, cameraHandler)
    }

    override fun release() {
        cameraThread.quitSafely()
        try {
            cameraThread.join()
        } catch (e: InterruptedException) {
            e.printStackTrace()
        }
    }
}