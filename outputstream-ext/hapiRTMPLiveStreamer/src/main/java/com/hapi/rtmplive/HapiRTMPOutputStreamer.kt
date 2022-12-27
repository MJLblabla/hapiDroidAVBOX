package com.hapi.rtmplive

import com.hapi.ioutput.*
import com.hapi.ioutput.muxer.internal.muxers.IMuxer
import com.hapi.ioutput.muxer.internal.muxers.flv.FlvMuxer

class HapiRTMPOutputStreamer : BaseOutputStreamer() {
    override val connection: IConnection = RTMPConnection()
    override val packetMuxer: IMuxer = FlvMuxer(null, false)
}