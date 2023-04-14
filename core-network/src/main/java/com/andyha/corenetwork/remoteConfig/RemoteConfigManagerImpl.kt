package com.andyha.corenetwork.remoteConfig

import android.content.Context
import com.andyha.coreextension.isCurrentLanguageVietnamese
import com.andyha.corenetwork.remoteConfig.RemoteConfigManager.Companion.REMOTE_CONFIG_FILE
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.google.gson.Gson
import dagger.hilt.android.qualifiers.ApplicationContext
import timber.log.Timber
import javax.inject.Inject
import kotlin.properties.Delegates


class RemoteConfigManagerImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val firebaseRemoteConfig: FirebaseRemoteConfig,
) : RemoteConfigManager {

    override var generalRemoteConfig by Delegates.notNull<GeneralRemoteConfig>()
    override var featureFlags by Delegates.notNull<FeatureFlags>()

    init {
        buildGeneralConfig()
        buildFeatureFlags()
        Timber.d("RemoteConfigManager: Init: $generalRemoteConfig")
    }

    override fun fetchAndActivateRemoteConfig() {
        firebaseRemoteConfig.fetchAndActivate()
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val updated = task.result
                    Timber.d("RemoteConfig: Config params updated: $updated")
                    Timber.d(
                        "RemoteConfig: New general configs: ${
                            firebaseRemoteConfig.getString(REMOTE_CONFIG_FILE)
                        }"
                    )
                    buildGeneralConfig()
                    buildFeatureFlags()
                } else {
                    Timber.d("RemoteConfig: Fetch failed")
                }
            }
    }

    override fun isCurrentLanguageVietnamese(): Boolean {
        return context.isCurrentLanguageVietnamese()
    }

    override fun getString(key: String): String = firebaseRemoteConfig.getString(key)

    override fun getLong(key: String): Long = firebaseRemoteConfig.getLong(key)

    override fun getInt(key: String): Int = firebaseRemoteConfig.getLong(key).toInt()

    override fun getDouble(key: String): Double = firebaseRemoteConfig.getDouble(key)

    override fun getBoolean(key: String): Boolean = firebaseRemoteConfig.getBoolean(key)

    private fun buildGeneralConfig() {
        generalRemoteConfig = if (firebaseRemoteConfig.getString(REMOTE_CONFIG_FILE).isNotEmpty()) {
            Gson().fromJson(
                firebaseRemoteConfig.getString(REMOTE_CONFIG_FILE),
                GeneralRemoteConfig::class.java
            )
        } else {
            GeneralRemoteConfig()
        }
    }

    private fun buildFeatureFlags() {
        featureFlags = FeatureFlags()
    }

    private fun getFeatureFlag(key: String): Boolean {
        val flag = firebaseRemoteConfig.getBoolean(key)
        Timber.d("RemoteConfig: Feature flag updated: $key => $flag")
        return flag
    }
}