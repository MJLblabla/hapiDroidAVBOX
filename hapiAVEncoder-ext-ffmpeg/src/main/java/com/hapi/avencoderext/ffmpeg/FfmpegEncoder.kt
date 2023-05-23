package com.hapi.avencoderext.ffmpeg

import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaFormat
import com.hapi.avencoder.*
import com.hapi.avparam.*
import java.io.File
import java.nio.ByteBuffer
import java.util.regex.Pattern

abstract class FfmpegEncoder<T> : com.hapi.avencoder.IEncoder {
    override lateinit var getContextStatus: (() -> EncoderStatus)

    companion object {
        // Used to load the 'avencoder' library on application startup.
        init {
            System.loadLibrary("avencoderffmpeg")
        }

        fun getCpuNumCores(): Int {
            val cup = try {
                //Get directory containing CPU info
                val dir = File("/sys/devices/system/cpu/")
                //Filter to only list the devices we care about
                val files = dir.listFiles { pathname ->
                    Pattern.matches(
                        "cpu[0-9]+",
                        pathname.name
                    )
                }
                //Return the number of cores (virtual CPU devices)
                Math.max((files?.size ?: 2) / 2, 1)
            } catch (e: Exception) {
                //Default to return 1 core
                1
            }
            AVLog.d("FfmpegEncoder", " getCpuNumCores   $cup ")
            return cup
        }
    }

    protected var nativeContext = -1L
    override var encoderCallBack: EncoderCallBack? = null

    override fun start() {
        lastMediaFormatKey = ""
        nativeStart(nativeContext)
    }

    override fun stop() {
        nativeStop(nativeContext)
    }

    override fun pause() {
        nativePause(nativeContext)
    }

    override fun resume() {
        nativeResume(nativeContext)
    }

    override fun updateBitRate(bitRate: Int) {
        nativeUpdateBitRate(nativeContext, bitRate)
    }

    override fun release() {
        nativeRelease(nativeContext)
        nativeContext = -1
    }

    protected external fun nativeCreate(mediaType: Int): Long


    protected external fun nativeConfigureVideo(
        nativeContext: Long,
        frameWidth: Int,
        frameHeight: Int,
        videoBitRate: Int,
        videoMinBitRate: Int,
        videoMaxBitRate: Int,
        fps: Int,
        threadCount: Int,
    )

    protected external fun nativeConfigureAudio(
        nativeContext: Long,
        out_sample_fmt: Int,
        audioChannelCount: Int,
        audioSampleRate: Int,
        audioBitrate: Int,
        threadCount: Int,
    )

    private external fun nativeStart(nativeContext: Long)
    private external fun nativeStop(nativeContext: Long)
    private external fun nativePause(nativeContext: Long)
    private external fun nativeResume(nativeContext: Long)
    private external fun nativeUpdateBitRate(nativeContext: Long, bitRate: Int)
    private external fun nativeRelease(nativeContext: Long)
    protected external fun nativeOnAudioFrame(
        nativeContext: Long,
        data: ByteBuffer,
        sample_fmt: Int,
        audioChannelCount: Int,
        audioSampleRate: Int,
        pts: Long
    )

    protected external fun nativeOnVideoFrame(
        nativeContext: Long,
        format: Int,
        data: ByteBuffer,
        width: Int,
        height: Int,
        frameTimestamp: Long,
        pts: Long
    )

    private var outputFormat: MediaFormat = MediaFormat()
    protected var info: MediaCodec.BufferInfo = MediaCodec.BufferInfo()
    private var outputBuffer: ByteBuffer? = null

    private fun mediaFormatKey(): String {
        var key = ""
        try {
            key = outputFormat.getInteger(MediaFormat.KEY_HEIGHT)
                .toString() + outputFormat.getInteger(MediaFormat.KEY_WIDTH)
                .toString() + outputFormat.getString("KEY_MIME")
        } catch (e: NullPointerException) {
            e.printStackTrace()
        }
        return key
    }

    private var lastMediaFormatKey = ""

    private fun getOutputBuffer(size: Int): ByteBuffer {
        var allocateSize = size
        if (allocateSize < 1024) {
            allocateSize = 1024
        }
        if (outputBuffer == null || (outputBuffer?.capacity() ?: 0) < allocateSize) {
            outputBuffer = ByteBuffer.allocateDirect(allocateSize)
        }
        outputBuffer?.clear()
        return outputBuffer!!
    }

    public fun onOutputBufferAvailable(newSize: Int, newTimeUs: Long, newFlags: Int, dts: Long) {
        info.set(0, newSize, newTimeUs, newFlags)
        outputBuffer?.position(0)
        outputBuffer?.limit(newSize)
        val currentKey = mediaFormatKey()
        if (lastMediaFormatKey != currentKey) {
            encoderCallBack?.onOutputFormatChanged(outputFormat)
        }
        lastMediaFormatKey = currentKey
        encoderCallBack?.onOutputBufferAvailable(outputBuffer!!, outputFormat, info, dts)
    }

    private fun setInteger(name: String, value: Int) {
        outputFormat.setInteger(name, value)
    }

    private fun setLong(name: String, value: Long) {
        outputFormat.setLong(name, value)
    }

    private fun setFloat(name: String, value: Float) {
        outputFormat.setFloat(name, value)
    }

    private fun setString(name: String, value: String) {
        outputFormat.setString(name, value)
    }

    private fun setByteBuffer(name: String, bytes: ByteArray) {
        outputFormat.setByteBuffer(name, ByteBuffer.wrap(bytes))
    }
}

class FfmpegAudioEncoder : FfmpegEncoder<com.hapi.avencoder.AudioEncodeFrame>(), IAudioEncoder {
    init {
        nativeContext = nativeCreate(1)
    }

    override fun configure(encodeParam: AudioEncodeParam): MediaFormat? {
        nativeConfigureAudio(
            nativeContext,
            //audio
            (encodeParam).audioFormat.ffmpegFMT,
            (encodeParam).channelConfig.count,
            (encodeParam).sampleRateInHz,
            (encodeParam).audioBitrate,
            getCpuNumCores(),
        )
        val mediaFormat = MediaFormat.createAudioFormat(
            MediaFormat.MIMETYPE_AUDIO_AAC,
            (encodeParam).sampleRateInHz,
            (encodeParam).channelConfig.count
        )
        mediaFormat.setInteger(
            MediaFormat.KEY_BIT_RATE,
            (encodeParam).audioBitrate
        )
        mediaFormat.setInteger(
            MediaFormat.KEY_AAC_PROFILE,
            MediaCodecInfo.CodecProfileLevel.AACObjectLC
        )
        mediaFormat.setInteger(KEY_MIN_BIT_RATE, encodeParam.minAudioBitRate)
        mediaFormat.setInteger(KEY_MAX_BIT_RATE, encodeParam.maxAudioBitRate)
        return mediaFormat
    }

    override fun onFrame(frame: AudioEncodeFrame) {
        nativeOnAudioFrame(
            nativeContext,
            frame.buffer,
            frame.audioFormat.ffmpegFMT,
            frame.channelConfig.count,
            frame.sampleRateInHz,
            frame.pts
        )
    }
}

class FfmpegVideoEncoder : FfmpegEncoder<com.hapi.avencoder.VideoEncodeFrame>(), IVideoEncoder {
    init {
        nativeContext = nativeCreate(2)
    }

    override fun onFrame(frame: VideoEncodeFrame) {
        nativeOnVideoFrame(
            nativeContext,
            frame.AVImgFmt.fmt,
            frame.buffer,
            frame.width,
            frame.height,
            frame.timestamp,
            frame.pts
        )
    }

    override fun configure(encodeParam: VideoEncodeParam): MediaFormat {
        nativeConfigureVideo(
            nativeContext,
            (encodeParam).frameWidth,
            (encodeParam).frameHeight,
            (encodeParam).videoBitRate,
            (encodeParam).minVideoBitRate,
            (encodeParam).maxVideoBitRate,
            (encodeParam).fps,
            getCpuNumCores(),
        )
        val mediaFormat = MediaFormat.createVideoFormat(
            MediaFormat.MIMETYPE_VIDEO_AVC,
            encodeParam.frameWidth,
            encodeParam.frameHeight
        )
        mediaFormat.setInteger(KEY_MIN_BIT_RATE, encodeParam.minVideoBitRate)
        mediaFormat.setInteger(KEY_MAX_BIT_RATE, encodeParam.maxVideoBitRate)
        mediaFormat.setInteger(MediaFormat.KEY_BIT_RATE, (encodeParam).videoBitRate)
        mediaFormat.setInteger(MediaFormat.KEY_FRAME_RATE, (encodeParam).fps)
        mediaFormat.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 1) //关键帧间隔时间 单位s
        mediaFormat.setInteger(
            MediaFormat.KEY_COLOR_FORMAT,
            MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420Planar,
        )
        return mediaFormat
    }

}