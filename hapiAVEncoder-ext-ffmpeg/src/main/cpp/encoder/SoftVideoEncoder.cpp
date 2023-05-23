//
// Created by 满家乐 on 2022/11/29.
//

#include "SoftVideoEncoder.h"

void SoftVideoEncoder::configure(EncodeParam &encodeParam) {
    IEncoder::configure(encodeParam);
    clear();
    mAVCodec = avcodec_find_encoder(AV_CODEC_ID_H264);
    int ret = openCodec();
    if (ret < 0) {
        LOGCATE("SoftVideoEncoder::OpenVideo Could not open video codec: %s", av_err2str(ret));
    }
    mAVFrame = av_frame_alloc();
    mAVFrame->format = mAVCodecCtx->pix_fmt;
    mAVFrame->width = mAVCodecCtx->width;
    mAVFrame->height = mAVCodecCtx->height;
    /* allocate the buffers for the frame data */
    ret = av_frame_get_buffer(mAVFrame, 1);
    if (ret < 0) {
        LOGCATE("SoftVideoEncoder::AllocVideoFrame Could not allocate frame data.");
    }
}

int SoftVideoEncoder::openCodec() {
    mAVCodecCtx = avcodec_alloc_context3(mAVCodec);
    mAVCodecCtx->flags |= AV_CODEC_FLAG_GLOBAL_HEADER;
    mAVCodecCtx->codec_type = AVMEDIA_TYPE_VIDEO;
    mAVCodecCtx->codec_id = mAVCodec->id;
    mAVCodecCtx->max_b_frames = 0;

    mAVCodecCtx->bit_rate = param.videoBitRate;
    //VBR
    mAVCodecCtx->flags |= AV_CODEC_FLAG_QSCALE;
    mAVCodecCtx->rc_min_rate = param.videoMinBitRate;
    mAVCodecCtx->rc_max_rate = param.videoMaxBitRate;
    mAVCodecCtx->bit_rate_tolerance = 1;

    mAVCodecCtx->width = param.frameWidth;
    mAVCodecCtx->height = param.frameHeight;
    mAVCodecCtx->time_base = (AVRational) {1, param.fps};
    mAVCodecCtx->gop_size = 15; /* emit one intra frame every twelve frames at most */
    mAVCodecCtx->pix_fmt = AV_PIX_FMT_YUV420P;
    if (param.threadCount > 1) {
        mAVCodecCtx->thread_count = param.threadCount;
    }
    av_opt_set(mAVCodecCtx->priv_data, "preset", "veryfast", 0);
    //  av_opt_set(mAVCodecCtx->priv_data, "tune", "zerolatency", 0);

    int ret = 0;
    ret = avcodec_open2(mAVCodecCtx, mAVCodec, nullptr);
    if (ret < 0) {
        LOGCATE("SoftVideoEncoder::OpenVideo Could not open video codec: %s", av_err2str(ret));
    }
    return ret;
}

void SoftVideoEncoder::startOpenCodec() {
    if (mAVCodecCtx == nullptr) {
        openCodec();
    }
}

void SoftVideoEncoder::encodeFrame(Frame *frame) {
    if (frame) {
        mAVFrame->data[0] = frame->data;
        mAVFrame->data[1] = mAVFrame->data[0] + mAVFrame->width * mAVFrame->height;
        mAVFrame->data[2] = mAVFrame->data[1] + mAVFrame->width / 2 * mAVFrame->height / 2;
        mAVFrame->linesize[0] = mAVFrame->width;
        mAVFrame->linesize[1] = mAVFrame->width / 2;
        mAVFrame->linesize[2] = mAVFrame->width / 2;
        mAVFrame->pts = frame->pts;
        int ret = avcodec_send_frame(mAVCodecCtx, mAVFrame);
        //  LOGCATE("encodeFrame  avcodec_send_frame---pts- %lld  %d", mAVFrame->pts,frameQueue.Size());
    } else {
        avcodec_send_frame(mAVCodecCtx, nullptr);
    }

    if (mAVPacket == nullptr) {
        mAVPacket = av_packet_alloc();
    }
    int ret = 0;
    while (ret == 0) {
        ret = avcodec_receive_packet(mAVCodecCtx, mAVPacket);
        if (ret == AVERROR(EAGAIN) || ret == AVERROR_EOF) {
            goto EXIT;
        } else if (ret < 0) {
            LOGCATE("SoftVideoEncoder::EncodeVideoFrame video avcodec_receive_packet fail. ret=%s",
                    av_err2str(ret));
            goto EXIT;
        }
        //  int64_t outPts = av_rescale_q(mAVPacket->pts, mAVCodecCtx->time_base, AV_TIME_BASE_Q);
        if (outPutCallFunc != nullptr) {
            outPutCallFunc(mAVPacket, mAVCodecCtx);
        }
    }
    EXIT:
    if (mAVPacket != nullptr) {
        av_packet_unref(mAVPacket);
    };
}

void SoftVideoEncoder::stopFlush() {
    encodeFrame(nullptr);
    if (mAVCodecCtx != nullptr) {
        avcodec_free_context(&mAVCodecCtx);
        mAVCodecCtx = nullptr;
    }
}

void SoftVideoEncoder::clear() {
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
    mAVCodec = nullptr;
}

void SoftVideoEncoder::updateBitRate(int bitRate) {

}

SoftVideoEncoder::~SoftVideoEncoder() {
    outPutCallFunc = nullptr;
    clear();
}

