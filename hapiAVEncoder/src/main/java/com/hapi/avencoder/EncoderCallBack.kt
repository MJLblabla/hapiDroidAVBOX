package com.hapi.avencoder

import android.media.MediaCodec
import android.media.MediaFormat
import java.nio.ByteBuffer

interface EncoderStatusCallBack {
    fun onEncoderStatusChange(encodeStatus: EncoderStatus) {}
}

interface EncoderCallBack : EncoderStatusCallBack {
    fun onOutputFormatChanged(mediaFormat: MediaFormat) {}
    fun onOutputBufferAvailable(
        outputBuffer: ByteBuffer,
        outputFormat: MediaFormat,
        info: MediaCodec.BufferInfo,
        dts: Long
    )
}