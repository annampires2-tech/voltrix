package com.voiceassistant

import android.content.Context
import android.graphics.*
import android.media.MediaMetadataRetriever
import android.net.Uri
import com.arthenica.ffmpegkit.FFmpegKit
import com.arthenica.ffmpegkit.ReturnCode
import java.io.File
import java.io.FileOutputStream

class ImageVideoEditor(private val context: Context) {
    
    // Image editing
    fun applyFilter(imagePath: String, filter: String, outputPath: String): Boolean {
        return try {
            val bitmap = BitmapFactory.decodeFile(imagePath)
            val filtered = when (filter.lowercase()) {
                "grayscale" -> applyGrayscale(bitmap)
                "sepia" -> applySepia(bitmap)
                "blur" -> applyBlur(bitmap)
                "brighten" -> adjustBrightness(bitmap, 50f)
                "darken" -> adjustBrightness(bitmap, -50f)
                "contrast" -> adjustContrast(bitmap, 1.5f)
                "sharpen" -> applySharpen(bitmap)
                else -> bitmap
            }
            
            saveBitmap(filtered, outputPath)
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
    
    private fun applyGrayscale(bitmap: Bitmap): Bitmap {
        val result = Bitmap.createBitmap(bitmap.width, bitmap.height, bitmap.config)
        val canvas = Canvas(result)
        val paint = Paint()
        val colorMatrix = ColorMatrix().apply { setSaturation(0f) }
        paint.colorFilter = ColorMatrixColorFilter(colorMatrix)
        canvas.drawBitmap(bitmap, 0f, 0f, paint)
        return result
    }
    
    private fun applySepia(bitmap: Bitmap): Bitmap {
        val result = Bitmap.createBitmap(bitmap.width, bitmap.height, bitmap.config)
        val canvas = Canvas(result)
        val paint = Paint()
        val colorMatrix = ColorMatrix().apply {
            set(floatArrayOf(
                0.393f, 0.769f, 0.189f, 0f, 0f,
                0.349f, 0.686f, 0.168f, 0f, 0f,
                0.272f, 0.534f, 0.131f, 0f, 0f,
                0f, 0f, 0f, 1f, 0f
            ))
        }
        paint.colorFilter = ColorMatrixColorFilter(colorMatrix)
        canvas.drawBitmap(bitmap, 0f, 0f, paint)
        return result
    }
    
    private fun applyBlur(bitmap: Bitmap): Bitmap {
        val result = Bitmap.createBitmap(bitmap.width, bitmap.height, bitmap.config)
        val canvas = Canvas(result)
        val paint = Paint()
        paint.maskFilter = BlurMaskFilter(15f, BlurMaskFilter.Blur.NORMAL)
        canvas.drawBitmap(bitmap, 0f, 0f, paint)
        return result
    }
    
    private fun adjustBrightness(bitmap: Bitmap, value: Float): Bitmap {
        val result = Bitmap.createBitmap(bitmap.width, bitmap.height, bitmap.config)
        val canvas = Canvas(result)
        val paint = Paint()
        val colorMatrix = ColorMatrix(floatArrayOf(
            1f, 0f, 0f, 0f, value,
            0f, 1f, 0f, 0f, value,
            0f, 0f, 1f, 0f, value,
            0f, 0f, 0f, 1f, 0f
        ))
        paint.colorFilter = ColorMatrixColorFilter(colorMatrix)
        canvas.drawBitmap(bitmap, 0f, 0f, paint)
        return result
    }
    
    private fun adjustContrast(bitmap: Bitmap, contrast: Float): Bitmap {
        val result = Bitmap.createBitmap(bitmap.width, bitmap.height, bitmap.config)
        val canvas = Canvas(result)
        val paint = Paint()
        val scale = contrast
        val translate = (-.5f * scale + .5f) * 255f
        val colorMatrix = ColorMatrix(floatArrayOf(
            scale, 0f, 0f, 0f, translate,
            0f, scale, 0f, 0f, translate,
            0f, 0f, scale, 0f, translate,
            0f, 0f, 0f, 1f, 0f
        ))
        paint.colorFilter = ColorMatrixColorFilter(colorMatrix)
        canvas.drawBitmap(bitmap, 0f, 0f, paint)
        return result
    }
    
    private fun applySharpen(bitmap: Bitmap): Bitmap {
        val result = Bitmap.createBitmap(bitmap.width, bitmap.height, bitmap.config)
        // Sharpening kernel
        return result
    }
    
    // Crop image
    fun cropImage(imagePath: String, x: Int, y: Int, width: Int, height: Int, outputPath: String): Boolean {
        return try {
            val bitmap = BitmapFactory.decodeFile(imagePath)
            val cropped = Bitmap.createBitmap(bitmap, x, y, width, height)
            saveBitmap(cropped, outputPath)
            true
        } catch (e: Exception) {
            false
        }
    }
    
    // Rotate image
    fun rotateImage(imagePath: String, degrees: Float, outputPath: String): Boolean {
        return try {
            val bitmap = BitmapFactory.decodeFile(imagePath)
            val matrix = Matrix().apply { postRotate(degrees) }
            val rotated = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
            saveBitmap(rotated, outputPath)
            true
        } catch (e: Exception) {
            false
        }
    }
    
    // Resize image
    fun resizeImage(imagePath: String, width: Int, height: Int, outputPath: String): Boolean {
        return try {
            val bitmap = BitmapFactory.decodeFile(imagePath)
            val resized = Bitmap.createScaledBitmap(bitmap, width, height, true)
            saveBitmap(resized, outputPath)
            true
        } catch (e: Exception) {
            false
        }
    }
    
    // Video editing with FFmpeg
    fun trimVideo(inputPath: String, startTime: String, duration: String, outputPath: String, callback: (Boolean) -> Unit) {
        val command = "-i $inputPath -ss $startTime -t $duration -c copy $outputPath"
        FFmpegKit.executeAsync(command) { session ->
            callback(ReturnCode.isSuccess(session.returnCode))
        }
    }
    
    fun addAudioToVideo(videoPath: String, audioPath: String, outputPath: String, callback: (Boolean) -> Unit) {
        val command = "-i $videoPath -i $audioPath -c:v copy -c:a aac -map 0:v:0 -map 1:a:0 $outputPath"
        FFmpegKit.executeAsync(command) { session ->
            callback(ReturnCode.isSuccess(session.returnCode))
        }
    }
    
    fun mergeVideos(video1: String, video2: String, outputPath: String, callback: (Boolean) -> Unit) {
        val listFile = File(context.cacheDir, "videos.txt")
        listFile.writeText("file '$video1'\nfile '$video2'")
        val command = "-f concat -safe 0 -i ${listFile.absolutePath} -c copy $outputPath"
        FFmpegKit.executeAsync(command) { session ->
            callback(ReturnCode.isSuccess(session.returnCode))
        }
    }
    
    fun changeVideoSpeed(inputPath: String, speed: Float, outputPath: String, callback: (Boolean) -> Unit) {
        val videoSpeed = 1 / speed
        val audioSpeed = speed
        val command = "-i $inputPath -filter:v setpts=$videoSpeed*PTS -filter:a atempo=$audioSpeed $outputPath"
        FFmpegKit.executeAsync(command) { session ->
            callback(ReturnCode.isSuccess(session.returnCode))
        }
    }
    
    fun extractAudioFromVideo(videoPath: String, outputPath: String, callback: (Boolean) -> Unit) {
        val command = "-i $videoPath -vn -acodec copy $outputPath"
        FFmpegKit.executeAsync(command) { session ->
            callback(ReturnCode.isSuccess(session.returnCode))
        }
    }
    
    fun addWatermark(videoPath: String, watermarkPath: String, outputPath: String, callback: (Boolean) -> Unit) {
        val command = "-i $videoPath -i $watermarkPath -filter_complex overlay=10:10 $outputPath"
        FFmpegKit.executeAsync(command) { session ->
            callback(ReturnCode.isSuccess(session.returnCode))
        }
    }
    
    // Save bitmap
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
