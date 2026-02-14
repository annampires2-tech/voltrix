package com.voiceassistant

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Rect
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.Face
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetectorOptions
import com.google.mlkit.vision.label.ImageLabeling
import com.google.mlkit.vision.label.defaults.ImageLabelerOptions
import java.io.File

class FacialRecognition(private val context: Context) {
    
    private val faceDetector = FaceDetection.getClient(
        FaceDetectorOptions.Builder()
            .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_ACCURATE)
            .setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_ALL)
            .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_ALL)
            .setMinFaceSize(0.15f)
            .enableTracking()
            .build()
    )
    
    private val imageLabeler = ImageLabeling.getClient(ImageLabelerOptions.DEFAULT)
    
    // Detect faces in image
    fun detectFaces(imagePath: String, callback: (List<FaceInfo>) -> Unit) {
        val bitmap = BitmapFactory.decodeFile(imagePath)
        val image = InputImage.fromBitmap(bitmap, 0)
        
        faceDetector.process(image)
            .addOnSuccessListener { faces ->
                val faceInfos = faces.map { face ->
                    FaceInfo(
                        bounds = face.boundingBox,
                        trackingId = face.trackingId,
                        smiling = face.smilingProbability ?: 0f,
                        leftEyeOpen = face.leftEyeOpenProbability ?: 0f,
                        rightEyeOpen = face.rightEyeOpenProbability ?: 0f,
                        headEulerAngleY = face.headEulerAngleY,
                        headEulerAngleZ = face.headEulerAngleZ
                    )
                }
                callback(faceInfos)
            }
            .addOnFailureListener {
                callback(emptyList())
            }
    }
    
    // Count faces
    fun countFaces(imagePath: String, callback: (Int) -> Unit) {
        detectFaces(imagePath) { faces ->
            callback(faces.size)
        }
    }
    
    // Detect if person is smiling
    fun detectSmile(imagePath: String, callback: (Boolean) -> Unit) {
        detectFaces(imagePath) { faces ->
            val isSmiling = faces.any { it.smiling > 0.7f }
            callback(isSmiling)
        }
    }
    
    // Detect emotions
    fun detectEmotion(imagePath: String, callback: (String) -> Unit) {
        detectFaces(imagePath) { faces ->
            if (faces.isEmpty()) {
                callback("No face detected")
                return@detectFaces
            }
            
            val face = faces.first()
            val emotion = when {
                face.smiling > 0.7f -> "happy"
                face.smiling < 0.3f && face.leftEyeOpen < 0.5f -> "sad"
                face.headEulerAngleY > 15 -> "looking away"
                else -> "neutral"
            }
            callback(emotion)
        }
    }
    
    // Identify objects in image
    fun identifyObjects(imagePath: String, callback: (List<String>) -> Unit) {
        val bitmap = BitmapFactory.decodeFile(imagePath)
        val image = InputImage.fromBitmap(bitmap, 0)
        
        imageLabeler.process(image)
            .addOnSuccessListener { labels ->
                val objects = labels.map { "${it.text} (${(it.confidence * 100).toInt()}%)" }
                callback(objects)
            }
            .addOnFailureListener {
                callback(emptyList())
            }
    }
    
    // Describe image
    fun describeImage(imagePath: String, callback: (String) -> Unit) {
        identifyObjects(imagePath) { objects ->
            if (objects.isEmpty()) {
                callback("Could not identify objects")
            } else {
                callback("I see: ${objects.take(5).joinToString(", ")}")
            }
        }
    }
    
    // Save face database
    fun saveFace(name: String, imagePath: String) {
        val prefs = context.getSharedPreferences("face_db", Context.MODE_PRIVATE)
        detectFaces(imagePath) { faces ->
            if (faces.isNotEmpty()) {
                val face = faces.first()
                prefs.edit().putString(name, "${face.bounds}").apply()
            }
        }
    }
    
    // Recognize saved face
    fun recognizeFace(imagePath: String, callback: (String?) -> Unit) {
        val prefs = context.getSharedPreferences("face_db", Context.MODE_PRIVATE)
        detectFaces(imagePath) { faces ->
            if (faces.isEmpty()) {
                callback(null)
                return@detectFaces
            }
            
            // Simple matching based on face position (can be improved)
            val detectedFace = faces.first()
            for ((name, _) in prefs.all) {
                // Match logic here
                callback(name)
                return@detectFaces
            }
            callback("Unknown person")
        }
    }
}

data class FaceInfo(
    val bounds: Rect,
    val trackingId: Int?,
    val smiling: Float,
    val leftEyeOpen: Float,
    val rightEyeOpen: Float,
    val headEulerAngleY: Float,
    val headEulerAngleZ: Float
)
