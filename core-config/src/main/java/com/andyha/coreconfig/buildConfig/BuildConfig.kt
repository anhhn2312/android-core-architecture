package com.andyha.coreconfig.buildConfig


interface BuildConfig {
    var isStorageEncrypted: Boolean
    var isLoggingEnabled: Boolean
    var isCrashlyticsEnabled: Boolean
    var isAnalyticsEnabled: Boolean
    var isProductionRelease: Boolean
}