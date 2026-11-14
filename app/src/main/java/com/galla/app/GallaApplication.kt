package com.galla.app

import android.app.Application

class GallaApplication : Application() {

    /** Built once; screens reach it through `LocalContainer` (see ui/nav). */
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}
