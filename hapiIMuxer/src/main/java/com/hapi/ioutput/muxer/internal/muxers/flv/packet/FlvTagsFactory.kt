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
import com.hapi.ioutput.muxer.internal.data.AVPacket
import com.hapi.ioutput.muxer.internal.utils.isAudio

class FlvTagFactory(
    private val frame: AVPacket,
    private val alsoWriteSequenceHeader: Boolean = true,
    private val config: MediaFormat
) {
    fun build(): List<FlvTag> {
        val flvTags = mutableListOf<FlvTag>()
        if (alsoWriteSequenceHeader && (frame.isKeyFrame || frame.mimeType.isAudio())) {
            // Create a reference FlvTag
            flvTags.add(FlvTag.createFlvTag(frame, isSequenceHeader = true, config))
        }

        flvTags.add(FlvTag.createFlvTag(frame, isSequenceHeader = false, config))
        return flvTags
    }
}
