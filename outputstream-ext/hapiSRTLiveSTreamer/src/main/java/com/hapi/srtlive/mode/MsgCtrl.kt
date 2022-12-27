
package com.hapi.srtlive.mode


/**
 * This class represents extra parameters for [Socket.send] and [Socket.recv]
 *
 * **See Also:** [srt_msgctrl](https://github.com/Haivision/srt/blob/master/docs/API/API-functions.md#srt_msgctrl)
 */
data class MsgCtrl(
    /**
     * Reserved for future use. Should be 0.
     */
    val flags: Int = 0,
    /**
     * The time (in ms) to wait for a successful delivery. -1 means no time limitation.
     */
    val ttl: Int = -1, // SRT_MSGTTL_INF
    /**
     * Required to be received in the order of sending.
     */
    val inOrder: Boolean = false,
    /**
     * Reserved for future use. Should be [Boundary.SUBSEQUENT].
     */
    val boundary: Boundary = Boundary.SUBSEQUENT,
    /**
     * Receiver: specifies the time when the packet was intended to be delivered to the receiving application (in microseconds since SRT clock epoch).
     * Sender: specifies the application-provided timestamp to be associated with the packet.
     */
    val srcTime: Long = 0,
    /**
     * Receiver only: reports the sequence number for the packet carrying out the message being returned.
     */
    val pktSeq: Int = -1, // SRT_SEQNO_NONE
    /**
     * Message number that can be sent by both sender and receiver.
     */
    val no: Int = -1 // SRT_MSGNO_NONE
)