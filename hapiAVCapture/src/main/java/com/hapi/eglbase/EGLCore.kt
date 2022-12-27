package com.hapi.eglbase

import android.graphics.SurfaceTexture
import android.opengl.EGL14.EGL_OPENGL_ES2_BIT
import android.opengl.EGLExt.EGL_OPENGL_ES3_BIT_KHR
import android.opengl.GLES11Ext
import android.opengl.GLES20
import android.opengl.GLES30
import android.opengl.GLUtils
import android.util.Log
import android.view.Surface
import javax.microedition.khronos.egl.EGL10
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.egl.EGLContext
import javax.microedition.khronos.egl.EGLSurface

const val FLAG_RECORDABLE = 0x01

const val EGL_RECORDABLE_ANDROID = 0x3142

class EGLCore {

    private val TAG = "EGLCore"

    // EGL相关变量
    private var mEGLDisplay = EGL10.EGL_NO_DISPLAY
    var mEGLContext = EGL10.EGL_NO_CONTEXT
        private set
    private var mEGLConfig: EGLConfig? = null
    private lateinit var mEgl: EGL10

    /**
     * 初始化EGLDisplay
     * @param eglContext 共享上下文
     */
    fun init(eglContext: EGLContext, flags: Int) {
        mEgl = EGLContext.getEGL() as EGL10
        if (mEGLDisplay !== EGL10.EGL_NO_DISPLAY) {
            throw RuntimeException("EGL already set up")
        }

        // 1，创建 EGLDisplay
        mEGLDisplay = mEgl.eglGetDisplay(EGL10.EGL_DEFAULT_DISPLAY)
        if (mEGLDisplay === EGL10.EGL_NO_DISPLAY) {
            throw RuntimeException("Unable to get EGL10 display")
        }

        // 2，初始化 EGLDisplay
        val version = IntArray(2)
        if (!mEgl.eglInitialize(mEGLDisplay, version)) {
            mEGLDisplay = EGL10.EGL_NO_DISPLAY
            throw RuntimeException("unable to initialize EGL10")
        }

        // 3，初始化EGLConfig，EGLContext上下文
        if (mEGLContext === EGL10.EGL_NO_CONTEXT) {
            val config =
                getConfig(flags, 2) ?: throw RuntimeException("Unable to find a suitable EGLConfig")
            val EGL_CONTEXT_CLIENT_VERSION = 0x3098
            val attrib_list = intArrayOf(
                EGL_CONTEXT_CLIENT_VERSION, 3,
                EGL10.EGL_NONE
            )
            val context = mEgl.eglCreateContext(
                mEGLDisplay, config, eglContext,
                attrib_list
            )
            mEGLConfig = config
            mEGLContext = context
        }
    }

    /**
     * 获取EGL配置信息
     * @param flags 初始化标记
     * @param version EGL版本
     */
    fun getConfig(flags: Int, version: Int): EGLConfig? {
        var renderableType: Int = EGL_OPENGL_ES2_BIT
        if (version >= 3) {
            // 配置EGL 3
            renderableType = renderableType or EGL_OPENGL_ES3_BIT_KHR
        }

        // 配置数组，主要是配置RAGA位数和深度位数
        // 两个为一对，前面是key，后面是value
        // 数组必须以EGL10.EGL_NONE结尾
        val attrList = intArrayOf(
            EGL10.EGL_RED_SIZE, 8,
            EGL10.EGL_GREEN_SIZE, 8,
            EGL10.EGL_BLUE_SIZE, 8,
            EGL10.EGL_ALPHA_SIZE, 8,
            EGL10.EGL_DEPTH_SIZE, 16,
            EGL10.EGL_STENCIL_SIZE, 8,
            EGL10.EGL_RENDERABLE_TYPE, renderableType,
            EGL10.EGL_NONE, 0, // placeholder for recordable [@-3]
            EGL10.EGL_NONE
        )
        //配置Android指定的标记
        if (flags and FLAG_RECORDABLE != 0) {
            attrList[attrList.size - 3] = EGL_RECORDABLE_ANDROID
            attrList[attrList.size - 2] = 1
        }
        val configs = arrayOfNulls<EGLConfig>(1)
        val numConfigs = IntArray(1)

        //获取可用的EGL配置列表
        if (!mEgl.eglChooseConfig(
                mEGLDisplay, attrList,
                configs, configs.size,
                numConfigs
            )
        ) {
            Log.w(TAG, "Unable to find RGB8888 / $version EGLConfig")
            return null
        }

        //使用系统推荐的第一个配置
        return configs[0]
    }

    /**
     * 创建可显示的渲染缓存
     * @param surface 渲染窗口的surface
     */
    fun createWindowSurface(surface: Any): EGLSurface {
        if (surface !is Surface && surface !is SurfaceTexture) {
            throw RuntimeException("Invalid surface: $surface")
        }
        val surfaceAttr = intArrayOf(EGL10.EGL_NONE)
        return mEgl.eglCreateWindowSurface(
            mEGLDisplay, mEGLConfig, surface,
            surfaceAttr
        )
    }

    /**
     * 创建离屏渲染缓存
     * @param width 缓存窗口宽
     * @param height 缓存窗口高
     */
    fun createOffscreenSurface(width: Int, height: Int): EGLSurface {
        val surfaceAttr = intArrayOf(
            EGL10.EGL_WIDTH, width,
            EGL10.EGL_HEIGHT, height,
            EGL10.EGL_NONE
        )
        return mEgl.eglCreatePbufferSurface(
            mEGLDisplay, mEGLConfig,
            surfaceAttr
        ) ?: throw RuntimeException("Surface was null")
    }

    /**
     * 将当前线程与上下文进行绑定
     */
    fun makeCurrent(eglSurface: EGLSurface) {
        if (mEGLDisplay === EGL10.EGL_NO_DISPLAY) {
            throw RuntimeException("EGLDisplay is null, call init first")
        }
        if (!mEgl.eglMakeCurrent(mEGLDisplay, eglSurface, eglSurface, mEGLContext)) {
            throw RuntimeException("makeCurrent(eglSurface) failed")
        }
    }

    /**
     * 将当前线程与上下文进行绑定
     */
    fun makeCurrent(drawSurface: EGLSurface, readSurface: EGLSurface) {
        if (mEGLDisplay === EGL10.EGL_NO_DISPLAY) {
            throw RuntimeException("EGLDisplay is null, call init first")
        }
        if (!mEgl.eglMakeCurrent(mEGLDisplay, drawSurface, readSurface, mEGLContext)) {
            throw RuntimeException("eglMakeCurrent(draw,read) failed")
        }
    }

    /**
     * 将缓存图像数据发送到设备进行显示
     */
    fun swapBuffers(eglSurface: EGLSurface): Boolean {
        return mEgl.eglSwapBuffers(mEGLDisplay, eglSurface)
    }

    /**
     * 销毁EGLSurface，并解除上下文绑定
     */
    fun destroySurface(elg_surface: EGLSurface) {
        mEgl.eglMakeCurrent(
            mEGLDisplay, EGL10.EGL_NO_SURFACE, EGL10.EGL_NO_SURFACE,
            EGL10.EGL_NO_CONTEXT
        )
        mEgl.eglDestroySurface(mEGLDisplay, elg_surface);
    }

    /**
     * 释放资源
     */
    fun release(eglSurface: EGLSurface?) {
        if (mEGLDisplay !== EGL10.EGL_NO_DISPLAY) {
            // Android is unusual in that it uses a reference-counted EGLDisplay.  So for
            // every eglInitialize() we need an eglTerminate().
            mEgl.eglMakeCurrent(
                mEGLDisplay, EGL10.EGL_NO_SURFACE, EGL10.EGL_NO_SURFACE,
                EGL10.EGL_NO_CONTEXT
            )
            mEgl.eglDestroyContext(mEGLDisplay, mEGLContext)
            eglSurface?.let {
                mEgl.eglDestroySurface(mEGLDisplay, eglSurface)
            }
            mEgl.eglTerminate(mEGLDisplay)
        }
        mEGLDisplay = EGL10.EGL_NO_DISPLAY
        mEGLContext = EGL10.EGL_NO_CONTEXT
        mEGLConfig = null
    }

    fun createOESTexture(): Int {
        val tex = IntArray(1)
        //创建一个纹理
        GLES30.glGenTextures(1, tex, 0)
        //绑定到外部纹理上
        GLES30.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, tex[0])
        //设置纹理过滤参数
        GLES30.glTexParameterf(
            GLES11Ext.GL_TEXTURE_EXTERNAL_OES,
            GLES30.GL_TEXTURE_MIN_FILTER,
            GLES30.GL_NEAREST.toFloat()
        )
        GLES30.glTexParameterf(
            GLES11Ext.GL_TEXTURE_EXTERNAL_OES,
            GLES30.GL_TEXTURE_MAG_FILTER,
            GLES30.GL_LINEAR.toFloat()
        )
        GLES30.glTexParameterf(
            GLES11Ext.GL_TEXTURE_EXTERNAL_OES,
            GLES30.GL_TEXTURE_WRAP_S,
            GLES30.GL_CLAMP_TO_EDGE.toFloat()
        )
        GLES30.glTexParameterf(
            GLES11Ext.GL_TEXTURE_EXTERNAL_OES,
            GLES30.GL_TEXTURE_WRAP_T,
            GLES30.GL_CLAMP_TO_EDGE.toFloat()
        )
        //解除纹理绑定
        GLES30.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, 0)
        return tex[0]
    }

    fun create2DTexture(): Int {
        val tex = IntArray(1)
        GLES30.glGenTextures(1, tex, 0)
        GLES30.glActiveTexture(GLES30.GL_TEXTURE0)
        GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, tex[0])
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
        return tex[0]
    }

}