/*
 * Copyright (C) 2022 Thibault B.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.hapi.ioutput.muxer.internal.muxers.flv.packet

import android.media.MediaFormat
import android.util.Size
import com.hapi.avparam.KEY_AUDIO_CHANNEL_CONFIG
import com.hapi.avparam.KEY_AUDIO_FORMAT
import com.hapi.ioutput.muxer.internal.muxers.flv.amf.containers.AmfContainer
import com.hapi.ioutput.muxer.internal.muxers.flv.amf.containers.AmfEcmaArray
import com.hapi.ioutput.muxer.internal.utils.getNumberOfChannels
import com.hapi.ioutput.muxer.internal.utils.isAudio
import com.hapi.ioutput.muxer.internal.utils.isVideo
import com.hapi.ioutput.muxer.internal.utils.numOfBits
import java.io.IOException
import java.nio.ByteBuffer

class OnMetadata(streams: List<MediaFormat>) :
    FlvTag(0, TagType.SCRIPT) {
    private val amfContainer = AmfContainer()

    init {
        amfContainer.add("onMetaData")
        val ecmaArray = AmfEcmaArray()
        ecmaArray.add("duration", 0.0)
        streams.forEach {

            if (it.getString(MediaFormat.KEY_MIME)!!.isAudio()) {
                ecmaArray.add(
                    "audiocodecid",
                    SoundFormat.fromMimeType(it.getString(MediaFormat.KEY_MIME)!!).value.toDouble()
                )
                ecmaArray.add("audiodatarate", it.getInteger(MediaFormat.KEY_BIT_RATE).toDouble() / 1000) // to Kpbs
                ecmaArray.add("audiosamplerate", it.getInteger(MediaFormat.KEY_SAMPLE_RATE).toDouble())
                ecmaArray.add(
                    "audiosamplesize",
                    it.getInteger(KEY_AUDIO_FORMAT).numOfBits().toDouble()
                )
                ecmaArray.add(
                    "stereo",
                    getNumberOfChannels(it.getInteger(KEY_AUDIO_CHANNEL_CONFIG)) == 2
                )

            } else
                if (it.getString(MediaFormat.KEY_MIME)!!.isVideo()) {
                    val resolution =
//                        if (manageVideoOrientation) {
//                        it.getOrientedResolution(context)
//                    } else {
                        Size(it.getInteger(MediaFormat.KEY_WIDTH),it.getInteger(MediaFormat.KEY_HEIGHT) )
                    // }
                    ecmaArray.add(
                        "videocodecid",
                        CodecID.fromMimeType(it.getString(MediaFormat.KEY_MIME)!!).value.toDouble()
                    )
                    ecmaArray.add(
                        "videodatarate",
                        it.getInteger(MediaFormat.KEY_BIT_RATE).toDouble() / 1000
                    ) // to Kpbs
                    ecmaArray.add("width", resolution.width.toDouble())
                    ecmaArray.add("height", resolution.height.toDouble())
                    ecmaArray.add("framerate", it.getInteger(MediaFormat.KEY_FRAME_RATE).toDouble())
                }
            else  {
            throw IOException("Not supported mime type: ${it.getString(MediaFormat.KEY_MIME)}")
        }
    }
    amfContainer.add(ecmaArray)
}

override fun writeTagHeader(buffer: ByteBuffer) {
    // Do nothing
}

override val tagHeaderSize: Int
    get() = 0

override fun writePayload(buffer: ByteBuffer) {
    amfContainer.encode(buffer)
}

override val payloadSize: Int
    get() = amfContainer.size
}