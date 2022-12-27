package com.hapi.ioutput

import android.media.MediaCodec
import android.media.MediaFormat
import com.hapi.ioutput.muxer.internal.data.Frame
import com.hapi.ioutput.muxer.internal.data.Packet
import com.hapi.ioutput.muxer.internal.muxers.IMuxer
import com.hapi.ioutput.muxer.internal.muxers.IMuxerListener
import java.nio.ByteBuffer

abstract class BaseOutputStreamer : OutputStreamer(), IMuxerListener {

    protected abstract val connection: IConnection
    protected abstract val packetMuxer: IMuxer

    override fun onOutputFrame(packet: Packet) {
        connection.sendPacket(packet)
    }

    override fun changeConnectedStatus(connectedStatus: ConnectedStatus) {
        if (connectedStatus == ConnectedStatus.CONNECTED_STATUS_CONNECTED) {
            packetMuxer.startStream()
        }
        if (connectedStatus == ConnectedStatus.CONNECTED_STATUS_DISCONNECTED) {
            packetMuxer.stopStream()
        }
        super.changeConnectedStatus(connectedStatus)
    }

    private var isInit = false
    override fun open(url: String) {
        if (!isInit) {
            isInit = true
            packetMuxer.listener = this
            connection.connectEventCallback = { code ->
                changeConnectedStatus(code.toConnectedStatus())
            }
            connection.connectMsgCall = { what, msg ->
                muxerCallBack?.onOutputStreamerEvent(what.toOutputStreamerEvent(), msg)
            }
        }
        connection.open(url, mMediaStreams)
    }

    override fun close() {
        connection.close()
    }

    override fun release() {
        connection.release()
    }

    override fun addStream(trackStreamID: String, mediaFormat: MediaFormat) {
        super.addStream(trackStreamID, mediaFormat)
        packetMuxer.addStreams(listOf(mediaFormat)).let {
            mMediaStreams.findByTrackStreamID(trackStreamID)
                ?.preparedMuxer(it[mediaFormat].toString())
        }
    }

    override fun removeStream(trackStreamID: String) {
        mMediaStreams.findByTrackStreamID(trackStreamID)?.let {
            packetMuxer.removeStreams(listOf(it.mediaMuxerSteamID.toInt()))
        }
        super.removeStream(trackStreamID)
    }

    override fun writePacket(
        trackStreamID: String,
        outputBuffer: ByteBuffer,
        outputFormat: MediaFormat,
        info: MediaCodec.BufferInfo
    ) {
        if (!mMediaStreams.isAllPrepared()) {
            return
        }

        val buffer = outputBuffer ?: return;
        if (info.flags == MediaCodec.BUFFER_FLAG_CODEC_CONFIG) {
            return
        }
        val isKey = info.flags == MediaCodec.BUFFER_FLAG_KEY_FRAME
        packetMuxer.encode(
            Frame(
                buffer,
                outputFormat.getString(MediaFormat.KEY_MIME)!!,
                info.presentationTimeUs,
                System.nanoTime() / 1000,
                //startTime + info.presentationTimeUs, // pts
                null, // dts
                isKey,
                if (mMediaStreams.findByTrackStreamID(trackStreamID)!!.isAudio()) {
                    listOf(
                        outputFormat.getByteBuffer("csd-0")!!,
                    )
                } else {
                    listOf(
                        outputFormat.getByteBuffer("csd-0")!!,
                        outputFormat.getByteBuffer("csd-1")!!
                    )
                }
            ), mMediaStreams.findByTrackStreamID(trackStreamID)!!.mediaMuxerSteamID.toInt()
        )
    }
}