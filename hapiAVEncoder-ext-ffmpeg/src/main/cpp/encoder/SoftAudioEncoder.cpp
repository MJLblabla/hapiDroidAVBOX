//
// Created by 满家乐 on 2022/11/30.
//

#include "SoftAudioEncoder.h"

void SoftAudioEncoder::configure(EncodeParam &encodeParam) {
    IEncoder::configure(encodeParam);
    clear();
    mAVCodec = avcodec_find_encoder(AV_CODEC_ID_AAC);
    int ret = openCodec();
    if (ret < 0) {
        LOGCATE("SoftAudioEncoder::OpenVideo Could not open AUDIO codec: %s", av_err2str(ret));
    }
    mAVFrame = av_frame_alloc();
    mAVFrame->nb_samples = mAVCodecCtx->frame_size;
    mAVFrame->format = mAVCodecCtx->sample_fmt;

    mFrameBufferSize = av_samples_get_buffer_size(nullptr, mAVCodecCtx->channels,
                                                  mAVCodecCtx->frame_size,
                                                  mAVCodecCtx->sample_fmt, 1);
    mFrameBuffer = (uint8_t *) av_malloc(mFrameBufferSize);
    avcodec_fill_audio_frame(mAVFrame, mAVCodecCtx->channels, mAVCodecCtx->sample_fmt,
                             (const uint8_t *) mFrameBuffer, mFrameBufferSize, 1);

    ret = av_frame_get_buffer(mAVFrame, 1);
    if (ret < 0) {
        LOGCATE("SoftAudioEncoder::AllocVideoFrame Could not allocate frame data.");
    }
}

void SoftAudioEncoder::startOpenCodec() {
    if (mAVCodecCtx == nullptr) {
        openCodec();
    }
}

int SoftAudioEncoder::openCodec() {
    mAVCodecCtx = avcodec_alloc_context3(mAVCodec);
    mAVCodecCtx->flags |= AV_CODEC_FLAG_GLOBAL_HEADER;
    mAVCodecCtx->sample_fmt = param.out_sample_fmt;
    mAVCodecCtx->sample_rate = param.audioSampleRate;
    mAVCodecCtx->channel_layout = param.audioChannelLayout;
    mAVCodecCtx->channels = param.getChannelCount();
    mAVCodecCtx->bit_rate = param.audioBitrate;
    if (param.threadCount > 1) {
        mAVCodecCtx->thread_count = param.threadCount;
    }
    int ret = 0;
    ret = avcodec_open2(mAVCodecCtx, mAVCodec, nullptr);
    return ret;
}

void SoftAudioEncoder::encodeFrame(Frame *frame) {
    if (frame != nullptr) {
        mAVFrame->data[0] = frame->data;
        mAVFrame->pts = frame->pts;
        //  relativelyPts += mAVFrame->nb_samples;
        int ret = avcodec_send_frame(mAVCodecCtx, mAVFrame);
        if (mAVPacket == nullptr) {
            av_new_packet(mAVPacket, mFrameBufferSize);
        }
    } else {
        avcodec_send_frame(mAVCodecCtx, nullptr);
    }

    int ret = 0;
    while (ret == 0) {
        ret = avcodec_receive_packet(mAVCodecCtx, mAVPacket);
        if (ret == AVERROR(EAGAIN) || ret == AVERROR_EOF) {
            goto EXIT;
        } else if (ret < 0) {
            LOGCATE("SoftAudioEncoder::EncodeVideoFrame video avcodec_receive_packet fail. ret=%s",
                    av_err2str(ret));
            goto EXIT;
        }
        // int64_t pts = av_rescale_q(mAVPacket->pts, mAVCodecCtx->time_base, AV_TIME_BASE_Q);
        if (outPutCallFunc != nullptr) {
            outPutCallFunc(mAVPacket, mAVCodecCtx);
        }
    }
    EXIT:
    if (mAVPacket != nullptr) {
        av_packet_unref(mAVPacket);
    };
    //  av_new_packet(&m_avPacket, m_frameBufferSize);
}

void SoftAudioEncoder::clear() {
    if (mAVCodecCtx != nullptr) {
        avcodec_free_context(&mAVCodecCtx);
        mAVCodecCtx = nullptr;
    }
    if (mAVFrame != nullptr) {
        av_frame_free(&mAVFrame);
        mAVFrame = nullptr;
    }
    if (mAVPacket != nullptr) {
        av_packet_free(&mAVPacket);
    }
    if (mFrameBuffer != nullptr) {
        delete mFrameBuffer;
    }
    mFrameBuffer = nullptr;
    mAVCodec = nullptr;
}

void SoftAudioEncoder::stopFlush() {
    encodeFrame(nullptr);
    if (mAVCodecCtx != nullptr) {
        avcodec_free_context(&mAVCodecCtx);
        mAVCodecCtx = nullptr;
    }
}

void SoftAudioEncoder::updateBitRate(int bitRate) {

}

SoftAudioEncoder::~SoftAudioEncoder() {
    outPutCallFunc = nullptr;
    clear();
}