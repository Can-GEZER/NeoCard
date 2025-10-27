package com.cangzr.neocard

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class NeoCardApplication : Application() {
    override fun onCreate() {
        super.onCreate()
    }
}

