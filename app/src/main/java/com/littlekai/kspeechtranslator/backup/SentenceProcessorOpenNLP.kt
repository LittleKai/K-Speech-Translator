//package com.littlekai.kspeechtranslator.backup
//
//import android.content.Context
//import android.util.Log
//import com.ibm.icu.text.BreakIterator
//import opennlp.tools.sentdetect.SentenceDetectorME
//import opennlp.tools.sentdetect.SentenceModel
//import opennlp.tools.tokenize.TokenizerME
//import opennlp.tools.tokenize.TokenizerModel
//import java.io.File
//import java.io.FileInputStream
//import java.io.InputStream
//import java.util.*
//import java.util.zip.ZipInputStream
//import kotlinx.coroutines.Dispatchers
//import kotlinx.coroutines.withContext
//import okhttp3.OkHttpClient
//import okhttp3.Request
//
//class SentenceProcessorOpenNLP(private val context: Context) {
//    private val TAG = "SentenceProcessor"
//    private val openNlpLanguages = listOf("da", "de", "en", "nl", "pt", "se", "fr")
//    private val sentenceDetectors = mutableMapOf<String, SentenceDetectorME>()
//    private val tokenizers = mutableMapOf<String, TokenizerME>()
//
//    private val fileUrl = "https://drive.google.com/uc?export=download&id=1ylK8swuuqsHj4RZFNZjCw0pIk1druIsD"
//
//    private val client = OkHttpClient()
//
//    suspend fun initialize() {
//        withContext(Dispatchers.IO) {
//            try {
//                val zipFile = downloadZipFile()
//                val unzipDir = extractZipFile(zipFile)
//                val assetsDir = File(unzipDir, "assets")
//
//                openNlpLanguages.forEach { lang ->
//                    val sentFile = File(assetsDir, "$lang-sent.bin")
//                    val tokenFile = File(assetsDir, "$lang-token.bin")
//
//                    if (!sentFile.exists() || !tokenFile.exists()) {
//                        Log.e(TAG, "Required files not found for language $lang")
//                        return@forEach
//                    }
//
//                    val sentenceModelIn: InputStream = FileInputStream(sentFile)
//                    val sentenceModel = SentenceModel(sentenceModelIn)
//                    sentenceDetectors[lang] = SentenceDetectorME(sentenceModel)
//
//                    val tokenizerModelIn: InputStream = FileInputStream(tokenFile)
//                    val tokenizerModel = TokenizerModel(tokenizerModelIn)
//                    tokenizers[lang] = TokenizerME(tokenizerModel)
//                }
//                // Delete the temporary files after use
//                unzipDir.deleteRecursively()
//                zipFile.delete()
//            } catch (e: Exception) {
//                Log.e(TAG, "Error during initialization", e)
//                throw e
//            }
//        }
//    }
//
//    private suspend fun downloadZipFile(): File = withContext(Dispatchers.IO) {
//        val tempFile = File(context.cacheDir, "temp.zip")
//
//        val request = Request.Builder().url(fileUrl).build()
//        val response = client.newCall(request).execute()
//
//        if (!response.isSuccessful) {
//            Log.e(TAG, "Failed to download file: ${response.code}")
//            throw Exception("Failed to download file: ${response.code}")
//        }
//
//        val body = response.body ?: throw Exception("Response body is null")
//
//        val contentType = body.contentType()?.toString() ?: ""
//        Log.d(TAG, "Response content type: $contentType")
//
//        body.byteStream().use { input ->
//            tempFile.outputStream().use { output ->
//                input.copyTo(output)
//            }
//        }
//
//        Log.d(TAG, "File downloaded, size: ${tempFile.length()} bytes")
//        return@withContext tempFile
//    }
//
//    private suspend fun extractZipFile(zipFile: File): File = withContext(Dispatchers.IO) {
//        val unzipDir = File(context.cacheDir, "extracted")
//        unzipDir.mkdirs()
//
//        ZipInputStream(FileInputStream(zipFile)).use { zipIn ->
//            var entry = zipIn.nextEntry
//            while (entry != null) {
//                val newFile = File(unzipDir, entry.name)
//                if (entry.isDirectory) {
//                    newFile.mkdirs()
//                } else {
//                    newFile.parentFile?.mkdirs()  // Ensure parent directory exists
//                    newFile.outputStream().use { zipIn.copyTo(it) }
//                }
//                Log.d(TAG, "Extracted: ${entry.name}")
//                entry = zipIn.nextEntry
//            }
//        }
//
//        return@withContext unzipDir
//    }
//
//    private fun extractDownloadUrl(html: String): String {
//        val regex = Regex("href=\"(/download\\?id=[^\"]*)\"")
//        val matchResult = regex.find(html) ?: throw Exception("Download URL not found in HTML")
//        val relativePath = matchResult.groupValues[1]
//        return "https://drive.usercontent.google.com$relativePath"
//    }
//
//    private suspend fun extractRarFile(rarFile: File): File = withContext(Dispatchers.IO) {
//        val unzipDir = File(context.cacheDir, "extracted")
//        unzipDir.mkdirs()
//
//        // Use a RAR extraction library here
//        // For example, you might use the 'junrar' library:
//        // com.github.junrar.Junrar.extract(rarFile, unzipDir)
//
//        // For now, we'll just log that we need to implement RAR extraction
//        Log.d(TAG, "RAR extraction not implemented. Files should be extracted to: ${unzipDir.absolutePath}")
//
//        return@withContext unzipDir
//    }
//
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
//}