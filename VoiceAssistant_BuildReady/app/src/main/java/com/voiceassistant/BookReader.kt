package com.voiceassistant

import android.content.Context
import android.net.Uri
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import java.io.BufferedReader
import java.io.File
import java.io.FileInputStream
import java.io.InputStreamReader

class BookReader(private val context: Context, private val tts: TextToSpeech) {
    
    private var currentBook: String? = null
    private var currentPage = 0
    private var bookContent: List<String> = emptyList()
    private var isReading = false
    private var readingSpeed = 1.0f
    
    private val prefs = context.getSharedPreferences("book_reader", Context.MODE_PRIVATE)
    
    // Load book from file
    fun loadBook(filePath: String): Boolean {
        return try {
            val file = File(filePath)
            when {
                filePath.endsWith(".txt") -> loadTextBook(file)
                filePath.endsWith(".pdf") -> loadPdfBook(file)
                filePath.endsWith(".epub") -> loadEpubBook(file)
                else -> false
            }
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
    
    private fun loadTextBook(file: File): Boolean {
        val text = file.readText()
        bookContent = splitIntoPages(text)
        currentBook = file.name
        currentPage = prefs.getInt("${file.name}_page", 0)
        return true
    }
    
    private fun loadPdfBook(file: File): Boolean {
        // PDF reading implementation
        try {
            val reader = com.itextpdf.kernel.pdf.PdfReader(file)
            val pdfDoc = com.itextpdf.kernel.pdf.PdfDocument(reader)
            val text = StringBuilder()
            
            for (i in 1..pdfDoc.numberOfPages) {
                val page = pdfDoc.getPage(i)
                val strategy = com.itextpdf.kernel.pdf.canvas.parser.listener.LocationTextExtractionStrategy()
                text.append(com.itextpdf.kernel.pdf.canvas.parser.PdfTextExtractor.getTextFromPage(page, strategy))
                text.append("\n")
            }
            
            pdfDoc.close()
            bookContent = splitIntoPages(text.toString())
            currentBook = file.name
            currentPage = prefs.getInt("${file.name}_page", 0)
            return true
        } catch (e: Exception) {
            e.printStackTrace()
            return false
        }
    }
    
    private fun loadEpubBook(file: File): Boolean {
        // EPUB reading implementation
        try {
            val book = nl.siegmann.epublib.epub.EpubReader().readEpub(FileInputStream(file))
            val text = StringBuilder()
            
            book.contents.forEach { resource ->
                val content = String(resource.data)
                text.append(content.replace(Regex("<[^>]*>"), ""))
                text.append("\n")
            }
            
            bookContent = splitIntoPages(text.toString())
            currentBook = file.name
            currentPage = prefs.getInt("${file.name}_page", 0)
            return true
        } catch (e: Exception) {
            e.printStackTrace()
            return false
        }
    }
    
    private fun splitIntoPages(text: String, wordsPerPage: Int = 300): List<String> {
        val words = text.split(Regex("\\s+"))
        val pages = mutableListOf<String>()
        
        for (i in words.indices step wordsPerPage) {
            val page = words.subList(i, minOf(i + wordsPerPage, words.size)).joinToString(" ")
            pages.add(page)
        }
        
        return pages
    }
    
    // Start reading
    fun startReading(callback: (String) -> Unit) {
        if (bookContent.isEmpty()) {
            callback("No book loaded")
            return
        }
        
        isReading = true
        tts.setSpeechRate(readingSpeed)
        readCurrentPage(callback)
    }
    
    private fun readCurrentPage(callback: (String) -> Unit) {
        if (currentPage >= bookContent.size) {
            callback("Book finished")
            isReading = false
            return
        }
        
        val pageText = bookContent[currentPage]
        callback("Reading page ${currentPage + 1} of ${bookContent.size}")
        
        tts.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) {}
            
            override fun onDone(utteranceId: String?) {
                if (isReading) {
                    currentPage++
                    saveProgress()
                    readCurrentPage(callback)
                }
            }
            
            override fun onError(utteranceId: String?) {
                callback("Reading error")
            }
        })
        
        tts.speak(pageText, TextToSpeech.QUEUE_FLUSH, null, "page_$currentPage")
    }
    
    // Stop reading
    fun stopReading() {
        isReading = false
        tts.stop()
        saveProgress()
    }
    
    // Pause reading
    fun pauseReading() {
        isReading = false
        tts.stop()
        saveProgress()
    }
    
    // Resume reading
    fun resumeReading(callback: (String) -> Unit) {
        startReading(callback)
    }
    
    // Next page
    fun nextPage(callback: (String) -> Unit) {
        if (currentPage < bookContent.size - 1) {
            currentPage++
            saveProgress()
            callback("Page ${currentPage + 1}")
        } else {
            callback("Last page")
        }
    }
    
    // Previous page
    fun previousPage(callback: (String) -> Unit) {
        if (currentPage > 0) {
            currentPage--
            saveProgress()
            callback("Page ${currentPage + 1}")
        } else {
            callback("First page")
        }
    }
    
    // Go to page
    fun goToPage(page: Int, callback: (String) -> Unit) {
        if (page in 0 until bookContent.size) {
            currentPage = page
            saveProgress()
            callback("Going to page ${page + 1}")
        } else {
            callback("Invalid page number")
        }
    }
    
    // Set reading speed
    fun setSpeed(speed: String) {
        readingSpeed = when (speed.lowercase()) {
            "slow" -> 0.7f
            "normal" -> 1.0f
            "fast" -> 1.3f
            "very fast" -> 1.6f
            else -> 1.0f
        }
        tts.setSpeechRate(readingSpeed)
    }
    
    // Add bookmark
    fun addBookmark(name: String = "bookmark") {
        currentBook?.let {
            prefs.edit().putInt("${it}_bookmark_$name", currentPage).apply()
        }
    }
    
    // Go to bookmark
    fun goToBookmark(name: String = "bookmark", callback: (String) -> Unit) {
        currentBook?.let {
            val page = prefs.getInt("${it}_bookmark_$name", -1)
            if (page >= 0) {
                currentPage = page
                callback("Going to bookmark")
            } else {
                callback("Bookmark not found")
            }
        }
    }
    
    // Get summary of current page (using AI)
    fun summarizePage(onlineAssistant: OnlineAssistant, callback: (String) -> Unit) {
        if (currentPage < bookContent.size) {
            val pageText = bookContent[currentPage]
            onlineAssistant.chatGPT("Summarize this in 2 sentences: $pageText") { summary ->
                callback(summary)
            }
        }
    }
    
    // Save progress
    private fun saveProgress() {
        currentBook?.let {
            prefs.edit().putInt("${it}_page", currentPage).apply()
        }
    }
    
    // Get current status
    fun getStatus(): String {
        return if (currentBook != null) {
            "Reading: $currentBook, Page ${currentPage + 1} of ${bookContent.size}"
        } else {
            "No book loaded"
        }
    }
    
    // List recent books
    fun getRecentBooks(): List<String> {
        return prefs.all.keys.filter { it.endsWith("_page") }
            .map { it.replace("_page", "") }
    }
}
