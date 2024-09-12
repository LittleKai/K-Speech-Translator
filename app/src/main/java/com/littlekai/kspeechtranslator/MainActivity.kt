package com.littlekai.kspeechtranslator

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.util.Log
import android.view.View
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.mlkit.common.model.DownloadConditions
import com.google.mlkit.nl.translate.TranslateLanguage
import com.google.mlkit.nl.translate.Translation
import com.google.mlkit.nl.translate.Translator
import com.google.mlkit.nl.translate.TranslatorOptions
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.util.*

class MainActivity : AppCompatActivity(), TextToSpeech.OnInitListener {

    private lateinit var speechRecognizer: SpeechRecognizer
    private lateinit var textToSpeech: TextToSpeech
    private lateinit var sourceLanguageSpinner: Spinner
    private lateinit var targetLanguageSpinner: Spinner
    private lateinit var recognizedTextView: TextView
    private lateinit var translatedTextView: TextView
    private lateinit var micButton: FloatingActionButton
    private lateinit var translator: Translator
    private val handler = Handler(Looper.getMainLooper())
    private var continuousListening = false
    private val recognizedTextBuilder = StringBuilder()
    private lateinit var settingsButton: ImageButton
    private var lastTranslatedText = ""
    private var geminiApiKey = ""

    private var minimumSpeechLength = 30000
    private var completeSilenceLength = 1500
    private var possiblySilenceLength = 1500
    private var isListening = false

    private lateinit var languageItems: List<LanguageItem>
    private lateinit var sourceAdapter: LanguageSpinnerAdapter
    private lateinit var targetAdapter: LanguageSpinnerAdapter
    private var sourceLanguage = AppConfig.DEFAULT_SOURCE_LANGUAGE
    private var targetLanguage = AppConfig.DEFAULT_TARGET_LANGUAGE
    private lateinit var sentenceProcessor: SentenceProcessor
    private var processSentenceStructure = true
    private var speakTranslatedSentence = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        initializeLanguageItems()
        initializeViews()
        setupSpinners()
        setupMicButton()
        setupSettingsButton()
        initializeSpeechRecognizer()
        initializeTextToSpeech()
        initializeTranslator()
        checkPermission()
        loadSettings()

        sentenceProcessor = SentenceProcessor(this, geminiApiKey)
    }

    private fun loadSettings() {
        val sharedPrefs = getSharedPreferences(AppConfig.PREF_NAME, Context.MODE_PRIVATE)
        processSentenceStructure =
            sharedPrefs.getBoolean(AppConfig.KEY_PROCESS_SENTENCE_STRUCTURE, true)
        speakTranslatedSentence =
            sharedPrefs.getBoolean(AppConfig.KEY_SPEAK_TRANSLATED_SENTENCE, false)
        minimumSpeechLength = sharedPrefs.getInt(
            AppConfig.KEY_MINIMUM_SPEECH_LENGTH,
            AppConfig.MINIMUM_SPEECH_LENGTH_MS
        )
        completeSilenceLength = sharedPrefs.getInt(
            AppConfig.KEY_COMPLETE_SILENCE_LENGTH,
            AppConfig.COMPLETE_SILENCE_LENGTH_MS
        )
        possiblySilenceLength = sharedPrefs.getInt(
            AppConfig.KEY_POSSIBLY_SILENCE_LENGTH,
            AppConfig.POSSIBLY_SILENCE_LENGTH_MS
        )
        geminiApiKey = sharedPrefs.getString(AppConfig.KEY_GEMINI_API_KEY, "") ?: ""
    }

    private fun initializeLanguageItems() {
        val downloadedModels = SharedPreferencesHelper.getDownloadedModels(this)
        languageItems = AppConfig.supportedLanguages.map { (name, code) ->
            LanguageItem(name, code, downloadedModels.contains(code))
        }
    }

    private fun initializeViews() {
        sourceLanguageSpinner = findViewById(R.id.sourceLanguageSpinner)
        targetLanguageSpinner = findViewById(R.id.targetLanguageSpinner)
        recognizedTextView = findViewById(R.id.recognizedTextView)
        translatedTextView = findViewById(R.id.translatedTextView)
        micButton = findViewById(R.id.micButton)
        settingsButton = findViewById(R.id.settingsButton)

    }

    private fun setupSettingsButton() {
        settingsButton.setOnClickListener {
            showSettingsDialog()
        }
    }

    private fun showSettingsDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_settings, null)
        val minimumSpeechLengthEdit = dialogView.findViewById<EditText>(R.id.minimumSpeechLengthEdit)
        val completeSilenceLengthEdit = dialogView.findViewById<EditText>(R.id.completeSilenceLengthEdit)
        val possiblySilenceLengthEdit = dialogView.findViewById<EditText>(R.id.possiblySilenceLengthEdit)
        val processSentenceStructureCheckbox = dialogView.findViewById<CheckBox>(R.id.processSentenceStructureCheckbox)
        val speakTranslatedSentenceCheckbox = dialogView.findViewById<CheckBox>(R.id.speakTranslatedSentenceCheckbox)
        val geminiApiKeyEdit = dialogView.findViewById<EditText>(R.id.geminiApiKeyEdit)

        minimumSpeechLengthEdit.setText(minimumSpeechLength.toString())
        completeSilenceLengthEdit.setText(completeSilenceLength.toString())
        possiblySilenceLengthEdit.setText(possiblySilenceLength.toString())
        processSentenceStructureCheckbox.isChecked = processSentenceStructure
        speakTranslatedSentenceCheckbox.isChecked = speakTranslatedSentence
        geminiApiKeyEdit.setText(geminiApiKey)

        AlertDialog.Builder(this)
            .setTitle("Speech Recognition Settings")
            .setView(dialogView)
            .setPositiveButton("Save") { _, _ ->
                minimumSpeechLength = minimumSpeechLengthEdit.text.toString().toIntOrNull() ?: minimumSpeechLength
                completeSilenceLength = completeSilenceLengthEdit.text.toString().toIntOrNull() ?: completeSilenceLength
                possiblySilenceLength = possiblySilenceLengthEdit.text.toString().toIntOrNull() ?: possiblySilenceLength
                processSentenceStructure = processSentenceStructureCheckbox.isChecked
                speakTranslatedSentence = speakTranslatedSentenceCheckbox.isChecked
                geminiApiKey = geminiApiKeyEdit.text.toString()

                saveSettings()
                updateSentenceProcessor()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun saveSettings() {
        val sharedPrefs = getSharedPreferences(AppConfig.PREF_NAME, Context.MODE_PRIVATE)
        sharedPrefs.edit().apply {
            putBoolean(AppConfig.KEY_PROCESS_SENTENCE_STRUCTURE, processSentenceStructure)
            putBoolean(AppConfig.KEY_SPEAK_TRANSLATED_SENTENCE, speakTranslatedSentence)
            putInt(AppConfig.KEY_MINIMUM_SPEECH_LENGTH, minimumSpeechLength)
            putInt(AppConfig.KEY_COMPLETE_SILENCE_LENGTH, completeSilenceLength)
            putInt(AppConfig.KEY_POSSIBLY_SILENCE_LENGTH, possiblySilenceLength)
            putString(AppConfig.KEY_GEMINI_API_KEY, geminiApiKey)

        }.apply()
    }

    private fun updateSentenceProcessor() {
        sentenceProcessor = SentenceProcessor(this, geminiApiKey)
    }

    private fun setupSpinners() {
        sourceAdapter = LanguageSpinnerAdapter(this, languageItems)
        targetAdapter = LanguageSpinnerAdapter(this, languageItems)

        sourceLanguageSpinner.adapter = sourceAdapter
        targetLanguageSpinner.adapter = targetAdapter

        sourceLanguageSpinner.setSelection(languageItems.indexOfFirst { it.code == AppConfig.DEFAULT_SOURCE_LANGUAGE })
        targetLanguageSpinner.setSelection(languageItems.indexOfFirst { it.code == AppConfig.DEFAULT_TARGET_LANGUAGE })

        val itemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>,
                view: View,
                position: Int,
                id: Long
            ) {
                val selectedItem = parent.getItemAtPosition(position) as LanguageItem
                if (parent.id == R.id.sourceLanguageSpinner) {
                    sourceLanguage = selectedItem.code
                } else if (parent.id == R.id.targetLanguageSpinner) {
                    targetLanguage = selectedItem.code
                }
                initializeTranslator()
            }

            override fun onNothingSelected(parent: AdapterView<*>) {}
        }

        sourceLanguageSpinner.onItemSelectedListener = itemSelectedListener
        targetLanguageSpinner.onItemSelectedListener = itemSelectedListener

//        findViewById<ImageButton>(R.id.swapLanguagesButton).setOnClickListener {
//            swapLanguages()
//        }
    }

    private fun swapLanguages() {
        val tempSourceIndex = sourceLanguageSpinner.selectedItemPosition
        val tempSourceLanguage = sourceLanguage

        sourceLanguageSpinner.setSelection(targetLanguageSpinner.selectedItemPosition)
        sourceLanguage = targetLanguage

        targetLanguageSpinner.setSelection(tempSourceIndex)
        targetLanguage = tempSourceLanguage

        // Cập nhật translator sau khi đổi ngôn ngữ
        initializeTranslator()
    }

    private fun initializeTranslator() {
        val options = TranslatorOptions.Builder()
            .setSourceLanguage(sourceLanguage)
            .setTargetLanguage(targetLanguage)
            .build()
        translator = Translation.getClient(options)

        // Models should already be downloaded, but we can check and download if needed
        val conditions = DownloadConditions.Builder()
            .requireWifi()
            .build()
        translator.downloadModelIfNeeded(conditions)
            .addOnSuccessListener {
                // Model is ready to use
            }
            .addOnFailureListener { exception ->
                Toast.makeText(
                    this,
                    "Error preparing translator: ${exception.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
    }

    private fun translateText(text: String) {
        translator.translate(text)
            .addOnSuccessListener { translatedText ->
                translatedTextView.text = translatedText
                lastTranslatedText = translatedText
            }
            .addOnFailureListener { exception ->
                Toast.makeText(this, "Translation failed: ${exception.message}", Toast.LENGTH_LONG)
                    .show()
            }
    }

    private fun setupMicButton() {
        micButton.setOnClickListener {
            if (isListening) {
                stopListening()
            } else {
                clearText() // Xóa text khi bắt đầu một phiên ghi âm mới
                startListening()
            }
        }
    }

    private fun initializeSpeechRecognizer() {
        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this)

        speechRecognizer.setRecognitionListener(object : RecognitionListener {
            override fun onResults(results: Bundle) {
                val matches = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                if (!matches.isNullOrEmpty()) {
                    val recognizedText = matches[0].trim()
                    updateRecognizedText(recognizedText)
                }
                if (continuousListening) {
                    handler.postDelayed({ startListening() }, 1000)
                }
            }

            override fun onPartialResults(partialResults: Bundle) {
                val matches =
                    partialResults.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                if (!matches.isNullOrEmpty()) {
                    val recognizedText = matches[0].trim()
                    updateRecognizedText(recognizedText)
                }
            }

            override fun onEndOfSpeech() {}

            override fun onError(error: Int) {
                when (error) {
                    SpeechRecognizer.ERROR_NO_MATCH,
                    SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> {
                        if (continuousListening) {
                            handler.postDelayed({ startListening() }, 1000)
                        }
                    }

                    else -> {
                        Toast.makeText(this@MainActivity, "Error: $error", Toast.LENGTH_LONG)
                            .show()
                    }
                }
            }

            // Implement other RecognitionListener methods
            override fun onReadyForSpeech(params: Bundle) {}
            override fun onBeginningOfSpeech() {}
            override fun onRmsChanged(rmsdB: Float) {}
            override fun onBufferReceived(buffer: ByteArray) {}
            override fun onEvent(eventType: Int, params: Bundle) {}
        })
    }

    private fun updateRecognizedText(newText: String) {
        val words = newText.split(" ")
        val currentWords = recognizedTextBuilder.toString().split(" ")

        val updatedWords = currentWords.toMutableList()

        for (word in words) {
            if (!updatedWords.contains(word)) {
                updatedWords.add(word)
            }
        }

        recognizedTextBuilder.clear()
        recognizedTextBuilder.append(updatedWords.joinToString(" "))

        recognizedTextView.text = recognizedTextBuilder

        // Translate the recognized text
        translateText(recognizedTextBuilder.toString())
    }


    private fun startListening() {

        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(
                RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
            )
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, sourceLanguage)
            putExtra(
                RecognizerIntent.EXTRA_SPEECH_INPUT_MINIMUM_LENGTH_MILLIS,
                AppConfig.MINIMUM_SPEECH_LENGTH_MS
            )
            putExtra(
                RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS,
                AppConfig.COMPLETE_SILENCE_LENGTH_MS
            )
            putExtra(
                RecognizerIntent.EXTRA_SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS,
                AppConfig.POSSIBLY_SILENCE_LENGTH_MS
            )
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
        }

        speechRecognizer.startListening(intent)
        isListening = true
        continuousListening = true
        updateMicButtonIcon()
    }

    private fun stopListening() {
        speechRecognizer.stopListening()
        isListening = false
        continuousListening = false
        updateMicButtonIcon()

        lifecycleScope.launch {
            try {
                var processedText = recognizedTextBuilder.toString()
                if (processSentenceStructure) {
                    processedText = sentenceProcessor.processSentence(processedText, sourceLanguage)
                }
                Log.d("MainActivity", "Processed text: $processedText")
                recognizedTextView.text = processedText
                translateText(processedText)
                if (speakTranslatedSentence) {
                    speakOut(lastTranslatedText)
                }
            } catch (e: Exception) {
                Log.e("MainActivity", "Error processing sentence", e)
            }
        }
    }

    private fun initializeTextToSpeech() {
        textToSpeech = TextToSpeech(this, this)
    }

    private fun updateMicButtonIcon() {
        micButton.setImageResource(
            if (isListening) android.R.drawable.ic_media_pause
            else android.R.drawable.ic_btn_speak_now
        )
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            val result = textToSpeech.setLanguage(Locale(targetLanguage))
            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                Toast.makeText(
                    this,
                    "Language not supported for Text-to-Speech",
                    Toast.LENGTH_LONG
                ).show()
            }
        } else {
            Toast.makeText(this, "Text-to-Speech initialization failed", Toast.LENGTH_LONG).show()
        }
    }

    private fun speakOut(text: String) {
        textToSpeech.speak(text, TextToSpeech.QUEUE_FLUSH, null, null)
    }


    private fun checkPermission() {
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.RECORD_AUDIO
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.RECORD_AUDIO),
                AppConfig.SPEECH_RECOGNITION_REQUEST_CODE
            )
        }
    }

    private fun clearText() {
        recognizedTextBuilder.clear()
        recognizedTextView.text = ""
        translatedTextView.text = ""
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == AppConfig.SPEECH_RECOGNITION_REQUEST_CODE && grantResults.isNotEmpty()) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "Permission Granted", Toast.LENGTH_LONG).show()
            } else {
                Toast.makeText(this, "Permission Denied", Toast.LENGTH_LONG).show()
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        speechRecognizer.destroy()
        textToSpeech.stop()
        textToSpeech.shutdown()
    }
}