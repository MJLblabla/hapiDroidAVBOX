#ifndef AVENCODER_AVRESAMPLE_H
#define AVENCODER_AVRESAMPLE_H
//
// Created by 1 on 2022/4/6.
//
#include "EncodeParam.h"
extern "C"
{
#include <libswresample/swresample.h>
}
#include "libyuv.h"

class AVResample {

private:
    EncodeParam encodeParam;
    SwrContext *m_pSwrCtx = nullptr;
    uint8_t *src_yuv_temp = nullptr;
    uint8_t *rotate_yuv_temp = nullptr;
    uint8_t *scale_yuv_temp = nullptr;


    // 指向输入缓冲区的指针
    uint8_t **inData = nullptr;
    // 输出缓冲区
    // 指向输出缓冲区的指针
    uint8_t **outData = nullptr;
    int outSamples=0;
    int64_t inSamples=0;
    int outBytesPerSample=0;
public:

    AVResample();

    ~AVResample();

    int start(const EncodeParam& param);

    //添加音频数据到音频队列
    void onFrame2Encode(AudioFrame *inputFrame, AudioFrame *resizeAudioFrame);
    int getOutAudioFrameSize(AudioFrame *inputFrame);
    //添加视频数据到视频队列
    void onFrame2Encode(VideoFrame *inputFrame, VideoFrame *resizeInputFrame);

};

#endif

