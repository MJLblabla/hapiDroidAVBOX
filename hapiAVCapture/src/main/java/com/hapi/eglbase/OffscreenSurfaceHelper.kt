package com.hapi.eglbase

import android.os.Handler
import android.os.HandlerThread
import java.util.concurrent.CountDownLatch
import javax.microedition.khronos.egl.EGL10
import javax.microedition.khronos.egl.EGLContext
import javax.microedition.khronos.egl.EGLSurface

class OffscreenSurfaceHelper {
    val elgCore = EGLCore()
    private var eglSurface: EGLSurface? = null
    private val mHandlerThread: HandlerThread = HandlerThread("HapiGLThread")
    val mWorkHandler by lazy { Handler(mHandlerThread.looper) }

    fun <T> runOnGLThread(wait: Boolean = false, run: () -> T?): T? {
        var ret: T? = null
        val countDownLatch = CountDownLatch(1)
        mWorkHandler.post {
            ret = run.invoke()
            if (wait) {
                countDownLatch.countDown()
            }
        }
        if (wait) {
            countDownLatch.await()
        }
        return ret
    }

    private fun startGLThread() {
        mHandlerThread.start()
    }

    private fun stopGLThread() {
        mHandlerThread.quit()
    }

    fun attachGLContext(func: (() -> Unit)?): EGLContext {
        startGLThread()
        runOnGLThread(true) {
            elgCore.init(EGL10.EGL_NO_CONTEXT, EGL_RECORDABLE_ANDROID)
            eglSurface = elgCore.createOffscreenSurface(0, 0)
            elgCore.makeCurrent(eglSurface!!)
            func?.invoke()
        }
        return elgCore.mEGLContext
    }

    fun getEGLContext(): EGLContext {
        return elgCore.mEGLContext
    }

    fun release(func: (() -> Unit)? = null) {
        runOnGLThread(true) {
            func?.invoke()
            elgCore.release(eglSurface)
        }
        stopGLThread()
    }
}