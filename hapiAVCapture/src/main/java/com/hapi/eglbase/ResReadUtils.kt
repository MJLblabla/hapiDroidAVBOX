package com.hapi.eglbase

import android.content.Context
import java.io.IOException
import java.io.InputStream

object ResReadUtils {
    fun readResource(context: Context, resource: String): String {
        var result = ""
        var ins: InputStream? = null
        try {
            ins = context.assets.open(resource)
            val lenght = ins.available()
            val buffer = ByteArray(lenght)
            ins.read(buffer)
            ins.close()
            result = String(buffer)
        } catch (e: IOException) {
            e.printStackTrace()
            return result
        } finally {
            try {
                ins?.close()
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }
        return result
    }
}