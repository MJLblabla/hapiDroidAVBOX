package com.hapi.ioutput

import com.hapi.ioutput.muxer.internal.data.Packet
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

abstract class IConnection {

    var connectEventCallback: (code: Int) -> Unit = { }
    var connectMsgCall: (Int, String) -> Unit = { _, _ -> }

    fun jniConnectedStatusChange(status: Int) {
        GlobalScope.launch(Dispatchers.Main) {
            connectEventCallback.invoke(status)
        }
    }

    fun jniMsgCall(what: Int, msg: String) {
        GlobalScope.launch(Dispatchers.Main) {
            connectMsgCall.invoke(what, msg)
        }
    }

    abstract fun open(url: String, mediaStreamList: MediaStreamList)
    abstract fun sendPacket(packet: Packet)
    abstract fun close()
    abstract fun release()
}