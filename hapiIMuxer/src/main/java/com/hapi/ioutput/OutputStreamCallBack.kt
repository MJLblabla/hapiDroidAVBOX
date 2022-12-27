package com.hapi.ioutput

enum class ConnectedStatus(val intStatus: Int) {
    CONNECTED_STATUS_NULL(1),
    CONNECTED_STATUS_START(2),
    CONNECTED_STATUS_CONNECTED(3),
    CONNECTED_STATUS_CONNECT_FAIL(4),
    CONNECTED_STATUS_OFFLINE(5),
    CONNECTED_STATUS_RECONNECTED(6),
    CONNECTED_STATUS_DISCONNECTED(8)
}

enum class OutputStreamerEvent(val intStatus: Int) {
    EVENT_SEND_PACKET_FAIL(0)
}

fun Int.toConnectedStatus(): ConnectedStatus {
    ConnectedStatus.values().forEach {
        if (it.intStatus == this) {
            return it;
        }
    }
    return ConnectedStatus.CONNECTED_STATUS_NULL;
}
fun Int.toOutputStreamerEvent():OutputStreamerEvent{
    OutputStreamerEvent.values().forEach {
        if (it.intStatus == this) {
            return it
        }
    }
    return OutputStreamerEvent.EVENT_SEND_PACKET_FAIL
}

interface MuxerCallBack {
    fun onMuxerConnectedStatus(status: ConnectedStatus)
    fun onOutputStreamerEvent(event: OutputStreamerEvent, msg: String)
}