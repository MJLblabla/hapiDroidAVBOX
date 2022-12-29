//
// Created by 满家乐 on 2022/11/29.
//

#include <jni.h>
#include <string>
#include "SoftAudioEncoder.h"
#include "SoftVideoEncoder.h"
#include "jni/SoftEncoderContext.h"

extern "C"
JNIEXPORT void JNICALL
Java_com_hapi_avencoderext_ffmpeg_FfmpegEncoder_nativeStart(JNIEnv *env, jobject thiz,
                                                            jlong native_context) {
    auto encoder = reinterpret_cast<SoftEncoderContext *> (native_context);
    encoder->resetFirstFrameFlag();
    encoder->softEncoder->start();
}
extern "C"
JNIEXPORT void JNICALL
Java_com_hapi_avencoderext_ffmpeg_FfmpegEncoder_nativeStop(JNIEnv *env, jobject thiz,
                                                           jlong native_context) {
    auto encoder = reinterpret_cast<SoftEncoderContext *> (native_context);
    encoder->softEncoder->stop();

}
extern "C"
JNIEXPORT void JNICALL
Java_com_hapi_avencoderext_ffmpeg_FfmpegEncoder_nativePause(JNIEnv *env, jobject thiz,
                                                            jlong native_context) {
    auto encoder = reinterpret_cast<SoftEncoderContext *> (native_context);
    encoder->softEncoder->pause();
}
extern "C"
JNIEXPORT void JNICALL
Java_com_hapi_avencoderext_ffmpeg_FfmpegEncoder_nativeResume(JNIEnv *env, jobject thiz,
                                                             jlong native_context) {
    auto encoder = reinterpret_cast<SoftEncoderContext *> (native_context);
    encoder->softEncoder->resume();
}
extern "C"
JNIEXPORT void JNICALL
Java_com_hapi_avencoderext_ffmpeg_FfmpegEncoder_nativeUpdateBitRate(JNIEnv *env, jobject thiz,
                                                                    jlong native_context,
                                                                    jint bit_rate) {
    auto encoder = reinterpret_cast<SoftEncoderContext *> (native_context);
    encoder->softEncoder->updateBitRate(bit_rate);
}

extern "C"
JNIEXPORT void JNICALL
Java_com_hapi_avencoderext_ffmpeg_FfmpegEncoder_nativeRelease(JNIEnv *env, jobject thiz,
                                                              jlong native_context) {
    auto encoder = reinterpret_cast<SoftEncoderContext *> (native_context);
    delete encoder;
    encoder = nullptr;
}

extern "C"
JNIEXPORT jlong JNICALL
Java_com_hapi_avencoderext_ffmpeg_FfmpegEncoder_nativeCreate(JNIEnv *env, jobject thiz,
                                                             jint media_type) {
    auto encoderContext = new SoftEncoderContext();
    encoderContext->create(env, thiz, media_type);
    return reinterpret_cast<jlong>(encoderContext);
}
extern "C"
JNIEXPORT void JNICALL
Java_com_hapi_avencoderext_ffmpeg_FfmpegEncoder_nativeOnAudioFrame(JNIEnv *env, jobject thiz,
                                                                   jlong native_context,
                                                                   jobject data,
                                                                   jint sample_fmt,
                                                                   jint audio_channel_count,
                                                                   jint audio_sample_rate,
                                                                   jlong pts) {
    auto encoder = reinterpret_cast<SoftEncoderContext *> (native_context);
    auto *c_array = reinterpret_cast<jbyte *>(env->GetDirectBufferAddress(
            data));
    jlong len_arr = env->GetDirectBufferCapacity(data);
    AudioFrame audioFrame ;
    audioFrame.dataSize = len_arr;
    audioFrame.data = reinterpret_cast<uint8_t *>(c_array);
    audioFrame.out_sample_fmt = AVSampleFormat(sample_fmt);
    audioFrame.audioSampleRate = audio_sample_rate;
    audioFrame.setChannel(audio_channel_count);
    audioFrame.pts = pts;
    encoder->softEncoder->onFrame(audioFrame);

}

extern "C"
JNIEXPORT void JNICALL
Java_com_hapi_avencoderext_ffmpeg_FfmpegEncoder_nativeOnVideoFrame(JNIEnv *env, jobject thiz,
                                                                   jlong native_context,
                                                                   jint format,
                                                                   jobject data, jint width,
                                                                   jint height,
                                                                   jlong frame_timestamp,
                                                                   jlong pts) {
    auto encoder = reinterpret_cast<SoftEncoderContext *> (native_context);

    auto *c_array = reinterpret_cast<jbyte *>(env->GetDirectBufferAddress(data));
    jlong len_arr = env->GetDirectBufferCapacity(data);
    VideoFrame videoFrame;
    videoFrame.dataSize = len_arr;
    videoFrame.data = reinterpret_cast<uint8_t *>(c_array);
    videoFrame.width = width;
    videoFrame.height = height;
    videoFrame.format = (format);
    videoFrame.frameTimestamp = frame_timestamp;
    videoFrame.pts = pts;
    encoder->softEncoder->onFrame(videoFrame);
}
extern "C"
JNIEXPORT jobject JNICALL
Java_com_hapi_avencoderext_ffmpeg_FfmpegEncoder_nativeAllocateAVFrameBuffer(JNIEnv *env,
                                                                            jobject thiz,
                                                                            jlong native_context,
                                                                            jint size) {
    auto encoder = reinterpret_cast<SoftEncoderContext *> (native_context);
    uint8_t *data = nullptr;
    encoder->softEncoder->allocateAVFrameBuffer(&data, size);
    return env->NewDirectByteBuffer(data, size);
}


extern "C"
JNIEXPORT void JNICALL
Java_com_hapi_avencoderext_ffmpeg_FfmpegEncoder_nativeConfigureVideo(JNIEnv *env, jobject thiz,
                                                                     jlong native_context,
                                                                     jint frame_width,
                                                                     jint frame_height,
                                                                     jint video_bit_rate,
                                                                     jint min_video_bit_rate,
                                                                     jint max_video_bit_rate,
                                                                     jint fps,
                                                                     jint thread_count) {
    auto encoder = reinterpret_cast<SoftEncoderContext *> (native_context);
    EncodeParam encodeParam;
    //video
    encodeParam.frameWidth = frame_width;
    encodeParam.frameHeight = frame_height;
    encodeParam.videoBitRate = video_bit_rate;
    encodeParam.videoMinBitRate = min_video_bit_rate;
    encodeParam.videoMaxBitRate = max_video_bit_rate;

    encodeParam.fps = fps;
    encodeParam.format = IMAGE_FORMAT_I420;
    encodeParam.threadCount = thread_count;
    encodeParam.hasVideo = false;
    encoder->softEncoder->configure(encodeParam);
}
extern "C"
JNIEXPORT void JNICALL
Java_com_hapi_avencoderext_ffmpeg_FfmpegEncoder_nativeConfigureAudio(JNIEnv *env, jobject thiz,
                                                                     jlong native_context,
                                                                     jint out_sample_fmt,
                                                                     jint audio_channel_count,
                                                                     jint audio_sample_rate,
                                                                     jint audio_bitrate,
                                                                     jint thread_count) {
    auto encoder = reinterpret_cast<SoftEncoderContext *> (native_context);
    EncodeParam encodeParam;
    encodeParam.hasVideo = true;
    encodeParam.out_sample_fmt = AV_SAMPLE_FMT_FLT;
    encodeParam.setChannel(audio_channel_count);
    encodeParam.audioSampleRate = audio_sample_rate;
    encodeParam.audioBitrate = audio_bitrate;
    encodeParam.hasAudio = true;
    encoder->softEncoder->configure(encodeParam);

}