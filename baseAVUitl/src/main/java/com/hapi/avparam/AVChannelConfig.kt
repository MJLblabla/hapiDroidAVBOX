package com.hapi.avparam

import android.media.AudioFormat

enum class AVChannelConfig(val androidChannel: Int, val FFmpegChannel: Int, val count: Int) {
    LEFT(AudioFormat.CHANNEL_IN_LEFT, 1, 1),
    MONO(AudioFormat.CHANNEL_IN_MONO, 4, 1),
    RIGHT(AudioFormat.CHANNEL_IN_RIGHT, 2, 1),
    STEREO(AudioFormat.CHANNEL_IN_STEREO, 3, 2)
}
