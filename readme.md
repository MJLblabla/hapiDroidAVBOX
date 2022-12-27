### 项目描述

![alt 属性文本](https://github.com/MJLblabla/hapiDroidAVBOX/blob/master/hapi.png?raw=true)

### 依赖

```
//
implementation 'com.github.MJLblabla.hapiDroidAVBOX:baseAVUitl:v1.0'
//采集
implementation 'com.github.MJLblabla.hapiDroidAVBOX:hapiAVCapture:v1.0'
//编码
implementation 'com.github.MJLblabla.hapiDroidAVBOX:hapiAVPackerClinet:v1.0'
implementation 'com.github.MJLblabla.hapiDroidAVBOX:hapiAVEncoder:v1.0'
implementation 'com.github.MJLblabla.hapiDroidAVBOX:hapiAVEncoder-ext-ffmpeg:v1.0'
//输出
implementation 'com.github.MJLblabla.hapiDroidAVBOX:hapiIMuxer:v1.0'
//录制
implementation 'com.github.MJLblabla.hapiDroidAVBOX:droidLocalMediaMuxer:v1.0'
//rtmp推流
implementation 'com.github.MJLblabla.hapiDroidAVBOX:hapiRTMPLiveStreamer:v1.0'
//srt推流
implementation 'com.github.MJLblabla.hapiDroidAVBOX:hapiSRTLiveSTreamer:v1.0' 
```

### 创建媒体轨道

```kotlin
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

   ```kotlin
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

```kotlin
//如果需要预览视频轨道
cameTrack.playerView = findViewById<HapiGLSurfacePreview>(R.id.preview)

//如果需要耳返
val mHapiSLAudioRender = HapiSLAudioRender()
microphoneTrack.mAudioRender = mHapiSLAudioRender

//开启相应的轨道
cameTrack.start()
microphoneTrack.start()
screenCaptureTrack.start(this)

//绑定轨道到推流器 或者 录制  一条轨道既可以推流也可以录制 比如 摄像头/麦克风 (720p)推流 +  摄像头/麦克风 (1080p)录制
//再比如  摄像头/麦克风 推流 + 屏幕采集+麦克风 录制
recordClient.attachTrack(cameTrack,encodeParams)
recordClient.attachTrack(microphoneTrack,encodeParams)

pusherClient.attachTrack(cameTrack,encodeParams)
pusherClient.attachTrack(microphoneTrack,encodeParams)
```

```kotlin
 //开始推流 
pusherClient.start(url)

//开始录制 
recordClient.start(recordUrl)
```

### 高级功能

### 美颜

### 图像ai