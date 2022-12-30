//
// Created by 满家乐 on 2022/11/30.
//

#ifndef AVENCODER_SOFTAUDIOENCODER_H
#define AVENCODER_SOFTAUDIOENCODER_H

#include "IEncoder.h"
#include "SoftOutPutCallFunc.h"

class SoftAudioEncoder : public IEncoder {

private:
    AVCodecContext *mAVCodecCtx = nullptr;
    AVCodec *mAVCodec = nullptr;
    AVFrame *mAVFrame = nullptr;
    AVPacket *mAVPacket = nullptr;
    int mFrameBufferSize;
    uint8_t *mFrameBuffer = nullptr;

    int openCodec();

    void clear();

protected:
    void encodeFrame(Frame *frame) override;

    void stopFlush() override;

    void startOpenCodec() override;

public:
    OutPutCallFunc outPutCallFunc = nullptr;

    void configure(EncodeParam &encodeParam) override;

    void updateBitRate(int bitRate) override;

    ~SoftAudioEncoder() override;
};

#endif //AVENCODER_SOFTAUDIOENCODER_H
