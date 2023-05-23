package com.hapi.ioutput

import android.media.MediaCodec
import android.media.MediaFormat
import java.nio.ByteBuffer

interface StreamObserver {

    fun addStream(
        trackStreamID: String,
        mediaFormat: MediaFormat
    )

    fun removeStream(
        trackStreamID: String
    )

    fun writePacket(
        trackStreamID: String,
        outputBuffer: ByteBuffer,
        outputFormat: MediaFormat,
        info: MediaCodec.BufferInfo,
        dts: Long
    )
}