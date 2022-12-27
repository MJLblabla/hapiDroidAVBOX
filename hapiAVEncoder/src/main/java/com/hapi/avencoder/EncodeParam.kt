package com.hapi.avencoder

import com.hapi.avparam.AVChannelConfig
import com.hapi.avparam.AVSampleFormat

interface EncodeParam

/**
 * Video encode param
 * 视频编码参数
 * @property frameWidth
 * @property frameHeight
 * @property videoBitRate
 * @property fps
 * @constructor Create empty Video encode param
 */
class VideoEncodeParam(
    var frameWidth: Int,
    var frameHeight: Int,
    val videoBitRate: Int,
    val fps: Int
) : EncodeParam {
    var encoderType = EncoderType.HWEncoder
    var minVideoBitRate: Int = -1
    var maxVideoBitRate: Int = -1

    init {
        if (maxVideoBitRate == -1) {
            maxVideoBitRate = videoBitRate;
        }
        if (minVideoBitRate == -1) {
            minVideoBitRate = videoBitRate
        }
    }

    /**
     * Encoder type
     *
     * @constructor Create empty Encoder type
     */
    enum class EncoderType {
        /**
         * 硬件编码
         */
        HWEncoder,
        /**
         * 软件编码
         */
        SOFT,
        /**
         * 硬件优先失败切软编
         */
        AUTO
    }
}

/**
 * Audio encode param
 * 音频编码参数
 * @property sampleRateInHz
 * @property channelConfig
 * @property audioFormat
 * @property audioBitrate
 * @constructor Create empty Audio encode param
 */
class AudioEncodeParam(
    val sampleRateInHz: Int,
    val channelConfig: AVChannelConfig,
    val audioFormat: AVSampleFormat,
    val audioBitrate: Int
) : EncodeParam {
    var minAudioBitRate: Int = -1
    var maxAudioBitRate: Int = -1

    init {
        if (maxAudioBitRate == -1) {
            maxAudioBitRate = audioBitrate;
        }
        if (minAudioBitRate == -1) {
            minAudioBitRate = audioBitrate
        }
    }
}


