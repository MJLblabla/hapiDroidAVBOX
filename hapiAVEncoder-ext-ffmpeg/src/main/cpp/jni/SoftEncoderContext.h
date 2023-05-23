//
// Created by 满家乐 on 2022/12/1.
//

#ifndef AVENCODER_SOFTENCODERCONTEXT_H
#define AVENCODER_SOFTENCODERCONTEXT_H

#include <jni.h>
#include "SoftVideoEncoder.h"
#include "SoftAudioEncoder.h"


class SoftEncoderContext {

private:
    JNIEnv *getJNIEnv(bool *isAttach) const;

    static void getSpsPps(const uint8_t *codec_extradata,
                          const int codec_extradata_size,
                          int &sps_index, int &pps_index,
                          int &sps_offset, int &pps_offset);

    static void callJavaOutPut(bool isH264, JNIEnv *env, jobject thiz, jmethodID getOutPut,
                               jmethodID callOnOutput,
                               uint8_t *data, int size, int64_t pts, int flag,int64_t dts);

public:
    IEncoder *softEncoder = nullptr;
    JavaVM *mJavaVM = nullptr;
    jobject mJavaObj = nullptr;
    bool isVideoGetExtra = false;
    bool isAudioGetExtra = false;

    void resetFirstFrameFlag();

    void create(JNIEnv *env, jobject thiz, jint media_type);

    ~SoftEncoderContext();

};


#endif //AVENCODER_SOFTENCODERCONTEXT_H
