package com.hapi.avcapture

import com.hapi.eglbase.OffscreenSurfaceHelper
import com.hapi.avparam.*
import java.util.*

const val DEFAULT_SAMPLE_RATE = 44100
val DEFAULT_CHANNEL_LAYOUT = AVChannelConfig.STEREO
val DEFAULT_SAMPLE_FORMAT = AVSampleFormat.ENCODING_PCM_16BIT

interface FrameCall<T> {
    fun onFrame(frame: T)
}

open class Track<T> {
    //帧回调
    var frameCall: FrameCall<T>? = null
    var isStart = false
        protected set
    var trackID = ""
        private set

    init {
        trackID = this.javaClass.simpleName + System.currentTimeMillis()
    }
}

abstract class IVideoTrack internal constructor() : Track<VideoFrame>() {

    var isRelease = false
        private set
    val innerFrameCalls = LinkedList<FrameCall<VideoFrame>>()
    private val textureFramePixelGetter = TextureFramePixelGetter()
    protected val offscreenSurfaceHelper: OffscreenSurfaceHelper = OffscreenSurfaceHelper()

    /**
     * 设置预览
     */
    var playerView: ICapturePreview? = null
        set(value) {
            field = value
            field?.init(offscreenSurfaceHelper.getEGLContext())
        }

    init {
        offscreenSurfaceHelper.attachGLContext {
            initOnGLThread()
        }
    }

    /**
     * Run on gl thread
     *
     * @param T
     * @param wait
     * @param run
     * @receiver
     * @return
     */
    fun <T> runOnGLThread(wait: Boolean = false, run: () -> T?): T? {
        return offscreenSurfaceHelper.runOnGLThread(wait, run)
    }

    protected fun innerPushFrame(frame: VideoFrame) {
        frameCall?.onFrame(frame)
        textureFramePixelGetter.texture2Pixel(frame, playerView, innerFrameCalls.isNotEmpty())
        innerFrameCalls.forEach {
            //多线程添加消费者可能不及时处理
            if (frame.buffer is VideoFrame.TextureBuffer) {
                return
            }
            it.onFrame(frame)
        }
    }

    protected open fun initOnGLThread() {}
    protected open fun releaseOnGLThread() {}
    protected open fun releaseOnMainThread() {}

    /**
     * Release
     *
     */
    fun release() {
        if (isRelease) {
            return
        }
        isRelease = true
        releaseOnMainThread()
        offscreenSurfaceHelper.release() {
            textureFramePixelGetter.release()
            releaseOnGLThread()
        }
    }
}

abstract class IAudioTrack internal constructor() : Track<AudioFrame>() {

    var audioRender: AudioRender? = null
    val innerFrameCalls = LinkedList<FrameCall<AudioFrame>>()
    internal fun innerPushFrame(frame: AudioFrame) {
        if (!isStart) {
            return
        }
        frameCall?.onFrame(frame)
        audioRender?.onAudioFrame(frame)
        innerFrameCalls.forEach {
            it.onFrame(frame)
        }
    }
}

class CustomAudioTrack internal constructor() : IAudioTrack() {

    fun pushFrame(frame: AudioFrame) {
        innerPushFrame(frame)
    }

    fun start() {
        isStart = true
    }

    fun stop() {
        isStart = false
    }
}

class CustomVideoTrack internal constructor() : IVideoTrack() {

    fun pushFrame(frame: VideoFrame) {
        innerPushFrame(frame)
    }

    fun start() {
        isStart = true
    }

    fun stop() {
        isStart = false
    }
}





