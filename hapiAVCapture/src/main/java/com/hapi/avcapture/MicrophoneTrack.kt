package com.hapi.avcapture

import android.Manifest
import android.annotation.SuppressLint
import android.media.AudioRecord
import android.media.MediaRecorder
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.*
import com.hapi.avcapture.permission.PermissionAnywhere
import com.hapi.avparam.AVChannelConfig
import com.hapi.avparam.AVSampleFormat
import java.nio.ByteBuffer

class MicrophoneTrack internal constructor(
    private val lifecycleOwner: LifecycleOwner? = null,
    private val sampleRateInHz: Int,
    private val AVChannelConfig: AVChannelConfig,
    private val audioFormat: AVSampleFormat
) : IAudioTrack() {

    private var mAudioRecord: AudioRecord? = null

    init {
        lifecycleOwner?.lifecycle?.addObserver(object : LifecycleEventObserver {
            override fun onStateChanged(source: LifecycleOwner, event: Lifecycle.Event) {
                if (event == Lifecycle.Event.ON_DESTROY) {
                    stop()
                }
            }
        })
    }

    fun startWithPermissionCheck(context: FragmentActivity, callback: TrackPermissionCallback) {
        val permission = Manifest.permission.RECORD_AUDIO
        PermissionAnywhere.requestPermission(
            context,
            arrayOf(
                permission
            )
        ) { grantedPermissions, _, _ ->
            if (grantedPermissions.size == 1) {
                start()
            }
            val permissionCode = ContextCompat.checkSelfPermission(context, permission)
            callback.onPermissionResult(permissionCode)
        }
    }

    @SuppressLint("MissingPermission")
    fun start() {
        if (isStart) {
            return
        }
        isStart = true
        val mMinBufferSize = AudioRecord.getMinBufferSize(
            sampleRateInHz, AVChannelConfig.androidChannel, audioFormat.androidFMT
        )
        if (AudioRecord.ERROR_BAD_VALUE == mMinBufferSize) {
            throw java.lang.Exception("parameters are not supported by the hardware.")
        }
        mAudioRecord = AudioRecord(
            MediaRecorder.AudioSource.MIC,
            sampleRateInHz, AVChannelConfig.androidChannel, audioFormat.androidFMT,
            mMinBufferSize
        )

        Thread(Runnable {
            mAudioRecord!!.startRecording()

            val sizeInBytes: Int = if (AVChannelConfig.count == 1) {
                2048
            } else {
                4096
            }
            val sampleBuffer = ByteBuffer.allocateDirect(sizeInBytes)
            val buffer=ByteArray(sizeInBytes)
            try {
                while (isStart && mAudioRecord != null && !Thread.currentThread().isInterrupted) {
                    val result = mAudioRecord!!.read(buffer, 0,sizeInBytes)
                    if (result > 0) {
                        sampleBuffer.clear()
                        sampleBuffer.put(buffer)
                        sampleBuffer.flip()
                        val outFrame = AudioFrame(
                            sampleRateInHz, AVChannelConfig, audioFormat,
                            sampleBuffer
                        )
                        innerPushFrame(outFrame)
                    }
                }
            } catch (e: java.lang.Exception) {
                e.printStackTrace()
            }
        }).start()
    }

    fun stop() {
        if (!isStart) {
            return
        }
        isStart = false
        mAudioRecord?.stop()
    }

}