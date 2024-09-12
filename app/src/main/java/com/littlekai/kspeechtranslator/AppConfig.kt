package com.littlekai.kspeechtranslator

object AppConfig {
    // Danh sách ngôn ngữ được hỗ trợ
    val supportedLanguages = mapOf(
        "English" to "en", "Vietnamese" to "vi", "French" to "fr",
        "German" to "de", "Spanish" to "es", "Italian" to "it",
        "Japanese" to "ja", "Korean" to "ko", "Russian" to "ru", "Chinese" to "zh",
        "Thai" to "th"
    )

    // Ngôn ngữ mặc định cho nguồn và đích
    const val DEFAULT_SOURCE_LANGUAGE = "en"
    const val DEFAULT_TARGET_LANGUAGE = "vi"

    // Cấu hình cho việc nhận dạng giọng nói
    const val KEY_PROCESS_SENTENCE_STRUCTURE = "process_sentence_structure"
    const val KEY_SPEAK_TRANSLATED_SENTENCE = "speak_translated_sentence"
    const val KEY_MINIMUM_SPEECH_LENGTH = "minimum_speech_length"
    const val KEY_COMPLETE_SILENCE_LENGTH = "complete_silence_length"
    const val KEY_POSSIBLY_SILENCE_LENGTH = "possibly_silence_length"
    const val KEY_GEMINI_API_KEY = "gemini_api_key"
    const val KEY_SPEECH_VOLUME_THRESHOLD = "speech_volume_threshold"
    const val KEY_USE_NOISE_SUPPRESSION = "use_noise_suppression"

    const val DEFAULT_SPEECH_VOLUME_THRESHOLD = 50
    const val SPEECH_RECOGNITION_REQUEST_CODE = 100
    const val MINIMUM_SPEECH_LENGTH_MS = 30000
    const val COMPLETE_SILENCE_LENGTH_MS = 1500
    const val POSSIBLY_SILENCE_LENGTH_MS = 1500

    // Các cài đặt khác
    const val MAX_TRANSLATION_LENGTH = 1000 // Số ký tự tối đa cho một lần dịch
    const val TRANSLATION_TIMEOUT_MS = 10000 // Thời gian chờ tối đa cho một lần dịch

    // Các khóa cho SharedPreferences
    const val PREF_NAME = "TranslationAppPrefs"
    const val KEY_DOWNLOADED_MODELS = "downloaded_models"
    const val KEY_FIRST_LAUNCH = "first_launch"

    // Các cài đặt liên quan đến UI
//    const val SPINNER_ITEM_LAYOUT = R.layout.spinner_item_language
    const val TRANSLATION_TEXT_SIZE_SP = 18f
    const val RECOGNIZED_TEXT_SIZE_SP = 16f

    // Các icon
    val ICON_CHECK = R.drawable.ic_check
    val ICON_DOWNLOAD = R.drawable.ic_download
//    val ICON_MIC = R.drawable.ic_mic
//    val ICON_TRANSLATE = R.drawable.ic_translate

    // Các màu sắc (nếu bạn muốn định nghĩa màu sắc cụ thể)
    const val COLOR_PRIMARY = "#3F51B5"
    const val COLOR_ACCENT = "#FF4081"
    const val COLOR_BACKGROUND = "#FFFFFF"
}