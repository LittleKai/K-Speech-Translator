//package com.littlekai.kspeechtranslator.backup
//
//import java.text.BreakIterator
//import java.util.*
//
//class SentenceProcessorICU4J {
//    fun process(text: String, languageCode: String): String {
//        val locale = Locale(languageCode)
//        val sentences = detectSentences(text, locale)
//        return sentences.joinToString(" ") { sentence ->
//            processSentence(sentence, locale)
//        }
//    }
//
//    private fun detectSentences(text: String, locale: Locale): List<String> {
//        val iterator = BreakIterator.getSentenceInstance(locale)
//        iterator.setText(text)
//        val sentences = mutableListOf<String>()
//        var start = iterator.first()
//        var end = iterator.next()
//
//        while (end != BreakIterator.DONE) {
//            val sentence = text.substring(start, end).trim()
//            if (sentence.isNotEmpty()) {
//                sentences.add(sentence)
//            }
//            start = end
//            end = iterator.next()
//        }
//
//        // If no sentences were detected, treat the whole text as one sentence
//        if (sentences.isEmpty()) {
//            sentences.add(text.trim())
//        }
//
//        return sentences
//    }
//
//    private fun processSentence(sentence: String, locale: Locale): String {
//        val wordIterator = BreakIterator.getWordInstance(locale)
//        wordIterator.setText(sentence)
//
//        val processedWords = mutableListOf<String>()
//        var start = wordIterator.first()
//        var end = wordIterator.next()
//        var isFirstWord = true
//
//        while (end != BreakIterator.DONE) {
//            val word = sentence.substring(start, end)
//            if (word.isNotBlank()) {
//                processedWords.add(if (isFirstWord) word.capitalize(locale) else word)
//                isFirstWord = false
//            }
//            start = end
//            end = wordIterator.next()
//        }
//
//        var processed = processedWords.joinToString(" ")
//
//        if (!".!?".contains(processed.last())) {
//            processed += "."
//        }
//
//        return processed
//    }
//
//    private fun String.capitalize(locale: Locale): String {
//        return this.replaceFirstChar { if (it.isLowerCase()) it.titlecase(locale) else it.toString() }
//    }
//}