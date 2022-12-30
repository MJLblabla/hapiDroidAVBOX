
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
};

class Frame {

public:
    int frameType = 0;
    uint8_t *data = nullptr;
    int dataSize = 0;
    int64_t pts = 0;

    int width = 0;
    int height = 0;
    int format = IMAGE_FORMAT_I420;
    int64_t frameTimestamp = 0;

    AVSampleFormat out_sample_fmt = AV_SAMPLE_FMT_NONE;
    int64_t audioChannelLayout = 1;
    int audioSampleRate = 0;

    Frame() = default;

    ~Frame() {
        if (data) {
            free(this->data);
        }
        data = nullptr;
    }

    void clone(uint8_t *buffer, Frame &other) {
        this->format = other.format;
        this->frameTimestamp = other.frameTimestamp;
        this->height = other.height;
        this->width = other.width;
        this->dataSize = other.dataSize;
        this->pts = other.pts;

        this->out_sample_fmt = other.out_sample_fmt;
        this->audioChannelLayout = other.audioChannelLayout;
        this->audioSampleRate = other.audioSampleRate;

        memcpy(buffer, other.data, other.dataSize);
        this->data = buffer;
    }

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
};

#endif