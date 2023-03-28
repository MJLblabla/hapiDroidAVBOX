package com.lcodecore.myapplication

import android.annotation.SuppressLint
import android.graphics.BitmapFactory
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.os.PersistableBundle
import android.util.Log
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.hapi.avcapture.*
import com.hapi.avcapturerender.HapiCaptureTextureView
import com.hapi.avencoder.AudioEncodeParam
import com.hapi.avencoder.VideoEncodeParam
import com.hapi.avpackerclinet.HapiAVPackerClient
import com.hapi.avparam.AVImgFmt
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File
import java.nio.ByteBuffer

class CustomTrackTestActivity : AppCompatActivity() {

    val customTrack = HapiTrackFactory.createCustomVideoTrack()
    var recordUrl = ""

    //麦克轨道
    private val microphoneTrack by lazy {
        HapiTrackFactory.createMicrophoneTrack(
            this, DEFAULT_SAMPLE_RATE,
            DEFAULT_CHANNEL_LAYOUT,
            DEFAULT_SAMPLE_FORMAT
        )
    }
    private val videoEncodeParam = VideoEncodeParam(
        480,
        640,
        1500 * 1000,
        30
    )
    private val audioEncodeParam = AudioEncodeParam(
        DEFAULT_SAMPLE_RATE,
        DEFAULT_CHANNEL_LAYOUT,
        DEFAULT_SAMPLE_FORMAT,
        96000
    )

    //本地录制
    private val recordClient by lazy {
        HapiAVPackerClient()
    }


    private fun start() {
        val bm = BitmapFactory.decodeResource(resources, R.mipmap.aaas)
        val nv21 = ImageUtils.bitmapToNv21(bm,bm.width,bm.height)

        //  Matrix.setRotateM(f16Matrix, 0, 360f - 90, 0f, 0f, 1.0f)
        lifecycleScope.launch(Dispatchers.IO) {
            while (true) {
                delay(100)
                customTrack.runOnGLThread {
                    val bb = ByteBuffer.allocateDirect(nv21.size)
                    bb.put(nv21)
                    bb.rewind()
                    customTrack.pushFrame(
                        VideoFrame(
                            bm.width,
                            bm.height,
                            270,
                            VideoFrame.ImgBuffer(bb, AVImgFmt.IMAGE_FORMAT_NV21)
                        )
                    )
                }
            }
        }
    }

    @SuppressLint("RestrictedApi")
    fun stop() {
        customTrack.stop()
        recordClient.stop()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        Log.d("mjl", " MainActivity3 on onCreate========")
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main3)


        recordClient.attachTrack(customTrack,videoEncodeParam)
        recordClient.attachTrack(microphoneTrack,audioEncodeParam)


        customTrack.playerView = findViewById<HapiCaptureTextureView>(R.id.preview)
        customTrack.start()
        start()
        microphoneTrack.start()

        findViewById<Button>(R.id.start).setOnClickListener {
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

    override fun onResume() {
        Log.d("mjl", " MainActivity3 on onResume========")
        super.onResume()
    }

    override fun onPause() {
        Log.d("mjl", " MainActivity3 on onPause========")
        super.onPause()

    }

    override fun onSaveInstanceState(outState: Bundle, outPersistentState: PersistableBundle) {
        super.onSaveInstanceState(outState, outPersistentState)
        Log.d("mjl", " MainActivity3 on onSaveInstanceState========")
    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        super.onRestoreInstanceState(savedInstanceState)
        Log.d("mjl", " MainActivity3 on onRestoreInstanceState========")
    }

    override fun onDestroy() {
        Log.d("mjl", " MainActivity3 on onDestroy========")
        super.onDestroy()
        stop()
        customTrack.release()
    }




}