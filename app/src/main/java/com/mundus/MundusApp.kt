package com.mundus

import android.app.Application
import com.mundus.di.AppContainer

class MundusApp : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}
