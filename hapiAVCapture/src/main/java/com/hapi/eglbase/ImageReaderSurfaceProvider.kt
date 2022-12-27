package com.hapi.eglbase

import android.annotation.SuppressLint
import android.graphics.ImageFormat
import android.graphics.PixelFormat
import android.media.Image
import android.media.ImageReader
import android.view.Surface
import com.hapi.avcapture.VideoFrame
import com.hapi.avparam.AVImgFmt
import java.nio.ByteBuffer

class ImageReaderSurfaceProvider(private val pixelFormat: Int = PixelFormat.RGBA_8888) :
    SurfaceProvider {

    private var imageReader: ImageReader? = null
    private var imgBuffer: ByteBuffer? = null
    private var yuvByteArray: ByteArray? = null
    var surface: Surface? = null

    @SuppressLint("WrongConstant")
    override fun createSurface(w: Int, h: Int): Surface {
        imageReader = ImageReader.newInstance(w, h, pixelFormat, 5)
        surface = imageReader!!.surface
        return surface!!
    }

    override fun setOnFrameAvailableListener(call: (frame: VideoFrame.FrameBuffer, timestamp: Long) -> Unit) {
        imageReader?.setOnImageAvailableListener({ reader ->
            val image = reader!!.acquireNextImage()

            val width = image.width
            val height = image.height
            val planes = image.planes
            val timestamp = image.timestamp
            if (image.format == PixelFormat.RGBA_8888) {
                val rowStride = planes[0].rowStride
                val pixelStride = planes[0].pixelStride
                //计算padding
                val rowPadding = rowStride - pixelStride * width
                if (null == imgBuffer) {
                    imgBuffer = ByteBuffer.allocateDirect(width * height * 4)
                }
                imgBuffer?.clear()
                //把数据从buffer拷贝到data
                copyToByteArray(planes[0].buffer, width, height, rowStride, rowPadding)
                imgBuffer?.flip()
                image?.close()
                call.invoke(
                    VideoFrame.ImgBuffer(
                        imgBuffer!!, AVImgFmt.IMAGE_FORMAT_RGBA
                    ).apply {
                        this.pixelStride = pixelStride
                        this.rowPadding = 0
                        this.rowStride = rowStride
                    }, timestamp
                )
                return@setOnImageAvailableListener
            }
            if (image.format == ImageFormat.YUV_420_888) {
                if (imgBuffer == null) {
                    imgBuffer = ByteBuffer.allocateDirect(width * height * 3 / 2)
                    yuvByteArray = ByteArray(width * height * 3 / 2)
                }
                imgBuffer!!.clear()
                readYuvDataToBuffer(image, yuvByteArray!!)
                imgBuffer!!.put(yuvByteArray!!)

                imgBuffer!!.flip()
                image?.close()
                call.invoke(
                    VideoFrame.ImgBuffer(
                        imgBuffer!!, AVImgFmt.IMAGE_FORMAT_I420
                    ), timestamp
                )
            }

        }, null)
    }

    override fun release() {
        imageReader?.close()
    }

    /**
     * take YUV data from image, output data format-> YYYYYYYYUUVV
     */
    private fun readYuvDataToBuffer(image: Image, data: ByteArray): Boolean {
        if (image.format != ImageFormat.YUV_420_888) {
            throw IllegalArgumentException("only support ImageFormat.YUV_420_888 for mow")
        }

        val imageWidth = image.width
        val imageHeight = image.height
        val planes = image.planes
        var offset = 0
        for (plane in planes.indices) {
            val buffer = planes[plane].buffer ?: return false
            val rowStride = planes[plane].rowStride
            val pixelStride = planes[plane].pixelStride
            val planeWidth = if (plane == 0) imageWidth else imageWidth / 2
            val planeHeight = if (plane == 0) imageHeight else imageHeight / 2
            if (pixelStride == 1 && rowStride == planeWidth) {
                buffer.get(data, offset, planeWidth * planeHeight)
                offset += planeWidth * planeHeight
            } else {
                // Copy pixels one by one respecting pixelStride and rowStride
                val rowData = ByteArray(rowStride)
                var colOffset: Int
                for (row in 0 until planeHeight - 1) {
                    colOffset = 0
                    buffer.get(rowData, 0, rowStride)
                    for (col in 0 until planeWidth) {
                        data[offset++] = rowData[colOffset]
                        colOffset += pixelStride
                    }
                }
                // Last row is special in some devices:
                // may not contain the full |rowStride| bytes of data
                colOffset = 0
                buffer.get(rowData, 0, Math.min(rowStride, buffer.remaining()))
                for (col in 0 until planeWidth) {
                    data[offset++] = rowData[colOffset]
                    colOffset += pixelStride
                }
            }
        }
        return true
    }


    private fun copyToByteArray(
        buffer: ByteBuffer,
        width: Int,
        height: Int,
        rowStride: Int,
        rowPadding: Int
    ) {

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
    }
}