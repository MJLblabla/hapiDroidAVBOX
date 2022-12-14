package com.hapi.avcapture

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.graphics.ImageFormat
import android.graphics.Rect
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.hardware.camera2.CaptureRequest
import android.util.Log
import android.util.Range
import android.util.Rational
import android.view.Surface
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.*
import com.hapi.avcapture.camera.*
import com.hapi.avcapture.permission.PermissionAnywhere
import com.hapi.eglbase.ImageReaderSurfaceProvider
import com.hapi.eglbase.OESSurfaceProvider
import kotlinx.coroutines.launch


/**
 * Camera x track
 *
 * @property width
 * @property height
 * @property context
 * @property lifecycleOwner
 * @constructor Create empty Camera x track
 */
class CameraXTrack internal constructor(
    private val width: Int,
    private val height: Int,
    private val fps: Int,
    private val lifecycleOwner: LifecycleOwner = CustomLifecycle()
) : IVideoTrack() {

    private val surfaceProvider = OESSurfaceProvider(offscreenSurfaceHelper)
    private var context: Context? = null
    private var cameraController: CameraController? = null
    private var autoPause = false

    init {
        lifecycleOwner.lifecycle.addObserver(object : LifecycleEventObserver {
            @SuppressLint("RestrictedApi")
            override fun onStateChanged(source: LifecycleOwner, event: Lifecycle.Event) {
                Log.d("CameraXTrack", "event ${event.name}")
                if (event == Lifecycle.Event.ON_DESTROY) {
                    release()
                }
                if (event == Lifecycle.Event.ON_PAUSE) {
                    if (isStart) {
                        autoPause = true
                        stop()
                    }
                }
                if (event == Lifecycle.Event.ON_RESUME) {
                    if (autoPause && context != null) {
                        autoPause = false
                        startInner(context!!)
                    }
                }
            }
        })
    }

    /**
     * ?????????????????????
     */
    var cameraFace = CameraFaceType.CAMERA_FACE_FRONT

    /**
     * ???????????????
     *
     */
    fun switchCamera() {
        context ?: return
        stop()
        if (context!!.isBackCamera(cameraFace.cameraID)) {
            cameraFace = CameraFaceType.CAMERA_FACE_FRONT
            cameraFace.cameraID = context!!.getFrontCameraList()[0]
        } else {
            cameraFace = CameraFaceType.CAMERA_FACE_BACK
            cameraFace.cameraID = context!!.getBackCameraList()[0]
        }
        startInner(context!!)
    }

    /**
     * ?????????????????????
     *
     * @return
     */
    fun getAvailableAutoFocusModes(): List<Int> {
        return context?.getAutoFocusModes(cameraFace.cameraID) ?: ArrayList<Int>()
    }

    /**
     * ????????????????????????
     *
     * @return
     */
    fun getAvailableWhiteBalanceModes(): List<Int> {
        return context?.getAutoWhiteBalanceModes(cameraFace.cameraID) ?: ArrayList<Int>()
    }

    /**
     * ?????????????????????
     *
     * @return
     */
    fun getAvailableZoomRatioRange(): Range<Float> {
        return context?.getZoomRatioRange(cameraFace.cameraID) ?: Range(1f, 1f)
    }

    fun getAvailableCompensationStep(): Rational {
        return context?.getExposureStep(cameraFace.cameraID) ?: Rational(1, 1)
    }

    /**
     * ?????????????????????
     *
     * @return
     */
    fun getAvailableCompensationRange(): Range<Int> {
        return context?.getExposureRange(cameraFace.cameraID) ?: Range(0, 0)
    }

    /**
     * ?????????????????????????????????
     *
     * @param T
     * @param key
     * @return
     */
    fun <T> getSetting(key: CaptureRequest.Key<T>?): T? {
        return cameraController?.getSetting(key)
    }

    /**
     * ??????????????????
     *
     * @param T
     * @param key
     * @param value
     */
    fun <T> setRepeatingSetting(key: CaptureRequest.Key<T>, value: T) {
        cameraController?.setRepeatingSetting(key, value)
    }

    /**
     * ??????????????????
     *
     * @param settingsMap
     */
    fun setRepeatingSettings(settingsMap: Map<CaptureRequest.Key<Any>, Any>) {
        cameraController?.setRepeatingSettings(settingsMap)
    }

    fun setBurstSettings(settingsMap: Map<CaptureRequest.Key<Any>, Any>) {
        cameraController?.setBurstSettings(settingsMap)
    }

    private fun getCameraCharacteristics(): CameraCharacteristics {
        val cameraManager = context!!.getSystemService(Context.CAMERA_SERVICE) as CameraManager
        return cameraManager.getCameraCharacteristics(cameraFace.cameraID)
    }

    /**
     * Get crop region
     *
     * @param zoomRatio
     * @return
     */
    fun getCropRegion(zoomRatio: Float): Rect {
        val characteristics: CameraCharacteristics = getCameraCharacteristics()
        val sensorRect =
            characteristics.get(CameraCharacteristics.SENSOR_INFO_ACTIVE_ARRAY_SIZE)!!
        val xCenter: Int = sensorRect.width() / 2
        val yCenter: Int = sensorRect.height() / 2
        val xDelta = (0.5f * sensorRect.width() / zoomRatio).toInt()
        val yDelta = (0.5f * sensorRect.height() / zoomRatio).toInt()
        return Rect(xCenter - xDelta, yCenter - yDelta, xCenter + xDelta, yCenter + yDelta)
    }

    /**
     * ???????????????????????????
     * @param callback
     */
    fun startWithPermissionCheck(context: FragmentActivity, callback: TrackPermissionCallback) {
        val permission = Manifest.permission.CAMERA
        PermissionAnywhere.requestPermission(
            context,
            arrayOf(
                permission
            )
        ) { grantedPermissions, _, _ ->
            if (grantedPermissions.size == 1) {
                start(context)
            }
            val permissionCode = ContextCompat.checkSelfPermission(context, permission)
            callback.onPermissionResult(permissionCode)
        }
    }

    /**
     * ???????????? ???????????????
     *
     */

    fun start(context: Context) {
        this.context = context
        startInner(context)
    }

    private var mCameraOutputOrientation = -1

    @SuppressLint("MissingPermission")
    private fun startInner(context: Context) {
        if (isRelease && isStart) {
            return
        }
        isStart = true
        if (cameraFace.cameraID.isEmpty()) {
            if (cameraFace == CameraFaceType.CAMERA_FACE_FRONT) {
                cameraFace.cameraID = context.getFrontCameraList()[0]
            } else {
                cameraFace.cameraID = context.getBackCameraList()[0]
            }
        }
        if (cameraController == null) {
            cameraController = CameraController(context)
            surfaceProvider.createSurface(width, height)
            surfaceProvider.setOnFrameAvailableListener { it, time ->
                if (mCameraOutputOrientation == -1) {
                    mCameraOutputOrientation =
                        context.getCameraOutputOrientation(cameraFace.cameraID)
                }
                var outW = width
                var outH = height
                if (mCameraOutputOrientation == 90 || mCameraOutputOrientation == 270) {
                    outH = width
                    outW = height
                }
                innerPushFrame(VideoFrame(outW, outH, mCameraOutputOrientation, it).apply {
                    timestamp = time
                })
            }
        }
        lifecycleOwner.lifecycleScope.launch {
            mCameraOutputOrientation = -1
            val targets = mutableListOf<Surface>()
            targets.add(surfaceProvider.surface!!)
            cameraController?.startCamera(cameraFace.cameraID, targets)
            cameraController?.startRequestSession(fps, targets)
        }
    }

    /**
     * ????????????
     */
    @SuppressLint("RestrictedApi")
    fun stop() {
        if (!isStart) {
            return
        }
        isStart = false
        cameraController?.stopCamera()
    }

    override fun releaseOnMainThread() {
        super.releaseOnMainThread()
        context = null
        stop()
    }

    override fun releaseOnGLThread() {
        super.releaseOnGLThread()
        surfaceProvider.release()
    }

    private class CustomLifecycle : LifecycleOwner {
        private val lifecycleRegistry: LifecycleRegistry = LifecycleRegistry(this)

        init {
            lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_RESUME)
        }

        fun doRelease() {
            lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)
        }

        override fun getLifecycle(): Lifecycle {
            return lifecycleRegistry
        }
    }
}