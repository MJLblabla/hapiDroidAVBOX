package com.hapi.avparam

import android.util.Log

object AVLog {
    var logAble = true
    fun d(tag: String, msg: String) {
        if (logAble) {
            Log.d(tag, msg)
        }
    }

    fun cost(cost: Long, tag: String, msg: String) {
        if (cost < 20) {
            return
        }
        Log.d(tag, msg)
    }
}