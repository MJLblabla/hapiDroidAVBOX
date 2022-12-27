package com.hapi.avparam

import android.media.AudioFormat


enum class AVSampleFormat(val androidFMT: Int, val ffmpegFMT: Int, val deep: Int) {
    ENCODING_PCM_8BIT(AudioFormat.ENCODING_PCM_8BIT, AV_SAMPLE_FMT_U8, 8),
    ENCODING_PCM_FLOAT(AudioFormat.ENCODING_PCM_FLOAT, AV_SAMPLE_FMT_FLT, 32),
    ENCODING_PCM_16BIT(AudioFormat.ENCODING_PCM_16BIT, AV_SAMPLE_FMT_S16, 16),
}

val AV_SAMPLE_FMT_NONE = (-1)
val AV_SAMPLE_FMT_U8 = (0)  ///< unsigned 8 bits
val AV_SAMPLE_FMT_S16 = (1)   ///< signed 16 bits
val AV_SAMPLE_FMT_S32 = (2)   ///< signed 32 bits
val AV_SAMPLE_FMT_FLT = (3)   ///< float
val AV_SAMPLE_FMT_DBL = (4)   ///< double

val AV_SAMPLE_FMT_U8P = (6)   ///< unsigned 8 bits, planar
val AV_SAMPLE_FMT_S16P = (7)  ///< signed 16 bits, planar
val AV_SAMPLE_FMT_S32P = (8)  ///< signed 32 bits, planar
val AV_SAMPLE_FMT_FLTP = (9)  ///< float, planar
val AV_SAMPLE_FMT_DBLP = (10)  ///< double, planar
val AV_SAMPLE_FMT_S64 = (11)   ///< signed 64 bits
val AV_SAMPLE_FMT_S64P = (12)  ///< signed 64 bits, planar

val AV_SAMPLE_FMT_NB = (13)        ///< Number of sample formats. DO NOT USE if linking dynamically

