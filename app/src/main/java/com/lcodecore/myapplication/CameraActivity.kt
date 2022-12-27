package com.lcodecore.myapplication

import android.annotation.SuppressLint
import android.hardware.camera2.CaptureRequest
import android.hardware.camera2.CaptureResult
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Environment
import android.util.Range
import android.view.View
import android.widget.Button
import android.widget.CompoundButton
import android.widget.RadioGroup
import androidx.annotation.RequiresApi
import androidx.appcompat.widget.SwitchCompat
import com.hapi.ioutput.ConnectedStatus
import com.hapi.avcapture.*
import com.hapi.avcapturerender.HapiAudioTrackRender
import com.hapi.avcapturerender.HapiCaptureTextureView
import com.hapi.avencoder.AudioEncodeParam
import com.hapi.avencoder.EncoderStatus
import com.hapi.avencoder.VideoEncodeParam
import com.hapi.avpackerclinet.HapiAVPackerClient
import com.hapi.avpackerclinet.PackerClientListener
import com.hapi.avparam.AVLog
import com.hapi.ioutput.OutputStreamerEvent
import java.io.File

class CameraActivity : AppCompatActivity() {
    var recordUrl = ""
    private val videoEncodeParam = VideoEncodeParam(
        480,
        640,
        800 * 1000,
        20
    ).apply {
        minVideoBitRate = 600 * 1000
        maxVideoBitRate = (1.2 * 1000 * 1000).toInt()
    }

    private val audioEncodeParam = AudioEncodeParam(
        DEFAULT_SAMPLE_RATE,
        DEFAULT_CHANNEL_LAYOUT,
        DEFAULT_SAMPLE_FORMAT,
        96000
    )

    private val packerClientListener = object : PackerClientListener {
        override fun onMuxerConnectedStatus(status: ConnectedStatus) {
            if (status == ConnectedStatus.CONNECTED_STATUS_START) {
                findViewById<RadioGroup>(R.id.rgEncodeType).visibility = View.GONE
            }

            if (status == ConnectedStatus.CONNECTED_STATUS_DISCONNECTED) {
                findViewById<RadioGroup>(R.id.rgEncodeType).visibility = View.VISIBLE
            }
            AVLog.d("packerClientListener", "onMuxerConnectedStatus $status")
        }

        override fun onOutputStreamerEvent(event: OutputStreamerEvent, msg: String) {

        }

        override fun onEncoderStatusChange(encodeStatus: EncoderStatus) {
            AVLog.d("packerClientListener", "onEncoderStatusChange $encodeStatus ")
        }
    }

    //本地录制
    private val recordClient by lazy {
        HapiAVPackerClient().apply {
            packerClientListener = this@CameraActivity.packerClientListener
        }
    }

    //推流
    private val pusherClient by lazy {
        HapiAVPackerClient().apply {
            packerClientListener = this@CameraActivity.packerClientListener
        }
    }

    //摄像头轨道
    private val cameraTrack by lazy {
        HapiTrackFactory.createCameraXTrack(this, 480, 640, 30)
    }

    //麦克轨道
    private val microphoneTrack by lazy {
        HapiTrackFactory.createMicrophoneTrack(
            this, DEFAULT_SAMPLE_RATE,
            DEFAULT_CHANNEL_LAYOUT,
            DEFAULT_SAMPLE_FORMAT
        )
    }

    private val mHapiAudioTrackRender = HapiAudioTrackRender()

    @SuppressLint("CutPasteId")
    @RequiresApi(Build.VERSION_CODES.M)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_camera)
        lifecycle.addObserver(recordClient)
        lifecycle.addObserver(pusherClient)

        //如果需要预览视频轨道
        cameraTrack.playerView = findViewById<HapiCaptureTextureView>(R.id.preview)
        //耳返回
        // microphoneTrack.audioRender = mHapiAudioTrackRender
        microphoneTrack.startWithPermissionCheck(this, object : TrackPermissionCallback {
            override fun onPermissionResult(permissionCode: Int) {

            }
        })
        cameraTrack.startWithPermissionCheck(this, object : TrackPermissionCallback {
            override fun onPermissionResult(permissionCode: Int) {

            }
        })
        recordClient.attachTrack(cameraTrack, videoEncodeParam)
        recordClient.attachTrack(microphoneTrack, audioEncodeParam)

        pusherClient.attachTrack(cameraTrack, videoEncodeParam)
        pusherClient.attachTrack(microphoneTrack, audioEncodeParam)

        val btnStop = findViewById<Button>(R.id.btStop)
        btnStop.setOnClickListener {
            if (it.isSelected) {
                recordClient.resume()
                btnStop.isSelected = false
                btnStop.text = "暂停录制"
            } else {
                recordClient.pause()
                btnStop.isSelected = true
                btnStop.text = "恢复录制"
            }
        }

        findViewById<RadioGroup>(R.id.rgEncodeType).setOnCheckedChangeListener { p0, id ->

            videoEncodeParam.encoderType = if (id == R.id.rbHWEncode) {
                VideoEncodeParam.EncoderType.HWEncoder
            } else {
                VideoEncodeParam.EncoderType.SOFT
            }
            recordClient.detachTrack(cameraTrack)
            pusherClient.detachTrack(cameraTrack)
            recordClient.attachTrack(cameraTrack, videoEncodeParam)
            pusherClient.attachTrack(cameraTrack, videoEncodeParam)
        }

        findViewById<Button>(R.id.btRecord)
            .setOnClickListener {
                btnStop.visibility = View.GONE
                if (it.isSelected) {
                    it.isSelected = false
                    (it as Button).text = "录制"
                    recordClient.stop()
                    MediaStoreUtils.insertVideoToMediaStore(this, recordUrl)
                } else {
                    btnStop.visibility = View.VISIBLE
                    btnStop.isSelected = false
                    btnStop.text = "暂停录制"
                    it.isSelected = true
                    (it as Button).text = "结束"

                    recordUrl = if (Build.VERSION.SDK_INT > 29) {
                        cacheDir.absolutePath
                    } else {
                        Environment.getExternalStorageDirectory().getAbsolutePath()
                            .toString()
                    } + "/${System.currentTimeMillis()}.mp4"

                    val file = File(recordUrl)
                    if (!file.exists()) {
                        file.createNewFile()
                    }

                    //开始录制
                    recordClient.start(recordUrl)
                }
            }

        val url = intent.getStringExtra("url") ?: ""
        findViewById<Button>(R.id.btPush).setOnClickListener {
            if (it.isSelected) {
                it.isSelected = false
                (it as Button).text = "推流"
                pusherClient.stop()
            } else {
                it.isSelected = true
                (it as Button).text = "结束"
                //开始推流
                pusherClient.start(url)

                findViewById<SwitchCompat>(R.id.swPushMuteVideo).isClickable = true
                findViewById<SwitchCompat>(R.id.swPushMuteAudio).isClickable = true
                findViewById<SwitchCompat>(R.id.swPushMuteVideo).isChecked = false
                findViewById<SwitchCompat>(R.id.swPushMuteAudio).isChecked = false
            }
        }

        findViewById<View>(R.id.switchButton).setOnClickListener {
            cameraTrack.switchCamera()
        }

        findViewById<View>(R.id.btnFlash).setOnClickListener {
            val mode = if (it.isSelected) {
                CaptureResult.FLASH_MODE_OFF
            } else {
                CaptureResult.FLASH_MODE_TORCH
            }
            it.isSelected = !it.isSelected
            cameraTrack.setRepeatingSetting(CaptureRequest.FLASH_MODE, mode)
        }


        val wbModes = ArrayList<Int>()
        var currentWBIndex = 0
        findViewById<View>(R.id.btnWhiteBalance).setOnClickListener {
            wbModes.clear()
            wbModes.addAll(cameraTrack.getAvailableWhiteBalanceModes())
            if (wbModes.isEmpty()) {
                return@setOnClickListener
            }
            currentWBIndex %= wbModes.size
            cameraTrack.setRepeatingSetting(
                CaptureRequest.CONTROL_AWB_MODE,
                wbModes[currentWBIndex++]
            )
        }

        var compensation: Int = -100
        var availableCompensationRange = Range(0, 0)
        findViewById<View>(R.id.btnExposureAdd).setOnClickListener {
            if (compensation == -100) {
                availableCompensationRange = cameraTrack.getAvailableCompensationRange()
                compensation =
                    cameraTrack.getSetting(CaptureRequest.CONTROL_AE_EXPOSURE_COMPENSATION) ?: 0
            }
            compensation += 1
            cameraTrack.setRepeatingSetting(
                CaptureRequest.CONTROL_AE_EXPOSURE_COMPENSATION,
                compensation.clamp(availableCompensationRange)
            )
        }
        findViewById<View>(R.id.btnExposureMinus).setOnClickListener {
            if (compensation == -100) {
                availableCompensationRange = cameraTrack.getAvailableCompensationRange()
                compensation =
                    cameraTrack.getSetting(CaptureRequest.CONTROL_AE_EXPOSURE_COMPENSATION) ?: 0
            }
            compensation -= 1
            cameraTrack.setRepeatingSetting(
                CaptureRequest.CONTROL_AE_EXPOSURE_COMPENSATION,
                compensation.clamp(availableCompensationRange)
            )
        }

        var availableRatioRange = Range(1f, 1f)
        var zoomRatio: Float = -100f

        findViewById<View>(R.id.btnZoomAdd).setOnClickListener {
            if (zoomRatio == -100f) {
                zoomRatio = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    cameraTrack.getSetting(CaptureRequest.CONTROL_ZOOM_RATIO) ?: 1f
                } else {
                    1f
                }
                availableRatioRange = cameraTrack.getAvailableZoomRatioRange()
            }
            zoomRatio += 0.4f
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                cameraTrack.setRepeatingSetting(
                    CaptureRequest.CONTROL_ZOOM_RATIO,
                    zoomRatio.clamp(availableRatioRange)
                )
            } else {
                cameraTrack.setRepeatingSetting(
                    CaptureRequest.SCALER_CROP_REGION,
                    cameraTrack.getCropRegion(
                        zoomRatio
                    )
                )
            }
        }

        findViewById<View>(R.id.btnZoomMinus).setOnClickListener {
            if (zoomRatio == -100f) {
                zoomRatio = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    cameraTrack.getSetting(CaptureRequest.CONTROL_ZOOM_RATIO) ?: 1f
                } else {
                    1f
                }
                availableRatioRange = cameraTrack.getAvailableZoomRatioRange()
            }
            zoomRatio -= 0.4f
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                cameraTrack.setRepeatingSetting(
                    CaptureRequest.CONTROL_ZOOM_RATIO,
                    zoomRatio.clamp(availableRatioRange)
                )
            } else {
                cameraTrack.setRepeatingSetting(
                    CaptureRequest.SCALER_CROP_REGION,
                    cameraTrack.getCropRegion(
                        zoomRatio
                    )
                )
            }
        }

        val focusModes = ArrayList<Int>()
        var currentFocusModelIndex = 0
        findViewById<View>(R.id.btnFocus).setOnClickListener {
            focusModes.clear()
            focusModes.addAll(cameraTrack.getAvailableAutoFocusModes())

            if (focusModes.isEmpty()) {
                return@setOnClickListener
            }
            currentFocusModelIndex %= focusModes.size
            cameraTrack.setRepeatingSetting(
                CaptureRequest.CONTROL_AF_MODE,
                focusModes[currentFocusModelIndex++]
            )
        }

        findViewById<SwitchCompat>(R.id.swPushMuteVideo).isClickable = false
        findViewById<SwitchCompat>(R.id.swPushMuteAudio).isClickable = false
        findViewById<SwitchCompat>(R.id.swPushMuteVideo).setOnCheckedChangeListener { buttonView, isChecked ->
            pusherClient.muteTrack(isChecked, cameraTrack)
        }

        findViewById<SwitchCompat>(R.id.swPushMuteAudio).setOnCheckedChangeListener { buttonView, isChecked ->
            pusherClient.muteTrack(isChecked, microphoneTrack)
        }
    }

    fun <T : Comparable<T>> T.clamp(min: T, max: T): T {
        return if (max >= min) {
            if (this < min) min else if (this > max) max else this
        } else {
            if (this < max) max else if (this > min) min else this
        }
    }

    fun <T : Comparable<T>> T.clamp(range: Range<T>) =
        this.clamp(range.lower, range.upper)

}