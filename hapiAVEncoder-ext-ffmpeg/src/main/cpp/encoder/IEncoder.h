//
// Created by 满家乐 on 2022/11/30.
//

#ifndef MY_APPLICATION_IENCODER_H
#define MY_APPLICATION_IENCODER_H


#include "EncodeParam.h"
#include "thread"
#include "unistd.h"
#include "LogUtil.h"
#include "BlockQueue.h"

using namespace std;


class IEncoder {

private:
    bool isAllocateAVFrameBuffer = false;
    std::mutex mutex;
    std::condition_variable cond;
    thread *encoderThread = nullptr;

protected:
    EncodeParam param;
    BlockQueue<uint8_t *> freeBufferQueue{10};
    BlockQueue<Frame *> frameQueue{10};
    EncoderState state;
    int64_t relativelyPts = 0;
    int64_t startTimestamp = 0;

    virtual void startOpenCodec() = 0;

    virtual void encodeFrame(Frame *frame) = 0;

    virtual void stopFlush() = 0;

    void allocateAVFrameBuffer(uint8_t **address, int size);

public:

    virtual void configure(EncodeParam &encodeParam);

    void start();

    void stop();

    void pause();

    void resume();

    void onFrame(Frame &frame);

    virtual void updateBitRate(int bitRate) = 0;

    virtual  ~IEncoder();
};

#endif //MY_APPLICATION_IENCODER_H
