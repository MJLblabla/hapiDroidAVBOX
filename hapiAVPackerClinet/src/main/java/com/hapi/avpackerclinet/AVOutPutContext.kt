package com.hapi.avpackerclinet

import android.media.MediaCodec
import android.media.MediaFormat
import android.net.Uri
import com.hapi.ioutput.AVOutputFormatFactory
import com.hapi.ioutput.OutputStreamer
import java.nio.ByteBuffer

class AVOutPutContext : OutputStreamer() {
    private var mOutputStreamer: OutputStreamer? = null
    private var mAVOutputFormatFactory = AVOutputFormatFactory()
    private var lastUrl = ""
    override fun open(url: String) {
        mOutputStreamer?.release()
        mOutputStreamer = mAVOutputFormatFactory.create(url)
        mOutputStreamer?.muxerCallBack = this.muxerCallBack
        mOutputStreamer?.bitrateRegulatorCallBack = this.bitrateRegulatorCallBack
        mMediaStreams.mMediaStreams.forEach {
            mOutputStreamer?.addStream(it.trackStreamID, it.mediaFormat)
        }
        lastUrl = url
        mOutputStreamer!!.open(url)
    }

    override fun addStream(trackStreamID: String, mediaFormat: MediaFormat) {
        super.addStream(trackStreamID, mediaFormat)
        mOutputStreamer?.addStream(trackStreamID, mediaFormat)
    }

    override fun removeStream(trackStreamID: String) {
        super.removeStream(trackStreamID)
        mOutputStreamer?.removeStream(trackStreamID)
    }

    override fun writePacket(
        trackStreamID: String,
        outputBuffer: ByteBuffer,
        outputFormat: MediaFormat,
        info: MediaCodec.BufferInfo,
        dts: Long
    ) {
        mOutputStreamer?.writePacket(trackStreamID, outputBuffer, outputFormat, info, dts)
    }

    override fun close() {
        mOutputStreamer?.close()
    }

    override fun release() {
        mOutputStreamer?.release()
        mOutputStreamer = null
    }
}