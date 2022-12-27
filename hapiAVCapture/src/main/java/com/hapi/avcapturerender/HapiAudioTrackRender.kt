package com.hapi.avcapturerender

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import android.os.Build
import com.hapi.avcapture.AudioRender
import com.hapi.avcapture.AudioFrame

class HapiAudioTrackRender : AudioRender {

    private var audioTrack: AudioTrack? = null
    private var isMute = false
    var isRelease = false
        private set

    /**
     * 构建 AudioTrack 实例对象
     */
    private fun createStreamModeAudioTrack(audioFrame: AudioFrame) {
        if (audioTrack == null) {
            val bufferSize = AudioTrack.getMinBufferSize(
                audioFrame.sampleRateInHz,
                audioFrame.AVChannelConfig.androidChannel,
                audioFrame.audioFormat.androidFMT
            )

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                audioTrack = AudioTrack.Builder()
                    .setAudioAttributes(
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_MEDIA)
                            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                            .setLegacyStreamType(AudioManager.STREAM_MUSIC)
                            .build()
                    )
                    .setAudioFormat(
                        AudioFormat.Builder()
                            .setEncoding(audioFrame.audioFormat.androidFMT)
                            .setSampleRate(audioFrame.sampleRateInHz)
                            .setChannelMask(audioFrame.AVChannelConfig.androidChannel)
                            .build()
                    )
                    .setTransferMode(AudioTrack.MODE_STREAM)
                    .setBufferSizeInBytes(bufferSize)
                    .build()
            } else {
                audioTrack = AudioTrack(
                    AudioManager.STREAM_MUSIC,
                    audioFrame.sampleRateInHz,
                    audioFrame.AVChannelConfig.androidChannel,
                    audioFrame.audioFormat.androidFMT,
                    bufferSize,
                    AudioTrack.MODE_STREAM
                )
            }
        }
        audioTrack?.play()
    }

    override fun onAudioFrame(audioFrame: AudioFrame) {
        if (isMute || isRelease) {
            return
        }
        if (audioTrack == null) {
            createStreamModeAudioTrack(audioFrame)
        }
        audioTrack?.write(audioFrame.data,  audioFrame.data.limit(),AudioTrack.WRITE_NON_BLOCKING)
    }


    fun release() {
        isRelease = true
        audioTrack?.stop()
        audioTrack?.release()
        audioTrack = null
    }

    fun mute(isMute: Boolean) {
        this.isMute = isMute
    }
}