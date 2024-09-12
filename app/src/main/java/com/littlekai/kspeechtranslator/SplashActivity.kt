package com.littlekai.kspeechtranslator

import android.content.Intent
import android.os.Bundle
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.mlkit.nl.translate.TranslateLanguage
import com.google.mlkit.nl.translate.Translation
import com.google.mlkit.nl.translate.TranslatorOptions
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.util.Locale

class SplashActivity : AppCompatActivity() {

    private lateinit var progressBar: ProgressBar
    private lateinit var statusText: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)

        progressBar = findViewById(R.id.progressBar)
        statusText = findViewById(R.id.statusText)

        if (SharedPreferencesHelper.isFirstLaunch(this)) {
            downloadAllModels()
        } else {
            launchMainActivity()
        }
    }

    private fun downloadAllModels() {
        val languages = AppConfig.supportedLanguages
        progressBar.max = languages.size

        CoroutineScope(Dispatchers.Main).launch {
            var downloadedCount = 0
            for ((languageName, languageCode) in languages) {
                statusText.text = "Downloading model for $languageName"
                try {
                    withContext(Dispatchers.IO) {
                        downloadModel(languageCode)
                    }
                    SharedPreferencesHelper.addDownloadedModel(this@SplashActivity, languageCode)
                    downloadedCount++
                    progressBar.progress = downloadedCount
                } catch (e: Exception) {
                    // Handle error
                }
            }
            SharedPreferencesHelper.setFirstLaunchComplete(this@SplashActivity)
            launchMainActivity()
        }
    }

    private suspend fun downloadModel(languageCode: String) {
        val options = TranslatorOptions.Builder()
            .setSourceLanguage(TranslateLanguage.ENGLISH)
            .setTargetLanguage(languageCode)
            .build()
        val translator = Translation.getClient(options)
        translator.downloadModelIfNeeded().await()
    }

    private fun getLanguageName(languageCode: String): String {
        val locale = Locale(languageCode)
        return locale.displayLanguage
    }

    private fun launchMainActivity() {
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }
}