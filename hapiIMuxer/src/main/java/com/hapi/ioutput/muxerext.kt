package com.hapi.ioutput

fun String.getFileType(): String {
    this.split(".").let {
        if (it.size > 1) {
            return it[it.size - 1]
        }
    }
    return ""
}