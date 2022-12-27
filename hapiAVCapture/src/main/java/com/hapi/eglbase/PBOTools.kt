package com.hapi.eglbase

import android.opengl.GLES20
import android.opengl.GLES30
import java.nio.ByteBuffer


class PBOTools {

    private val pbos = IntArray(2)
    var width: Int = 0
    var height: Int = 0
    fun initPBOs(w: Int, h: Int) {
        this.width = w
        this.height = h
        val size = width * height * 4
        GLES30.glGenBuffers(2, pbos, 0)
        GLES30.glBindBuffer(GLES30.GL_PIXEL_PACK_BUFFER, pbos[0])
        GLES30.glBufferData(GLES30.GL_PIXEL_PACK_BUFFER, size, null, GLES30.GL_STATIC_READ)
        GLES30.glBindBuffer(GLES30.GL_PIXEL_PACK_BUFFER, pbos[1])
        GLES30.glBufferData(GLES30.GL_PIXEL_PACK_BUFFER, size, null, GLES30.GL_STATIC_READ)
        GLES30.glBindBuffer(GLES30.GL_PIXEL_PACK_BUFFER, 0)
    }

    private var index = 0;
    private var nextIndex = 1;
    fun readPixelsFromPBO(srcBuff: ByteBuffer,frameBuffer: Int) {

        //绑定到第一个PBO
        GLES30.glBindBuffer(GLES30.GL_PIXEL_PACK_BUFFER, pbos[index])
        GLES30.glReadPixels(0, 0, width, height, GLES20.GL_RGBA, GLES30.GL_UNSIGNED_BYTE, 0)
        //绑定到第二个PBO
        GLES30.glBindBuffer(GLES30.GL_PIXEL_PACK_BUFFER, pbos[nextIndex])
        //glMapBufferRange会等待DMA传输完成，所以需要交替使用pbo
        //映射内存

        val buffer= GLES30.glMapBufferRange(
            GLES30.GL_PIXEL_PACK_BUFFER,
            0, width * height * 4, GLES30.GL_MAP_READ_BIT
        ) as ByteBuffer
        srcBuff.clear()
        srcBuff.put(buffer)
        srcBuff.flip()
        // PushLog.e("glMapBufferRange: " + GLES30.glGetError());
        //解除映射
        GLES30.glUnmapBuffer(GLES30.GL_PIXEL_PACK_BUFFER)
        //解除绑定PBO
        GLES30.glBindBuffer(GLES30.GL_PIXEL_PACK_BUFFER, GLES30.GL_NONE)

        //交换索引
        index = (index + 1) % 2
        nextIndex = (nextIndex + 1) % 2
    }

    /**
     * 最后记得要释放
     */
    fun releasePBO() {
        GLES30.glDeleteBuffers(pbos.size, pbos, 0)
    }
}