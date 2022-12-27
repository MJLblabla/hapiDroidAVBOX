#include <jni.h>
#include <string>
#include "AVResample.h"
#include <jni.h>


extern "C"
JNIEXPORT jlong
Java_com_hapi_avencoder_AVResampleContext_nativeCreateContext(JNIEnv *env,
                                                              jobject thiz) {

    auto mAVResample = new AVResample();
    return reinterpret_cast<jlong>(mAVResample);
}
extern "C"
JNIEXPORT void JNICALL
Java_com_hapi_avencoder_AVResampleContext_nativeDestroyContext(JNIEnv *env,
                                                               jobject thiz,
                                                               jlong handler) {
    auto *avResample = reinterpret_cast<AVResample *>(handler);
    delete avResample;
}
//extern "C"
//JNIEXPORT void JNICALL
//Java_com_hapi_avencoder_AVResampleContext_native_1Start(JNIEnv *env, jobject thiz,
//                                                        jlong handler,
//                                                        jint frame_width,
//                                                        jint frame_height,
//                                                        jint video_bit_rate, jint fps,
//                                                        jint fmat,
//                                                        jint out_sample_fmt,
//                                                        jint audioChannelCount,
//                                                        jint audio_sample_rate,
//                                                        jint audio_bitrate) {
//
//    auto *avResample = reinterpret_cast<AVResample *>(handler);
//    EncodeParam encodeParam;
//    //video
//    encodeParam.frameWidth = frame_width;
//    encodeParam.frameHeight = frame_height;
//    encodeParam.videoBitRate = video_bit_rate;
//    encodeParam.fps = fps;
//    encodeParam.format = (fmat);
//    if (video_bit_rate == 0) {
//        encodeParam.hasVideo = false;
//    } else {
//        encodeParam.hasVideo = true;
//    }
//    encodeParam.out_sample_fmt = AVSampleFormat(out_sample_fmt);
//    encodeParam.setChannel(audioChannelCount);
//    encodeParam.audioSampleRate = audio_sample_rate;
//    encodeParam.audioBitrate = audio_bitrate;
//    if (audio_bitrate == 0) {
//        encodeParam.hasAudio = false;
//    } else {
//        encodeParam.hasAudio = true;
//    }
//    avResample->start(encodeParam);
//}

extern "C"
JNIEXPORT void JNICALL
Java_com_hapi_avencoder_AVResampleContext_nativeOnVideoData(JNIEnv *env, jobject thiz,
                                                            jlong handler, jint format,
                                                            jobject data, jint width, jint height,
                                                            jint rotation_degrees,
                                                            jint pixel_stride, jint row_padding,
                                                            jobject out_frame) {

    auto *avResample = reinterpret_cast<AVResample *>(handler);

    auto *const dataBuffer = reinterpret_cast<jbyte *>(env->GetDirectBufferAddress(data));
    jlong len_arr = env->GetDirectBufferCapacity(data);

    VideoFrame videoFrame;
    videoFrame.dataSize = len_arr;
    videoFrame.data = reinterpret_cast<uint8_t *>(dataBuffer);
    videoFrame.width = width;
    videoFrame.height = height;
    videoFrame.format = (format);
    videoFrame.rotationDegrees = rotation_degrees;
    videoFrame.rowPadding = row_padding;
    videoFrame.pixelStride = pixel_stride;

    auto *const c_array_out = reinterpret_cast<jbyte *>(env->GetDirectBufferAddress(out_frame));
    jlong len_arr_out = env->GetDirectBufferCapacity(out_frame);
    VideoFrame outFrame;
    outFrame.data = (uint8_t *) c_array_out;
    avResample->onFrame2Encode(&videoFrame, &outFrame);
    // memcpy(c_array_out, outFrame.data, outFrame.dataSize);
    outFrame.data = nullptr;
    videoFrame.data = nullptr;

}
extern "C"
JNIEXPORT void JNICALL
Java_com_hapi_avencoder_AVResampleContext_nativeOnAudioData(JNIEnv *env,
                                                            jobject thiz,
                                                            jlong handler,
                                                            jobject data,
                                                            jint sample_fmt,
                                                            jint audio_channel_count,
                                                            jint audio_sample_rate,
                                                            jobject out_frame) {

    auto *avResample = reinterpret_cast<AVResample *>(handler);

    auto *c_array = reinterpret_cast<jbyte *>(env->GetDirectBufferAddress(data));
    jlong len_arr = env->GetDirectBufferCapacity(data);

    AudioFrame audioFrame;
    audioFrame.dataSize = len_arr;
    audioFrame.data = reinterpret_cast<uint8_t *>(c_array);
    audioFrame.out_sample_fmt = AVSampleFormat(sample_fmt);
    audioFrame.audioSampleRate = audio_sample_rate;
    audioFrame.setChannel(audio_channel_count);

    auto *c_array_out = reinterpret_cast<jbyte *>(env->GetDirectBufferAddress(
            out_frame));
    jlong len_arr_out = env->GetDirectBufferCapacity(out_frame);
    AudioFrame outFrame;
    avResample->onFrame2Encode(&audioFrame, &outFrame);
    memcpy(c_array_out, outFrame.data, outFrame.dataSize);
    audioFrame.data = nullptr;
    outFrame.data = nullptr;
}

extern "C"
JNIEXPORT jint JNICALL
Java_com_hapi_avencoder_AVResampleContext_nativeGetResizeAudioFrameSize(JNIEnv *env, jobject thiz,
                                                                        jlong handler,
                                                                        jint data_size,
                                                                        jint sample_fmt,
                                                                        jint audio_channel_count,
                                                                        jint audio_sample_rate) {
    auto *avResample = reinterpret_cast<AVResample *>(handler);
    AudioFrame audioFrame;
    audioFrame.dataSize = data_size;
    audioFrame.out_sample_fmt = AVSampleFormat(sample_fmt);
    audioFrame.audioSampleRate = audio_sample_rate;
    audioFrame.setChannel(audio_channel_count);
    return avResample->getOutAudioFrameSize(&audioFrame);
}
extern "C"
JNIEXPORT void JNICALL
Java_com_hapi_avencoder_AVResampleContext_nativeStartVideo(JNIEnv *env, jobject thiz,
                                                           jlong handler, jint frame_width,
                                                           jint frame_height,
                                                           jint video_bit_rate, jint fps,
                                                           jint img_format) {
    auto *avResample = reinterpret_cast<AVResample *>(handler);
    EncodeParam encodeParam;
    //video
    encodeParam.frameWidth = frame_width;
    encodeParam.frameHeight = frame_height;
    encodeParam.videoBitRate = video_bit_rate;
    encodeParam.fps = fps;
    encodeParam.format = img_format;
    encodeParam.hasVideo = false;
    avResample->start(encodeParam);
}
extern "C"
JNIEXPORT void JNICALL
Java_com_hapi_avencoder_AVResampleContext_nativeStartAudio(JNIEnv *env, jobject thiz,
                                                           jlong handler, jint out_sample_fmt,
                                                           jint audio_channel_count,
                                                           jint audio_sample_rate,
                                                           jint audio_bitrate) {
    auto *avResample = reinterpret_cast<AVResample *>(handler);
    EncodeParam encodeParam;
    encodeParam.out_sample_fmt = AVSampleFormat(out_sample_fmt);
    encodeParam.setChannel(audio_channel_count);
    encodeParam.audioSampleRate = audio_sample_rate;
    encodeParam.audioBitrate = audio_bitrate;
    encodeParam.hasAudio = true;
    avResample->start(encodeParam);
}