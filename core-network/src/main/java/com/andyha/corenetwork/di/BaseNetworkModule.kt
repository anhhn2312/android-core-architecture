package com.andyha.corenetwork.di

import android.content.Context
import com.andyha.coreconfig.buildConfig.BuildConfig
import com.andyha.coredata.storage.preference.AppSharedPreference
import com.andyha.coreextension.toJson
import com.andyha.corenetwork.ResponseConverter
import com.andyha.corenetwork.config.NetworkConfig
import com.andyha.corenetwork.config.TokenRefresher
import com.andyha.corenetwork.interceptor.AutoAuthenticator
import com.andyha.corenetwork.interceptor.RequestInterceptor
import com.andyha.corenetwork.qualifier.ForLogging
import com.andyha.corenetwork.qualifier.ForRequestInterceptor
import com.andyha.corenetwork.remoteConfig.GeneralRemoteConfig
import com.andyha.corenetwork.remoteConfig.RemoteConfigManager
import com.andyha.corenetwork.remoteConfig.RemoteConfigManager.Companion.MINIMUM_FETCH_INTERVAL_DEV
import com.andyha.corenetwork.remoteConfig.RemoteConfigManager.Companion.MINIMUM_FETCH_INTERVAL_PROD
import com.andyha.corenetwork.remoteConfig.RemoteConfigManager.Companion.REMOTE_CONFIG_FILE
import com.andyha.corenetwork.remoteConfig.RemoteConfigManagerImpl
import com.google.firebase.ktx.Firebase
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.google.firebase.remoteconfig.ktx.remoteConfig
import com.google.firebase.remoteconfig.ktx.remoteConfigSettings
import com.google.gson.FieldNamingPolicy
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import okhttp3.Authenticator
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import javax.inject.Singleton


@InstallIn(SingletonComponent::class)
@Module
class BaseNetworkModule {

    // =============================================================================================
    // Shared

    @Singleton
    @Provides
    fun provideGson(): Gson = GsonBuilder()
        .setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES)
        .setDateFormat("yyyy-MM-dd HH:mm:ss")
        .disableHtmlEscaping()
        .setPrettyPrinting()
        .create()


    @Singleton
    @Provides
    fun provideRetrofitBuilder(gson: Gson, okHttpClient: OkHttpClient): Retrofit.Builder =
        Retrofit.Builder()
            .addConverterFactory(ResponseConverter(gson))
            .client(okHttpClient)


    // =============================================================================================
    // Interceptor(s)

    @Singleton
    @Provides
    @ForRequestInterceptor
    fun provideRequestInterceptor(
        @ApplicationContext context: Context,
        prefs: AppSharedPreference,
    ): Interceptor = RequestInterceptor(context, prefs)


    @Singleton
    @Provides
    @ForLogging
    fun provideLoggingInterceptor(buildConfig: BuildConfig): Interceptor? {
        return if (buildConfig.isLoggingEnabled) {
            HttpLoggingInterceptor().apply { level = HttpLoggingInterceptor.Level.BODY }
        } else {
            null
        }
    }

    // =============================================================================================

    @Singleton
    @Provides
    fun provideAutoAuthenticator(tokenRefresher: TokenRefresher): Authenticator =
        AutoAuthenticator(tokenRefresher)


    // =============================================================================================
    // Remote config

    @Singleton
    @Provides
    fun provideRemoteConfigManager(remoteConfigManagerImpl: RemoteConfigManagerImpl): RemoteConfigManager {
        return remoteConfigManagerImpl
    }

    @Singleton
    @Provides
    fun provideFirebaseRemoteConfig(
        networkConfig: NetworkConfig,
        buildConfig: BuildConfig
    ): FirebaseRemoteConfig {
        val remoteConfig = Firebase.remoteConfig
        val configSettings = remoteConfigSettings {
            minimumFetchIntervalInSeconds =
                if (buildConfig.isProductionRelease)
                    MINIMUM_FETCH_INTERVAL_PROD
                else
                    MINIMUM_FETCH_INTERVAL_DEV
        }
        remoteConfig.setDefaultsAsync(getRemoteDefaultConfig(networkConfig))
        remoteConfig.setConfigSettingsAsync(configSettings)
        return remoteConfig
    }

    private fun getRemoteDefaultConfig(networkConfig: NetworkConfig): Map<String, String> {
        val remoteConfig = GeneralRemoteConfig()

        return mutableMapOf(REMOTE_CONFIG_FILE to remoteConfig.toJson())
    }
}