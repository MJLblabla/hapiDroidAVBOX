package com.hapi.droidmediamuxer

import android.media.MediaCodec
import android.media.MediaFormat
import android.media.MediaMuxer
import android.util.Log
import com.hapi.avparam.AVLog
import com.hapi.avparam.KEY_AUDIO_FORMAT
import com.hapi.ioutput.ConnectedStatus
import com.hapi.ioutput.OutputStreamer
import com.hapi.ioutput.getFileType
import java.nio.ByteBuffer

class HapiMediaMuxer : OutputStreamer() {

    private var isStart = false
    private var isTrackOk = false;
    private var mMuxer: MediaMuxer? = null

    private fun writeData(outputBuffer: ByteBuffer, bufferInfo: MediaCodec.BufferInfo, track: Int) {
        if (!mMediaStreams.isAllPrepared()) {
            return
        }
        if (!isTrackOk) {
            isTrackOk = true
            AVLog.d("HapiMediaMuxer", "mMuxer?.start()")
            mMuxer?.start()
        }

        //  Log.d("HapiMediaMuxer"," writeData"+track+ "   "+bufferInfo.size+"  "+bufferInfo.flags+"  pts "+bufferInfo.presentationTimeUs)
        if (bufferInfo.flags and MediaCodec.BUFFER_FLAG_CODEC_CONFIG != 0) {
            bufferInfo.size = 0
        } else if (bufferInfo.size != 0) {
            outputBuffer.position(bufferInfo.offset)
            outputBuffer.limit(bufferInfo.offset + bufferInfo.size)
            try {
                mMuxer!!.writeSampleData(track, outputBuffer, bufferInfo)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    override fun open(url: String) {
        isStart = true
        isTrackOk = false
        changeConnectedStatus(ConnectedStatus.CONNECTED_STATUS_START)
        val type = when (url.getFileType()) {
            "mp4" -> MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4
            "webm" -> MediaMuxer.OutputFormat.MUXER_OUTPUT_WEBM
            "3gp" -> MediaMuxer.OutputFormat.MUXER_OUTPUT_3GPP
            "heif" -> MediaMuxer.OutputFormat.MUXER_OUTPUT_HEIF
            "ogg" -> MediaMuxer.OutputFormat.MUXER_OUTPUT_OGG
            else -> MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4
        }
        mMuxer = MediaMuxer(url, type)
        changeConnectedStatus(ConnectedStatus.CONNECTED_STATUS_CONNECTED)
    }

    override fun close() {
        try {
            mMuxer?.stop()
        } catch (e: java.lang.IllegalStateException) {
            e.printStackTrace()
        }
        isStart = false
        changeConnectedStatus(ConnectedStatus.CONNECTED_STATUS_DISCONNECTED)
        try {
            mMuxer?.release()
        } catch (e: IllegalStateException) {
            e.printStackTrace()
        }
    }

    override fun release() {
    }

    override fun writePacket(
        trackStreamID: String,
        outputBuffer: ByteBuffer,
        outputFormat: MediaFormat,
        info: MediaCodec.BufferInfo
    ) {
        if ((mMediaStreams.findByTrackStreamID(trackStreamID)?.mediaMuxerSteamID
                ?: "").isEmpty() && info.flags == MediaCodec.BUFFER_FLAG_CODEC_CONFIG
        ) {
            mMuxer?.let {
                val trackID = it.addTrack(outputFormat).toString()
                mMediaStreams.preparedMuxer(trackStreamID, trackID)
                AVLog.d(
                    "HapiMediaMuxer",
                    "preparedMuxer  $trackStreamID ${outputFormat.getString(MediaFormat.KEY_MIME)} $trackID "
                )
            }
        }
        if (!isStart) {
            return
        }
        if (mMediaStreams.isAllPrepared()) {
            writeData(
                outputBuffer,
                info,
                mMediaStreams.findByTrackStreamID(trackStreamID)!!.mediaMuxerSteamID.toInt()
            )
        }
    }
}