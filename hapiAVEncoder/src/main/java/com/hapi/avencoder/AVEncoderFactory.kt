package com.hapi.avencoder

import com.hapi.avencoder.hw.AudioMediaEncoder
import com.hapi.avencoder.hw.VideoMediaEncoder


object AVEncoderFactory {
    fun configureVideoEncoder(videoEncodeParam: VideoEncodeParam): VideoEncoderContext {
        var videoMediaEncoder =
            if (videoEncodeParam.encoderType == VideoEncodeParam.EncoderType.SOFT) {
                val clazz = Class.forName("com.hapi.avencoderext.ffmpeg.FfmpegVideoEncoder")
                clazz.newInstance() as IVideoEncoder
            } else {
                VideoMediaEncoder()
            }
        var encoderContext = VideoEncoderContext(videoMediaEncoder)
        var outFormat = encoderContext.configure(videoEncodeParam)
        if (outFormat == null && videoEncodeParam.encoderType == VideoEncodeParam.EncoderType.AUTO) {
            encoderContext.release()
            val clazz = Class.forName("com.hapi.avencoderext.ffmpeg.FfmpegVideoEncoder")
            videoMediaEncoder = clazz.newInstance() as IVideoEncoder
            encoderContext = VideoEncoderContext(videoMediaEncoder)
            outFormat = encoderContext.configure(videoEncodeParam)
        }
        return encoderContext
    }

    fun configureAudioEncoder(audioEncodeParam: AudioEncodeParam): AudioEncoderContext {
        return AudioEncoderContext(AudioMediaEncoder()).apply {
            configure(audioEncodeParam)
        }
    }
}