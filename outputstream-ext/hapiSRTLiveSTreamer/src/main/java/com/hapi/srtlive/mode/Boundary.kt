package com.hapi.srtlive.mode

/**
 * To be use as [MsgCtrl.boundary]
 */

/**
 * To be use as [MsgCtrl.boundary]
 */
enum class Boundary(val intv: Int) {
    /**
     * Middle packet of a message
     */
    SUBSEQUENT(0),

    /**
     * Last packet of a message
     */
    LAST(1),

    /**
     * First packet of a message
     */
    FIRST(3),

    /**
     * Solo message packet
     */
    SOLO(2)
}