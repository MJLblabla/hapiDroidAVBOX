package com.hapi.eglbase

import android.annotation.SuppressLint
import android.graphics.PixelFormat
import android.media.ImageReader
import android.opengl.GLES20
import android.opengl.GLES30
import android.view.Surface
import com.hapi.avcapture.VideoFrame
import com.hapi.avparam.AVImgFmt
import java.nio.ByteBuffer

class Image2DReaderSurfaceProvider(
    private val offscreenSurfaceHelper: OffscreenSurfaceHelper
) : SurfaceProvider {

    private var imageReader: ImageReader? = null
    private var imgBuffer: ByteBuffer? = null
    private var textureId = -1

    @SuppressLint("WrongConstant")
    override fun createSurface(w: Int, h: Int): Surface {
        imageReader = ImageReader.newInstance(w, h, PixelFormat.RGBA_8888, 5)
        return imageReader!!.surface
    }

    private val f16Matrix: FloatArray = arrayOf<Float>(
        1f, 0f, 0f, 0f,
        0f, 1f, 0f, 0f,
        0f, 0f, 1f, 0f,
        0f, 0f, 0f, 1f,
    ).toFloatArray()

    override fun setOnFrameAvailableListener(call: (frame: VideoFrame.FrameBuffer,timestamp: Long) -> Unit) {
        imageReader?.setOnImageAvailableListener({ reader ->
            val image = reader!!.acquireNextImage()

            val planes = image.planes
            val width = image.width
            val height = image.height
            val rowStride = planes[0].rowStride
            val pixelStride = planes[0].pixelStride
            //计算padding
            val rowPadding = rowStride - pixelStride * width
            //把数据从buffer拷贝到data
            copyToByteArray(planes[0].buffer, width, height, rowStride, rowPadding)
            val timestamp = image.timestamp
            image?.close()
            offscreenSurfaceHelper.runOnGLThread {
                if (textureId == -1) {
                    textureId = offscreenSurfaceHelper.elgCore.create2DTexture()
                }
                GLES30.glActiveTexture(GLES30.GL_TEXTURE0)
                GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, textureId)
                GLES30.glTexImage2D(
                    GLES30.GL_TEXTURE_2D,
                    0,
                    GLES30.GL_RGBA,
                    width,
                    height,
                    0,
                    GLES30.GL_RGBA,
                    GLES30.GL_UNSIGNED_BYTE,
                    imgBuffer
                )
                GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, GLES20.GL_NONE)

                call.invoke(
                    VideoFrame.TextureBuffer(
                        textureId, f16Matrix, AVImgFmt.IMAGE_FORMAT_TEXTURE_2D
                    ),timestamp
                )
            }
        }, null)
    }

    override fun release() {
        imageReader?.close()
        GLES30.glDeleteTextures(1, intArrayOf(textureId), 0)
    }

    private fun copyToByteArray(
        buffer: ByteBuffer,
        width: Int,
        height: Int,
        rowStride: Int,
        rowPadding: Int
    ) {
        if (null == imgBuffer) {
            imgBuffer = ByteBuffer.allocateDirect(width * height * 4)
        }
        imgBuffer?.clear()
        if (rowPadding == 0) {
            imgBuffer?.put(buffer)
        } else {
            for (i in 0 until height) {
                buffer.limit(buffer.capacity())
                val start = i * rowStride
                val end = (i + 1) * rowStride - rowPadding
                buffer.position(start)
                buffer.limit(end)
                imgBuffer?.put(buffer.slice())
            }
            buffer.clear()
        }

        imgBuffer?.flip()
    }
}