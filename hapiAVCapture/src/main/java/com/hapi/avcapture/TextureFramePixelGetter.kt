package com.hapi.avcapture

import com.hapi.avcapture.render.RGBACoverRender
import com.hapi.avcapture.render.YUVCoverRender
import com.hapi.avparam.AVLog
import com.hapi.avparam.AVImgFmt

internal class TextureFramePixelGetter {
    private var rgbaCoverRender = RGBACoverRender()
    private var yuvCoverRender = YUVCoverRender()

    fun texture2Pixel(frame: VideoFrame, playerView: ICapturePreview?, needOutPut: Boolean) {
        if (playerView == null && !needOutPut) {
            return
        }

        var hasPlayerRender = playerView == null

        if (!hasPlayerRender && frame.buffer.imgFmt() != AVImgFmt.IMAGE_FORMAT_TEXTURE_OES) {
            playerView?.render(frame)
            hasPlayerRender = true
        }

        if (frame.buffer.imgFmt() == AVImgFmt.IMAGE_FORMAT_TEXTURE_2D || frame.buffer.imgFmt() == AVImgFmt.IMAGE_FORMAT_TEXTURE_OES) {
            val canCover2YUV = (frame.width % 4 == 0 && frame.height % 4 == 0)
            val canDirectCover2YUV = canCover2YUV && frame.rotationDegrees == 0
            var startTime = System.currentTimeMillis()
            var endTime = startTime
            var cost = 0L
            if (!canDirectCover2YUV || !hasPlayerRender) {
                rgbaCoverRender.draw(frame)

                if (!hasPlayerRender) {
                    playerView?.let {
                        // oes 纹理 换成 FBO的2d 不闪屏
                        rgbaCoverRender.cover2TextureFrame(frame)
                        it.render(frame)
                    }
                }

                if(needOutPut){
                    if (canCover2YUV) {
                        rgbaCoverRender.cover2TextureFrame(frame)
                    } else {
                        rgbaCoverRender.cover2ImgFrame(frame)
                    }
                }
                rgbaCoverRender.finishDraw()

                endTime = System.currentTimeMillis();
                cost = endTime - startTime
                startTime = endTime
                AVLog.cost(cost, "HapiCaptureRender", "rgbaCoverRender.draw() ${cost} ")
            }
            if (!needOutPut) {
                return
            }
            if (canCover2YUV) {
                yuvCoverRender.draw(frame)
                yuvCoverRender.cover2ImgFrame(frame)
                yuvCoverRender.finishDraw()
                endTime = System.currentTimeMillis();
                cost = endTime - startTime
                AVLog.cost(cost, "HapiCaptureRender", "yuvCoverRender.draw() ${cost} ")
            }
        }
    }

    fun release() {
        rgbaCoverRender.release()
        yuvCoverRender.release()
    }
}
