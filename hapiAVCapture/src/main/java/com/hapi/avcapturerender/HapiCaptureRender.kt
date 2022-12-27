package com.hapi.avcapturerender

import android.content.Context
import android.opengl.*
import com.hapi.avparam.AVLog
import com.hapi.avcapture.VideoFrame
import com.hapi.avparam.AVImgFmt
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10


internal class HapiCaptureRender(private val shareEGLLockObj: Any?, val context: Context) :
    GLSurfaceView.Renderer {

    private val mOESCaptureRender = OESCaptureRender(context)
    private val mSampler2DCaptureRender = Sampler2DCaptureRender(context)
    private val mImgFrameWrap = ImgFrameWrap()
    override fun onSurfaceCreated(p0: GL10?, p1: EGLConfig?) {
        AVLog.d("HapiCaptureRender", "onSurfaceCreated ---------------")
    }

    override fun onSurfaceChanged(p0: GL10?, p1: Int, p2: Int) {
        GLES30.glViewport(0, 0, p1, p2);
        AVLog.d("HapiCaptureRender", "onSurfaceChanged $p1 $p1")
    }

    fun render(videoFrame: VideoFrame) {
        synchronized(mImgFrameWrap) {
            mImgFrameWrap.parse(videoFrame)
        }
    }

    private fun onDrawFrame() {
        synchronized(mImgFrameWrap) {
            if (mImgFrameWrap.avImgFmt == AVImgFmt.IMAGE_FORMAT_TEXTURE_OES) {
                mOESCaptureRender.onDrawFrame(mImgFrameWrap)
            } else {
                mSampler2DCaptureRender.onDrawFrame(mImgFrameWrap)
            }
        }
    }

    override fun onDrawFrame(p0: GL10?) {
        if (mImgFrameWrap.avImgFmt == null) {
            AVLog.d("HapiCaptureRender", "onDrawFrame but mImgFrameWrap.imgFmt == null")
            return
        }
        onDrawFrame()
    }

}




