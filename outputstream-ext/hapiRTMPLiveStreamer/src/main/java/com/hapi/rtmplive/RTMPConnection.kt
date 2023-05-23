package com.hapi.rtmplive

import com.hapi.ioutput.IConnection
import com.hapi.ioutput.MediaStreamList
import com.hapi.ioutput.muxer.internal.data.FormatPacket
import java.nio.ByteBuffer

class RTMPConnection : IConnection() {

    companion object {
        // Used to load the 'rtmpmuxer' library on application startup.
        init {
            System.loadLibrary("rtmpmuxer")
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
        native_open(mNativeContextHandler, url)
    }

    override fun sendPacket(packet: FormatPacket) {
        if (mNativeContextHandler == -1L) {
            return
        }
        synchronized(this) {
            native_sendPacket(
                mNativeContextHandler,
                packet.buffer,
                packet.buffer.position(),
                packet.buffer.limit()
            )
        }
    }

    override fun close() {
        synchronized(this) {
            if (mNativeContextHandler == -1L) {
                return
            }
            native_close(mNativeContextHandler)
        }
    }

    override fun release() {
        synchronized(this) {
            if (mNativeContextHandler == -1L) {
                return
            }
            native_uninit(mNativeContextHandler)
            mNativeContextHandler = -1L
        }
    }

    private external fun native_init(): Long
    private external fun native_uninit(handler: Long)
    private external fun native_open(handler: Long, url: String)
    private external fun native_sendPacket(
        handler: Long,
        packet: ByteBuffer,
        offset: Int,
        limit: Int
    )

    private external fun native_close(handler: Long)
}