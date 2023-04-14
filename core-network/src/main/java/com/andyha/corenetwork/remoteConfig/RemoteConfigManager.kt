package com.andyha.corenetwork.remoteConfig


interface RemoteConfigManager {
    var generalRemoteConfig: GeneralRemoteConfig
    var featureFlags: FeatureFlags

    fun fetchAndActivateRemoteConfig()

    fun isCurrentLanguageVietnamese(): Boolean

    fun getString(key: String): String
    fun getLong(key: String): Long
    fun getInt(key: String): Int
    fun getDouble(key: String): Double
    fun getBoolean(key: String): Boolean

    companion object {
        const val MINIMUM_FETCH_INTERVAL_DEV = 30L// in seconds
        const val MINIMUM_FETCH_INTERVAL_PROD = 3600L // in seconds

        // General
        const val REMOTE_CONFIG_FILE = "general_config_0_0_1"

        // Feature flags
        const val FLAG_FEATURE_A = "flag_feature_a"
    }
}