package com.hapi.ioutput

import android.media.MediaFormat


class MediaStreamList {
    val mMediaStreams = ArrayList<MediaStream>()

    fun add(stream: MediaStream) {
        mMediaStreams.add(stream)
    }

    fun remove(trackStreamID: String) {
        findByTrackStreamID(trackStreamID)?.let {
            mMediaStreams.remove(it)
        }
    }

    fun clear() {
        mMediaStreams.clear()
    }

    fun findByTrackStreamID(trackStreamID: String): MediaStream? {
        return mMediaStreams.find { it.trackStreamID == trackStreamID }
    }

    fun findByMediaFormat(format: MediaFormat): MediaStream? {
        return mMediaStreams.find { it.mediaFormat == format }
    }

    fun preparedMuxer(trackStreamID: String, mediaMuxerSteamID: String) {
        findByTrackStreamID(trackStreamID)?.mediaMuxerSteamID = mediaMuxerSteamID
    }

    fun isAllPrepared(): Boolean {
        var prepared = true
        mMediaStreams.forEach {
            if (it.mediaMuxerSteamID.isEmpty()) {
                prepared = false
            }
        }
        return prepared
    }

    fun getInputBW(): Int {
        var inputbw = 0
        mMediaStreams.forEach {
            inputbw += it.mediaFormat.getInteger(MediaFormat.KEY_BIT_RATE)
        }
        return inputbw
    }

}