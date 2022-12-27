package com.hapi.ioutput

import com.hapi.ioutput.BaseOutputStreamer
import com.hapi.ioutput.FileWriterConnection
import com.hapi.ioutput.IConnection
import com.hapi.ioutput.muxer.internal.muxers.IMuxer

class FileOutputStreamer(muxer: IMuxer) : BaseOutputStreamer() {
    override val connection: IConnection = FileWriterConnection()
    override val packetMuxer: IMuxer = muxer
}