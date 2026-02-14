package com.voiceassistant

import android.content.Context
import java.util.*

data class EmotionalState(
    val emotion: String,
    val intensity: Float,
    val timestamp: Long
)

class EmotionalIntelligence(private val context: Context) {
    
    private val prefs = context.getSharedPreferences("emotional_ai", Context.MODE_PRIVATE)
    
    // Emotion keywords
    private val emotionKeywords = mapOf(
        "happy" to listOf("happy", "great", "awesome", "wonderful", "excited", "love", "amazing", "fantastic"),
        "sad" to listOf("sad", "depressed", "down", "unhappy", "miserable", "crying", "upset"),
        "angry" to listOf("angry", "mad", "furious", "annoyed", "frustrated", "hate"),
        "anxious" to listOf("anxious", "worried", "nervous", "stressed", "scared", "afraid"),
        "tired" to listOf("tired", "exhausted", "sleepy", "fatigue", "drained"),
        "excited" to listOf("excited", "thrilled", "pumped", "energized"),
        "calm" to listOf("calm", "relaxed", "peaceful", "chill", "zen"),
        "confused" to listOf("confused", "lost", "don't understand", "unclear"),
        "grateful" to listOf("thank", "grateful", "appreciate", "thanks")
    )
    
    // Detect emotion from text
    fun detectEmotion(text: String): EmotionalState {
        val lowerText = text.lowercase()
        var detectedEmotion = "neutral"
        var maxMatches = 0
        
        for ((emotion, keywords) in emotionKeywords) {
            val matches = keywords.count { lowerText.contains(it) }
            if (matches > maxMatches) {
                maxMatches = matches
                detectedEmotion = emotion
            }
        }
        
        val intensity = when (maxMatches) {
            0 -> 0.3f
            1 -> 0.6f
            else -> 1.0f
        }
        
        val state = EmotionalState(detectedEmotion, intensity, System.currentTimeMillis())
        saveEmotionalState(state)
        return state
    }
    
    // Generate empathetic response
    fun generateEmpatheticResponse(emotion: String, intensity: Float): String {
        return when (emotion) {
            "happy" -> when {
                intensity > 0.8f -> "That's wonderful! I'm so happy for you!"
                intensity > 0.5f -> "That's great to hear!"
                else -> "I'm glad you're feeling good"
            }
            "sad" -> when {
                intensity > 0.8f -> "I'm really sorry you're feeling this way. I'm here for you. Would you like to talk about it?"
                intensity > 0.5f -> "I understand you're feeling down. Is there anything I can do to help?"
                else -> "I hope things get better soon"
            }
            "angry" -> when {
                intensity > 0.8f -> "I can sense you're really upset. Take a deep breath. I'm here to help."
                intensity > 0.5f -> "I understand your frustration. Let's work through this together."
                else -> "I hear you. How can I assist?"
            }
            "anxious" -> when {
                intensity > 0.8f -> "It's okay to feel anxious. Let's take this one step at a time. Would you like me to play calming music?"
                intensity > 0.5f -> "I understand you're worried. Remember to breathe. I'm here to help."
                else -> "Try not to worry too much. I've got your back."
            }
            "tired" -> "You sound tired. Maybe you should rest? I can set a reminder for later."
            "excited" -> "Your excitement is contagious! Let's make the most of this energy!"
            "grateful" -> "You're very welcome! I'm always happy to help you."
            "confused" -> "No worries, let me explain that better. What specifically would you like to know?"
            else -> "I'm listening. How can I help you today?"
        }
    }
    
    // Save emotional state
    private fun saveEmotionalState(state: EmotionalState) {
        val history = getEmotionalHistory().toMutableList()
        history.add(state)
        
        // Keep last 20 states
        if (history.size > 20) {
            history.removeAt(0)
        }
        
        val json = com.google.gson.Gson().toJson(history)
        prefs.edit().putString("emotion_history", json).apply()
    }
    
    // Get emotional history
    fun getEmotionalHistory(): List<EmotionalState> {
        val json = prefs.getString("emotion_history", "[]") ?: "[]"
        val type = object : com.google.gson.reflect.TypeToken<List<EmotionalState>>() {}.type
        return com.google.gson.Gson().fromJson(json, type)
    }
    
    // Analyze emotional pattern
    fun analyzeEmotionalPattern(): String {
        val history = getEmotionalHistory()
        if (history.isEmpty()) return "neutral"
        
        val recentEmotions = history.takeLast(5)
        val emotionCounts = recentEmotions.groupingBy { it.emotion }.eachCount()
        val dominantEmotion = emotionCounts.maxByOrNull { it.value }?.key ?: "neutral"
        
        return dominantEmotion
    }
    
    // Check if user needs support
    fun needsEmotionalSupport(): Boolean {
        val history = getEmotionalHistory().takeLast(3)
        val negativeEmotions = listOf("sad", "angry", "anxious")
        return history.count { it.emotion in negativeEmotions } >= 2
    }
    
    // Suggest mood-based action
    fun suggestMoodAction(emotion: String): String {
        return when (emotion) {
            "sad" -> "Would you like me to play some uplifting music or tell you a joke?"
            "angry" -> "How about some calming music or a breathing exercise?"
            "anxious" -> "Let me play some relaxing sounds. Take deep breaths."
            "tired" -> "You should rest. Want me to set an alarm for a power nap?"
            "happy" -> "Great mood! Want to capture this moment with a note?"
            else -> ""
        }
    }
    
    // Get appropriate tone for response
    fun getResponseTone(emotion: String): String {
        return when (emotion) {
            "sad", "anxious" -> "gentle"
            "angry" -> "calm"
            "happy", "excited" -> "enthusiastic"
            "tired" -> "soft"
            else -> "neutral"
        }
    }
}
