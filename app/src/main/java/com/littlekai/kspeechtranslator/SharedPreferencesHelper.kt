package com.littlekai.kspeechtranslator

import android.content.Context
import android.content.SharedPreferences

object SharedPreferencesHelper {
    private const val PREF_NAME = "TranslationModelPrefs"
    private const val KEY_DOWNLOADED_MODELS = "downloaded_models"
    private const val KEY_FIRST_LAUNCH = "first_launch"

    private fun getSharedPreferences(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
    }

    fun getDownloadedModels(context: Context): Set<String> {
        return getSharedPreferences(context).getStringSet(KEY_DOWNLOADED_MODELS, setOf()) ?: setOf()
    }

    fun addDownloadedModel(context: Context, languageCode: String) {
        val downloadedModels = getDownloadedModels(context).toMutableSet()
        downloadedModels.add(languageCode)
        getSharedPreferences(context).edit().putStringSet(KEY_DOWNLOADED_MODELS, downloadedModels).apply()
    }

    fun isFirstLaunch(context: Context): Boolean {
        return getSharedPreferences(context).getBoolean(KEY_FIRST_LAUNCH, true)
    }

    fun setFirstLaunchComplete(context: Context) {
        getSharedPreferences(context).edit().putBoolean(KEY_FIRST_LAUNCH, false).apply()
    }
}