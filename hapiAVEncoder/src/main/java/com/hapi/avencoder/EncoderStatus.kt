package com.hapi.avencoder

enum class EncoderStatus(val intStatus: Int) {
    STATE_UNKNOWN(-2),
    STATE_PREPARE(-1),
    STATE_ENCODING(0),
    STATE_PAUSE(1),
    STATE_STOP(2),
    STATE_RELEASE(3);
}