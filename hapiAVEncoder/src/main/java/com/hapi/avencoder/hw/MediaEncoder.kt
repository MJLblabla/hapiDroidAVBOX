package com.hapi.avencoder.hw

import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaCodecInfo.CodecCapabilities.*
import android.media.MediaFormat
import android.media.MediaFormat.*
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.HandlerThread
import android.util.Size
import com.hapi.avencoder.*
import com.hapi.avparam.*
import java.nio.ByteBuffer
import java.util.concurrent.TimeUnit
import kotlin.math.abs

abstract class MediaEncoder<T> : IEncoder {
    protected open val tag = ""
    override lateinit var getContextStatus: (() -> EncoderStatus)
    override var encoderCallBack: EncoderCallBack? = null
    private val lock = Object()
    private var lockToStop = false
    protected var mediaCodec: MediaCodec? = null
    private var lastConfigMediaFormat: MediaFormat? = null
    protected val mAVFrameQueue = AVFrameQueue<T>()
    private val mEncoderHandler: android.os.Handler by lazy {
        val handlerThread = HandlerThread("MediaEncoder")
        handlerThread.start()
        Handler(handlerThread.looper)
    }
    private val mMediaCodecCallBack =
        object : MediaCodec.Callback() {
            override fun onInputBufferAvailable(codec: MediaCodec, id: Int) {
                // AVLog.d("MediaEncoder${tag}", "onInputBufferAvailable: MediaCodec")
                if (getContextStatus() == EncoderStatus.STATE_STOP ||
                    getContextStatus() == EncoderStatus.STATE_RELEASE
                ) {
                    return
                }
                var frame: T? = null// = mFrameQueue.poll(200, TimeUnit.MILLISECONDS)
                while (frame == null && (getContextStatus() != EncoderStatus.STATE_STOP || getContextStatus() != EncoderStatus.STATE_RELEASE || getContextStatus() != EncoderStatus.STATE_PAUSE)) {
                    frame = mAVFrameQueue.popFrame(200, TimeUnit.MILLISECONDS)
                }
                synchronized(lock) {
                    if (lockToStop) {
                        return
                    }
                    try {
                        val inputBuffer = codec.getInputBuffer(id)
                        inputBuffer!!.clear()
                        consumeFrame(frame, codec, id)
                    } catch (e: IllegalStateException) {
                        AVLog.d("mediaCodec", " IllegalStateException  input id $id")
                        e.printStackTrace()
                    }
                }
            }

            override fun onOutputBufferAvailable(
                codec: MediaCodec,
                index: Int,
                info: MediaCodec.BufferInfo
            ) {
                //   AVLog.d("MediaEncoder${tag}", "onOutputBufferAvailable: MediaCodec")
                synchronized(lock) {
                    if (lockToStop) {
                        // codec.releaseOutputBuffer(index, false)
                        return
                    }
                    encoderCallBack?.onOutputBufferAvailable(
                        codec.getOutputBuffer(index)!!,
                        codec.outputFormat,
                        info
                    )
                    codec.releaseOutputBuffer(index, false)
                }
            }

            override fun onError(p0: MediaCodec, p1: MediaCodec.CodecException) {
                //  AVLog.d("MediaEncoder${tag}", "onError(p0: MediaCodec")
                p1.printStackTrace()
            }

            override fun onOutputFormatChanged(codec: MediaCodec, format: MediaFormat) {
                //  AVLog.d("MediaEncoder${tag}", "onOutputFormatChanged: MediaCodec")
                encoderCallBack?.onOutputFormatChanged(format)
            }
        }

    protected abstract fun consumeFrame(frame: T?, codec: MediaCodec, id: Int)

    override fun updateBitRate(bitRate: Int) {
        val bundle = Bundle()
        bundle.putInt(MediaCodec.PARAMETER_KEY_VIDEO_BITRATE, bitRate)
        mediaCodec?.setParameters(bundle)
    }

    protected fun setMediaCall(configMediaFormat: MediaFormat) {
        lastConfigMediaFormat = configMediaFormat
    }

    override fun start() {
        synchronized(lock) {
            if (mediaCodec == null) {
                mediaCodec =
                    MediaCodec.createEncoderByType(lastConfigMediaFormat!!.getString(KEY_MIME)!!)
                mediaCodec!!.configure(
                    lastConfigMediaFormat,
                    null,
                    null,
                    MediaCodec.CONFIGURE_FLAG_ENCODE
                )
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                mediaCodec?.setCallback(mMediaCodecCallBack, mEncoderHandler)
            } else {
                mediaCodec?.setCallback(mMediaCodecCallBack)
            }
            lockToStop = false
            mediaCodec!!.start()
        }
    }

    override fun stop() {
        synchronized(lock) {
            lockToStop = true
            mediaCodec!!.flush()
            mediaCodec!!.stop()
            mAVFrameQueue.clear()
            mediaCodec?.reset()
            mediaCodec?.release()
            mediaCodec = null
        }
    }

    override fun release() {
        synchronized(lock) {
            mediaCodec?.release()
            mEncoderHandler.looper.quit()
        }
    }

    override fun resume() {

    }

    override fun pause() {

    }

    protected fun allocateAVFrameBuffer(size: Int): ByteBuffer {
        return mAVFrameQueue.allocateAVFrameBuffer(size)
    }
}

class VideoMediaEncoder : MediaEncoder<VideoEncodeFrame>(), IVideoEncoder {
    override val tag = "video"
    private var outFrame: ByteArray? = null
    private var baseTime = 0.0;
    private var fpsCount = 0;

    override fun configure(encodeParam: VideoEncodeParam): MediaFormat? {
        var tryWidth = encodeParam.frameWidth
        var tryHeight = encodeParam.frameHeight

        mediaCodec = MediaCodec.createEncoderByType(MIMETYPE_VIDEO_AVC)
        val videoCapabilities =
            mediaCodec?.codecInfo?.getCapabilitiesForType(MIMETYPE_VIDEO_AVC)?.videoCapabilities!!
        var isSupported = videoCapabilities.isSizeSupported(tryWidth, tryHeight)
        val hAlignment = 32//videoCapabilities.heightAlignment
        val wAlignment = 32//videoCapabilities.widthAlignment
        val heightRange = videoCapabilities.supportedHeights
        val widthRange = videoCapabilities.supportedWidths

        AVLog.d(
            "MediaEncoder", "目标宽高 $tryWidth  $tryHeight"
        )
        AVLog.d(
            "MediaEncoder",
            " 原始 isSupported  $isSupported  hAlignment $hAlignment  wAlignment $wAlignment  widthRange ${widthRange.lower} ${widthRange.upper} heightRange ${heightRange.lower} ${heightRange.upper} "
        )
        var mediaFormat = MediaFormat.createVideoFormat(MIMETYPE_VIDEO_AVC, tryWidth, tryHeight)
        mediaFormat.setInteger(MediaFormat.KEY_BIT_RATE, encodeParam.videoBitRate)
        mediaFormat.setInteger(KEY_MIN_BIT_RATE, encodeParam.minVideoBitRate)
        mediaFormat.setInteger(KEY_MAX_BIT_RATE, encodeParam.maxVideoBitRate)
        mediaFormat.setInteger(
            MediaFormat.KEY_FRAME_RATE,
            encodeParam.fps
        )
        mediaFormat.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 1) //关键帧间隔时间 单位s

        isSupported = true
        if (!tryConfigureI420(mediaFormat)) {
            if (!tryConfigureNv12(mediaFormat)) {
                if (!tryConfigureYUV420Flexible(mediaFormat)) {
                    isSupported = false
                }
            }
        }
        AVLog.d(
            "MediaEncoder",
            " isSupported  $isSupported  尝试原始视频配置 $tryWidth $tryHeight "
        )

        if (!isSupported) {
            tryWidth = (tryWidth / wAlignment) * wAlignment
            tryHeight = tryHeight / hAlignment * hAlignment
            isSupported = videoCapabilities.isSizeSupported(tryWidth, tryHeight)
            AVLog.d(
                "MediaEncoder",
                " isSupported  $isSupported  调节对齐后是否支持 $tryWidth $tryHeight "
            )
            if (!isSupported) {
                val targetRatio = (tryHeight / tryWidth.toDouble()).format2()
                val minRatio = heightRange.lower / widthRange.upper
                val maxRatio = heightRange.upper / widthRange.lower
                AVLog.d(
                    "MediaEncoder",
                    "targetRatio $targetRatio minRatio $minRatio $maxRatio"
                )
                val sizes = ArrayList<Size>()
                var minRatioDiff = Double.MAX_VALUE
                for (h in heightRange.lower..heightRange.upper) {
                    if (h % hAlignment != 0) {
                        continue
                    }
                    for (w in widthRange.lower..widthRange.upper) {
                        if (w % wAlignment != 0) {
                            continue
                        }
                        val ratio = (h / w.toDouble()).format2()
                        val diff = abs(ratio - targetRatio)
                        if (diff < minRatioDiff) {
                            sizes.clear()
                            sizes.add(Size(w, h))
                            minRatioDiff = diff
                        }
                        if (diff == minRatioDiff) {
                            sizes.add(Size(w, h))
                        }
                    }
                }
                var size = Size(0, 0)
                var minSizeDiff = Int.MAX_VALUE
                sizes.forEach {
                    val diff = abs(it.width + it.height - (tryWidth + tryHeight))
                    if (diff < minSizeDiff) {
                        size = it
                        minSizeDiff = diff
                    }
                }
                tryWidth = size.width
                tryHeight = size.height
                isSupported = videoCapabilities.isSizeSupported(tryWidth, tryHeight)
                AVLog.d(
                    "MediaEncoder",
                    " isSupported  $isSupported  双层循 $tryWidth $tryHeight "
                )
            }
            mediaFormat = MediaFormat.createVideoFormat(MIMETYPE_VIDEO_AVC, tryWidth, tryHeight)
            mediaFormat.setInteger(MediaFormat.KEY_FRAME_RATE, encodeParam.fps)
            mediaFormat.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 1) //关键帧间隔时间 单位s
            mediaFormat.setInteger(MediaFormat.KEY_BIT_RATE, encodeParam.videoBitRate)
            mediaFormat.setInteger(KEY_MIN_BIT_RATE, encodeParam.minVideoBitRate)
            mediaFormat.setInteger(KEY_MAX_BIT_RATE, encodeParam.maxVideoBitRate)
            isSupported = true
            if (!tryConfigureI420(mediaFormat)) {
                if (!tryConfigureNv12(mediaFormat)) {
                    if (!tryConfigureYUV420Flexible(mediaFormat)) {
                        isSupported = false
                    }
                }
            }
        }

        outFrame = ByteArray(tryWidth * tryHeight * 3 / 2)
        baseTime = 1.0 / encodeParam.fps
        fpsCount = 0
        return if (isSupported) {
            encodeParam.frameWidth = tryWidth
            encodeParam.frameHeight = tryHeight
            setMediaCall(mediaFormat)
            mediaFormat
        } else {
            null
        }
    }

    override fun onFrame(frame: VideoEncodeFrame) {
        if (mediaCodec == null || getContextStatus() != EncoderStatus.STATE_ENCODING) {
            return
        }
        if (mAVFrameQueue.frameSize() > mAVFrameQueue.maxFreeBufferSize) {
            return
        }
        mAVFrameQueue.pushFrame(VideoEncodeFrame(
            frame.width,
            frame.height,
            0,
            allocateAVFrameBuffer(frame.buffer.capacity()).apply {
                clear()
                put(frame.buffer)
                flip()
            },
            frame.AVImgFmt
        ).apply {
            pts = frame.pts
        })
    }

    private fun Double.format2(): Double {
        val l = (this * 100).toLong()
        val d = l / 100.0
        return d
    }

    private fun tryConfigureYUV420Flexible(mediaFormat: MediaFormat): Boolean {
        var isConfigure = true
        mediaFormat.setInteger(
            MediaFormat.KEY_COLOR_FORMAT,
            COLOR_FormatYUV420Flexible,
        )
        try {
            mediaCodec!!.configure(mediaFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
        } catch (e: IllegalStateException) {
            e.printStackTrace()
            isConfigure = false
        }
        AVLog.d("MediaEncoder", "tryConfigureYUV420Flexible" + isConfigure)
        return isConfigure
    }

    private fun tryConfigureI420(mediaFormat: MediaFormat): Boolean {
        var isConfigure = true
        mediaFormat.setInteger(
            MediaFormat.KEY_COLOR_FORMAT,
            COLOR_FormatYUV420Planar,
        )
        try {
            mediaCodec!!.configure(mediaFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
        } catch (e: IllegalStateException) {
            e.printStackTrace()
            isConfigure = false
        }
        AVLog.d("MediaEncoder", "tryConfigureI420" + isConfigure)
        return isConfigure
    }

    private fun tryConfigureNv12(mediaFormat: MediaFormat): Boolean {
        var isConfigure = true
        mediaFormat.setInteger(
            MediaFormat.KEY_COLOR_FORMAT,
            COLOR_FormatYUV420SemiPlanar,
        )
        try {
            mediaCodec!!.configure(mediaFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
        } catch (e: IllegalStateException) {
            e.printStackTrace()
            isConfigure = false
        }
        AVLog.d("MediaEncoder", "tryConfigureNV21" + isConfigure)
        return isConfigure
    }

    override fun consumeFrame(frame: VideoEncodeFrame?, codec: MediaCodec, id: Int) {
        var len = 0
        val pts = frame?.pts ?: 0L
        if (frame != null) {
            val inputBuffer = codec.getInputBuffer(id)
            len = (frame.buffer).limit()
            inputBuffer?.put(frame.buffer)
            fpsCount++
        }
        codec.queueInputBuffer(id, 0, len, pts.toLong(), 0)

        frame?.buffer?.let {
            mAVFrameQueue.recycleByteBuffer(it)
        }
    }

    override fun start() {
        super.start()
        fpsCount = 0
    }
}

class AudioMediaEncoder : MediaEncoder<AudioEncodeFrame>(), IAudioEncoder {

    override val tag = "audio"

    override fun configure(encodeParam: AudioEncodeParam): MediaFormat? {
        val mediaFormat = MediaFormat.createAudioFormat(
            MediaFormat.MIMETYPE_AUDIO_AAC,
            encodeParam.sampleRateInHz,
            encodeParam.channelConfig.count
        )
        mediaFormat.setInteger(MediaFormat.KEY_BIT_RATE, encodeParam.audioBitrate)
        mediaFormat.setInteger(KEY_MIN_BIT_RATE, encodeParam.minAudioBitRate)
        mediaFormat.setInteger(KEY_MAX_BIT_RATE, encodeParam.maxAudioBitRate)

        mediaFormat.setInteger(KEY_AUDIO_FORMAT, encodeParam.audioFormat.androidFMT)
        mediaFormat.setInteger(KEY_AUDIO_CHANNEL_CONFIG, encodeParam.channelConfig.androidChannel)

        mediaFormat.setInteger(
            MediaFormat.KEY_AAC_PROFILE,
            MediaCodecInfo.CodecProfileLevel.AACObjectLC
        )

        mediaCodec = MediaCodec.createEncoderByType(MediaFormat.MIMETYPE_AUDIO_AAC)
        mediaCodec?.configure(mediaFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)

        setMediaCall(mediaFormat)
        return mediaFormat
    }

    override fun consumeFrame(frame: AudioEncodeFrame?, codec: MediaCodec, id: Int) {
        var len = 0
        if (frame != null) {
            val inputBuffer = codec.getInputBuffer(id)
            inputBuffer?.put(frame.buffer)
            len = frame.buffer.limit()
        }
        codec.queueInputBuffer(id, 0, len, frame?.pts ?: 0, 0)
        frame?.buffer?.let {
            mAVFrameQueue.recycleByteBuffer(it)
        }
    }

    override fun onFrame(frame: AudioEncodeFrame) {
        if (mediaCodec == null || getContextStatus() != EncoderStatus.STATE_ENCODING) {
            return
        }
        if (mAVFrameQueue.frameSize() > mAVFrameQueue.maxFreeBufferSize) {
            return
        }
        mAVFrameQueue.pushFrame(AudioEncodeFrame(
            frame.sampleRateInHz,
            frame.channelConfig,
            frame.audioFormat,
            allocateAVFrameBuffer(frame.buffer.capacity()).apply {
                clear()
                put(frame.buffer)
                flip()
            }
        ).apply {
            this.pts = frame.pts
        })
    }
}