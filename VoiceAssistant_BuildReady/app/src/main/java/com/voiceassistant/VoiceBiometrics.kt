package com.voiceassistant

import android.content.Context
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import kotlin.math.sqrt

class VoiceBiometrics(private val context: Context) {
    
    private val prefs = context.getSharedPreferences("voice_biometrics", Context.MODE_PRIVATE)
    
    // Voice features for identification
    data class VoiceProfile(
        val userId: String,
        val pitchMean: Float,
        val pitchVariance: Float,
        val speakingRate: Float,
        val energyMean: Float,
        val mfccFeatures: FloatArray
    )
    
    // Register user's voice
    fun registerVoice(userId: String, audioSamples: FloatArray): Boolean {
        val profile = extractVoiceFeatures(audioSamples)
        
        // Save profile
        prefs.edit().apply {
            putString("${userId}_profile", serializeProfile(profile.copy(userId = userId)))
            apply()
        }
        
        return true
    }
    
    // Verify speaker identity
    fun verifyVoice(audioSamples: FloatArray, expectedUserId: String): Boolean {
        val currentProfile = extractVoiceFeatures(audioSamples)
        val savedProfile = loadProfile(expectedUserId) ?: return false
        
        val similarity = calculateSimilarity(currentProfile, savedProfile)
        
        // Threshold for verification (0.8 = 80% match)
        return similarity > 0.8f
    }
    
    // Identify speaker from registered users
    fun identifySpeaker(audioSamples: FloatArray): String? {
        val currentProfile = extractVoiceFeatures(audioSamples)
        
        var bestMatch: String? = null
        var bestSimilarity = 0f
        
        // Check all registered users
        prefs.all.keys.filter { it.endsWith("_profile") }.forEach { key ->
            val userId = key.replace("_profile", "")
            val savedProfile = loadProfile(userId)
            
            if (savedProfile != null) {
                val similarity = calculateSimilarity(currentProfile, savedProfile)
                if (similarity > bestSimilarity && similarity > 0.75f) {
                    bestSimilarity = similarity
                    bestMatch = userId
                }
            }
        }
        
        return bestMatch
    }
    
    // Extract voice features
    private fun extractVoiceFeatures(audioSamples: FloatArray): VoiceProfile {
        // Calculate pitch (fundamental frequency)
        val pitch = calculatePitch(audioSamples)
        val pitchMean = pitch.average().toFloat()
        val pitchVariance = calculateVariance(pitch)
        
        // Calculate speaking rate (zero crossing rate)
        val speakingRate = calculateZeroCrossingRate(audioSamples)
        
        // Calculate energy
        val energy = audioSamples.map { it * it }.average().toFloat()
        
        // Calculate MFCC (Mel-frequency cepstral coefficients)
        val mfcc = calculateMFCC(audioSamples)
        
        return VoiceProfile(
            userId = "",
            pitchMean = pitchMean,
            pitchVariance = pitchVariance,
            speakingRate = speakingRate,
            energyMean = energy,
            mfccFeatures = mfcc
        )
    }
    
    // Calculate pitch using autocorrelation
    private fun calculatePitch(samples: FloatArray): FloatArray {
        val pitches = mutableListOf<Float>()
        val frameSize = 1024
        
        for (i in 0 until samples.size - frameSize step frameSize / 2) {
            val frame = samples.sliceArray(i until i + frameSize)
            val pitch = autocorrelation(frame)
            pitches.add(pitch)
        }
        
        return pitches.toFloatArray()
    }
    
    private fun autocorrelation(frame: FloatArray): Float {
        var maxCorr = 0f
        var maxLag = 0
        
        for (lag in 20..400) {
            var corr = 0f
            for (i in 0 until frame.size - lag) {
                corr += frame[i] * frame[i + lag]
            }
            if (corr > maxCorr) {
                maxCorr = corr
                maxLag = lag
            }
        }
        
        return 16000f / maxLag // Convert to Hz
    }
    
    // Calculate zero crossing rate
    private fun calculateZeroCrossingRate(samples: FloatArray): Float {
        var crossings = 0
        for (i in 1 until samples.size) {
            if ((samples[i] >= 0 && samples[i - 1] < 0) || (samples[i] < 0 && samples[i - 1] >= 0)) {
                crossings++
            }
        }
        return crossings.toFloat() / samples.size
    }
    
    // Simplified MFCC calculation
    private fun calculateMFCC(samples: FloatArray): FloatArray {
        // Simplified version - returns 13 coefficients
        val mfcc = FloatArray(13)
        val frameSize = 512
        
        for (i in 0 until minOf(13, samples.size / frameSize)) {
            val frame = samples.sliceArray(i * frameSize until (i + 1) * frameSize)
            mfcc[i] = frame.map { it * it }.average().toFloat()
        }
        
        return mfcc
    }
    
    private fun calculateVariance(values: FloatArray): Float {
        val mean = values.average().toFloat()
        return values.map { (it - mean) * (it - mean) }.average().toFloat()
    }
    
    // Calculate similarity between two voice profiles
    private fun calculateSimilarity(profile1: VoiceProfile, profile2: VoiceProfile): Float {
        // Weighted similarity calculation
        val pitchSim = 1f - minOf(kotlin.math.abs(profile1.pitchMean - profile2.pitchMean) / 200f, 1f)
        val rateSim = 1f - minOf(kotlin.math.abs(profile1.speakingRate - profile2.speakingRate) / 0.5f, 1f)
        val energySim = 1f - minOf(kotlin.math.abs(profile1.energyMean - profile2.energyMean) / 1f, 1f)
        
        // MFCC cosine similarity
        val mfccSim = cosineSimilarity(profile1.mfccFeatures, profile2.mfccFeatures)
        
        // Weighted average
        return (pitchSim * 0.3f + rateSim * 0.2f + energySim * 0.2f + mfccSim * 0.3f)
    }
    
    private fun cosineSimilarity(a: FloatArray, b: FloatArray): Float {
        var dotProduct = 0f
        var normA = 0f
        var normB = 0f
        
        for (i in a.indices) {
            dotProduct += a[i] * b[i]
            normA += a[i] * a[i]
            normB += b[i] * b[i]
        }
        
        return dotProduct / (sqrt(normA) * sqrt(normB))
    }
    
    // Serialize/deserialize profile
    private fun serializeProfile(profile: VoiceProfile): String {
        return "${profile.userId}|${profile.pitchMean}|${profile.pitchVariance}|${profile.speakingRate}|${profile.energyMean}|${profile.mfccFeatures.joinToString(",")}"
    }
    
    private fun loadProfile(userId: String): VoiceProfile? {
        val data = prefs.getString("${userId}_profile", null) ?: return null
        val parts = data.split("|")
        
        return VoiceProfile(
            userId = parts[0],
            pitchMean = parts[1].toFloat(),
            pitchVariance = parts[2].toFloat(),
            speakingRate = parts[3].toFloat(),
            energyMean = parts[4].toFloat(),
            mfccFeatures = parts[5].split(",").map { it.toFloat() }.toFloatArray()
        )
    }
    
    // Get all registered users
    fun getRegisteredUsers(): List<String> {
        return prefs.all.keys.filter { it.endsWith("_profile") }
            .map { it.replace("_profile", "") }
    }
    
    // Delete user profile
    fun deleteVoiceProfile(userId: String) {
        prefs.edit().remove("${userId}_profile").apply()
    }
}
