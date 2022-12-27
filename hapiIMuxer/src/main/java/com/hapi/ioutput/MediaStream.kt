package com.hapi.ioutput

import android.media.MediaFormat

class MediaStream(
    var trackStreamID: String,
    var mediaFormat: MediaFormat
) {
    var mediaMuxerSteamID = ""

    fun preparedMuxer(mediaMuxerSteamID: String) {
        this.mediaMuxerSteamID = mediaMuxerSteamID
    }

    fun isVideo(): Boolean {
        return mediaFormat.getString(MediaFormat.KEY_MIME)?.startsWith("video") ?: false
    }

    fun isAudio(): Boolean {
        return mediaFormat.getString(MediaFormat.KEY_MIME)?.startsWith("audio") ?: false
    }
}
