package com.lumeire.app

import android.app.Application

class LumeireApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        ApiClient.init(this)
    }
}