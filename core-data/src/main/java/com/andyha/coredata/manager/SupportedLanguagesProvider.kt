package com.andyha.coredata.manager

import android.content.Context
import com.andyha.coredata.R
import com.andyha.coreextension.localehelper.Locales
import com.andyha.coreextension.localehelper.currentLocale
import java.util.*


class SupportedLanguagesProviderImpl : SupportedLanguagesProvider {
    override fun provideSupportedLanguages(context: Context): List<Pair<Locale, Int>> {
        return listOf(
            Pair(Locales.Vietnamese, R.string.common_lang_vn),
            Pair(Locales.English, R.string.common_lang_en),
        ).filter { it.first == context.currentLocale }
    }
}

interface SupportedLanguagesProvider {
    fun provideSupportedLanguages(context: Context): List<Pair<Locale, Int>>
}