package com.hapi.avcapturerender

import android.opengl.Matrix
import com.hapi.avcapture.VideoFrame
import com.hapi.avparam.AVImgFmt
import java.nio.ByteBuffer

class ImgFrameWrap {
    var width: Int = 0
    var height: Int = 0
    var textureID = -2
    var rotationDegrees: Int = 0
    var dataPP0: ByteBuffer? = null
    var dataPP1: ByteBuffer? = null
    var dataPP2: ByteBuffer? = null
    var avImgFmt: AVImgFmt? = null
    lateinit var f16Matrix: FloatArray
    var frameID = -1;

    fun parse(videoFrame: VideoFrame) {
        frameID++
        val buffer = videoFrame.buffer
        f16Matrix = FloatArray(16)
        avImgFmt = videoFrame.buffer.imgFmt()
        this.width = videoFrame.width
        this.height = videoFrame.height
        this.rotationDegrees = videoFrame.rotationDegrees
        if (buffer is VideoFrame.TextureBuffer) {
            f16Matrix = (buffer).f16Matrix
            textureID = buffer.textureID
            return
        }

        val imgBuffer = (buffer as VideoFrame.ImgBuffer).data
        val bufferLimit = imgBuffer.limit()

        do {
            val mRotationMatrix: FloatArray = arrayOf<Float>(
                1f, 0f, 0f, 0f,
                0f, 1f, 0f, 0f,
                0f, 0f, 1f, 0f,
                0f, 0f, 0f, 1f,
            ).toFloatArray()
            Matrix.setRotateM(mRotationMatrix, 0, 360f - rotationDegrees, 0f, 0f, 1.0f)
            f16Matrix = mRotationMatrix

            if ((dataPP0?.capacity() ?: 0) < imgBuffer.limit()) {
                dataPP0 = ByteBuffer.allocate(imgBuffer.limit())
            }
            dataPP0!!.clear()
            dataPP0?.put(imgBuffer)
            dataPP0?.flip()
            if (buffer.imgFmt() == AVImgFmt.IMAGE_FORMAT_RGBA) {
                break
            }

            if (buffer.imgFmt() == AVImgFmt.IMAGE_FORMAT_NV21 || buffer.imgFmt() == AVImgFmt.IMAGE_FORMAT_NV12) {
                val uvStart = videoFrame.width * videoFrame.height
                val uvSize = videoFrame.width * videoFrame.height / 2

                if ((dataPP1?.capacity() ?: 0) < uvSize) {
                    dataPP1 = ByteBuffer.allocate(uvSize)
                }
                imgBuffer.limit(uvSize + uvStart)
                imgBuffer.position(uvStart)

                dataPP1?.clear()
                dataPP1?.put(imgBuffer.slice())
                dataPP1?.flip()
                break
            }

            if (buffer.imgFmt() == AVImgFmt.IMAGE_FORMAT_I420) {
                val uStart = videoFrame.width * videoFrame.height
                val uSize = videoFrame.width * videoFrame.height / 4
                val vStart = uStart + uSize
                val vSize = uSize

                if ((dataPP1?.capacity() ?: 0) < uSize) {
                    dataPP1 = ByteBuffer.allocate(uSize)
                }
                imgBuffer.limit(uStart + uSize)
                imgBuffer.position(uStart)
                dataPP1?.clear()
                dataPP1?.put(imgBuffer.slice())
                dataPP1?.flip()

                if ((dataPP2?.capacity() ?: 0) < vSize) {
                    dataPP2 = ByteBuffer.allocate(vSize)
                }
                imgBuffer.limit(vSize + vStart)
                imgBuffer.position(vStart)

                dataPP2?.clear()
                dataPP2?.put(imgBuffer.slice())
                dataPP2?.flip()

                break
            }
        } while (true)
        imgBuffer.limit(bufferLimit)
        imgBuffer.position(0)
    }
}