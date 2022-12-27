package com.hapi.avcapture

import com.hapi.avparam.AVImgFmt
import java.nio.ByteBuffer

class VideoFrame(
    var width: Int,
    var height: Int,
    var rotationDegrees: Int = 0,
    var buffer: FrameBuffer
) {
    var timestamp: Long = 0L

    interface FrameBuffer {
        fun imgFmt(): AVImgFmt
    }

    class ImgBuffer(val data: ByteBuffer, private val mAVImgFmt: AVImgFmt) : FrameBuffer {
        //像素步幅
        var pixelStride: Int = 4

        //行填充数据（得到的数据宽度大于指定的width,多出来的数据就是填充数据）
        var rowPadding: Int = 0

        //行距
        var rowStride: Int = 0

        override fun imgFmt(): AVImgFmt {
            return mAVImgFmt
        }
    }

    class TextureBuffer(
        val textureID: Int,
        var f16Matrix: FloatArray,
        private val mAVImgFmt: AVImgFmt
    ) : FrameBuffer {
        override fun imgFmt(): AVImgFmt {
            return mAVImgFmt
        }
    }

}