package com.hapi.eglbase

import android.graphics.SurfaceTexture
import android.view.Surface
import com.hapi.avcapture.VideoFrame
import com.hapi.avparam.AVImgFmt

class OESSurfaceProvider(
    private val offscreenSurfaceHelper: OffscreenSurfaceHelper
) : SurfaceProvider {

    private var surfaceTexture: SurfaceTexture? = null
    var surface: Surface? = null
        private set
    var oesTextureID = 0
        private set

    private fun createOESTexture() {
        oesTextureID = offscreenSurfaceHelper.elgCore.createOESTexture()
    }

    override fun createSurface(w: Int, h: Int): Surface {
        if (oesTextureID == 0) {
            offscreenSurfaceHelper.runOnGLThread(true) {
                createOESTexture()
            }
        }
        surface?.release()
        surfaceTexture?.release()
        surfaceTexture = SurfaceTexture(oesTextureID, false)
        surfaceTexture?.setDefaultBufferSize(w, h)
        surface = Surface(surfaceTexture)
        return surface!!
    }

    override fun setOnFrameAvailableListener(call: (frame: VideoFrame.FrameBuffer, timestamp: Long) -> Unit) {
        surfaceTexture?.setOnFrameAvailableListener({
            val transformMatrix = FloatArray(16)
            var timestamp = 0L
            try {
                surfaceTexture?.updateTexImage()
                timestamp = surfaceTexture?.timestamp ?: 0L
                surfaceTexture?.getTransformMatrix(transformMatrix)
            } catch (e: RuntimeException) {
                e.printStackTrace()
            }
            call.invoke(
                VideoFrame.TextureBuffer(
                    oesTextureID,
                    transformMatrix,
                    AVImgFmt.IMAGE_FORMAT_TEXTURE_OES
                ),
                timestamp
            )
        }, offscreenSurfaceHelper.mWorkHandler)
    }

    override fun release() {
        surface?.release()
        surfaceTexture?.release()
    }
}