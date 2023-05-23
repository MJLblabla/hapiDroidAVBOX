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
package com.hapi.ioutput.muxer.internal.muxers.flv

import android.media.MediaFormat
import com.hapi.ioutput.muxer.internal.data.AVPacket
import com.hapi.ioutput.muxer.internal.data.FormatPacket
import com.hapi.ioutput.muxer.internal.muxers.IMuxer
import com.hapi.ioutput.muxer.internal.muxers.IMuxerListener
import com.hapi.ioutput.muxer.internal.muxers.flv.packet.FlvHeader
import com.hapi.ioutput.muxer.internal.muxers.flv.packet.FlvTagFactory
import com.hapi.ioutput.muxer.internal.utils.isAudio
import com.hapi.ioutput.muxer.internal.utils.isVideo
import java.util.LinkedList

class FlvMuxer(
    override var listener: IMuxerListener? = null,
    private val writeToFile: Boolean,
) : IMuxer {

    override val helper = FlvMuxerHelper()
    private val streams = LinkedList<Pair<Int, MediaFormat>>()
    private fun findMediaFormatByID(id: Int): MediaFormat {
        return (streams.find {
            it.first == id
        })!!.second
    }

    private val hasAudio: Boolean
        get() = streams.any { it.second.getString(MediaFormat.KEY_MIME)!!.isAudio() }
    private val hasVideo: Boolean
        get() = streams.any { it.second.getString(MediaFormat.KEY_MIME)!!.isVideo() }
    private var startUpTime: Long? = null
    private var hasFirstFrame = false

    override var manageVideoOrientation: Boolean = false

    override fun encode(avPacket: AVPacket, streamPid: Int) {
        if (!hasFirstFrame) {
            hasFirstFrame = true
            // Metadata
//            listener?.onOutputFrame(
//                Packet(
//                    OnMetadata(streams.get(streamPid)).write(),
//                    frame.pts
//                )
//            )
        }
        val flvTags = FlvTagFactory(avPacket, true, findMediaFormatByID(streamPid)).build()
        flvTags.forEach {
            listener?.onOutputFrame(FormatPacket(it.write()))
        }
    }

    override fun addStreams(streamsConfig: List<MediaFormat>): Map<MediaFormat, Int> {
        val streamMap = mutableMapOf<MediaFormat, Int>()
        streamsConfig.forEach {
            val id = if (streams.isEmpty()) {
                1
            } else {
                streams.last.first + 1
            }
            streamMap[it] = id
            streams.add(Pair(id, it))
        }
        requireStreams()
        return streamMap
    }

    override fun removeStreams(streamsPid: List<Int>) {
        streamsPid.forEach { id ->
            streams.find {
                it.first == id
            }.let {
                streams.remove(it)
            }
        }
    }

    override fun configure(config: Unit) {
        // Nothing to configure
    }

    override fun startStream() {
        // Header
        if (writeToFile) {
            listener?.onOutputFrame(
                FormatPacket(
                    FlvHeader(hasAudio, hasVideo).write()
                )
            )
        }
    }

    override fun stopStream() {
        startUpTime = null
        hasFirstFrame = false
    }

    override fun release() {
        streams.clear()
    }

    /**
     * Check that there shall be no more than one audio and one video stream
     */
    private fun requireStreams() {
        require(streams.count {
            it.second.getString(MediaFormat.KEY_MIME)!!.isAudio()
        } <= 1) { "Only one audio stream is supported by FLV" }
        require(streams.count {
            it.second.getString(MediaFormat.KEY_MIME)!!.isVideo()
        } <= 1) { "Only one video stream is supported by FLV" }
    }

}