package com.hapi.avcapture

import javax.microedition.khronos.egl.EGLContext

interface ICapturePreview {
    fun init(share_context: EGLContext)
    fun render(videoFrame: VideoFrame)
}