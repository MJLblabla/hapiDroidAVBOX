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
package com.hapi.ioutput.muxer.internal.utils.av.audio

import android.media.MediaFormat
import java.nio.ByteBuffer

data class AudioSpecificConfig(
    private val decoderSpecificInformation: ByteBuffer,
    private val audioConfig: MediaFormat
) {
    fun write(buffer: ByteBuffer) {
        if (audioConfig.getString(MediaFormat.KEY_MIME) == MediaFormat.MIMETYPE_AUDIO_AAC) {
            buffer.put(decoderSpecificInformation)
        } else {
            throw NotImplementedError("No support for ${audioConfig.getString(MediaFormat.KEY_MIME)}")
        }
    }
}