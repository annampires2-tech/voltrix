package com.voiceassistant

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions

class OCRProcessor(private val context: Context) {
    
    private val textRecognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
    
    // Extract text from image (OCR)
    fun extractTextFromImage(imagePath: String, callback: (String) -> Unit) {
        try {
            val bitmap = BitmapFactory.decodeFile(imagePath)
            val image = InputImage.fromBitmap(bitmap, 0)
            
            textRecognizer.process(image)
                .addOnSuccessListener { visionText ->
                    val extractedText = visionText.text
                    callback(extractedText)
                }
                .addOnFailureListener {
                    callback("")
                }
        } catch (e: Exception) {
            callback("")
        }
    }
    
    // Extract text from multiple images
    fun extractTextFromImages(imagePaths: List<String>, callback: (String) -> Unit) {
        val allText = StringBuilder()
        var processed = 0
        
        imagePaths.forEach { path ->
            extractTextFromImage(path) { text ->
                allText.append(text).append("\n\n")
                processed++
                
                if (processed == imagePaths.size) {
                    callback(allText.toString())
                }
            }
        }
    }
    
    // Scan document and extract structured text
    fun scanDocument(imagePath: String, callback: (DocumentData) -> Unit) {
        extractTextFromImage(imagePath) { text ->
            val lines = text.split("\n")
            val paragraphs = text.split("\n\n")
            
            callback(DocumentData(
                fullText = text,
                lines = lines,
                paragraphs = paragraphs,
                wordCount = text.split("\\s+".toRegex()).size
            ))
        }
    }
    
    // Extract specific data (emails, phone numbers, etc.)
    fun extractData(imagePath: String, callback: (ExtractedData) -> Unit) {
        extractTextFromImage(imagePath) { text ->
            val emails = Regex("[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}").findAll(text).map { it.value }.toList()
            val phones = Regex("\\+?\\d[\\d\\s-]{8,}\\d").findAll(text).map { it.value }.toList()
            val urls = Regex("https?://[^\\s]+").findAll(text).map { it.value }.toList()
            val dates = Regex("\\d{1,2}[/-]\\d{1,2}[/-]\\d{2,4}").findAll(text).map { it.value }.toList()
            
            callback(ExtractedData(
                emails = emails,
                phoneNumbers = phones,
                urls = urls,
                dates = dates
            ))
        }
    }
    
    // Translate extracted text
    fun extractAndTranslate(imagePath: String, targetLang: String, onlineAssistant: OnlineAssistant, callback: (String) -> Unit) {
        extractTextFromImage(imagePath) { text ->
            if (text.isNotEmpty()) {
                onlineAssistant.translate(text, targetLang) { translated ->
                    callback(translated)
                }
            } else {
                callback("No text found")
            }
        }
    }
}

data class DocumentData(
    val fullText: String,
    val lines: List<String>,
    val paragraphs: List<String>,
    val wordCount: Int
)

data class ExtractedData(
    val emails: List<String>,
    val phoneNumbers: List<String>,
    val urls: List<String>,
    val dates: List<String>
)
