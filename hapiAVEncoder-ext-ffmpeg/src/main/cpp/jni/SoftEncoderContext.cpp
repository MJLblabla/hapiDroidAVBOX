//
// Created by 满家乐 on 2022/12/1.
//

#include "SoftEncoderContext.h"

extern "C"
{
#include "libavcodec/jni.h"
}

void SoftEncoderContext::resetFirstFrameFlag() {
    isVideoGetExtra = false;
    isAudioGetExtra = false;
}

void SoftEncoderContext::callJavaOutPut(bool isH264, JNIEnv *env, jobject jobj, jmethodID getOutPut,
                                        jmethodID callOnOutput, uint8_t *data, int size,
                                        int64_t pts, int flag) {

    auto startTime = GetSysCurrentTimeNS();
    if (isH264) {
        int index0 = data[0];
        int index1 = data[1];
        int index2 = data[2];
        int index3 = data[3];

        if (index0 != 0 ||
            index1 != 0 ||
            index2 != 0 ||
            index3 != 1
                ) {
            auto dataObject = env->CallObjectMethod(jobj, getOutPut, size + 1);
            auto *const dataBuffer = reinterpret_cast<jbyte *>(env->GetDirectBufferAddress(
                    dataObject));
            memcpy(dataBuffer + 1, data, size);
            env->CallVoidMethod(jobj, callOnOutput, size + 1, pts, flag);
        } else {
            auto dataObject = env->CallObjectMethod(jobj, getOutPut, size);
            auto *const dataBuffer = reinterpret_cast<jbyte *>(env->GetDirectBufferAddress(
                    dataObject));
            memcpy(dataBuffer, data, size);
            env->CallVoidMethod(jobj, callOnOutput, size, pts, flag);
        }
    } else {
        auto dataObject = env->CallObjectMethod(jobj, getOutPut, size);
        auto *const dataBuffer = reinterpret_cast<jbyte *>(env->GetDirectBufferAddress(dataObject));
        memcpy(dataBuffer, data, size);
        env->CallVoidMethod(jobj, callOnOutput, size, pts, flag);
    }
    auto endTime = GetSysCurrentTimeNS();
   //LOGCATE("  callJavaOutPut  cost %lld ", endTime - startTime);

}

void SoftEncoderContext::create(JNIEnv *env, jobject thiz, jint media_type) {
    env->GetJavaVM(&mJavaVM);
    mJavaObj = env->NewGlobalRef(thiz);
    av_jni_set_java_vm(mJavaVM, NULL);
    isAudioGetExtra = false;
    isVideoGetExtra = false;
    jmethodID mediaFormatSetInt = env->GetMethodID(env->GetObjectClass(mJavaObj),
                                                   "setInteger", "(Ljava/lang/String;I)V");
    jmethodID mediaFormatSetLong = env->GetMethodID(env->GetObjectClass(mJavaObj),
                                                    "setLong", "(Ljava/lang/String;J)V");
    jmethodID mediaFormatSetFloat = env->GetMethodID(env->GetObjectClass(mJavaObj),
                                                     "setFloat", "(Ljava/lang/String;F)V");
    jmethodID mediaFormatSetString = env->GetMethodID(env->GetObjectClass(mJavaObj),
                                                      "setString",
                                                      "(Ljava/lang/String;Ljava/lang/String;)V");
    jmethodID mediaFormatSetBuffer = env->GetMethodID(env->GetObjectClass(mJavaObj),
                                                      "setByteBuffer",
                                                      "(Ljava/lang/String;[B)V");

    jmethodID javaOnOutputBufferAvailable = env->GetMethodID(env->GetObjectClass(mJavaObj),
                                                             "onOutputBufferAvailable",
                                                             "(IJI)V");

    jmethodID javaGetOutputBuffer = env->GetMethodID(env->GetObjectClass(mJavaObj),
                                                     "getOutputBuffer",
                                                     "(I)Ljava/nio/ByteBuffer;");

    if (media_type == 1) {
        softEncoder =(new SoftAudioEncoder());
        auto *e = dynamic_cast<SoftAudioEncoder *>(softEncoder);
        e->outPutCallFunc = [this, javaGetOutputBuffer, mediaFormatSetInt, mediaFormatSetLong, mediaFormatSetFloat, mediaFormatSetString, mediaFormatSetBuffer, javaOnOutputBufferAvailable](
                int64_t pts, AVPacket *avPacket,
                AVCodecContext *codecParameters) {
            bool isAttach = false;
            JNIEnv *env = this->getJNIEnv(&isAttach);
            if (env == nullptr)
                return;
            if (!isAudioGetExtra) {
                isAudioGetExtra = true;

                jstring keyMaxBitrate = env->NewStringUTF("max-bitrate");
                env->CallVoidMethod(this->mJavaObj, mediaFormatSetInt, keyMaxBitrate,
                                    codecParameters->bit_rate);

                jstring keyBitrate = env->NewStringUTF("bitrate");
                env->CallVoidMethod(this->mJavaObj, mediaFormatSetInt, keyBitrate,
                                    codecParameters->bit_rate);

                jstring keyAacSbrMode = env->NewStringUTF("aac-sbr-mode");
                env->CallVoidMethod(this->mJavaObj, mediaFormatSetInt, keyAacSbrMode,
                                    3);
                jstring keySampleRate = env->NewStringUTF("sample-rate");
                env->CallVoidMethod(this->mJavaObj, mediaFormatSetInt, keySampleRate,
                                    codecParameters->sample_rate);
                jstring keyMime = env->NewStringUTF("mime");
                jstring audio_mp4a_latm = env->NewStringUTF("audio/mp4a-latm");
                env->CallVoidMethod(this->mJavaObj, mediaFormatSetString, keyMime, audio_mp4a_latm);

                jstring keyProfile = env->NewStringUTF("profile");
                env->CallVoidMethod(this->mJavaObj, mediaFormatSetInt, keyProfile,
                                    codecParameters->profile);


                jstring keyChannelCount = env->NewStringUTF("channel-count");
                env->CallVoidMethod(this->mJavaObj, mediaFormatSetInt, keyChannelCount,
                                    codecParameters->channels);

                jbyteArray adtsArray = env->NewByteArray(codecParameters->extradata_size);
                env->SetByteArrayRegion(adtsArray, 0, codecParameters->extradata_size,
                                        (jbyte *) codecParameters->extradata);
                jstring keyCsd0 = env->NewStringUTF("csd-0");
                env->CallVoidMethod(this->mJavaObj, mediaFormatSetBuffer, keyCsd0, adtsArray);

                this->callJavaOutPut(false, env, this->mJavaObj, javaGetOutputBuffer,
                                     javaOnOutputBufferAvailable,
                                     codecParameters->extradata, codecParameters->extradata_size, 0,
                                     2);

                env->DeleteLocalRef(keyCsd0);
                env->DeleteLocalRef(keyMaxBitrate);
                env->DeleteLocalRef(keyBitrate);
                env->DeleteLocalRef(keyProfile);
                env->DeleteLocalRef(keyAacSbrMode);
                env->DeleteLocalRef(keySampleRate);
                env->DeleteLocalRef(keyChannelCount);
                env->DeleteLocalRef(keyMime);
            }

            int flag = avPacket->flags;
            if (flag == 2) {
                flag = 8;
            }
            this->callJavaOutPut(false, env, this->mJavaObj, javaGetOutputBuffer,
                                 javaOnOutputBufferAvailable,
                                 avPacket->data, avPacket->size, pts, flag);
            if (isAttach)
                this->mJavaVM->DetachCurrentThread();
        };
    } else {
        softEncoder = new SoftVideoEncoder();
        auto *e = dynamic_cast<SoftVideoEncoder *>(softEncoder);
        e->outPutCallFunc = [this, javaGetOutputBuffer, mediaFormatSetInt, mediaFormatSetLong, mediaFormatSetFloat, mediaFormatSetString, mediaFormatSetBuffer, javaOnOutputBufferAvailable](
                int64_t pts, AVPacket *avPacket,
                AVCodecContext *codecParameters) {
            bool isAttach = false;
            JNIEnv *env = this->getJNIEnv(&isAttach);
            if (env == nullptr)
                return;

            if (!isVideoGetExtra) {
                isVideoGetExtra = true;
                int sps_index;
                int pps_index;
                int sps_offset;
                int pps_offset;
                SoftEncoderContext::getSpsPps(
                        codecParameters->extradata,
                        codecParameters->extradata_size,
                        sps_index, pps_index,
                        sps_offset, pps_offset
                );

                jbyteArray spsArray = env->NewByteArray(sps_offset);
                env->SetByteArrayRegion(spsArray, 0, sps_offset,
                                        (jbyte *) codecParameters->extradata + sps_index);
                jstring keyCsd0 = env->NewStringUTF("csd-0");
                env->CallVoidMethod(this->mJavaObj, mediaFormatSetBuffer, keyCsd0, spsArray);

                jbyteArray ppsArray = env->NewByteArray(pps_offset);
                env->SetByteArrayRegion(ppsArray, 0, pps_offset,
                                        (jbyte *) codecParameters->extradata + pps_index);
                jstring keyCsd1 = env->NewStringUTF("csd-1");
                env->CallVoidMethod(this->mJavaObj, mediaFormatSetBuffer, keyCsd1, ppsArray);

                jstring keyBitrateMode = env->NewStringUTF("bitrate-mode");
                env->CallVoidMethod(this->mJavaObj, mediaFormatSetInt, keyBitrateMode, 1);

                jstring keyMaxBitrate = env->NewStringUTF("max-bitrate");
                env->CallVoidMethod(this->mJavaObj, mediaFormatSetInt, keyMaxBitrate,
                                    codecParameters->bit_rate);

                jstring keyBitrate = env->NewStringUTF("bitrate");
                env->CallVoidMethod(this->mJavaObj, mediaFormatSetInt, keyBitrate,
                                    codecParameters->bit_rate);

                jstring keyMime = env->NewStringUTF("mime");
                jstring video_avc = env->NewStringUTF("video/avc");
                env->CallVoidMethod(this->mJavaObj, mediaFormatSetString, keyMime, video_avc);


                jstring keyProfile = env->NewStringUTF("profile");
                env->CallVoidMethod(this->mJavaObj, mediaFormatSetInt, keyProfile,
                                    8);
                //codecParameters->profile);

                jstring keyWidth = env->NewStringUTF("width");
                env->CallVoidMethod(this->mJavaObj, mediaFormatSetInt, keyWidth,
                                    codecParameters->width);

                jstring keyHeight = env->NewStringUTF("height");
                env->CallVoidMethod(this->mJavaObj, mediaFormatSetInt, keyHeight,
                                    codecParameters->height);


                jstring keyColorFormat = env->NewStringUTF("color-format");
                env->CallVoidMethod(this->mJavaObj, mediaFormatSetInt, keyColorFormat, 19);


                jstring keyFrameRate = env->NewStringUTF("frame-rate");
                env->CallVoidMethod(this->mJavaObj, mediaFormatSetInt, keyFrameRate,
                                    codecParameters->time_base.den);

                this->callJavaOutPut(true, env, this->mJavaObj, javaGetOutputBuffer,
                                     javaOnOutputBufferAvailable,
                                     codecParameters->extradata, codecParameters->extradata_size, 0,
                                     2);

                env->DeleteLocalRef(spsArray);
                env->DeleteLocalRef(keyCsd0);
                env->DeleteLocalRef(ppsArray);
                env->DeleteLocalRef(keyCsd1);
                env->DeleteLocalRef(keyBitrateMode);
                env->DeleteLocalRef(keyMaxBitrate);
                env->DeleteLocalRef(keyBitrate);
                env->DeleteLocalRef(keyProfile);
                env->DeleteLocalRef(keyWidth);
                env->DeleteLocalRef(keyHeight);
                env->DeleteLocalRef(keyColorFormat);
                env->DeleteLocalRef(keyFrameRate);
                env->DeleteLocalRef(keyMime);
            }

            int flag = avPacket->flags;
            if (flag == 2) {
                flag = 8;
            }

            this->callJavaOutPut(true, env, this->mJavaObj, javaGetOutputBuffer,
                                 javaOnOutputBufferAvailable,
                                 avPacket->data, avPacket->size, pts, flag);

            if (isAttach)
                this->mJavaVM->DetachCurrentThread();
        };
    }
}

void SoftEncoderContext::getSpsPps(const uint8_t *extra_data, const int extra_size,
                                   int &sps_pos, int &pps_pos,
                                   int &sps_length, int &pps_length) {
    int pos = 0;
    while (pos < (extra_size - 4)) {
        if (extra_data[pos] == 0 &&
            extra_data[pos + 1] == 0 &&
            extra_data[pos + 2] == 0 &&
            extra_data[pos + 3] == 1) {
            if ((extra_data[pos + 4] & 0x1f) == 7) {//sps
                sps_pos = pos;
            } else if ((extra_data[pos + 4] & 0x1f) == 8) {//pps
                pps_pos = pos;
            }
        }
        pos++;
    }
    sps_length = pps_pos - sps_pos;
    pps_length = extra_size - pps_pos;
}


JNIEnv *SoftEncoderContext::getJNIEnv(bool *isAttach) const {
    JNIEnv *env;
    int status;
    if (nullptr == mJavaVM) {
        LOGCATE("FFMediaPlayer::GetJNIEnv m_JavaVM == nullptr");
        return nullptr;
    }
    *isAttach = false;
    status = mJavaVM->GetEnv((void **) &env, JNI_VERSION_1_4);
    if (status != JNI_OK) {
        status = mJavaVM->AttachCurrentThread(&env, nullptr);
        if (status != JNI_OK) {
            LOGCATE("FFMediaPlayer::GetJNIEnv failed to attach current thread");
            return nullptr;
        }
        *isAttach = true;
    }
    return env;
}

SoftEncoderContext::~SoftEncoderContext() {
    delete softEncoder;
    softEncoder = nullptr;
    bool isAttach = false;
    getJNIEnv(&isAttach)->DeleteGlobalRef(mJavaObj);
    if (isAttach)
        mJavaVM->DetachCurrentThread();
}