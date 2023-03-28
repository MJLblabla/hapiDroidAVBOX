//
// Created by 1 on 2022/4/6.
//

#include "AVResample.h"
#include "LogUtil.h"

AVResample::AVResample() {

}

AVResample::~AVResample() {
    if (m_pSwrCtx) {
        swr_free(&m_pSwrCtx);
    }
    m_pSwrCtx = nullptr;
    if (src_yuv_temp == nullptr) {
        free(src_yuv_temp);
    }
    src_yuv_temp = nullptr;

    if (rotate_yuv_temp == nullptr) {
        free(rotate_yuv_temp);
    }
    rotate_yuv_temp = nullptr;

    if (scale_yuv_temp == nullptr) {
        free(scale_yuv_temp);
    }
    scale_yuv_temp = nullptr;

    if (inData == nullptr) {
        free(inData);
    }
    inData = nullptr;

    if (outData == nullptr) {
        free(outData);
    }
    outData = nullptr;
}

int AVResample::start(const EncodeParam &param) {
    encodeParam = param;
    return 1;
}

void AVResample::onFrame2Encode(AudioFrame *inputFrame, AudioFrame *resizeAudioFrame) {

    resizeAudioFrame->out_sample_fmt = encodeParam.out_sample_fmt;
    resizeAudioFrame->audioChannelLayout = encodeParam.audioChannelLayout;
    resizeAudioFrame->audioSampleRate = encodeParam.audioSampleRate;
    resizeAudioFrame->dataSize = inputFrame->dataSize;
    // 返回结果
    int ret = 0;

    if (m_pSwrCtx == nullptr) {
        m_pSwrCtx = swr_alloc();
        m_pSwrCtx = swr_alloc_set_opts(m_pSwrCtx,
                                       encodeParam.audioChannelLayout,
                                       encodeParam.out_sample_fmt,
                                       encodeParam.audioSampleRate,

                                       inputFrame->audioChannelLayout,
                                       inputFrame->out_sample_fmt,
                                       inputFrame->audioSampleRate,
                                       0,
                                       0);//输入格式
        swr_init(m_pSwrCtx);

        // 缓冲区大小
        int inLineSize = 0;
        // 声道数
        int inChs = inputFrame->getChannelCount();
        // 每个样本的大小
        int inBytesPerSample = inChs * av_get_bytes_per_sample(inputFrame->out_sample_fmt);
        // 输入缓冲区样本数量
        inSamples = inputFrame->dataSize / inBytesPerSample;

        // 缓冲区大小
        int outLineSize = 0;
        // 声道数
        int outChs = resizeAudioFrame->getChannelCount();
        // 每个样本的大小
        outBytesPerSample = outChs * av_get_bytes_per_sample(resizeAudioFrame->out_sample_fmt);
        // 输出缓冲区样本数量
        outSamples = av_rescale_rnd(
                resizeAudioFrame->audioSampleRate,
                inSamples,
                inputFrame->audioSampleRate,
                AV_ROUND_UP);
        //创建输入缓冲区：
        ret = av_samples_alloc_array_and_samples(
                &inData,
                &inLineSize,
                inChs,
                inSamples,
                inputFrame->out_sample_fmt,
                0);
        if (ret < 0) {
            LOGCATE(" av_samples_alloc_array_and_samples EORRO ");
        }
        //创建输出缓冲区：
        ret = av_samples_alloc_array_and_samples(
                &outData,
                &outLineSize,
                outChs,
                outSamples,
                resizeAudioFrame->out_sample_fmt,
                0);
        if (ret < 0) {
            LOGCATE(" av_samples_alloc_array_and_samples EORRO ");
        }
    }

    memcpy(*inData, inputFrame->data, inputFrame->dataSize);
    ret = swr_convert(m_pSwrCtx,
                      outData,
                      outSamples,
                      (const uint8_t **) inData,
                      inSamples);
    resizeAudioFrame->data = *outData;
    if (ret < 0) {
        LOGCATE(" swr_convert EORRO ");
    }
    resizeAudioFrame->dataSize = outSamples * outBytesPerSample * encodeParam.getChannelCount();

}

int AVResample::getOutAudioFrameSize(AudioFrame *inputFrame) {

    // 缓冲区大小
    int inLineSize = 0;
    // 声道数
    int inChs = inputFrame->getChannelCount();
    // 每个样本的大小
    int inBytesPerSample = inChs * av_get_bytes_per_sample(inputFrame->out_sample_fmt);
    // 输入缓冲区样本数量
    int64_t inSamples = inputFrame->dataSize / inBytesPerSample;

    // 缓冲区大小
    int outLineSize = 0;
    // 声道数
    int outChs = encodeParam.getChannelCount();
    // 每个样本的大小
    int outBytesPerSample = outChs * av_get_bytes_per_sample(encodeParam.out_sample_fmt);
    // 输出缓冲区样本数量
    int outSamples = av_rescale_rnd(
            encodeParam.audioSampleRate,
            inSamples,
            inputFrame->audioSampleRate,
            AV_ROUND_UP);

    return outSamples * outBytesPerSample * encodeParam.getChannelCount();
}

void AVResample::onFrame2Encode(VideoFrame *inputFrame, VideoFrame *resizeInputFrame) {

    auto pBuffer = inputFrame->data;
    int size = inputFrame->dataSize;

    uint8_t *i420;

    //先转换成i420
    int widthsrc = inputFrame->width;
    int heightsrc = inputFrame->height;
    int src_y_size = widthsrc * heightsrc;
    int src_u_size = (widthsrc >> 1) * (heightsrc >> 1);

    if (src_yuv_temp == nullptr && IMAGE_FORMAT_I420 != inputFrame->format) {
        src_yuv_temp = static_cast<uint8_t *>(malloc(widthsrc * heightsrc * 3 / 2));
    }
    switch (inputFrame->format) {
        case IMAGE_FORMAT_I420:
            i420 = pBuffer;
            break;
        case IMAGE_FORMAT_NV12:
            libyuv::NV12ToI420(pBuffer, widthsrc,
                               pBuffer + src_y_size, widthsrc,


                               src_yuv_temp, widthsrc,
                               src_yuv_temp + src_y_size, widthsrc >> 1,
                               src_yuv_temp + src_y_size + src_u_size, widthsrc >> 1,
                               widthsrc, heightsrc);
            i420 = src_yuv_temp;
            break;
        case IMAGE_FORMAT_NV21:
            libyuv::NV21ToI420(pBuffer, widthsrc,
                               pBuffer + src_y_size, widthsrc,
                               src_yuv_temp, widthsrc,
                               src_yuv_temp + src_y_size, widthsrc >> 1,
                               src_yuv_temp + src_y_size + src_u_size, widthsrc >> 1,
                               widthsrc, heightsrc);
            i420 = src_yuv_temp;
            break;
        case IMAGE_FORMAT_RGBA:
            libyuv::ABGRToI420(pBuffer, widthsrc * 4 + inputFrame->rowPadding,
                               src_yuv_temp, widthsrc,
                               src_yuv_temp + src_y_size, widthsrc / 2,
                               src_yuv_temp + src_y_size + src_u_size, widthsrc / 2,
                               widthsrc, heightsrc);
            i420 = src_yuv_temp;
            break;
    }

    uint8_t *i420Rote;
    if (inputFrame->rotationDegrees != 0) {
        if (rotate_yuv_temp == nullptr) {
            rotate_yuv_temp = static_cast<uint8_t *>(malloc(widthsrc * heightsrc * 3 / 2));
        }
        libyuv::RotationMode mode = libyuv::kRotate0;
        int roteW = widthsrc;
        int roteH = heightsrc;
        if (inputFrame->rotationDegrees == 90) {
            mode = libyuv::kRotate90;
            roteH = widthsrc;
            roteW = heightsrc;
        } else if (inputFrame->rotationDegrees == 180) {
            mode = libyuv::kRotate180;
        } else if (inputFrame->rotationDegrees == 270) {
            mode = libyuv::kRotate270;
            roteH = widthsrc;
            roteW = heightsrc;
        }
        libyuv::I420Rotate(
                i420, widthsrc,
                i420 + src_y_size, widthsrc / 2,
                i420 + src_y_size + src_u_size, widthsrc / 2,
                rotate_yuv_temp, roteW,
                rotate_yuv_temp + src_y_size, roteW / 2,
                rotate_yuv_temp + src_y_size + src_u_size, roteW / 2,
                widthsrc, heightsrc, mode
        );
        widthsrc = roteW;
        heightsrc = roteH;
        i420Rote = rotate_yuv_temp;
    } else {
        i420Rote = i420;
    }

    uint8_t *i420Scale;
    if (widthsrc != encodeParam.frameWidth &&
        heightsrc != encodeParam.frameHeight
            ) {

        int widthTarget = encodeParam.frameWidth;
        int heightTarget = encodeParam.frameHeight;
        int tar_y_size = widthTarget * heightTarget;
        int tar_u_size = (widthTarget >> 1) * (heightTarget >> 1);

        if (scale_yuv_temp == nullptr) {
            scale_yuv_temp = static_cast<uint8_t *>(malloc(
                    encodeParam.frameWidth * encodeParam.frameHeight * 3 / 2));
        }
        libyuv::I420Scale(
                i420Rote, widthsrc,
                i420Rote + src_y_size, widthsrc / 2,
                i420Rote + src_y_size + src_u_size, widthsrc / 2,
                widthsrc, heightsrc,

                scale_yuv_temp, widthTarget,
                scale_yuv_temp + tar_y_size, widthTarget / 2,
                scale_yuv_temp + tar_y_size + tar_u_size, widthTarget / 2,
                widthTarget, heightTarget,
                libyuv::kFilterBox
        );
        i420Scale = scale_yuv_temp;

        widthsrc = widthTarget;
        heightsrc = heightTarget;
        src_y_size = widthsrc * heightsrc;
        src_u_size = (widthsrc >> 1) * (heightsrc >> 1);
    } else {
        i420Scale = i420Rote;
    }

    int outSize = encodeParam.frameWidth * encodeParam.frameHeight * 3 / 2;
    switch (encodeParam.format) {
        case IMAGE_FORMAT_I420:
            memcpy(resizeInputFrame->data, i420Scale, outSize);
            break;
        case IMAGE_FORMAT_NV12:
            libyuv::I420ToNV12(i420Scale, widthsrc,
                               i420Scale + src_y_size, widthsrc >> 1,
                               i420Scale + src_y_size + src_u_size, widthsrc >> 1,

                               resizeInputFrame->data, widthsrc,
                               resizeInputFrame->data + src_y_size, widthsrc,
                               widthsrc, heightsrc);

            break;
        case IMAGE_FORMAT_NV21:
            libyuv::I420ToNV21(i420Scale, widthsrc,
                               i420Scale + src_y_size, widthsrc >> 1,
                               i420Scale + src_y_size + src_u_size, widthsrc >> 1,

                               resizeInputFrame->data, widthsrc,
                               resizeInputFrame->data + src_y_size, widthsrc,
                               widthsrc, heightsrc);
            break;
    }

    i420 = nullptr;
    i420Scale = nullptr;
    i420Rote = nullptr;

    resizeInputFrame->dataSize = outSize;
    // resizeInputFrame->format = IMAGE_FORMAT_I420;
    resizeInputFrame->width = encodeParam.frameWidth;
    resizeInputFrame->height = encodeParam.frameHeight;

}
