package com.hapi.avencoder

import com.hapi.avparam.AVLog
import java.util.LinkedList

class FPSFilter {

    var targetFPS = 0
    private var srcFPS = 0
    private var relativelyFrameCount = 0
    private var absFrameCount = 0.0
    private val lastFrameTimestamps = LinkedList<Long>()
    private var lastStartTime = 0L
    private var lastPTS = 0L
    private var lastStartPTS = 0L
    fun start() {
        absFrameCount = 0.0
        lastPTS = 0L
        reset()
    }

    fun reset() {
        srcFPS = 0
        relativelyFrameCount = 0
        lastFrameTimestamps.clear()
        lastStartTime = 0L
        lastStartPTS = lastPTS
    }

    fun filter(frame: VideoEncodeFrame, outputFrameCall: (frame: VideoEncodeFrame) -> Unit) {
        if (frame.timestamp <= 0) {
            frame.pts = (((absFrameCount++) * (1.0 / targetFPS)) * 1000000L).toLong()
            outputFrameCall.invoke(frame)
            return
        }

        if (lastStartTime <= 0L) {
            lastStartTime = frame.timestamp
        }

        relativelyFrameCount++
        if (lastFrameTimestamps.size >= 10) {
            lastFrameTimestamps.removeFirst()
        }
        lastFrameTimestamps.add(frame.timestamp)

        if (lastFrameTimestamps.size < 10) {
            frame.pts = (((absFrameCount++) * (1.0 / targetFPS)) * 1000000L).toLong()
            outputFrameCall.invoke(frame)
            return
        }

        if (relativelyFrameCount % 2 == 0) {
            val frameTimeDiff =
                (((frame.timestamp - lastFrameTimestamps[0]) / lastFrameTimestamps.size.toDouble() - 1) / 1000 / 1000 / 1000)
            srcFPS = (1 / frameTimeDiff).toInt()
        }

//        if (srcFPS <= targetFPS) {
//            val diff =
//                ((frame.timestamp - lastFrameTimestamps[lastFrameTimestamps.size - 2]) / 1000.0 / 1000 / 1000) / (1.0 / targetFPS)
//            absFrameCount += diff
//            frame.pts = (((absFrameCount) * (1.0 / targetFPS)) * 1000000L).toLong()
//            outputFrameCall.invoke(frame)
//            AVLog.d("FPSFilter", "  srcFPS ${srcFPS} ${absFrameCount}")
//            return
//        }

        if (targetFPS < srcFPS) {
            val filterIndex = if (srcFPS % (srcFPS - targetFPS) == 0) {
                srcFPS / (srcFPS - targetFPS)
            } else {
                srcFPS / (srcFPS - targetFPS) + 1
            }
            if (relativelyFrameCount % filterIndex == 0) {
                //丢弃
                return
            }
        }
        absFrameCount++
        val pts = frame.timestamp - lastStartTime + lastStartPTS
        lastPTS = pts

        frame.pts = (pts/1000.0).toLong()
   //     AVLog.d("FPSFilter", "  srcFPS ${srcFPS} ${absFrameCount} ${relativelyFrameCount}  ${frame.pts}")
//        frame.pts = (((absFrameCount++) * (1.0 / targetFPS)) * 1000000L).toLong()
        outputFrameCall.invoke(frame)
    }
}