
### 项目描述
#### 采集模块
摄像头采集 --------
麦克风采集 --------
屏幕采集   -------
自定义轨道 --------

#### 


### 依赖

```
 //client -> 采集 编码模块 必须依赖 //也可以单独采集':hapiAVCapture'
    implementation project(':hapiAVPackerClinet')
    //可选模块
    implementation project(':hapiMp4Muxer') //mp4 录制
    implementation project(':hapiSrtMuxer') //srt 推流
    implementation project(':hapiRTMPMuxer')//rtmp推流
```

### 创建媒体轨道

```
   //摄像头轨道
    private val cameTrack by lazy {
        HapiTrackFactory.createCameraXTrack(this, this, 720, 1280)

    //麦克轨道
    private val microphoneTrack by lazy {
        HapiTrackFactory.createMicrophoneTrack(
            this, DEFAULT_SAMPLE_RATE,
            DEFAULT_CHANNEL_LAYOUT,
            DEFAULT_SAMPLE_FORMAT
        )
    }

    //屏幕采集轨道
    private val screenCaptureTrack by lazy {
        HapiTrackFactory.createScreenCaptureTrack(this)
    }

    //自定义轨道
    private val customAudioTrack by lazy {
        HapiTrackFactory.createCustomAudioTrack()
    }
   ```

### 创建推流 / 录制

   ```
    //本地录制
    private val recordClient by lazy {
        HapiAVPackerClient()
    }

    //推流
    private val pusherClient by lazy {
        HapiAVPackerClient().apply {
            mMuxerConnectCallBack = object : MuxerConnectCallBack {
                //推流链接状态回调
                override fun onConnectedStatus(status: ConnectedStatus, msg: String?) {}
            }
        }
    }
```

### 开始推流/录制

```
        //如果需要预览视频轨道
        cameTrack.playerView = findViewById<HapiGLSurfacePreview>(R.id.preview)

        //如果需要耳返
        val mHapiSLAudioRender = HapiSLAudioRender()
        microphoneTrack.mAudioRender = mHapiSLAudioRender

        //开启相应的轨道
        cameTrack.start()
        microphoneTrack.start()

        screenCaptureTrack.start(this, object : ScreenServicePlugin.ScreenCaptureServiceCallBack {
            override fun onStart() {
                Log.d("screenCaptureTrack", "onStart")
            }

            override fun onError(code: Int, msg: String?) {
                Log.d("screenCaptureTrack", "onError")
            }
        }

        )
        // recordClient.attachTrack(screenCaptureTrack)

        //绑定轨道到推流器 或者 录制  一条轨道既可以推流也可以录制 比如 摄像头/麦克风 (720p)推流 +  摄像头/麦克风 (1080p)录制
        //再比如  摄像头/麦克风 推流 + 屏幕采集+麦克风 录制
        recordClient.attachTrack(cameTrack)
        recordClient.attachTrack(microphoneTrack)

        pusherClient.attachTrack(cameTrack)
        pusherClient.attachTrack(microphoneTrack)
```

```
 //开始推流 传编码参数
 pusherClient.start(
     url, EncodeParam(
         videoEncodeParam,
         audioEncodeParam
     )
 )
             
 //开始录制 传编码参数
    recordClient.start(
        recordUrl, EncodeParam(
            videoEncodeParam,
            audioEncodeParam
        )
    )
```

### 高级功能

### 美颜

### 图像ai