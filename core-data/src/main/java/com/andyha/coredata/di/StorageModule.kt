package com.andyha.coredata.di

import android.content.Context
import com.andyha.coreconfig.buildConfig.BuildConfig
import com.andyha.coredata.storage.preference.AppSharedPreference
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton


@Module
@InstallIn(SingletonComponent::class)
class StorageModule {
    @Singleton
    @Provides
    fun provideSharedPreference(
        @ApplicationContext context: Context,
        buildConfig: BuildConfig
    ): AppSharedPreference =
        AppSharedPreference.getInstance(context, buildConfig)
}