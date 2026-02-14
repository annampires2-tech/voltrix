package com.voiceassistant

import android.content.Context
import android.graphics.*
import com.arthenica.ffmpegkit.FFmpegKit
import com.arthenica.ffmpegkit.ReturnCode
import java.io.File
import java.io.FileOutputStream

class MemeGenerator(private val context: Context) {
    
    // Create meme from image
    fun createImageMeme(
        imagePath: String,
        topText: String,
        bottomText: String,
        outputPath: String
    ): Boolean {
        return try {
            val bitmap = BitmapFactory.decodeFile(imagePath)
            val memeBitmap = addTextToImage(bitmap, topText, bottomText)
            saveBitmap(memeBitmap, outputPath)
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
    
    // Extract frame from video and make meme
    fun createVideoMeme(
        videoPath: String,
        timestamp: String,
        topText: String,
        bottomText: String,
        outputPath: String,
        callback: (Boolean) -> Unit
    ) {
        val framePath = "${context.cacheDir}/meme_frame.jpg"
        
        // Extract frame at timestamp
        val extractCommand = "-i $videoPath -ss $timestamp -vframes 1 $framePath"
        
        FFmpegKit.executeAsync(extractCommand) { session ->
            if (ReturnCode.isSuccess(session.returnCode)) {
                val success = createImageMeme(framePath, topText, bottomText, outputPath)
                callback(success)
            } else {
                callback(false)
            }
        }
    }
    
    // Add text to image with meme styling
    private fun addTextToImage(bitmap: Bitmap, topText: String, bottomText: String): Bitmap {
        val result = bitmap.copy(Bitmap.Config.ARGB_8888, true)
        val canvas = Canvas(result)
        
        // Meme font settings
        val paint = Paint().apply {
            color = Color.WHITE
            textSize = bitmap.width * 0.08f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
            style = Paint.Style.FILL
            isAntiAlias = true
        }
        
        // Black stroke for text
        val strokePaint = Paint().apply {
            color = Color.BLACK
            textSize = bitmap.width * 0.08f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
            style = Paint.Style.STROKE
            strokeWidth = 8f
            isAntiAlias = true
        }
        
        val centerX = bitmap.width / 2f
        
        // Draw top text
        if (topText.isNotEmpty()) {
            val topY = bitmap.height * 0.15f
            val topLines = wrapText(topText.uppercase(), paint, bitmap.width * 0.9f)
            topLines.forEachIndexed { index, line ->
                val y = topY + (index * paint.textSize * 1.2f)
                canvas.drawText(line, centerX, y, strokePaint)
                canvas.drawText(line, centerX, y, paint)
            }
        }
        
        // Draw bottom text
        if (bottomText.isNotEmpty()) {
            val bottomY = bitmap.height * 0.9f
            val bottomLines = wrapText(bottomText.uppercase(), paint, bitmap.width * 0.9f)
            bottomLines.forEachIndexed { index, line ->
                val y = bottomY - ((bottomLines.size - index - 1) * paint.textSize * 1.2f)
                canvas.drawText(line, centerX, y, strokePaint)
                canvas.drawText(line, centerX, y, paint)
            }
        }
        
        return result
    }
    
    // Wrap text to fit width
    private fun wrapText(text: String, paint: Paint, maxWidth: Float): List<String> {
        val words = text.split(" ")
        val lines = mutableListOf<String>()
        var currentLine = ""
        
        words.forEach { word ->
            val testLine = if (currentLine.isEmpty()) word else "$currentLine $word"
            val width = paint.measureText(testLine)
            
            if (width > maxWidth && currentLine.isNotEmpty()) {
                lines.add(currentLine)
                currentLine = word
            } else {
                currentLine = testLine
            }
        }
        
        if (currentLine.isNotEmpty()) {
            lines.add(currentLine)
        }
        
        return lines
    }
    
    // Generate meme with AI caption
    fun generateAIMeme(imagePath: String, outputPath: String, onlineAssistant: OnlineAssistant, callback: (Boolean) -> Unit) {
        // Use AI to generate funny caption
        onlineAssistant.chatGPT("Generate a funny meme caption for this image. Give me top text and bottom text separated by |") { response ->
            val parts = response.split("|")
            val topText = parts.getOrNull(0)?.trim() ?: "WHEN YOU"
            val bottomText = parts.getOrNull(1)?.trim() ?: "BOTTOM TEXT"
            
            val success = createImageMeme(imagePath, topText, bottomText, outputPath)
            callback(success)
        }
    }
    
    // Popular meme templates
    fun getMemeTemplate(templateName: String): MemeTemplate? {
        return memeTemplates[templateName.lowercase()]
    }
    
    private val memeTemplates = mapOf(
        "drake" to MemeTemplate("Drake", "drake.jpg", 2),
        "distracted boyfriend" to MemeTemplate("Distracted Boyfriend", "distracted.jpg", 3),
        "two buttons" to MemeTemplate("Two Buttons", "buttons.jpg", 3),
        "change my mind" to MemeTemplate("Change My Mind", "mind.jpg", 1),
        "is this" to MemeTemplate("Is This", "isthis.jpg", 2),
        "expanding brain" to MemeTemplate("Expanding Brain", "brain.jpg", 4)
    )
    
    // Create meme from template
    fun createFromTemplate(
        templateName: String,
        texts: List<String>,
        outputPath: String
    ): Boolean {
        val template = getMemeTemplate(templateName) ?: return false
        
        // For simple templates, use top/bottom text
        return if (texts.size >= 2) {
            createImageMeme(
                "${context.cacheDir}/${template.imagePath}",
                texts[0],
                texts.getOrNull(1) ?: "",
                outputPath
            )
        } else {
            false
        }
    }
    
    // Extract funny moments from video
    fun extractFunnyMoments(videoPath: String, callback: (List<String>) -> Unit) {
        // Extract frames at intervals
        val moments = mutableListOf<String>()
        val duration = getVideoDuration(videoPath)
        
        // Extract 5 frames evenly distributed
        for (i in 1..5) {
            val timestamp = (duration / 6) * i
            val framePath = "${context.cacheDir}/moment_$i.jpg"
            
            val command = "-i $videoPath -ss $timestamp -vframes 1 $framePath"
            FFmpegKit.execute(command)
            
            if (File(framePath).exists()) {
                moments.add(framePath)
            }
        }
        
        callback(moments)
    }
    
    private fun getVideoDuration(videoPath: String): Int {
        // Get video duration in seconds
        return 60 // Placeholder
    }
    
    // Add caption to video (like TikTok)
    fun addCaptionToVideo(
        videoPath: String,
        caption: String,
        outputPath: String,
        callback: (Boolean) -> Unit
    ) {
        val command = "-i $videoPath -vf \"drawtext=text='$caption':fontcolor=white:fontsize=48:box=1:boxcolor=black@0.5:boxborderw=5:x=(w-text_w)/2:y=h-th-20\" -codec:a copy $outputPath"
        
        FFmpegKit.executeAsync(command) { session ->
            callback(ReturnCode.isSuccess(session.returnCode))
        }
    }
    
    // Create GIF meme from video
    fun createGIFMeme(
        videoPath: String,
        startTime: String,
        duration: String,
        topText: String,
        bottomText: String,
        outputPath: String,
        callback: (Boolean) -> Unit
    ) {
        val textFilter = "drawtext=text='$topText':fontcolor=white:fontsize=48:x=(w-text_w)/2:y=20:box=1:boxcolor=black@0.5," +
                "drawtext=text='$bottomText':fontcolor=white:fontsize=48:x=(w-text_w)/2:y=h-th-20:box=1:boxcolor=black@0.5"
        
        val command = "-i $videoPath -ss $startTime -t $duration -vf \"$textFilter,fps=15,scale=480:-1:flags=lanczos\" -loop 0 $outputPath"
        
        FFmpegKit.executeAsync(command) { session ->
            callback(ReturnCode.isSuccess(session.returnCode))
        }
    }
    
    // Deep fry meme (oversaturated, distorted)
    fun deepFryMeme(imagePath: String, outputPath: String): Boolean {
        return try {
            val bitmap = BitmapFactory.decodeFile(imagePath)
            val friedBitmap = applyDeepFryEffect(bitmap)
            saveBitmap(friedBitmap, outputPath)
            true
        } catch (e: Exception) {
            false
        }
    }
    
    private fun applyDeepFryEffect(bitmap: Bitmap): Bitmap {
        val result = bitmap.copy(Bitmap.Config.ARGB_8888, true)
        val canvas = Canvas(result)
        val paint = Paint()
        
        // Increase saturation and contrast
        val colorMatrix = ColorMatrix(floatArrayOf(
            2f, 0f, 0f, 0f, 50f,
            0f, 2f, 0f, 0f, 50f,
            0f, 0f, 2f, 0f, 50f,
            0f, 0f, 0f, 1f, 0f
        ))
        
        paint.colorFilter = ColorMatrixColorFilter(colorMatrix)
        canvas.drawBitmap(bitmap, 0f, 0f, paint)
        
        return result
    }
    
    private fun saveBitmap(bitmap: Bitmap, path: String): Boolean {
        return try {
            FileOutputStream(path).use { out ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 90, out)
            }
            true
        } catch (e: Exception) {
            false
        }
    }
}

data class MemeTemplate(
    val name: String,
    val imagePath: String,
    val textFields: Int
)
