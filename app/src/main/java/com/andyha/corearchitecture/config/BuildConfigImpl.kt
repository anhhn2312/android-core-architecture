package com.andyha.corearchitecture.config

import android.content.Context
import com.andyha.corearchitecture.R
import com.andyha.coreconfig.buildConfig.BuildConfig
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject


class BuildConfigImpl @Inject constructor(
    @ApplicationContext private val context: Context
) : BuildConfig {
    override var isStorageEncrypted = context.resources.getBoolean(R.bool.isStorageEncrypted)
    override var isLoggingEnabled = context.resources.getBoolean(R.bool.isLoggingEnabled)
    override var isCrashlyticsEnabled = context.resources.getBoolean(R.bool.isCrashlyticsEnabled)
    override var isAnalyticsEnabled = context.resources.getBoolean(R.bool.isAnalyticsEnabled)
    override var isProductionRelease = context.resources.getBoolean(R.bool.isProductionRelease)
}