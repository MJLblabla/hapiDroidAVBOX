package com.hapi.avpackerclinet

import android.media.MediaCodec
import android.media.MediaFormat
import android.net.Uri
import com.hapi.ioutput.OutputStreamer
import java.nio.ByteBuffer

class AVOutPutContext : OutputStreamer() {
    private var mOutputStreamer: OutputStreamer? = null
    private fun reflectionMuxer(className: String): OutputStreamer {
        val clz = Class.forName(className)
        return clz.newInstance() as OutputStreamer
    }

    private var lastUrl = ""
    override fun open(url: String) {
        var isSameMuxer = false
        val router = Uri.parse(url)
        val scheme = router.scheme
        val className = when {
            (url.endsWith("mp4") && (url.startsWith("/") || url.startsWith("file"))) -> ("com.hapi.droidmediamuxer.HapiMediaMuxer")
            scheme == "srt" -> ("com.hapi.srtlive.HapiSRTLiveStreamer")
            scheme == "rtmp" -> ("com.hapi.rtmplive.HapiRTMPOutputStreamer")
            else -> throw  Exception(" unSupport url")
        }
        isSameMuxer = (className == mOutputStreamer?.javaClass?.canonicalName) && lastUrl == url
        if (!isSameMuxer) {
            mOutputStreamer?.release()
            mOutputStreamer = reflectionMuxer(className)
            mOutputStreamer?.muxerCallBack = this.muxerCallBack
            mOutputStreamer?.bitrateRegulatorCallBack = this.bitrateRegulatorCallBack
            mMediaStreams.mMediaStreams.forEach {
                mOutputStreamer?.addStream(it.trackStreamID, it.mediaFormat)
            }
        }
        lastUrl=url
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
        info: MediaCodec.BufferInfo
    ) {
        mOutputStreamer?.writePacket(trackStreamID, outputBuffer, outputFormat, info)
    }

    override fun close() {
        mOutputStreamer?.close()
    }

    override fun release() {
        mOutputStreamer?.release()
        mOutputStreamer = null
    }
}