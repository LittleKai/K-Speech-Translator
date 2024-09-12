//package com.littlekai.kspeechtranslator.backup
//
//import android.content.Context
//import com.ibm.icu.text.BreakIterator
//import opennlp.tools.sentdetect.SentenceDetectorME
//import opennlp.tools.sentdetect.SentenceModel
//import opennlp.tools.tokenize.TokenizerME
//import opennlp.tools.tokenize.TokenizerModel
//import java.io.InputStream
//import java.util.*
//
//class SentenceProcessor1(private val context: Context) {
//    private val openNlpLanguages = listOf("da", "de", "en", "nl", "pt", "se","fr")
//    private val sentenceDetectors = mutableMapOf<String, SentenceDetectorME>()
//    private val tokenizers = mutableMapOf<String, TokenizerME>()
//
//    init {
//        openNlpLanguages.forEach { lang ->
//            val sentenceModelIn: InputStream = context.assets.open("$lang-sent.bin")
//            val sentenceModel = SentenceModel(sentenceModelIn)
//            sentenceDetectors[lang] = SentenceDetectorME(sentenceModel)
//
//            val tokenizerModelIn: InputStream = context.assets.open("$lang-token.bin")
//            val tokenizerModel = TokenizerModel(tokenizerModelIn)
//            tokenizers[lang] = TokenizerME(tokenizerModel)
//        }
//    }
//
//    fun process(text: String, languageCode: String): String {
//        val sentences = if (openNlpLanguages.contains(languageCode)) {
//            sentenceDetectors[languageCode]?.sentDetect(text) ?: icuSentenceDetect(text, languageCode)
//        } else {
//            icuSentenceDetect(text, languageCode)
//        }
//
//        return sentences.joinToString(" ") { sentence ->
//            processSentence(sentence, languageCode)
//        }
//    }
//
//    private fun icuSentenceDetect(text: String, languageCode: String): Array<String> {
//        val iterator = BreakIterator.getSentenceInstance(Locale(languageCode))
//        iterator.setText(text)
//        val sentences = mutableListOf<String>()
//        var start = iterator.first()
//        var end = iterator.next()
//        while (end != BreakIterator.DONE) {
//            sentences.add(text.substring(start, end).trim())
//            start = end
//            end = iterator.next()
//        }
//        return sentences.toTypedArray()
//    }
//
//    private fun processSentence(sentence: String, languageCode: String): String {
//        val tokens = if (openNlpLanguages.contains(languageCode)) {
//            tokenizers[languageCode]?.tokenize(sentence) ?: sentence.split(" ").toTypedArray()
//        } else {
//            sentence.split(" ").toTypedArray()
//        }
//
//        val processedTokens = tokens.mapIndexed { index, token ->
//            when {
//                index == 0 -> token.capitalize()
//                languageCode == "en" && token.toLowerCase() == "i" -> "I"
//                else -> token
//            }
//        }
//
//        var processed = processedTokens.joinToString(" ")
//
//        if (!".!?".contains(processed.last())) {
//            processed += "."
//        }
//
//        return processed
//    }
//
//}