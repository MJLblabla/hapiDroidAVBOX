
#ifndef HAPIEncodeParam_H
#define HAPIEncodeParam_H

extern "C"
{
#include <libavutil/samplefmt.h>
#include <libavutil/channel_layout.h>
#include "libavutil/imgutils.h"
}

enum EncoderState {
    STATE_UNKNOWN = -1,
    STATE_PREPARE,
    STATE_DECODING,
    STATE_PAUSE,
    STATE_STOP
};
#define IMAGE_FORMAT_RGBA           0x01
#define IMAGE_FORMAT_NV21           0x02
#define IMAGE_FORMAT_NV12           0x03
#define IMAGE_FORMAT_I420           0x04


#define DEFAULT_SAMPLE_RATE    44100
#define DEFAULT_CHANNEL_LAYOUT AV_CH_LAYOUT_STEREO


class EncodeParam {
public:
    //video
    int frameWidth;
    int frameHeight;
    int videoBitRate;
    int videoMinBitRate;
    int videoMaxBitRate;

    int fps;
    bool hasVideo = false;
    int format = IMAGE_FORMAT_I420;
    int threadCount = -1;
    //audio
    AVSampleFormat out_sample_fmt;
    int64_t audioChannelLayout;
    int audioSampleRate;
    int audioBitrate;

    bool hasAudio = false;

    void setChannel(int channelCount) {
        if (channelCount == 1) {
            audioChannelLayout = av_get_default_channel_layout(channelCount);
        } else {
            audioChannelLayout = AV_CH_LAYOUT_STEREO;
        }
    }

    EncodeParam() {}

    ~EncodeParam() {}

    int getChannelCount() {
        return av_get_channel_layout_nb_channels(audioChannelLayout);
        //return 0;
    }

    int getSampleDeep() {
        int sampleDeep = 0;
        switch (out_sample_fmt) {
            case AV_SAMPLE_FMT_NONE :
                sampleDeep = 0;
                break;
            case AV_SAMPLE_FMT_U8:
                sampleDeep = 8;
                break;          ///< unsigned 8 bits
            case AV_SAMPLE_FMT_S16:
                sampleDeep = 16;
                break;       ///< signed 16 bits
            case AV_SAMPLE_FMT_S32:
                sampleDeep = 32;
                break;         ///< signed 32 bits
            case AV_SAMPLE_FMT_FLT:
                sampleDeep = 0;
                break;         ///< float
            case AV_SAMPLE_FMT_DBL:
                sampleDeep = 0;
                break;         ///< double

            case AV_SAMPLE_FMT_U8P:
                sampleDeep = 8;
                break;        ///< unsigned 8 bits, planar
            case AV_SAMPLE_FMT_S16P:
                sampleDeep = 16;
                break;        ///< signed 16 bits, planar
            case AV_SAMPLE_FMT_S32P:
                sampleDeep = 32;
                break;        ///< signed 32 bits, planar
            case AV_SAMPLE_FMT_FLTP:
                sampleDeep = 0;
                break;       ///< float, planar
            case AV_SAMPLE_FMT_DBLP:
                sampleDeep = 0;
                break;        ///< double, planar
            case AV_SAMPLE_FMT_S64:
                sampleDeep = 64;
                break;         ///< signed 64 bits
            case AV_SAMPLE_FMT_S64P:
                sampleDeep = 64;
                break;       ///< signed 64 bits, planar

            case AV_SAMPLE_FMT_NB  :
                sampleDeep = 0;
                break;         ///< Number of sample formats. DO NOT USE if linking dynamically
        }
        return sampleDeep;
    }
};

class Frame {

public:
    int frameType = 0;
    uint8_t *data = nullptr;
    int dataSize = 0;
    int64_t pts = 0;
    Frame() = default;

    virtual ~Frame() {
        if (data) {
            free(this->data);
        }
        data = nullptr;
    }
};

class VideoFrame : public Frame {
public:
    int width = 0;
    int height = 0;
    int format = IMAGE_FORMAT_I420;
    int64_t frameTimestamp = 0;

    VideoFrame() {
        frameType = 0;
    };

    ~VideoFrame() = default;
};

class AudioFrame : public Frame {
public:
    AudioFrame() {
        frameType = 1;
    };

    ~AudioFrame() = default;

    AVSampleFormat out_sample_fmt{};
    int64_t audioChannelLayout{};
    int audioSampleRate{};

    void setChannel(int channelCount) {
        if (channelCount == 1) {
            audioChannelLayout = av_get_default_channel_layout(channelCount);
        } else {
            audioChannelLayout = AV_CH_LAYOUT_STEREO;
        }
    }

    int getChannelCount() const {
        // return 0;
        return av_get_channel_layout_nb_channels(audioChannelLayout);
    }

    int getSampleDeep() const {
        int sampleDeep = 0;
        switch (out_sample_fmt) {
            case AV_SAMPLE_FMT_NONE :
                sampleDeep = 0;
                break;
            case AV_SAMPLE_FMT_U8:
                sampleDeep = 8;
                break;          ///< unsigned 8 bits
            case AV_SAMPLE_FMT_S16:
                sampleDeep = 16;
                break;       ///< signed 16 bits
            case AV_SAMPLE_FMT_S32:
                sampleDeep = 32;
                break;         ///< signed 32 bits
            case AV_SAMPLE_FMT_FLT:
                sampleDeep = 0;
                break;         ///< float
            case AV_SAMPLE_FMT_DBL:
                sampleDeep = 0;
                break;         ///< double

            case AV_SAMPLE_FMT_U8P:
                sampleDeep = 8;
                break;        ///< unsigned 8 bits, planar
            case AV_SAMPLE_FMT_S16P:
                sampleDeep = 16;
                break;        ///< signed 16 bits, planar
            case AV_SAMPLE_FMT_S32P:
                sampleDeep = 32;
                break;        ///< signed 32 bits, planar
            case AV_SAMPLE_FMT_FLTP:
                sampleDeep = 0;
                break;       ///< float, planar
            case AV_SAMPLE_FMT_DBLP:
                sampleDeep = 0;
                break;        ///< double, planar
            case AV_SAMPLE_FMT_S64:
                sampleDeep = 64;
                break;         ///< signed 64 bits
            case AV_SAMPLE_FMT_S64P:
                sampleDeep = 64;
                break;       ///< signed 64 bits, planar

            case AV_SAMPLE_FMT_NB  :
                sampleDeep = 0;
                break;         ///< Number of sample formats. DO NOT USE if linking dynamically
        }
        return sampleDeep;
    }
};

#endif