package com.hapi.ioutput

import android.net.Uri
import com.hapi.ioutput.muxer.internal.muxers.flv.FlvMuxer
import com.hapi.ioutput.muxer.internal.muxers.ts.TSMuxer
import com.hapi.ioutput.muxer.internal.muxers.ts.data.TsServiceInfo

class AVOutputFormatFactory {

    private fun reflectionMuxer(className: String): OutputStreamer {
        val clz = Class.forName(className)
        return clz.newInstance() as OutputStreamer
    }

    fun create(url: String): OutputStreamer {
        val router = Uri.parse(url)
        val fileType = url.getFileType()
        val scheme = router.scheme
        return when {
            (url.startsWith("/") || url.startsWith("file"))
                    && (fileType == "mp4" || fileType == "webm" || fileType == "3gp" || fileType == "heif" || fileType == "ogg")
            -> reflectionMuxer("com.hapi.droidmediamuxer.HapiMediaMuxer")
            scheme == "rtmp" -> reflectionMuxer("com.hapi.rtmplive.HapiRTMPOutputStreamer")
            scheme == "srt" -> reflectionMuxer("com.hapi.srtlive.HapiSRTLiveStreamer")
            (url.startsWith("/") || url.startsWith("file")) && (fileType == "ts" || fileType == "flv") -> FileOutputStreamer(
                if (fileType == "ts") {
                    TSMuxer().apply {
                        addService(
                            TsServiceInfo(
                                TsServiceInfo.ServiceType.DIGITAL_TV,
                                0x4698,
                                "HapiSRTMuxer",
                                "HapiSRTMuxer"
                            )
                        )
                    }
                } else {
                    FlvMuxer(null, true)
                }
            )
            else -> throw  Exception(" unSupport url")
        }
    }
}