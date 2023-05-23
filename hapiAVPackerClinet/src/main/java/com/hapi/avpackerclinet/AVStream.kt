package com.hapi.avpackerclinet

import android.media.MediaCodec
import android.media.MediaFormat
import com.hapi.ioutput.ConnectedStatus
import com.hapi.ioutput.MuxerCallBack
import com.hapi.ioutput.StreamObserver
import com.hapi.avcapture.*
import com.hapi.avencoder.*
import com.hapi.avparam.AVImgFmt
import com.hapi.ioutput.OutputStreamerEvent
import java.nio.ByteBuffer

abstract class AVStream<T>(open val track: Track<T>) {
    var streamObserver: StreamObserver? = null
    protected var isMute = false
    var encoderStatusCallBack: EncoderStatusCallBack? = null
    val trackStreamID by lazy {
        track.javaClass.name + System.currentTimeMillis()
    }
    var encoderContext: EncoderContext? = null
        protected set
    protected var mEncodeStatus: EncoderStatus = EncoderStatus.STATE_UNKNOWN
    protected val muxerStatusCallBack = object : MuxerCallBack {
        override fun onMuxerConnectedStatus(status: ConnectedStatus) {
            if (status == ConnectedStatus.CONNECTED_STATUS_CONNECTED) {
                encoderContext?.start()
            }
            if (status == ConnectedStatus.CONNECTED_STATUS_DISCONNECTED) {
                encoderContext?.stop()
            }
        }

        override fun onOutputStreamerEvent(event: OutputStreamerEvent, msg: String) {
        }
    }
    protected val mFrameCall = object : FrameCall<T> {

        var silenceVideoFrame: ByteBuffer? = null
        fun createSilenceVideoFrame(videoFrame: VideoFrame): VideoEncodeFrame {
            val size = videoFrame.width * videoFrame.height * 3 / 2
            if (silenceVideoFrame?.capacity() != size) {
                silenceVideoFrame = ByteBuffer.allocateDirect(size)
                silenceVideoFrame!!.put(ByteArray(size) {
                    if (it < videoFrame.width * videoFrame.height) {
                        0.toByte()
                    } else {
                        128.toByte()
                    }
                })
            }
            silenceVideoFrame!!.clear()
            silenceVideoFrame!!.limit(size)
            silenceVideoFrame!!.position(0)

            return VideoEncodeFrame(
                width = videoFrame.width,
                height = videoFrame.height,
                rotationDegrees = videoFrame.rotationDegrees,
                buffer = silenceVideoFrame!!,
                AVImgFmt = AVImgFmt.IMAGE_FORMAT_I420
            ).apply {
                timestamp = videoFrame.timestamp
            }
        }

        override fun onFrame(frame: T) {
            if (encoderContext == null) {
                return
            }
            if (frame is VideoFrame) {
                val videoFrame = if (isMute) {
                    createSilenceVideoFrame(frame)
                } else {
                    (frame.toVideoEncodeFrame())
                }
                (encoderContext as VideoEncoderContext).onFrame(videoFrame)
            }
            if (frame is AudioFrame) {
                val audioFrame = frame.toAudioEncodeFrame(isMute)
                (encoderContext as AudioEncoderContext).onFrame(audioFrame)
            }
        }
    }

    protected val mEncoderCallBack = object : EncoderCallBack {
        override fun onOutputBufferAvailable(
            outputBuffer: ByteBuffer,
            outputFormat: MediaFormat,
            info: MediaCodec.BufferInfo,
            dts: Long
        ) {
            streamObserver?.writePacket(trackStreamID, outputBuffer, outputFormat, info,dts)
        }

        override fun onEncoderStatusChange(encodeStatus: EncoderStatus) {
            mEncodeStatus = encodeStatus
            encoderStatusCallBack?.onEncoderStatusChange(encodeStatus)
        }

        override fun onOutputFormatChanged(mediaFormat: MediaFormat) {
            super.onOutputFormatChanged(mediaFormat)
        }
    }

    abstract fun create()

    open fun start() {
        encoderContext?.start()
    }

    open fun stop() {
        encoderContext?.stop()
    }

    open fun release() {
        encoderStatusCallBack = null
        encoderContext?.release()
        streamObserver?.removeStream(trackStreamID)
    }

    open fun mute(mute: Boolean) {
        isMute = mute
    }

}

class VideoAVStream(
    override val track: IVideoTrack,
    private val encodeParam: VideoEncodeParam
) : AVStream<VideoFrame>(track) {

    override fun create() {
        encoderContext = AVEncoderFactory.configureVideoEncoder(encodeParam).apply {
            setEncoderCallBack(mEncoderCallBack)
        }
        streamObserver?.addStream(trackStreamID, encoderContext!!.configureMediaFormat!!)
    }

    override fun start() {
        super.start()
        track.innerFrameCalls.add(mFrameCall)
    }

    override fun stop() {
        super.stop()
        track.innerFrameCalls.remove(mFrameCall)
    }
}

class AudioAVStream(
    override val track: IAudioTrack,
    private val encodeParam: AudioEncodeParam
) : AVStream<AudioFrame>(track) {

    override fun create() {
        encoderContext = AVEncoderFactory.configureAudioEncoder(encodeParam).apply {
            setEncoderCallBack(mEncoderCallBack)
        }
        streamObserver?.addStream(trackStreamID, encoderContext!!.configureMediaFormat!!)
    }

    override fun start() {
        super.start()
        track.innerFrameCalls.add(mFrameCall)
    }

    override fun stop() {
        super.stop()
        track.innerFrameCalls.remove(mFrameCall)
    }
}