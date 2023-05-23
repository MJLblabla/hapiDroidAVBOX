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
import com.hapi.ioutput.muxer.internal.utils.put
import com.hapi.ioutput.muxer.internal.utils.putInt24
import com.hapi.ioutput.muxer.internal.utils.shl
import com.hapi.ioutput.muxer.internal.utils.isAudio
import com.hapi.ioutput.muxer.internal.utils.isVideo
import java.io.IOException
import java.nio.ByteBuffer

abstract class FlvTag(
    private var ts: Long,
    private val type: TagType,
    private val isEncrypted: Boolean = false /* Not supported yet */
) {
    protected abstract fun writeTagHeader(buffer: ByteBuffer)
    protected abstract val tagHeaderSize: Int
    protected abstract fun writePayload(buffer: ByteBuffer)
    protected abstract val payloadSize: Int

    fun write(): ByteBuffer {
        val dataSize = tagHeaderSize + payloadSize
        val flvTagSize = FLV_HEADER_TAG_SIZE + dataSize
        val buffer =
            ByteBuffer.allocateDirect(flvTagSize + 4) // 4 - PreviousTagSize

        // FLV Tag
        buffer.put((isEncrypted shl 5) or (type.value))
        buffer.putInt24(dataSize)
        val tsInMs = (ts / 1000).toInt() // to ms
        buffer.putInt24(tsInMs)
        buffer.put((tsInMs shr 24).toByte())
        buffer.putInt24(0) // Stream ID

        writeTagHeader(buffer)

        if (isEncrypted) {
            throw NotImplementedError("Filter/encryption is not implemented yet")
            // EncryptionTagHeader
            // FilterParams
        }

        writePayload(buffer)

        buffer.putInt(flvTagSize)

        buffer.rewind()

        return buffer
    }

    companion object {
        private const val FLV_HEADER_TAG_SIZE = 11

        fun createFlvTag(frame: AVPacket, isSequenceHeader: Boolean, config: MediaFormat): FlvTag {
            return when {
                frame.mimeType.isAudio() -> if (isSequenceHeader) {
                    AudioTag(frame.pts, frame.dts, frame.extra!![0], true, config)
                } else {
                    AudioTag(frame.pts, frame.dts, frame.buffer, false, config)
                }

                frame.mimeType.isVideo() -> if (isSequenceHeader) {
                    VideoTag(
                        frame.pts,
                        frame.dts,
                        frame.extra!!,
                        frame.isKeyFrame,
                        true,
                        config
                    )
                } else {
                    VideoTag(
                        frame.pts,
                        frame.dts,
                        frame.buffer,
                        frame.isKeyFrame,
                        false,
                        config
                    )
                }

                else -> {
                    throw IOException("Frame is neither video nor audio: ${frame.mimeType}")
                }
            }
        }
    }
}

enum class TagType(val value: Int) {
    AUDIO(8),
    VIDEO(9),
    SCRIPT(18),
}

