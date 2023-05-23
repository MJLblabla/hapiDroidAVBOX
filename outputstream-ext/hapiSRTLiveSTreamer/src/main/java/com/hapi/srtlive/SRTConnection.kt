package com.hapi.srtlive

import android.net.Uri
import com.hapi.ioutput.IConnection
import com.hapi.ioutput.MediaStreamList
import com.hapi.ioutput.muxer.internal.data.FormatPacket
import com.hapi.srtlive.mode.Boundary
import com.hapi.srtlive.mode.MsgCtrl
import com.hapi.srtlive.mode.SRTConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.InetAddress
import java.nio.ByteBuffer

class SRTConnection : IConnection() {

    private external fun stringFromJNI(): String

    companion object {
        // Used to load the 'srtmuxer' library on application startup.
        init {
            System.loadLibrary("srtmuxer")
        }
    }

    private var mNativeContextHandler: Long = -1L

    init {
        mNativeContextHandler = native_init();
    }

    override fun open(url: String, mediaStreamList: MediaStreamList) {
        if (mNativeContextHandler == -1L) {
            return
        }
        GlobalScope.launch {
            val config = SRTConfig()
            val router = Uri.parse(url)
            val scheme = router.scheme
            val inetAddress = withContext(Dispatchers.IO) {
                InetAddress.getByName(router.host)
            }
            val ip: String? = inetAddress.hostAddress // 获取主机ip
            val path = router.path
            val port = router.port
            var streamId = router.getQueryParameter("streamid")!!
            if (streamId.isEmpty()) {
                val ar = url.split("streamid=")
                streamId = ar[1]
            }
            config.streamId = streamId;
            config.port = port
            config.ipAddress = ip ?: ""
            config.maxBW = 0

            val inputBW = mediaStreamList.getInputBW()
            config.inputBW = inputBW

            native_open(
                mNativeContextHandler,
                config.streamId,
                config.ipAddress,
                config.port,
                config.payloadSize,
                config.maxBW,
                config.inputBW
            )
        }

    }

    override fun sendPacket(packet: FormatPacket) {
        val boundary = when {
            packet.isFirstPacketFrame && packet.isLastPacketFrame -> Boundary.SOLO
            packet.isFirstPacketFrame -> Boundary.FIRST
            packet.isLastPacketFrame -> Boundary.LAST
            else -> Boundary.SUBSEQUENT
        }
        val msgCtrl =
            if (packet.frameAbsTimestamp == 0L) {
                MsgCtrl(boundary = boundary)
            } else {
                MsgCtrl(
                    ttl = 500,
                    srcTime = packet.frameAbsTimestamp,
                    boundary = boundary
                )
            }
        native_send(
            mNativeContextHandler,
            packet.buffer,
            packet.buffer.position(),
            packet.buffer.limit(),
            msgCtrl.ttl,
            msgCtrl.srcTime,
            msgCtrl.boundary.intv
        )
    }

    override fun release() {
        native_uninit(mNativeContextHandler)
        mNativeContextHandler = -1;
    }


    fun getStats(): Stats? {
        if (mNativeContextHandler == -1L) {
            return null
        }
        return native_getStats(mNativeContextHandler)
    }

    override fun close() {
        if (mNativeContextHandler == -1L) {
            return
        }
        native_close(mNativeContextHandler)
    }

    private external fun native_init(): Long
    private external fun native_uninit(handler: Long)
    private external fun native_open(
        handler: Long,
        streamId: String,
        ipAddress: String,
        port: Int,
        payloadSize: Int,
        maxBW: Int,
        inputBW: Int,
    )

    private external fun native_getStats(handler: Long): Stats
    private external fun native_send(
        handler: Long,
        msg: ByteBuffer,
        position: Int,
        offset: Int,
        ttl: Int,
        srcTime: Long,
        boundary: Int
    )

    private external fun native_close(handler: Long)

}