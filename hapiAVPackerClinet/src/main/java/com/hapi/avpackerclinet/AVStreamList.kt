package com.hapi.avpackerclinet

import com.hapi.ioutput.StreamObserver
import com.hapi.avcapture.IAudioTrack
import com.hapi.avcapture.IVideoTrack
import com.hapi.avcapture.Track
import com.hapi.avencoder.AudioEncodeParam
import com.hapi.avencoder.EncoderStatus
import com.hapi.avencoder.EncoderStatusCallBack
import com.hapi.avencoder.VideoEncodeParam
import java.util.LinkedList

class AVStreamList {
    val mediaStreams = LinkedList<AVStream<*>>()
    var encoderStatusCallBack: EncoderStatusCallBack? = null

    private var encodeStatus: EncoderStatus? = null
    private fun checkEncoderStatusChange(encodeStatus: EncoderStatus) {
        if (encodeStatus.intStatus != (this.encodeStatus?.intStatus ?: -1000)) {
            this.encodeStatus = encodeStatus
            encoderStatusCallBack?.onEncoderStatusChange(encodeStatus)
        }
    }

    fun findAVStreamByTrackID(trackID: String): AVStream<*>? {
        return mediaStreams.find { it.track.trackID == trackID }
    }

    fun findAVStreamByStreamID(streamID: String): AVStream<*>? {
        return mediaStreams.find { it.trackStreamID == streamID }
    }

    fun findFistStreamEncodeStatus(): EncoderStatus? {
        if (mediaStreams.isEmpty()) {
            return null
        }
        return mediaStreams[0].encoderContext?.encodeStatus
    }

    fun addVideoAVStream(
        track: IVideoTrack,
        encodeParam: VideoEncodeParam,
        streamObserver: StreamObserver
    ): Boolean {
        if (findAVStreamByTrackID(track.trackID) != null) {
            return false
        }
        val stream = VideoAVStream(track, encodeParam).apply {
            this.encoderStatusCallBack = object : EncoderStatusCallBack {
                override fun onEncoderStatusChange(encodeStatus: EncoderStatus) {
                    checkEncoderStatusChange(encodeStatus)
                }
            }
            this.streamObserver = streamObserver
        }
        mediaStreams.add(stream)
        stream.create()
        return true
    }

    fun addAudioAVStream(
        track: IAudioTrack,
        encodeParam: AudioEncodeParam,
        streamObserver: StreamObserver
    ): Boolean {
        if (findAVStreamByTrackID(track.trackID) != null) {
            return false
        }
        val stream = AudioAVStream(track, encodeParam).apply {
            this.encoderStatusCallBack = object : EncoderStatusCallBack {
                override fun onEncoderStatusChange(encodeStatus: EncoderStatus) {
                    checkEncoderStatusChange(encodeStatus)
                }
            }
            this.streamObserver = streamObserver
        }
        mediaStreams.add(stream)
        stream.create()
        return true
    }

    fun removeAVStream(track: Track<*>) {
        findAVStreamByTrackID(track.trackID)?.let {
            it.release()
            mediaStreams.remove(it)
        }
    }
}