package com.andyha.corearchitecture.di

import android.content.Context
import com.andyha.corearchitecture.database.AppDatabase
import com.andyha.coreconfig.buildConfig.BuildConfig
import com.andyha.coredata.storage.preference.AppSharedPreference
import com.andyha.featureweatherkit.data.dao.LocationDetectedDao
import com.andyha.featureweatherkit.data.dao.WeatherDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton


@InstallIn(SingletonComponent::class)
@Module
class DatabaseModule {

    @Singleton
    @Provides
    fun provideDatabase(
        @ApplicationContext context: Context,
        prefs: AppSharedPreference,
        buildConfig: BuildConfig
    ): AppDatabase =
        AppDatabase.getInstance(
            context,
            prefs,
            encrypted = buildConfig.isStorageEncrypted,
            memorySecured = false
        )

    @Singleton
    @Provides
    fun provideUserDao(db: AppDatabase): WeatherDao = db.getWeatherDao()

    @Singleton
    @Provides
    fun provideLocationDetectedDao(db: AppDatabase): LocationDetectedDao = db.getLocationDetectedDao()
}