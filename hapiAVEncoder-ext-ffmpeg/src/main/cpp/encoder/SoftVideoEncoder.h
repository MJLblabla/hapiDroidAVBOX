//
// Created by 满家乐 on 2022/11/29.
//

#ifndef MY_APPLICATION_ISOFTENCODER_H
#define MY_APPLICATION_ISOFTENCODER_H

#include "IEncoder.h"
#include "SoftOutPutCallFunc.h"

class SoftVideoEncoder : public IEncoder {

private:
    AVCodecContext *mAVCodecCtx = nullptr;
    AVCodec *mAVCodec = nullptr;
    AVFrame *mAVFrame = nullptr;
    AVPacket *mAVPacket = nullptr;

    void clear();
    int openCodec();

protected:
    void encodeFrame(Frame *frame) override;

    void stopFlush() override;

    void startOpenCodec() override;

public:
    OutPutCallFunc outPutCallFunc = nullptr;

    void configure(EncodeParam &encodeParam) override;

    void updateBitRate(int bitRate) override;

    ~SoftVideoEncoder() override;
};


#endif //MY_APPLICATION_ISOFTENCODER_H
