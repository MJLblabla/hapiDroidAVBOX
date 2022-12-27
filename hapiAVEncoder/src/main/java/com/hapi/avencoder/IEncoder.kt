package com.hapi.avencoder

import android.media.MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420PackedSemiPlanar
import android.media.MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420SemiPlanar
import android.media.MediaFormat
import com.hapi.avparam.AVImgFmt
import java.nio.ByteBuffer

interface IEncoder {
    var encoderCallBack: EncoderCallBack?
    var getContextStatus: () -> EncoderStatus
    fun start()
    fun stop()
    fun pause()
    fun resume()
    fun release()
    fun updateBitRate(bitRate: Int)
    fun allocateAVFrameBuffer(size: Int): ByteBuffer
}

interface IVideoEncoder : IEncoder {
    fun configure(encodeParam: VideoEncodeParam): MediaFormat?
    fun onFrame(frame: VideoEncodeFrame)
}

interface IAudioEncoder : IEncoder {
    fun configure(encodeParam: AudioEncodeParam): MediaFormat?
    fun onFrame(frame: AudioEncodeFrame)
}

open class EncoderContext internal constructor(private val encoder: IEncoder) {
    var configureMediaFormat: MediaFormat? = null
        protected set
    var encodeStatus = EncoderStatus.STATE_UNKNOWN
        set(value) {
            field = value
            encoder.encoderCallBack?.onEncoderStatusChange(encodeStatus)
        }

    protected val mAVResampleContext = AVResampleContext()

    init {
        encoder.getContextStatus = {
            this.encodeStatus
        }
        mAVResampleContext.init()
    }

    fun setEncoderCallBack(encoderCallBack: EncoderCallBack) {
        encoder.encoderCallBack = encoderCallBack
    }

    open fun start() {
        if (encodeStatus != EncoderStatus.STATE_PREPARE) {
            throw IllegalStateException(" encodeStatus != EncoderStatus.STATE_PREPARE ")
        }
        encodeStatus = EncoderStatus.STATE_ENCODING
        encoder.start()
    }

    fun stop() {
        if (encodeStatus == EncoderStatus.STATE_STOP || encodeStatus == EncoderStatus.STATE_RELEASE || encodeStatus == EncoderStatus.STATE_PREPARE) {
            return
        }
        encodeStatus = EncoderStatus.STATE_STOP
        encoder.stop()
        encodeStatus = EncoderStatus.STATE_PREPARE
    }

    open fun pause(): Boolean {
        if (encodeStatus != EncoderStatus.STATE_ENCODING) {
            return false
        }
        encodeStatus = EncoderStatus.STATE_PAUSE
        encoder.pause()
        return true
    }

    fun resume(): Boolean {
        if (encodeStatus != EncoderStatus.STATE_PAUSE) {
            return false
        }
        encodeStatus = EncoderStatus.STATE_ENCODING
        encoder.resume()
        return true
    }

    fun updateBitRate(bitRate: Int) {
        encoder.updateBitRate(bitRate)
    }

    fun release() {
        encoder.release()
        encodeStatus = EncoderStatus.STATE_RELEASE
        mAVResampleContext.unit()
    }
}

class VideoEncoderContext internal constructor(val videoEncoder: IVideoEncoder) :
    EncoderContext(videoEncoder) {
    private var fpsFilter = FPSFilter()
    private var mParam: VideoEncodeParam? = null
    internal fun configure(encodeParam: VideoEncodeParam): MediaFormat? {
        fpsFilter.targetFPS = encodeParam.fps
        this.configureMediaFormat = videoEncoder.configure(encodeParam)
        if (configureMediaFormat == null) {
            return null
        }
        mParam = encodeParam
        mAVResampleContext.configFormat =
            when (configureMediaFormat!!.getInteger(MediaFormat.KEY_COLOR_FORMAT)) {
                COLOR_FormatYUV420SemiPlanar -> AVImgFmt.IMAGE_FORMAT_NV12
                COLOR_FormatYUV420PackedSemiPlanar -> AVImgFmt.IMAGE_FORMAT_NV21
                else -> AVImgFmt.IMAGE_FORMAT_I420
            }
        mAVResampleContext.setParam(encodeParam)
        encodeStatus = EncoderStatus.STATE_PREPARE
        return configureMediaFormat
    }

    override fun start() {
        super.start()
        fpsFilter.start()
    }

    override fun pause(): Boolean {
        val doPause = super.pause()
        if (doPause) {
            fpsFilter.reset()
        }
        return doPause
    }

    fun onFrame(frame: VideoEncodeFrame) {
        if (encodeStatus != EncoderStatus.STATE_ENCODING) {
            return
        }
        fpsFilter.filter(frame) { filterFrame ->
            val outputBuffer = videoEncoder.allocateAVFrameBuffer(
                mParam!!.frameHeight * mParam!!.frameWidth * 3 / 2
            )
            outputBuffer.clear()
            if (
                filterFrame.width != mParam!!.frameWidth ||
                filterFrame.height != mParam!!.frameHeight ||
                filterFrame.rotationDegrees != 0 ||
                filterFrame.AVImgFmt.fmt != mAVResampleContext.configFormat.fmt
            ) {
                mAVResampleContext.onVideoData(filterFrame, outputBuffer)
            } else {
                outputBuffer.put(filterFrame.buffer)
            }
            outputBuffer.limit(outputBuffer.capacity())
            outputBuffer.position(0)
            videoEncoder.onFrame(VideoEncodeFrame(
                mParam!!.frameWidth,
                mParam!!.frameHeight,
                0,
                outputBuffer,
                mAVResampleContext.configFormat
            ).apply {
                pts = filterFrame.pts
            })
        }
    }
}

class AudioEncoderContext internal constructor(private val audioEncoder: IAudioEncoder) :
    EncoderContext(audioEncoder) {
    private var mParam: AudioEncodeParam? = null
    private var mSamplesCount = 0;
    private var baseTime = 0L;

    internal fun configure(encodeParam: AudioEncodeParam): MediaFormat? {
        this.configureMediaFormat = audioEncoder.configure(encodeParam)
        if (configureMediaFormat == null) {
            return null
        }
        mParam = encodeParam
        mAVResampleContext.setParam(encodeParam)
        encodeStatus = EncoderStatus.STATE_PREPARE
        //一秒x次采样 * 一次采样deep bit * 通道数 = 一秒数据大小
        baseTime =
            (encodeParam.sampleRateInHz * encodeParam.audioFormat.deep * encodeParam.channelConfig.count).toLong()
        return this.configureMediaFormat
    }

    override fun start() {
        super.start()
        mSamplesCount = 0
    }

    fun onFrame(frame: AudioEncodeFrame) {
        if (encodeStatus != EncoderStatus.STATE_ENCODING) {
            return
        }
        val param = mParam!!
        val outputBuffer = if (
            frame.audioFormat != param.audioFormat ||
            frame.AVChannelConfig != param.channelConfig ||
            frame.sampleRateInHz != param.sampleRateInHz
        ) {
            if (mAVResampleContext.audioFrameOutPutSize <= 0L) {
                mAVResampleContext.getResizeAudioFrameSize(frame)
            }
            val outputBuffer =
                audioEncoder.allocateAVFrameBuffer(mAVResampleContext.audioFrameOutPutSize)
            outputBuffer.clear()
            mAVResampleContext.onAudioData(frame, outputBuffer)
            outputBuffer
        } else {
            val audioFrameOutPutSize = frame.buffer.limit()
            val outputBuffer = audioEncoder.allocateAVFrameBuffer(
                audioFrameOutPutSize
            )
            outputBuffer.clear()
            outputBuffer.put(frame.buffer)
            outputBuffer
        }
        outputBuffer.limit(outputBuffer.capacity())
        outputBuffer.position(0)
        val audioPts = (mSamplesCount * 8 * 1000000.0 / baseTime)
        mSamplesCount += outputBuffer.capacity()

        audioEncoder.onFrame(
            AudioEncodeFrame(
                param.sampleRateInHz,
                param.channelConfig,
                param.audioFormat,
                outputBuffer
            ).apply {
                this.pts = audioPts.toLong()
            }
        )
    }
}


