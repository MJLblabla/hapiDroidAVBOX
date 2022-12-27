package com.hapi.avparam

enum class AVImgFmt(val fmt: Int) {
    IMAGE_FORMAT_RGBA(0x01),
    IMAGE_FORMAT_NV21(0x02),
    IMAGE_FORMAT_NV12(0x03),
    IMAGE_FORMAT_I420(0x04),
    IMAGE_FORMAT_TEXTURE_OES(0x05),
    IMAGE_FORMAT_TEXTURE_2D(0x06),
}
