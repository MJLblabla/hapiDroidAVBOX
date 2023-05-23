package com.hapi.ioutput

import com.hapi.ioutput.muxer.internal.data.FormatPacket
import java.io.File
import java.io.FileOutputStream

class FileWriterConnection : IConnection() {

    private var bufferWriter: FileOutputStream? = null
    override fun open(url: String, mediaStreamList: MediaStreamList) {
        val file = File(url)
        if (!file.exists()) {
            file.createNewFile()
        }
        jniConnectedStatusChange(ConnectedStatus.CONNECTED_STATUS_START.intStatus)
        bufferWriter = FileOutputStream(file)
        jniConnectedStatusChange(ConnectedStatus.CONNECTED_STATUS_CONNECTED.intStatus)
    }

    override fun sendPacket(packet: FormatPacket) {
        bufferWriter?.write(packet.buffer.array())
    }

    override fun close() {
        bufferWriter?.flush()
        bufferWriter?.close()
        jniConnectedStatusChange(ConnectedStatus.CONNECTED_STATUS_DISCONNECTED.intStatus)
    }

    override fun release() {
    }
}