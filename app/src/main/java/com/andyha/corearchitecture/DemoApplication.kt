package com.andyha.corearchitecture

import android.app.Application
import com.andyha.coreconfig.buildConfig.BuildConfig
import com.andyha.coreutils.timber.AppTree
import dagger.hilt.android.HiltAndroidApp
import timber.log.Timber
import javax.inject.Inject


@HiltAndroidApp
class DemoApplication : Application() {

    @Inject
    lateinit var buildConfig: BuildConfig

    override fun onCreate() {
        super.onCreate()
        initLogging()
    }

    private fun initLogging() {
        if (buildConfig.isLoggingEnabled) {
            Timber.plant(AppTree())
        }
    }
}