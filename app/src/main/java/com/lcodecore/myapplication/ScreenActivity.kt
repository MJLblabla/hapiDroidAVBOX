package com.lcodecore.myapplication

import android.content.res.Resources
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.util.Log
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import com.hapi.avcapture.*
import com.hapi.avcapture.screen.ScreenRecordService
import com.hapi.avcapturerender.HapiCaptureTextureView
import com.hapi.avpackerclinet.HapiAVPackerClient
import com.hapi.avencoder.AudioEncodeParam
import com.hapi.avencoder.VideoEncodeParam
import com.hapi.avparam.AVChannelConfig
import java.io.File

class ScreenActivity : AppCompatActivity() {

    var recordUrl = ""
    val videoEncodeParam = VideoEncodeParam(
        ScreenRecordService.screenWidth,
        ScreenRecordService.screenHeight,
        1000 * 1000,
        25
    )
    val audioEncodeParam = AudioEncodeParam(
        DEFAULT_SAMPLE_RATE,
        AVChannelConfig.MONO,
        DEFAULT_SAMPLE_FORMAT,
        96000
    )

    //本地录制
    private val recordClient by lazy {
        HapiAVPackerClient()
    }

    //屏幕采集轨道
    private val screenCaptureTrack by lazy {
        HapiTrackFactory.createScreenCaptureTrack(this)
    }

    //麦克轨道
    private val microphoneTrack by lazy {
        HapiTrackFactory.createMicrophoneTrack(
            this, DEFAULT_SAMPLE_RATE,
            DEFAULT_CHANNEL_LAYOUT,
            DEFAULT_SAMPLE_FORMAT
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_screen)
        lifecycle.addObserver(recordClient)
        screenCaptureTrack.playerView = findViewById<HapiCaptureTextureView>(R.id.preview)
        screenCaptureTrack.start(this, object : ScreenCaptureTrack.ScreenCaptureServiceCallBack {
            override fun onStart() {
                Log.d("screenCaptureTrack", "onStart")
            }

            override fun onError(code: Int, msg: String) {
                Log.d("screenCaptureTrack", "onError")
            }
        })
        microphoneTrack.startWithPermissionCheck(this, object : TrackPermissionCallback {
            override fun onPermissionResult(permissionCode: Int) {

            }
        })

        recordClient.attachTrack(screenCaptureTrack, videoEncodeParam)
        recordClient.attachTrack(microphoneTrack, audioEncodeParam)

        findViewById<Button>(R.id.btRecord)
            .setOnClickListener {

                if (it.isSelected) {
                    it.isSelected = false
                    (it as Button).text = "录制"
                    recordClient.stop()
                    MediaStoreUtils.insertVideoToMediaStore(this, recordUrl)
                } else {
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
                    //开始录制 传编码参数
                    recordClient.start(recordUrl)
                }
            }
    }

    fun getScreenWidth(): Int {
        //  int newW = (w/32)*32;
        return Resources.getSystem().displayMetrics.widthPixels / 32 * 32
    }

    fun getScreenHeight(): Int {
        //  int newH = (h/32)*32;
        return Resources.getSystem().displayMetrics.heightPixels / 32 * 32
    }
}