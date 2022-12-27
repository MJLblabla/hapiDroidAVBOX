package com.hapi.avpackerclinet

import com.hapi.avcapture.AudioFrame
import com.hapi.avcapture.VideoFrame
import com.hapi.avencoder.AudioEncodeFrame
import com.hapi.avencoder.VideoEncodeFrame
import java.nio.ByteBuffer


fun VideoFrame.toVideoEncodeFrame(): VideoEncodeFrame {
    val times = this.timestamp
    return VideoEncodeFrame(
        width = this.width,
        height = this.height,
        rotationDegrees = this.rotationDegrees,
        buffer = (this.buffer as VideoFrame.ImgBuffer).data,
        AVImgFmt = this.buffer.imgFmt()
    ).apply {
        timestamp = times
    }
}

fun AudioFrame.toAudioEncodeFrame(silence: Boolean = false): AudioEncodeFrame {
    return AudioEncodeFrame(
        sampleRateInHz = this.sampleRateInHz,
        AVChannelConfig = this.AVChannelConfig,
        audioFormat = this.audioFormat,
        buffer = if (silence) {
            ByteBuffer.allocateDirect(this.data.limit() - this.data.position())
        } else {
            this.data
        },
    )
}