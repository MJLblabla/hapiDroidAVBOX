package com.hapi.ioutput

import android.media.MediaFormat

abstract class OutputStreamer : StreamObserver {
    //链接回调
    var muxerCallBack: MuxerCallBack? = null

    //码率调节
    var bitrateRegulatorCallBack: ((Int, streamID: String) -> Unit)? = null

    protected val mMediaStreams = MediaStreamList()
    var connectedStatus = ConnectedStatus.CONNECTED_STATUS_NULL
        private set


    protected open fun changeConnectedStatus(connectedStatus: ConnectedStatus) {
        this.connectedStatus = connectedStatus
        muxerCallBack?.onMuxerConnectedStatus(connectedStatus)
    }

    abstract fun open(url: String)

    override fun addStream(
        trackStreamID: String,
        mediaFormat: MediaFormat
    ) {
        mMediaStreams.add(MediaStream(trackStreamID, mediaFormat))
    }

    override fun removeStream(
        trackStreamID: String
    ) {
        mMediaStreams.remove(trackStreamID)
    }

    abstract fun close()
    abstract fun release()

}