package com.hapi.avencoder

import com.hapi.avparam.AVChannelConfig
import com.hapi.avparam.AVImgFmt
import com.hapi.avparam.AVSampleFormat
import java.nio.ByteBuffer

class VideoEncodeFrame(
    var width: Int,
    var height: Int,
    var rotationDegrees: Int = 0,
    var buffer: ByteBuffer,
    var AVImgFmt: AVImgFmt
) {
    var timestamp: Long = 0
    var pixelStride = 0
    var rowPadding = 0
    var pts = 0L
}

class AudioEncodeFrame(
    val sampleRateInHz: Int,
    val AVChannelConfig: AVChannelConfig,
    val audioFormat: AVSampleFormat,
    val buffer: ByteBuffer
) {
    var timestamp: Long = 0
    var pts = 0L
}
