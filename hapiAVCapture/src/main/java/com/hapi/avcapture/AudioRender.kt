package com.hapi.avcapture

interface AudioRender {
    fun onAudioFrame(audioFrame: AudioFrame)
}