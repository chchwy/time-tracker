package com.mattchang.timetracker.util

import android.content.Context
import android.content.res.Configuration
import java.util.Locale

object LocaleHelper {

    private const val PREF_NAME = "settings"
    private const val KEY_LANGUAGE = "language"

    const val LANG_ZH = "zh"
    const val LANG_EN = "en"

    fun getSavedLanguage(context: Context): String =
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            .getString(KEY_LANGUAGE, LANG_ZH) ?: LANG_ZH

    fun saveLanguage(context: Context, lang: String) {
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            .edit().putString(KEY_LANGUAGE, lang).apply()
    }

    fun applyLocale(context: Context): Context {
        val lang = getSavedLanguage(context)
        val locale = if (lang == LANG_EN) Locale.ENGLISH else Locale("zh", "TW")
        Locale.setDefault(locale)
        val config = Configuration(context.resources.configuration)
        config.setLocale(locale)
        return context.createConfigurationContext(config)
    }
}
