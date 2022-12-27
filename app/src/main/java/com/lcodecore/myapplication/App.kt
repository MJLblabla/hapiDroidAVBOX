package com.lcodecore.myapplication

import android.app.Application
import android.util.Log

class App : Application() {

    override fun onCreate() {
        super.onCreate()
        Log.d("mjl","Application onCreate =========================")
    }

    override fun onLowMemory() {
        super.onLowMemory()
    }
}