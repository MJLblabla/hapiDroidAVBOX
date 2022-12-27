package com.hapi.avcapture.render

import android.opengl.GLES11Ext
import android.opengl.GLES20
import android.opengl.GLES30
import android.util.Size
import com.hapi.avcapture.VideoFrame
import com.hapi.avparam.AVLog
import com.hapi.avparam.AVImgFmt
import com.hapi.eglbase.FBOTools
import com.hapi.eglbase.ShaderUtils
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import java.nio.ShortBuffer


abstract class ICoverRender {
    protected var outBuff: ByteBuffer? = null
    protected var viewPortSize: Size = Size(0, 0)
    protected var outBufferSize: Size = Size(0, 0)
    private var fboTools = FBOTools()

    private val vboIds = IntArray(2)
    private val vaoIds = IntArray(1)
    protected abstract val fragment2DShader: String
    protected abstract val fragmentOESShader: String
    protected var mProgram = -100
    private var uTextureMatrixLocation = 0
    private var uTextureSamplerLocation = 0

    private val texVertex2d = floatArrayOf(
        0.5f, 0.5f, //纹理坐标V0
        1f, 1f,     //纹理坐标V1
        0f, 1f,     //纹理坐标V2
        0f, 0f,   //纹理坐标V3
        1f, 0f //纹理坐标V4
    )
    private val texVertex2DBuffer: FloatBuffer by lazy {

        ByteBuffer.allocateDirect(texVertex2d.size * 4).order(ByteOrder.nativeOrder())
            .asFloatBuffer().put(texVertex2d).apply {
                position(0)
            }
    }
    private val texVertexOES = floatArrayOf(
        0.5f, 0.5f, //纹理坐标V0
        1f, 0f,     //纹理坐标V1
        0f, 0f,     //纹理坐标V2
        0f, 1f,   //纹理坐标V3
        1f, 1f //纹理坐标V4
    )
    private val texVertexOESBuffer: FloatBuffer by lazy {

        ByteBuffer.allocateDirect(texVertexOES.size * 4).order(ByteOrder.nativeOrder())
            .asFloatBuffer().put(texVertexOES).apply {
                position(0)
            }
    }
    private val vertexShader = """#version 300 es
layout (location = 0) in vec4 vPosition;
layout (location = 1) in vec4 aTextureCoord;
uniform mat4 uTextureMatrix;
out vec2 yuvTexCoords;
void main() { 
     gl_Position  = vPosition;
     yuvTexCoords = (uTextureMatrix * aTextureCoord).xy;
}"""

    /**
     * 顶点坐标
     * (x,y,z)
     */
    private val POSITION_VERTEX = floatArrayOf(
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

    abstract fun initViewPortSize(videoFrame: VideoFrame)
    protected open fun initProgramExtra() {
    }

    private fun initProgram(
        width: Int,
        height: Int,
        fragmentShader: String,
        TEX_VERTEX: FloatArray,
        mTexVertexBuffer: FloatBuffer
    ) {
        AVLog.d("HapiSurfaceProvider", "onSurfaceCreated ---------------")
        //编译
        val vertexShaderId = ShaderUtils.compileVertexShader(vertexShader)
        val fragmentShaderId = ShaderUtils.compileFragmentShader(fragmentShader)
        //链接程序片段
        mProgram = ShaderUtils.linkProgram(vertexShaderId, fragmentShaderId)
        uTextureMatrixLocation = GLES30.glGetUniformLocation(mProgram, "uTextureMatrix")
        //获取Shader中定义的变量在program中的位置
        uTextureSamplerLocation = GLES30.glGetUniformLocation(mProgram, "texSampler")
        fboTools.createFBOTexture(width, height)
        fboTools.createFrameBuffer()
        initProgramExtra()


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

    fun finishDraw() {
        fboTools.unbindFBO()
    }

    fun draw(videoFrame: VideoFrame) {
        val imgFmt = (videoFrame.buffer as VideoFrame.TextureBuffer).imgFmt()
        initViewPortSize(videoFrame)
        if (mProgram <= 0) {
            val texVertex =
                if (videoFrame.buffer.imgFmt() == AVImgFmt.IMAGE_FORMAT_TEXTURE_OES) {
                    texVertexOES
                } else {
                    texVertex2d
                }
            val fragmentShader =
                if (videoFrame.buffer.imgFmt() == AVImgFmt.IMAGE_FORMAT_TEXTURE_OES) {
                    fragmentOESShader
                } else {
                    fragment2DShader
                }

            val texVertexBuffer =
                if (videoFrame.buffer.imgFmt() == AVImgFmt.IMAGE_FORMAT_TEXTURE_OES) {
                    texVertexOESBuffer
                } else {
                    texVertex2DBuffer
                }
            initProgram(
                viewPortSize.width,
                viewPortSize.height,
                fragmentShader,
                texVertex,
                texVertexBuffer
            )
        }
        GLES30.glViewport(0, 0, viewPortSize.width, viewPortSize.height)

        fboTools.bindFBO()
        GLES30.glClear(GLES30.GL_COLOR_BUFFER_BIT)
        //使用程序片段
        GLES30.glUseProgram(mProgram)

        // 绑定VAO
        GLES30.glBindVertexArray(vaoIds[0])
        //激活纹理单元0
        GLES30.glActiveTexture(GLES30.GL_TEXTURE0)
        if (imgFmt == AVImgFmt.IMAGE_FORMAT_TEXTURE_OES) {
            //绑定外部纹理到纹理单元0
            GLES30.glBindTexture(
                GLES11Ext.GL_TEXTURE_EXTERNAL_OES,
                (videoFrame.buffer as VideoFrame.TextureBuffer).textureID
            )
        } else {
            //绑定外部纹理到纹理单元0
            GLES30.glBindTexture(
                GLES30.GL_TEXTURE_2D,
                (videoFrame.buffer as VideoFrame.TextureBuffer).textureID
            )
        }
        //将此纹理单元床位片段着色器的uTextureSampler外部纹理采样器
        GLES30.glUniform1i(uTextureSamplerLocation, 0)
        //将纹理矩阵传给片段着色器
        GLES30.glUniformMatrix4fv(
            uTextureMatrixLocation,
            1,
            false,
            (videoFrame.buffer as VideoFrame.TextureBuffer).f16Matrix,
            0
        )

        bindFragmentExtra(videoFrame)
        GLES30.glDrawElements(
            GLES20.GL_TRIANGLES, VERTEX_INDEX.size, GLES20.GL_UNSIGNED_SHORT, mVertexIndexBuffer
        )
        GLES30.glBindVertexArray(0)
        GLES30.glFinish()
    }

    protected open fun bindFragmentExtra(videoFrame: VideoFrame) {}
    abstract fun cover2ImgFrame(videoFrame: VideoFrame)

    fun cover2TextureFrame(videoFrame: VideoFrame) {
        videoFrame.width = outBufferSize.width
        videoFrame.height = outBufferSize.height
        videoFrame.rotationDegrees = 0
        val f16Matrix: FloatArray = arrayOf<Float>(
            1f, 0f, 0f, 0f,
            0f, 1f, 0f, 0f,
            0f, 0f, 1f, 0f,
            0f, 0f, 0f, 1f,
        ).toFloatArray()

        videoFrame.buffer = VideoFrame.TextureBuffer(
            fboTools.textures[0],
            f16Matrix,
            AVImgFmt.IMAGE_FORMAT_TEXTURE_2D
        )
    }

    fun release() {
        fboTools.deleteFBO()
        GLES30.glDeleteBuffers(2, vboIds, 0)
        GLES30.glDeleteVertexArrays(1, vaoIds, 0)
    }
}

class RGBACoverRender : ICoverRender() {

    override var fragment2DShader: String =
        """#version 300 es
precision mediump float;
uniform sampler2D texSampler;
in vec2 yuvTexCoords;
out vec4 vFragColor;
void main() {
     vec4 vCameraColor = texture(texSampler,yuvTexCoords);
     vFragColor = vCameraColor;
}"""

    override var fragmentOESShader: String =
        """#version 300 es
//OpenGL ES3.0外部纹理扩展
#extension GL_OES_EGL_image_external_essl3 : require
precision mediump float;
uniform samplerExternalOES texSampler;
in vec2 yuvTexCoords;
out vec4 vFragColor;
void main() {
     vec4 vCameraColor = texture(texSampler,yuvTexCoords);
     vFragColor = vCameraColor;
}"""

    override fun initViewPortSize(videoFrame: VideoFrame) {
        var outW = videoFrame.width
        var outH = videoFrame.height
        if (videoFrame.rotationDegrees == 90 || videoFrame.rotationDegrees == 270) {
            outW = videoFrame.height
            outH = videoFrame.width
        }
        viewPortSize = Size(outW, outH)
        outBufferSize = Size(outW, outH)
    }

    override fun cover2ImgFrame(videoFrame: VideoFrame) {
        val startTime = System.currentTimeMillis()
        if (outBuff == null) {
            outBuff = ByteBuffer.allocateDirect(outBufferSize.width * outBufferSize.height * 4)
        }
        outBuff?.clear()
        GLES20.glReadPixels(
            0,
            0,
            viewPortSize.width,
            viewPortSize.height,
            GLES20.GL_RGBA,
            GLES20.GL_UNSIGNED_BYTE, outBuff
        )

        videoFrame.width = outBufferSize.width
        videoFrame.height = outBufferSize.height
        videoFrame.rotationDegrees = 0
        videoFrame.buffer = VideoFrame.ImgBuffer(outBuff!!, AVImgFmt.IMAGE_FORMAT_RGBA)
        val endTime = System.currentTimeMillis()
        val cost = endTime - startTime
        AVLog.cost(cost, "cover2ImgFrame", " cost $cost")
    }
}

/**
 * Y u v cover render
 * 在gpu上实现rgba 转 yuv 算法存在缺陷目前
 * @constructor Create empty Y u v cover render
 */
class YUVCoverRender : ICoverRender() {
    private var uOffsetLocation = 0
    private var uImgSizeLocation = 0

    override var fragmentOESShader: String =
        """#version 300 es
#extension GL_OES_EGL_image_external_essl3 : require
precision mediump float;
in vec2 yuvTexCoords;
layout(location = 0) out vec4 outColor;
uniform samplerExternalOES texSampler;
uniform float u_Offset;
uniform vec2 u_ImgSize;
//Y =  0.299R + 0.587G + 0.114B
//U = -0.147R - 0.289G + 0.436B
//V =  0.615R - 0.515G - 0.100B
const vec3 COEF_Y = vec3( 0.299,  0.587,  0.114);
const vec3 COEF_U = vec3(-0.147, -0.289,  0.436);
const vec3 COEF_V = vec3( 0.615, -0.515, -0.100);
const float U_DIVIDE_LINE = 2.0 / 3.0;
const float V_DIVIDE_LINE = 5.0 / 6.0;
void main()
{
    vec2 texelOffset = vec2(u_Offset, 0.0);
    if(yuvTexCoords.y <= U_DIVIDE_LINE) {
        vec2 texCoord = vec2(yuvTexCoords.x, yuvTexCoords.y * 3.0 / 2.0);
        vec4 color0 = texture(texSampler, texCoord);
        vec4 color1 = texture(texSampler, texCoord + texelOffset);
        vec4 color2 = texture(texSampler, texCoord + texelOffset * 2.0);
        vec4 color3 = texture(texSampler, texCoord + texelOffset * 3.0);

        float y0 = dot(color0.rgb, COEF_Y);
        float y1 = dot(color1.rgb, COEF_Y);
        float y2 = dot(color2.rgb, COEF_Y);
        float y3 = dot(color3.rgb, COEF_Y);
        outColor = vec4(y0, y1, y2, y3);
    }
    else if(yuvTexCoords.y <= V_DIVIDE_LINE){
        float offsetY = 1.0 / 3.0 / u_ImgSize.y;
        vec2 texCoord;
        if(yuvTexCoords.x <= 0.5) {
            texCoord = vec2(yuvTexCoords.x * 2.0, (yuvTexCoords.y - U_DIVIDE_LINE) * 2.0 * 3.0);
        }
        else {
            texCoord = vec2((yuvTexCoords.x - 0.5) * 2.0, ((yuvTexCoords.y - U_DIVIDE_LINE) * 2.0 + offsetY) * 3.0);
        }

        vec4 color0 = texture(texSampler, texCoord);
        vec4 color1 = texture(texSampler, texCoord + texelOffset * 2.0);
        vec4 color2 = texture(texSampler, texCoord + texelOffset * 4.0);
        vec4 color3 = texture(texSampler, texCoord + texelOffset * 6.0);

        float u0 = dot(color0.rgb, COEF_U) + 0.5;
        float u1 = dot(color1.rgb, COEF_U) + 0.5;
        float u2 = dot(color2.rgb, COEF_U) + 0.5;
        float u3 = dot(color3.rgb, COEF_U) + 0.5;
        outColor = vec4(u0, u1, u2, u3);
    }
    else {
        float offsetY = 1.0 / 3.0 / u_ImgSize.y;
        vec2 texCoord;
        if(yuvTexCoords.x <= 0.5) {
            texCoord = vec2(yuvTexCoords.x * 2.0, (yuvTexCoords.y - V_DIVIDE_LINE) * 2.0 * 3.0);
        }
        else {
            texCoord = vec2((yuvTexCoords.x - 0.5) * 2.0, ((yuvTexCoords.y - V_DIVIDE_LINE) * 2.0 + offsetY) * 3.0);
        }

        vec4 color0 = texture(texSampler, texCoord);
        vec4 color1 = texture(texSampler, texCoord + texelOffset * 2.0);
        vec4 color2 = texture(texSampler, texCoord + texelOffset * 4.0);
        vec4 color3 = texture(texSampler, texCoord + texelOffset * 6.0);

        float v0 = dot(color0.rgb, COEF_V) + 0.5;
        float v1 = dot(color1.rgb, COEF_V) + 0.5;
        float v2 = dot(color2.rgb, COEF_V) + 0.5;
        float v3 = dot(color3.rgb, COEF_V) + 0.5;
        outColor = vec4(v0, v1, v2, v3);
    }
}"""
    override var fragment2DShader: String =
        """#version 300 es
precision mediump float;
in vec2 yuvTexCoords;
layout(location = 0) out vec4 outColor;
uniform sampler2D texSampler;
uniform float u_Offset;
uniform vec2 u_ImgSize;
//Y =  0.299R + 0.587G + 0.114B
//U = -0.147R - 0.289G + 0.436B
//V =  0.615R - 0.515G - 0.100B
const vec3 COEF_Y = vec3( 0.299,  0.587,  0.114);
const vec3 COEF_U = vec3(-0.147, -0.289,  0.436);
const vec3 COEF_V = vec3( 0.615, -0.515, -0.100);
const float U_DIVIDE_LINE = 2.0 / 3.0;
const float V_DIVIDE_LINE = 5.0 / 6.0;
void main()
{
    vec2 texelOffset = vec2(u_Offset, 0.0);
    if(yuvTexCoords.y <= U_DIVIDE_LINE) {
        vec2 texCoord = vec2(yuvTexCoords.x, yuvTexCoords.y * 3.0 / 2.0);
        vec4 color0 = texture(texSampler, texCoord);
        vec4 color1 = texture(texSampler, texCoord + texelOffset);
        vec4 color2 = texture(texSampler, texCoord + texelOffset * 2.0);
        vec4 color3 = texture(texSampler, texCoord + texelOffset * 3.0);

        float y0 = dot(color0.rgb, COEF_Y);
        float y1 = dot(color1.rgb, COEF_Y);
        float y2 = dot(color2.rgb, COEF_Y);
        float y3 = dot(color3.rgb, COEF_Y);
        outColor = vec4(y0, y1, y2, y3);
    }
    else if(yuvTexCoords.y <= V_DIVIDE_LINE){
        float offsetY = 1.0 / 3.0 / u_ImgSize.y;
        vec2 texCoord;
        if(yuvTexCoords.x <= 0.5) {
            texCoord = vec2(yuvTexCoords.x * 2.0, (yuvTexCoords.y - U_DIVIDE_LINE) * 2.0 * 3.0);
        }
        else {
            texCoord = vec2((yuvTexCoords.x - 0.5) * 2.0, ((yuvTexCoords.y - U_DIVIDE_LINE) * 2.0 + offsetY) * 3.0);
        }

        vec4 color0 = texture(texSampler, texCoord);
        vec4 color1 = texture(texSampler, texCoord + texelOffset * 2.0);
        vec4 color2 = texture(texSampler, texCoord + texelOffset * 4.0);
        vec4 color3 = texture(texSampler, texCoord + texelOffset * 6.0);

        float u0 = dot(color0.rgb, COEF_U) + 0.5;
        float u1 = dot(color1.rgb, COEF_U) + 0.5;
        float u2 = dot(color2.rgb, COEF_U) + 0.5;
        float u3 = dot(color3.rgb, COEF_U) + 0.5;
        outColor = vec4(u0, u1, u2, u3);
    }
    else {
        float offsetY = 1.0 / 3.0 / u_ImgSize.y;
        vec2 texCoord;
        if(yuvTexCoords.x <= 0.5) {
            texCoord = vec2(yuvTexCoords.x * 2.0, (yuvTexCoords.y - V_DIVIDE_LINE) * 2.0 * 3.0);
        }
        else {
            texCoord = vec2((yuvTexCoords.x - 0.5) * 2.0, ((yuvTexCoords.y - V_DIVIDE_LINE) * 2.0 + offsetY) * 3.0);
        }

        vec4 color0 = texture(texSampler, texCoord);
        vec4 color1 = texture(texSampler, texCoord + texelOffset * 2.0);
        vec4 color2 = texture(texSampler, texCoord + texelOffset * 4.0);
        vec4 color3 = texture(texSampler, texCoord + texelOffset * 6.0);

        float v0 = dot(color0.rgb, COEF_V) + 0.5;
        float v1 = dot(color1.rgb, COEF_V) + 0.5;
        float v2 = dot(color2.rgb, COEF_V) + 0.5;
        float v3 = dot(color3.rgb, COEF_V) + 0.5;
        outColor = vec4(v0, v1, v2, v3);
    }
}"""

    override fun initProgramExtra() {
        uOffsetLocation = GLES30.glGetUniformLocation(mProgram, "u_Offset")
        uImgSizeLocation = GLES30.glGetUniformLocation(mProgram, "u_ImgSize")
    }

    override fun initViewPortSize(videoFrame: VideoFrame) {
        var outW = videoFrame.width
        var outH = videoFrame.height
        if (videoFrame.rotationDegrees == 90 || videoFrame.rotationDegrees == 270) {
            outW = videoFrame.height
            outH = videoFrame.width
        }
        viewPortSize = Size(outW / 4, (outH * 1.5).toInt())
        outBufferSize = Size(outW, outH)
    }

    override fun bindFragmentExtra(videoFrame: VideoFrame) {
        val texelOffset = (1f / outBufferSize.width)
        GLES30.glUniform1f(uOffsetLocation, texelOffset)
        GLES30.glUniform2f(
            uImgSizeLocation,
            outBufferSize.width.toFloat(),
            outBufferSize.height.toFloat()
        )
    }

    override fun cover2ImgFrame(videoFrame: VideoFrame) {
        if (outBuff == null) {
            outBuff = ByteBuffer.allocateDirect(outBufferSize.width * outBufferSize.height * 3 / 2)
        }
        outBuff?.clear()
        GLES20.glReadPixels(
            0,
            0,
            viewPortSize.width,
            viewPortSize.height,
            GLES20.GL_RGBA,
            GLES20.GL_UNSIGNED_BYTE, outBuff
        )
        videoFrame.width = outBufferSize.width
        videoFrame.height = outBufferSize.height
        videoFrame.rotationDegrees = 0
        videoFrame.buffer = VideoFrame.ImgBuffer(outBuff!!, AVImgFmt.IMAGE_FORMAT_I420)
    }
}

