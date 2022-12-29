//
// Created by 满家乐 on 2022/11/30.
//

#include "IEncoder.h"

void IEncoder::onFrame(Frame &frame) {
    if (state == STATE_DECODING) {
        if (frame.frameType == 0) {
            auto videoFrame = new VideoFrame(std::move(reinterpret_cast<VideoFrame &>(frame)));
            frameQueue.PushBack(videoFrame);
        }
        if (frame.frameType == 1) {
            auto audioFrame = new AudioFrame(std::move(reinterpret_cast<AudioFrame &>(frame)));
            frameQueue.PushBack(audioFrame);
        }
    }
}

void IEncoder::configure(EncodeParam &encodeParam) {
    this->param = encodeParam;
    state = STATE_PREPARE;
}

void IEncoder::start() {
    startOpenCodec();
    if (encoderThread != nullptr) {
        delete encoderThread;
        encoderThread = nullptr;
    }
    frameQueue.Reset();
    freeBufferQueue.Reset();
    state = STATE_DECODING;
    relativelyPts = 0;
    startTimestamp = GetSysCurrentTimeNS() / 1000;
    encoderThread = new std::thread([this] {
        while (this->state != STATE_STOP) {
            while (this->state == STATE_PAUSE) {
                std::unique_lock<std::mutex> lock(mutex);
                cond.wait_for(lock, std::chrono::milliseconds(200));
            }
            if (this->state != STATE_STOP) {
                Frame *frame = nullptr;
                frameQueue.PopFront(frame);
                if (frame) {
                    encodeFrame(frame);
                    if (isAllocateAVFrameBuffer && frame != nullptr &&
                        freeBufferQueue.Size() < freeBufferQueue.Capacity() - 1) {
                        freeBufferQueue.PushFront(frame->data);
                        frame->data = nullptr;
                    }
                }
                delete frame;
                frame = nullptr;
            }
        }
        while (!freeBufferQueue.Empty()) {
            uint8_t *data = nullptr;
            freeBufferQueue.PopBack(data);
            if (data != nullptr) {
                free(data);
            }
            data = nullptr;
        }
        while (!frameQueue.Empty()) {
            Frame *frame = nullptr;
            frameQueue.PopBack(frame);
            delete frame;
        }
    });
}

void IEncoder::pause() {
    std::unique_lock<std::mutex> lock(mutex);
    state = STATE_PAUSE;
}

void IEncoder::resume() {
    // std::unique_lock<std::mutex> lock(mutex);
    state = STATE_DECODING;
    cond.notify_all();
}

void IEncoder::stop() {
    state = STATE_STOP;
    frameQueue.Close();
    freeBufferQueue.Close();
    if (encoderThread != nullptr) {
        encoderThread->join();
        delete encoderThread;
        encoderThread = nullptr;
    }
    stopFlush();
}

IEncoder::~IEncoder() {
    delete encoderThread;
    encoderThread = nullptr;
}

void IEncoder::allocateAVFrameBuffer(uint8_t **address, int size) {
    isAllocateAVFrameBuffer = true;
    if (freeBufferQueue.Empty()) {
        *address = static_cast<uint8_t *>(malloc(size));
    } else {
        freeBufferQueue.PopFront(*address);
    }
}
