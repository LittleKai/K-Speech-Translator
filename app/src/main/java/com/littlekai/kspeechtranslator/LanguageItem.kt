package com.littlekai.kspeechtranslator

data class LanguageItem(
    val name: String,
    val code: String,
    var isDownloaded: Boolean = false
)