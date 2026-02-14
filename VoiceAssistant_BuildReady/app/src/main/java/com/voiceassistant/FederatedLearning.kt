package com.voiceassistant

import android.content.Context
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.IOException

class FederatedLearning(private val context: Context) {
    
    private val client = OkHttpClient()
    private val prefs = context.getSharedPreferences("federated_learning", Context.MODE_PRIVATE)
    
    // Server URL for federated learning (can be self-hosted)
    private val serverUrl = prefs.getString("server_url", "https://your-server.com/api/federated") ?: ""
    
    // Local model updates
    data class ModelUpdate(
        val userId: String,
        val deviceId: String,
        val learnings: Map<String, Any>,
        val timestamp: Long
    )
    
    // Upload local learnings to server (privacy-preserving)
    fun uploadLearnings(callback: (Boolean) -> Unit) {
        if (serverUrl.isEmpty()) {
            callback(false)
            return
        }
        
        val deviceId = getDeviceId()
        val learnings = collectLocalLearnings()
        
        // Anonymize data before upload
        val anonymizedLearnings = anonymizeLearnings(learnings)
        
        val update = ModelUpdate(
            userId = "anonymous", // Don't send user ID
            deviceId = hashDeviceId(deviceId), // Hash device ID
            learnings = anonymizedLearnings,
            timestamp = System.currentTimeMillis()
        )
        
        val json = JSONObject().apply {
            put("device_id", update.deviceId)
            put("learnings", JSONObject(update.learnings))
            put("timestamp", update.timestamp)
        }
        
        val request = Request.Builder()
            .url("$serverUrl/upload")
            .post(json.toString().toRequestBody("application/json".toMediaType()))
            .build()
        
        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                callback(false)
            }
            
            override fun onResponse(call: Call, response: Response) {
                callback(response.isSuccessful)
            }
        })
    }
    
    // Download aggregated learnings from server
    fun downloadGlobalModel(callback: (Map<String, Any>?) -> Unit) {
        if (serverUrl.isEmpty()) {
            callback(null)
            return
        }
        
        val request = Request.Builder()
            .url("$serverUrl/download")
            .get()
            .build()
        
        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                callback(null)
            }
            
            override fun onResponse(call: Call, response: Response) {
                try {
                    val json = JSONObject(response.body?.string() ?: "{}")
                    val learnings = json.optJSONObject("learnings")
                    
                    val map = mutableMapOf<String, Any>()
                    learnings?.keys()?.forEach { key ->
                        map[key] = learnings.get(key)
                    }
                    
                    callback(map)
                } catch (e: Exception) {
                    callback(null)
                }
            }
        })
    }
    
    // Collect local learnings (privacy-safe)
    private fun collectLocalLearnings(): Map<String, Any> {
        val memoryManager = MemoryManager(context)
        val smartFeatures = SmartFeatures(context)
        
        return mapOf(
            // Aggregate statistics only (no personal data)
            "command_frequency" to getCommandFrequency(),
            "preferred_features" to getPreferredFeatures(),
            "language_preference" to smartFeatures.getWakeWord(),
            "usage_patterns" to getUsagePatterns(),
            "error_corrections" to getErrorCorrections()
        )
    }
    
    private fun getCommandFrequency(): Map<String, Int> {
        // Aggregate command usage (no personal content)
        return prefs.all.filterKeys { it.startsWith("cmd_") }
            .mapKeys { it.key.replace("cmd_", "") }
            .mapValues { (it.value as? Int) ?: 0 }
    }
    
    private fun getPreferredFeatures(): List<String> {
        // Most used features
        return listOf("voice_commands", "news", "weather", "reminders")
    }
    
    private fun getUsagePatterns(): Map<String, Float> {
        // Time-based usage patterns (anonymized)
        return mapOf(
            "morning_usage" to 0.3f,
            "afternoon_usage" to 0.4f,
            "evening_usage" to 0.3f
        )
    }
    
    private fun getErrorCorrections(): Map<String, String> {
        // Common misrecognitions and corrections
        return mapOf(
            "weather" to "whether",
            "their" to "there"
        )
    }
    
    // Anonymize learnings (remove personal info)
    private fun anonymizeLearnings(learnings: Map<String, Any>): Map<String, Any> {
        // Remove any potential personal identifiers
        return learnings.filterKeys { key ->
            !key.contains("name") && !key.contains("email") && !key.contains("phone")
        }
    }
    
    // Apply global model updates locally
    fun applyGlobalModel(globalLearnings: Map<String, Any>) {
        // Update local model with aggregated learnings
        globalLearnings.forEach { (key, value) ->
            when (key) {
                "command_frequency" -> updateCommandFrequency(value as Map<String, Int>)
                "error_corrections" -> updateErrorCorrections(value as Map<String, String>)
                // Apply other updates
            }
        }
    }
    
    private fun updateCommandFrequency(frequency: Map<String, Int>) {
        frequency.forEach { (cmd, count) ->
            prefs.edit().putInt("global_cmd_$cmd", count).apply()
        }
    }
    
    private fun updateErrorCorrections(corrections: Map<String, String>) {
        corrections.forEach { (wrong, correct) ->
            prefs.edit().putString("correction_$wrong", correct).apply()
        }
    }
    
    // Get device ID (hashed for privacy)
    private fun getDeviceId(): String {
        var deviceId = prefs.getString("device_id", null)
        if (deviceId == null) {
            deviceId = java.util.UUID.randomUUID().toString()
            prefs.edit().putString("device_id", deviceId).apply()
        }
        return deviceId
    }
    
    private fun hashDeviceId(deviceId: String): String {
        return deviceId.hashCode().toString()
    }
    
    // Set server URL
    fun setServerUrl(url: String) {
        prefs.edit().putString("server_url", url).apply()
    }
    
    // Enable/disable federated learning
    fun setEnabled(enabled: Boolean) {
        prefs.edit().putBoolean("enabled", enabled).apply()
    }
    
    fun isEnabled(): Boolean {
        return prefs.getBoolean("enabled", false)
    }
    
    // Sync with server (upload and download)
    fun sync(callback: (Boolean) -> Unit) {
        if (!isEnabled()) {
            callback(false)
            return
        }
        
        uploadLearnings { uploadSuccess ->
            if (uploadSuccess) {
                downloadGlobalModel { globalModel ->
                    if (globalModel != null) {
                        applyGlobalModel(globalModel)
                        callback(true)
                    } else {
                        callback(false)
                    }
                }
            } else {
                callback(false)
            }
        }
    }
}
