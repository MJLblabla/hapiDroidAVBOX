package com.hapi.eglbase

import android.view.Surface
import com.hapi.avcapture.VideoFrame

interface SurfaceProvider {
    fun setOnFrameAvailableListener(call: (frame: VideoFrame.FrameBuffer,timestamp: Long) -> Unit)
    fun createSurface(w: Int, h: Int):Surface
    fun release()
}