/*
 * Copyright (C) 2021 Thibault B.
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
package com.hapi.ioutput.muxer.internal.data

import java.nio.ByteBuffer

/**
 * Packet internal representation.
 * A [AVPacket] is composed by multiple packets.
 */
open class FormatPacket(
    /**
     * Contains data.
     */
    var buffer: ByteBuffer,
) {
    /**
     * [Boolean.true] if this is the first packet that describes a frame.
     */
    var isFirstPacketFrame: Boolean = true

    /**
     * [Boolean.true] if this is the last packet that describes a frame.
     */
    var isLastPacketFrame: Boolean = true

    var frameAbsTimestamp: Long = -1
}