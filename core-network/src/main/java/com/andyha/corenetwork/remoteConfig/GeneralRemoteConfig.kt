package com.andyha.corenetwork.remoteConfig

import com.google.gson.annotations.SerializedName


data class GeneralRemoteConfig(
    @SerializedName("critical_versions")
    var hotFixVersions: String = "",

    @SerializedName("weather_base_url")
    var weatherBaseUrl: String = "",

    @SerializedName("weather_api_key")
    var weatherApiKey: String = ""
)