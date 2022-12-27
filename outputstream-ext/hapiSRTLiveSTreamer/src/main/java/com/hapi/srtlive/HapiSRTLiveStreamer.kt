package com.hapi.srtlive

import android.media.MediaFormat
import com.hapi.ioutput.*
import com.hapi.ioutput.muxer.internal.muxers.IMuxer
import com.hapi.ioutput.muxer.internal.muxers.ts.TSMuxer
import com.hapi.ioutput.muxer.internal.muxers.ts.data.TsServiceInfo
import com.hapi.srtlive.bitrateregulator.SrtBitrateRegulator

class HapiSRTLiveStreamer : BaseOutputStreamer() {

    override val connection: SRTConnection = SRTConnection()
    private val tsServiceInfo: TsServiceInfo by lazy {
        TsServiceInfo(
            TsServiceInfo.ServiceType.DIGITAL_TV,
            0x4698,
            "HapiSRTMuxer",
            "HapiSRTMuxer"
        )
    }
    override val packetMuxer: IMuxer = TSMuxer().apply {
        addService(tsServiceInfo)
    }

    private val mSrtBitrateRegulator by lazy {
        val srtBitrateRegulator = SrtBitrateRegulator()
        srtBitrateRegulator.mVideoBitrateRegulatorCallBack = bitrateRegulatorCallBack
        srtBitrateRegulator.mAudioBitrateRegulatorCallBack = bitrateRegulatorCallBack
        srtBitrateRegulator.srtConnection = connection
        srtBitrateRegulator
    }

    override fun changeConnectedStatus(connectedStatus: ConnectedStatus) {
        if (connectedStatus == ConnectedStatus.CONNECTED_STATUS_CONNECTED) {
            mSrtBitrateRegulator.start()
        }
        if (connectedStatus == ConnectedStatus.CONNECTED_STATUS_DISCONNECTED) {
            mSrtBitrateRegulator.stop()
        }
        super.changeConnectedStatus(connectedStatus)
    }

    override fun addStream(trackStreamID: String, mediaFormat: MediaFormat) {
        super.addStream(trackStreamID, mediaFormat)
        mMediaStreams.mMediaStreams.forEach {
            if (it.isVideo()) {
                mSrtBitrateRegulator.videoStream = it
            }
            if (it.isAudio()) {
                mSrtBitrateRegulator.audioStream = it
            }
        }
    }

    override fun removeStream(trackStreamID: String) {
        super.removeStream(trackStreamID)
        mSrtBitrateRegulator.videoStream = null
        mSrtBitrateRegulator.audioStream = null
        mMediaStreams.mMediaStreams.forEach {
            if (it.isVideo()) {
                mSrtBitrateRegulator.videoStream = it
            }
            if (it.isAudio()) {
                mSrtBitrateRegulator.audioStream = it
            }
        }
    }
}