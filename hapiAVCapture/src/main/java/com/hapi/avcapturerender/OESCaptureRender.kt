package com.hapi.avcapturerender

import android.content.Context
import android.opengl.GLES11Ext
import android.opengl.GLES20
import android.opengl.GLES30
import com.hapi.avparam.AVLog
import com.hapi.eglbase.ResReadUtils
import com.hapi.eglbase.ShaderUtils
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import java.nio.ShortBuffer

class OESCaptureRender(val context: Context) {

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

    val TEX_VERTEX_OES = floatArrayOf(
        0.5f, 0.5f, //纹理坐标V0
        1f, 1f,     //纹理坐标V1
        0f, 1f,     //纹理坐标V2
        0f, 0f,   //纹理坐标V3
        1f, 0f //纹理坐标V4
    )
    private val mTexVertexOESBuffer: FloatBuffer by lazy {
        ByteBuffer.allocateDirect(TEX_VERTEX_OES.size * 4).order(ByteOrder.nativeOrder())
            .asFloatBuffer().put(TEX_VERTEX_OES).apply {
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
    private val vboIds = IntArray(2)
    private val vaoIds = IntArray(1)

    /**
     * 矩阵索引
     */
    private var uTextureMatrixLocation = 0

    //oes
    private var uTextureSamplerLocation = 0

    private var isCreate = false
    private fun onSurfaceCreated() {
        val vertexShaderId = ShaderUtils.compileVertexShader(
            ResReadUtils.readResource(
                context, "hapiavcapture_vertex_shader.glsl"
            )
        )
        val fragmentShaderId = ShaderUtils.compileFragmentShader(
            ResReadUtils.readResource(
                context, "hapiavcapture_oes_fragment_shader.glsl"
            )
        )
        //链接程序片段
        mProgram = ShaderUtils.linkProgram(vertexShaderId, fragmentShaderId)
        uTextureMatrixLocation = GLES30.glGetUniformLocation(mProgram, "uTextureMatrix")
        uTextureSamplerLocation = GLES30.glGetUniformLocation(mProgram, "yuvTexSampler")
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
            TEX_VERTEX_OES.size * 4,
            mTexVertexOESBuffer,
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

    fun onDrawFrame(imgFrameWrap: ImgFrameWrap) {
        if (!isCreate) {
            onSurfaceCreated()
        }
        GLES30.glClear(GLES30.GL_COLOR_BUFFER_BIT)
        //使用程序片段
        GLES30.glUseProgram(mProgram)
        //5. 绑定VAO
        GLES30.glBindVertexArray(vaoIds[0])
        //激活纹理单元0
        GLES30.glActiveTexture(GLES30.GL_TEXTURE0)
        //绑定外部纹理到纹理单元0
        GLES30.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, imgFrameWrap.textureID)
        //oes纹理
        //将此纹理单元床位片段着色器的uTextureSampler外部纹理采样器
        GLES30.glUniform1i(uTextureSamplerLocation, 0)

        //将纹理矩阵传给片段着色器
        GLES30.glUniformMatrix4fv(uTextureMatrixLocation, 1, false, imgFrameWrap.f16Matrix, 0)

        val error = GLES30.glGetError()
        GLES30.glDrawElements(
            GLES20.GL_TRIANGLES,
            VERTEX_INDEX.size,
            GLES20.GL_UNSIGNED_SHORT,
            mVertexIndexBuffer
        )
        //. 解绑VAO
        GLES30.glBindVertexArray(0)
        GLES30.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, 0)

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