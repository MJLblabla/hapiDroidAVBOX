package com.hapi.avcapture

import com.hapi.avparam.AVChannelConfig
import com.hapi.avparam.AVSampleFormat
import java.nio.ByteBuffer

class AudioFrame(
    val sampleRateInHz: Int,
    val AVChannelConfig: AVChannelConfig,
    val audioFormat: AVSampleFormat,
    val data: ByteBuffer
)

