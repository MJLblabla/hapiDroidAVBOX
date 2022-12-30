package com.hapi.avencoder

import com.hapi.avparam.*
import java.nio.ByteBuffer

class AVResampleContext {
    var configFormat = AVImgFmt.IMAGE_FORMAT_RGBA
    var audioFrameOutPutSize = 0
        private set

    companion object {
        // Used to load the 'avencoder' library on application startup.
        init {
            System.loadLibrary("avencoder")
        }
    }

    private var mNativeContextHandler: Long = -1;
    fun init() {
        audioFrameOutPutSize = 0
        if (mNativeContextHandler != -1L) {
            unit()
        }
        mNativeContextHandler = nativeCreateContext()
    }

    fun unit() {
        if (mNativeContextHandler != -1L) {
            nativeDestroyContext(mNativeContextHandler)
        }
        mNativeContextHandler = -1;
    }

    fun setParam(videoEncodeParam: VideoEncodeParam) {
        if (mNativeContextHandler == -1L) {
            return;
        }
        nativeStartVideo(
            mNativeContextHandler,
            videoEncodeParam.frameWidth,
            videoEncodeParam.frameHeight,
            videoEncodeParam.videoBitRate,
            videoEncodeParam.fps,
            configFormat.fmt
        )
    }

    fun setParam(audioEncodeParam: AudioEncodeParam) {
        if (mNativeContextHandler == -1L) {
            return
        }
        nativeStartAudio(
            mNativeContextHandler,
            //audio
            audioEncodeParam.audioFormat.ffmpegFMT,
            audioEncodeParam.channelConfig.count,
            audioEncodeParam.sampleRateInHz,
            audioEncodeParam.audioBitrate
        )
    }

    fun onAudioData(audioFrame: AudioEncodeFrame, outFrame: ByteBuffer) {
        nativeOnAudioData(
            mNativeContextHandler,
            audioFrame.buffer,
            audioFrame.audioFormat.ffmpegFMT,
            audioFrame.channelConfig.count,
            audioFrame.sampleRateInHz, outFrame
        )
    }

    fun getResizeAudioFrameSize(
        audioFrame: AudioEncodeFrame
    ): Int {
        audioFrameOutPutSize = nativeGetResizeAudioFrameSize(
            mNativeContextHandler,
            audioFrame.buffer.limit(),
            audioFrame.audioFormat.ffmpegFMT,
            audioFrame.channelConfig.count,
            audioFrame.sampleRateInHz
        )
        return audioFrameOutPutSize;
    }

    fun onVideoData(videoFrame: VideoEncodeFrame, outFrame: ByteBuffer) {
        nativeOnVideoData(
            mNativeContextHandler,
            videoFrame.AVImgFmt.fmt,
            (videoFrame.buffer),
            videoFrame.width,
            videoFrame.height,
            videoFrame.rotationDegrees,
            (videoFrame).pixelStride,
            (videoFrame).rowPadding,
            outFrame
        )
    }

    private external fun nativeCreateContext(): Long
    private external fun nativeDestroyContext(handler: Long)
    private external fun nativeStartVideo(
        handler: Long,
        frameWidth: Int,
        frameHeight: Int,
        videoBitRate: Int,
        fps: Int,
        imgFormat: Int,
    )

    private external fun nativeStartAudio(
        handler: Long,
        //audio
        out_sample_fmt: Int,
        audioChannelCount: Int,
        audioSampleRate: Int,
        audioBitrate: Int,
    )

    private external fun nativeOnAudioData(
        handler: Long,
        data: ByteBuffer,
        sample_fmt: Int,
        audioChannelCount: Int,
        audioSampleRate: Int,
        outFrame: ByteBuffer
    )

    private external fun nativeGetResizeAudioFrameSize(
        handler: Long,
        dataSize: Int,
        sample_fmt: Int,
        audioChannelCount: Int,
        audioSampleRate: Int
    ): Int

    private external fun nativeOnVideoData(
        handler: Long,
        format: Int,
        data: ByteBuffer,
        width: Int,
        height: Int,
        rotationDegrees: Int,
        pixelStride: Int, rowPadding: Int,
        outFrame: ByteBuffer
    )
}