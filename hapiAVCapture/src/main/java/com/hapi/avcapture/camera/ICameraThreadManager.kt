package com.hapi.avcapture.camera

import android.hardware.camera2.CameraCaptureSession
import android.hardware.camera2.CameraDevice
import android.hardware.camera2.CameraManager
import android.hardware.camera2.CaptureRequest
import android.view.Surface

/**
 * Encapsulates camera2 API changes and deprecation.
 */
interface ICameraThreadManager {

    /**
     * Opens camera device.
     *
     * @param manager the [CameraManager]
     * @param cameraId the camera id
     * @param callback an implementation of [CameraDevice.StateCallback]
     */
    fun openCamera(
        manager: CameraManager,
        cameraId: String,
        callback: CameraDevice.StateCallback
    )

    /**
     * Create a camera capture session.
     *
     * @param camera the [CameraDevice]
     * @param targets list of surfaces
     * @param callback an implementation of [CameraCaptureSession.StateCallback]
     */
    fun createCaptureSession(
        camera: CameraDevice,
        targets: List<Surface>,
        callback: CameraCaptureSession.StateCallback
    )

    /**
     * Set a repeating request.
     *
     * @param captureSession the [CameraCaptureSession]
     * @param captureRequest the [CaptureRequest]
     * @param callback an implementation of [CameraCaptureSession.CaptureCallback]
     * @return A unique capture sequence ID
     */
    fun setRepeatingSingleRequest(
        captureSession: CameraCaptureSession,
        captureRequest: CaptureRequest,
        callback: CameraCaptureSession.CaptureCallback
    ): Int

    /**
     * Capture multiple burst requests.
     *
     * @param captureSession the [CameraCaptureSession]
     * @param captureRequests the list of [CaptureRequest]
     * @param callback an implementation of [CameraCaptureSession.CaptureCallback]
     * @return A unique capture sequence ID
     */
    fun captureBurstRequests(
        captureSession: CameraCaptureSession,
        captureRequests: List<CaptureRequest>,
        callback: CameraCaptureSession.CaptureCallback
    ): Int

    /**
     * Release internal object.
     */
    fun release()
}