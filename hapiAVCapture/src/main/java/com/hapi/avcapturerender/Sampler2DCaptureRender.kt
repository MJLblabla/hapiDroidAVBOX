package com.hapi.avcapturerender

import android.content.Context
import android.opengl.GLES20
import android.opengl.GLES30
import com.hapi.avparam.AVLog
import com.hapi.eglbase.ResReadUtils
import com.hapi.eglbase.ShaderUtils
import com.hapi.avparam.AVImgFmt
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import java.nio.ShortBuffer

class Sampler2DCaptureRender(val context: Context) {

    val POSITION_VERTEX = floatArrayOf(
        0f, 0f, 0f,     //顶点坐标V0
        1f, 1f, 0f,     //顶点坐标V1
        -1f, 1f, 0f,    //顶点坐标V2
        -1f, -1f, 0f,   //顶点坐标V3
        1f, -1f, 0f //顶点坐标V4
        //顶点坐标V4
    )
    private val vertexBuffer by lazy {
        ByteBuffer.allocateDirect(POSITION_VERTEX.size * 4).order(ByteOrder.nativeOrder())
            .asFloatBuffer().apply {
                //分配内存空间,每个浮点型占4字节空间
                //传入指定的坐标数据
                put(POSITION_VERTEX)
                position(0)
            }
    }

    /**
     * Tex Vertex
     * (0,0)          (1,0)
     * ----------------
     * ｜
     * ｜
     * ｜(0,1)         (1,1)
     */
    val TEX_VERTEX = floatArrayOf(
        0.5f, 0.5f, //纹理坐标V0
        1f, 0f,     //纹理坐标V1
        0f, 0f,     //纹理坐标V2
        0f, 1f,   //纹理坐标V3
        1f, 1f //纹理坐标V4
    )
    private val mTexVertexBuffer: FloatBuffer by lazy {
        ByteBuffer.allocateDirect(TEX_VERTEX.size * 4).order(ByteOrder.nativeOrder())
            .asFloatBuffer().put(TEX_VERTEX).apply {
                position(0)
            }
    }

    private val VERTEX_INDEX = shortArrayOf(
        0, 1, 2,  //V0,V1,V2 三个顶点组成一个三角形
        0, 2, 3,  //V0,V2,V3 三个顶点组成一个三角形
        0, 3, 4,  //V0,V3,V4 三个顶点组成一个三角形
        0, 4, 1 //V0,V4,V1 三个顶点组成一个三角形
    )

    private val mVertexIndexBuffer: ShortBuffer by lazy {
        ByteBuffer.allocateDirect(VERTEX_INDEX.size * 2).order(ByteOrder.nativeOrder())
            .asShortBuffer().put(VERTEX_INDEX).apply {
                position(0)
            }
    }
    private var mProgram = 0

    /**
     * 矩阵索引
     */
    private var uTextureMatrixLocation = 0

    private var isCreate = false

    //2d
    private var uTexture0SamplerLocation = 0
    private var uTexture1SamplerLocation = 0
    private var uTexture2SamplerLocation = 0
    private var uImgTypeLocation = 0

    //2d 纹理
    private val sampler2DTextureIds = IntArray(3)
    private val vboIds = IntArray(2)
    private val vaoIds = IntArray(1)
    private fun onSurfaceCreated() {
        val vertexShaderId = ShaderUtils.compileVertexShader(
            ResReadUtils.readResource(context, "hapiavcapture_vertex_shader.glsl")
        )
        val fragmentShaderId = ShaderUtils.compileFragmentShader(
            ResReadUtils.readResource(context, "hapiavcapture_2dfragment_shader.glsl")
        )
        //链接程序片段
        mProgram = ShaderUtils.linkProgram(vertexShaderId, fragmentShaderId)
        uTextureMatrixLocation = GLES30.glGetUniformLocation(mProgram, "uTextureMatrix")

        uImgTypeLocation = GLES30.glGetUniformLocation(mProgram, "u_nImgType")
        uTexture0SamplerLocation = GLES30.glGetUniformLocation(mProgram, "s_texture0")
        uTexture1SamplerLocation = GLES30.glGetUniformLocation(mProgram, "s_texture1")
        uTexture2SamplerLocation = GLES30.glGetUniformLocation(mProgram, "s_texture2")
        var error = GLES30.glGetError()
        checkCreate2DTextureIds()
        error = GLES30.glGetError()
        isCreate = true

        //1. 生成1个缓冲ID
        GLES30.glGenBuffers(2, vboIds, 0)
        GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, vboIds[0])
        GLES30.glBufferData(
            GLES30.GL_ARRAY_BUFFER,
            POSITION_VERTEX.size * 4,
            vertexBuffer,
            GLES30.GL_STATIC_DRAW
        )
        GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, vboIds[1])
        GLES30.glBufferData(
            GLES30.GL_ARRAY_BUFFER,
            TEX_VERTEX.size * 4,
            mTexVertexBuffer,
            GLES30.GL_STATIC_DRAW
        )

        //生成1个缓冲ID
        GLES30.glGenVertexArrays(1, vaoIds, 0);

        //绑定VAO
        GLES30.glBindVertexArray(vaoIds[0])

        GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, vboIds[0])
        // 将顶点位置数据送入渲染管线
        GLES30.glVertexAttribPointer(0, 3, GLES30.GL_FLOAT, false, 3 * 4, 0)
        //启用顶点位置属性
        GLES30.glEnableVertexAttribArray(0)
        GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, GLES30.GL_NONE)

        GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, vboIds[1])
        GLES30.glVertexAttribPointer(1, 2, GLES30.GL_FLOAT, false, 2 * 4, 0)
        //启用顶点位置属性
        GLES30.glEnableVertexAttribArray(1)
        GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, GLES30.GL_NONE)

        //4. 解绑VAO
        GLES30.glBindVertexArray(GLES30.GL_NONE)
    }

    private fun checkCreate2DTextureIds() {
        GLES30.glGenTextures(3, sampler2DTextureIds, 0)
        for (i in 0 until 3) {
            GLES30.glActiveTexture(GLES30.GL_TEXTURE0 + i)
            GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, sampler2DTextureIds[i])
            GLES30.glTexParameterf(
                GLES30.GL_TEXTURE_2D,
                GLES30.GL_TEXTURE_WRAP_S,
                GLES30.GL_REPEAT.toFloat()
            )
            GLES30.glTexParameterf(
                GLES30.GL_TEXTURE_2D,
                GLES30.GL_TEXTURE_WRAP_T,
                GLES30.GL_REPEAT.toFloat()
            )

            GLES30.glTexParameteri(
                GLES30.GL_TEXTURE_2D,
                GLES30.GL_TEXTURE_MIN_FILTER,
                GLES30.GL_LINEAR
            )
            GLES30.glTexParameteri(
                GLES30.GL_TEXTURE_2D,
                GLES30.GL_TEXTURE_MAG_FILTER,
                GLES30.GL_LINEAR
            )
            GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, GLES20.GL_NONE)
        }
    }

    fun onDrawFrame(imgFrameWrap: ImgFrameWrap) {
        if (!isCreate) {
            onSurfaceCreated()
        }
        var error = GLES30.glGetError()
        GLES30.glClear(GLES30.GL_COLOR_BUFFER_BIT)
        //使用程序片段
        GLES30.glUseProgram(mProgram)

        //5. 绑定VAO
        GLES30.glBindVertexArray(vaoIds[0])
        //mImgFrameWrap.parse(pendingRenderFrame)
        when {
            imgFrameWrap.avImgFmt == AVImgFmt.IMAGE_FORMAT_RGBA -> {
                GLES30.glActiveTexture(GLES30.GL_TEXTURE0)
                GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, sampler2DTextureIds[0])
                GLES30.glTexImage2D(
                    GLES30.GL_TEXTURE_2D,
                    0,
                    GLES30.GL_RGBA,
                    imgFrameWrap.width,
                    imgFrameWrap.height,
                    0,
                    GLES30.GL_RGBA,
                    GLES30.GL_UNSIGNED_BYTE,
                    imgFrameWrap.dataPP0!!
                )
                GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, GLES20.GL_NONE)
                // Bind the RGBA map
                GLES30.glActiveTexture(GLES30.GL_TEXTURE0)
                GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, sampler2DTextureIds[0])
                GLES30.glUniform1i(uTexture0SamplerLocation, 0)
            }

            imgFrameWrap.avImgFmt == AVImgFmt.IMAGE_FORMAT_NV21
                    || imgFrameWrap.avImgFmt == AVImgFmt.IMAGE_FORMAT_NV12
            -> {
                GLES30.glActiveTexture(GLES30.GL_TEXTURE0)
                GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, sampler2DTextureIds[0])
                GLES30.glTexImage2D(
                    GLES30.GL_TEXTURE_2D,
                    0,
                    GLES30.GL_LUMINANCE,
                    imgFrameWrap.width,
                    imgFrameWrap.height,
                    0,
                    GLES30.GL_LUMINANCE,
                    GLES30.GL_UNSIGNED_BYTE,
                    imgFrameWrap.dataPP0!!
                )
                GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, GLES20.GL_NONE)

                GLES30.glActiveTexture(GLES30.GL_TEXTURE1)
                GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, sampler2DTextureIds[1])

                GLES30.glTexImage2D(
                    GLES30.GL_TEXTURE_2D,
                    0,
                    GLES30.GL_LUMINANCE_ALPHA,
                    imgFrameWrap.width.shr(1),
                    imgFrameWrap.height.shr(1),
                    0,
                    GLES30.GL_LUMINANCE_ALPHA,
                    GLES30.GL_UNSIGNED_BYTE,
                    imgFrameWrap.dataPP1!!
                )
                GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, GLES20.GL_NONE)

                // Bind the RGBA map
                GLES30.glActiveTexture(GLES30.GL_TEXTURE0)
                GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, sampler2DTextureIds[0])
                GLES30.glUniform1i(uTexture0SamplerLocation, 0)

                // Bind the RGBA map
                GLES30.glActiveTexture(GLES30.GL_TEXTURE1)
                GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, sampler2DTextureIds[1])
                GLES30.glUniform1i(uTexture1SamplerLocation, 1)
            }

            imgFrameWrap.avImgFmt == AVImgFmt.IMAGE_FORMAT_I420 -> {
                GLES30.glActiveTexture(GLES30.GL_TEXTURE0)
                GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, sampler2DTextureIds[0])
                GLES30.glTexImage2D(
                    GLES30.GL_TEXTURE_2D,
                    0,
                    GLES30.GL_LUMINANCE,
                    imgFrameWrap.width,
                    imgFrameWrap.height,
                    0,
                    GLES30.GL_LUMINANCE,
                    GLES30.GL_UNSIGNED_BYTE,
                    imgFrameWrap.dataPP0!!
                )
                GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, GLES20.GL_NONE)

                GLES30.glActiveTexture(GLES30.GL_TEXTURE1)
                GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, sampler2DTextureIds[1])
                GLES30.glTexImage2D(
                    GLES30.GL_TEXTURE_2D,
                    0,
                    GLES30.GL_LUMINANCE,
                    imgFrameWrap.width.shr(1),
                    imgFrameWrap.height.shr(1),
                    0,
                    GLES30.GL_LUMINANCE,
                    GLES30.GL_UNSIGNED_BYTE,
                    (imgFrameWrap.dataPP1!!)
                )
                GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, GLES20.GL_NONE)

                GLES30.glActiveTexture(GLES30.GL_TEXTURE2)
                GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, sampler2DTextureIds[2])
                GLES30.glTexImage2D(
                    GLES30.GL_TEXTURE_2D,
                    0,
                    GLES30.GL_LUMINANCE,
                    imgFrameWrap.width.shr(1),
                    imgFrameWrap.height.shr(1),
                    0,
                    GLES30.GL_LUMINANCE,
                    GLES30.GL_UNSIGNED_BYTE,
                    (imgFrameWrap.dataPP2!!)
                )
                GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, GLES20.GL_NONE)

                // Bind the RGBA map
                GLES30.glActiveTexture(GLES30.GL_TEXTURE0)
                GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, sampler2DTextureIds[0])
                GLES30.glUniform1i(uTexture0SamplerLocation, 0)

                // Bind the RGBA map
                GLES30.glActiveTexture(GLES30.GL_TEXTURE1)
                GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, sampler2DTextureIds[1])
                GLES30.glUniform1i(uTexture1SamplerLocation, 1)

                GLES30.glActiveTexture(GLES30.GL_TEXTURE2)
                GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, sampler2DTextureIds[2])
                GLES30.glUniform1i(uTexture2SamplerLocation, 2)
            }

            imgFrameWrap.avImgFmt == AVImgFmt.IMAGE_FORMAT_TEXTURE_2D -> {
                // Bind the RGBA map
                //激活纹理单元0
                GLES30.glActiveTexture(GLES30.GL_TEXTURE0)
                //绑定外部纹理到纹理单元0
                GLES30.glBindTexture(
                    GLES30.GL_TEXTURE_2D,
                    imgFrameWrap.textureID
                )
                //将此纹理单元床位片段着色器的uTextureSampler外部纹理采样器
                GLES30.glUniform1i(uTexture0SamplerLocation, 0)
            }
        }
        error = GLES30.glGetError()
        //将纹理矩阵传给片段着色器
        GLES30.glUniformMatrix4fv(
            uTextureMatrixLocation,
            1,
            false,
            imgFrameWrap.f16Matrix,
            0
        )
        error = GLES30.glGetError()
//        //定点坐标
//        GLES30.glEnableVertexAttribArray(0)
//        GLES30.glVertexAttribPointer(0, 3, GLES30.GL_FLOAT, false, 0, vertexBuffer)
//        //纹理坐标
//        GLES30.glEnableVertexAttribArray(1)
//        GLES30.glVertexAttribPointer(1, 2, GLES30.GL_FLOAT, false, 0, mTexVertexBuffer)

        //设置图片格式
        GLES30.glUniform1i(uImgTypeLocation, imgFrameWrap.avImgFmt!!.fmt)
        error = GLES30.glGetError()
        GLES30.glDrawElements(
            GLES20.GL_TRIANGLES,
            VERTEX_INDEX.size,
            GLES20.GL_UNSIGNED_SHORT,
            mVertexIndexBuffer
        )
        GLES30.glBindVertexArray(0)

        error = GLES30.glGetError()
        if (GLES30.GL_NO_ERROR != error) {
            AVLog.d("OESCaptureRender", "GLES30.glGetError " + error)
        }
        GLES30.glFinish()
    }

    fun release() {
        GLES30.glDeleteBuffers(2, vboIds, 0)
        GLES30.glDeleteVertexArrays(1, vaoIds, 0)
    }
}