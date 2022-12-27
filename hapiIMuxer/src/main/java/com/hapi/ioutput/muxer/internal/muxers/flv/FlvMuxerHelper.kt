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
import com.hapi.ioutput.muxer.internal.muxers.IAudioMuxerHelper
import com.hapi.ioutput.muxer.internal.muxers.IMuxerHelper
import com.hapi.ioutput.muxer.internal.muxers.IVideoMuxerHelper
import com.hapi.ioutput.muxer.internal.muxers.flv.packet.CodecID
import com.hapi.ioutput.muxer.internal.muxers.flv.packet.SoundFormat
import com.hapi.ioutput.muxer.internal.muxers.flv.packet.SoundRate
import com.hapi.ioutput.muxer.internal.muxers.flv.packet.SoundSize

class FlvMuxerHelper : IMuxerHelper {
    override val video = VideoFlvMuxerHelper()
    override val audio = AudioFlvMuxerHelper()
}

class VideoFlvMuxerHelper : IVideoMuxerHelper {
    /**
     * Get FLV Muxer supported video encoders list
     */
    override val supportedEncoders: List<String>
        get() {
            return CodecID.values().mapNotNull {
                try {
                    it.toMimeType()
                } catch (e: Exception) {
                    null
                }
            }.filter {
                listOf(
                    MediaFormat.MIMETYPE_VIDEO_AVC,
                    MediaFormat.MIMETYPE_VIDEO_HEVC
                ).contains(it)
            }
        }
}

class AudioFlvMuxerHelper : IAudioMuxerHelper {
    /**
     * Get FLV Muxer supported audio encoders list
     */
    override val supportedEncoders: List<String>
        get() {
            return SoundFormat.values().mapNotNull {
                try {
                    it.toMimeType()
                } catch (e: Exception) {
                    null
                }
            }
        }

    override fun getSupportedSampleRates() = SoundRate.values().map { it.toSampleRate() }

    override fun getSupportedByteFormats() = SoundSize.values().map { it.toByteFormat() }
}