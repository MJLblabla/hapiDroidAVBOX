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
    private var outputBuffer: ByteBuffer? = null
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
            val buffer = if (
                filterFrame.width != mParam!!.frameWidth ||
                filterFrame.height != mParam!!.frameHeight ||
                filterFrame.rotationDegrees != 0 ||
                filterFrame.AVImgFmt.fmt != mAVResampleContext.configFormat.fmt
            ) {
                if (outputBuffer == null) {
                    outputBuffer =
                        ByteBuffer.allocateDirect(mParam!!.frameHeight * mParam!!.frameWidth * 3 / 2)
                }
                outputBuffer!!.clear()
                mAVResampleContext.onVideoData(filterFrame, outputBuffer!!)
                outputBuffer!!.limit(outputBuffer!!.capacity())
                outputBuffer!!.position(0)
                outputBuffer
            } else {
                (filterFrame.buffer)
            }
            videoEncoder.onFrame(VideoEncodeFrame(
                mParam!!.frameWidth,
                mParam!!.frameHeight,
                0,
                buffer!!,
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
    private var outputBuffer: ByteBuffer? = null
    internal fun configure(encodeParam: AudioEncodeParam): MediaFormat? {
        this.configureMediaFormat = audioEncoder.configure(encodeParam)
        if (configureMediaFormat == null) {
            return null
        }
        mParam = encodeParam
        mAVResampleContext.setParam(encodeParam)
        encodeStatus = EncoderStatus.STATE_PREPARE
        //??????x????????? * ????????????deep bit * ????????? = ??????????????????
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
        val buffer = if (
            frame.audioFormat != param.audioFormat ||
            frame.channelConfig != param.channelConfig ||
            frame.sampleRateInHz != param.sampleRateInHz
        ) {
            if (mAVResampleContext.audioFrameOutPutSize <= 0L) {
                mAVResampleContext.getResizeAudioFrameSize(frame)
            }
            if (outputBuffer == null) {
                outputBuffer = ByteBuffer.allocateDirect(mAVResampleContext.audioFrameOutPutSize)
            }
            outputBuffer!!.clear()
            mAVResampleContext.onAudioData(frame, outputBuffer!!)
            outputBuffer!!.limit(outputBuffer!!.capacity())
            outputBuffer!!.position(0)
            outputBuffer
        } else {
            (frame.buffer)
        }

        val audioPts = (mSamplesCount * 8 * 1000000.0 / baseTime)
        mSamplesCount += buffer!!.capacity()

        audioEncoder.onFrame(
            AudioEncodeFrame(
                param.sampleRateInHz,
                param.channelConfig,
                param.audioFormat,
                buffer
            ).apply {
                this.pts = audioPts.toLong()
            }
        )
    }
}


