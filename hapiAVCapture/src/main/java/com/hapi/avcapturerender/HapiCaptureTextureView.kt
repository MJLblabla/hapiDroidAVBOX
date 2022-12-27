package com.hapi.avcapturerender

import android.content.Context
import android.util.AttributeSet
import com.hapi.avcapture.ICapturePreview
import com.hapi.avparam.AVLog
import com.hapi.avcapture.VideoFrame
import javax.microedition.khronos.egl.EGL10
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.egl.EGLContext
import javax.microedition.khronos.egl.EGLDisplay

class HapiCaptureTextureView : GLTextureView, ICapturePreview {

    internal class MyEGLContextFactory(private var share_context: EGLContext? = null) :
        GLTextureView.EGLContextFactory {
        override fun createContext(
            egl: EGL10,
            display: EGLDisplay,
            eglConfig: EGLConfig
        ): EGLContext {
            val EGL_CONTEXT_CLIENT_VERSION = 0x3098
            val attrib_list = intArrayOf(EGL_CONTEXT_CLIENT_VERSION, 2, EGL10.EGL_NONE)
            if (share_context == null) {
                val context =
                    egl.eglCreateContext(display, eglConfig, EGL10.EGL_NO_CONTEXT, attrib_list)
                share_context = context
                return context
            }
            return egl.eglCreateContext(display, eglConfig, share_context, attrib_list)
        }
        override fun destroyContext(egl: EGL10, display: EGLDisplay, context: EGLContext) {
            egl.eglDestroyContext(display, context)
        }
    }

    private lateinit var hapiCaptureRender: HapiCaptureRender
    private val videoSizeAdapter = VideoSizeAdapter()

    constructor(context: Context) : this(context, null)
    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs) {
    }

    override fun init(share_context: EGLContext) {
        setEGLContextFactory(MyEGLContextFactory(share_context))
        hapiCaptureRender = HapiCaptureRender(share_context,context)
        setEGLContextClientVersion(3);
        setEGLConfigChooser(8, 8, 8, 8, 16, 0);
        isOpaque = false
        setRenderer(hapiCaptureRender)
        keepScreenOn = true
    }

    override fun render(videoFrame: VideoFrame) {
        hapiCaptureRender.render(videoFrame)
        requestRender()
        if (videoSizeAdapter.adaptVideoSize(
                videoFrame.width,
                videoFrame.height,
                videoFrame.rotationDegrees
            )
        ) {
            post {
               AVLog.d("HapiSurfaceProvider", "requestLayout ---------------")
                requestLayout()
            }
        }
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        videoSizeAdapter.onMeasure(widthMeasureSpec, heightMeasureSpec).let {
            super.onMeasure(
                MeasureSpec.makeMeasureSpec(it.w, MeasureSpec.EXACTLY),
                MeasureSpec.makeMeasureSpec(it.h, MeasureSpec.EXACTLY)
            )
        }
    }
}