package com.andyha.corenetwork.remoteConfig


inline val RemoteConfigManager.weatherBaseUrl: String
    get() = generalRemoteConfig.weatherBaseUrl

inline val RemoteConfigManager.weatherApiKey: String
    get() = generalRemoteConfig.weatherApiKey

inline val RemoteConfigManager.hotfixVersions: String
    get() = generalRemoteConfig.hotFixVersions