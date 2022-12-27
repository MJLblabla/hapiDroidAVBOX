package com.hapi.avcapture

import android.content.ComponentName
import android.content.Context
import android.os.IBinder
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import com.hapi.avcapture.screen.*
import com.hapi.avparam.AVLog
import com.hapi.eglbase.ImageReaderSurfaceProvider

class ScreenCaptureTrack internal constructor(
    private val lifecycleOwner: LifecycleOwner? = null,
    val with: Int,
    val height: Int
) : IVideoTrack() {
    private val surfaceProvider =
        ImageReaderSurfaceProvider()

     //OESSurfaceProvider(offscreenSurfaceHelper)
    //Image2DReaderSurfaceProvider(offscreenSurfaceHelper)
    private var mediaProjectionService: ScreenRecordService? = null
    private var serviceConnection: ScreenRecordService.ScreenRecordServiceConnection? = null
    private val mImageListener by lazy {
        object : ImageListener {
            override fun onImageAvailable(videoFrame: VideoFrame) {
                innerPushFrame(videoFrame)
            }

            override fun onBindingDied() {
            }
        }
    }

    init {
        lifecycleOwner?.lifecycle?.addObserver(object : LifecycleEventObserver {
            override fun onStateChanged(source: LifecycleOwner, event: Lifecycle.Event) {
                if (event == Lifecycle.Event.ON_DESTROY) {
                    release()
                }
            }
        })
    }

    private fun startRecordService(
        activity: FragmentActivity,
        callback: ScreenCaptureServiceCallBack
    ) {
        serviceConnection = object : ScreenRecordService.ScreenRecordServiceConnection() {
            override fun onStartError(code: Int, msg: String) {
                callback.onError(code, msg)
            }

            override fun onServiceConnected(name: ComponentName, service: IBinder) {
                //  mediaProjectionService = new Messenger(service);
                mediaProjectionService =
                    (service as ScreenRecordService.MediaProjectionBinder).service
                mediaProjectionService?.start(
                    with, height,
                    resultCode,
                    data,
                    surfaceProvider,
                    mImageListener
                )
                callback.onStart()
            }

            override fun onServiceDisconnected(name: ComponentName) {
                AVLog.d("ServiceConnection", "onServiceDisconnected ")
            }

            override fun onBindingDied(name: ComponentName) {
                AVLog.d("ServiceConnection", "onBindingDied ")
            }

            override fun onNullBinding(name: ComponentName) {
                AVLog.d("ServiceConnection", "onNullBinding ")
            }
        }
        ScreenRecordService.bindService(activity, serviceConnection!!)
    }

    private var context: Context? = null
    fun start(
        activity: FragmentActivity,
        callback: ScreenCaptureServiceCallBack
    ) {
        if (isStart) {
            return
        }
        context = activity.applicationContext
        startRecordService(activity, object :
            ScreenCaptureServiceCallBack {
            override fun onStart() {
                isStart = true
                callback.onStart()
            }

            override fun onError(code: Int, msg: String) {
                callback.onError(code, msg)
            }
        })
    }

    fun stop() {
        if (!isStart) {
            return
        }
        isStart = false
        context?.unbindService(serviceConnection!!)
        mediaProjectionService?.stopMediaRecorder()
        mediaProjectionService?.stopSelf()
        mediaProjectionService = null
    }

    override fun releaseOnMainThread() {
        super.releaseOnMainThread()
        stop()
    }

    override fun releaseOnGLThread() {
        super.releaseOnGLThread()
        surfaceProvider.release()
    }

    interface ScreenCaptureServiceCallBack {
        fun onStart()
        fun onError(code: Int, msg: String)
    }
}