package com.voiceassistant

import android.content.Context
import java.util.*

data class UserPattern(
    val action: String,
    val time: Int, // Hour of day
    val dayOfWeek: Int,
    val frequency: Int
)

data class Prediction(
    val action: String,
    val confidence: Float,
    val reason: String
)

class PredictiveActions(private val context: Context, private val memoryManager: MemoryManager) {
    
    private val prefs = context.getSharedPreferences("predictive_ai", Context.MODE_PRIVATE)
    
    // Learn user patterns
    fun learnPattern(action: String) {
        val cal = Calendar.getInstance()
        val hour = cal.get(Calendar.HOUR_OF_DAY)
        val dayOfWeek = cal.get(Calendar.DAY_OF_WEEK)
        
        val key = "${action}_${hour}_$dayOfWeek"
        val currentFreq = prefs.getInt(key, 0)
        prefs.edit().putInt(key, currentFreq + 1).apply()
        
        // Save last action time
        prefs.edit().putLong("last_$action", System.currentTimeMillis()).apply()
    }
    
    // Predict next action
    fun predictNextAction(): Prediction? {
        val cal = Calendar.getInstance()
        val hour = cal.get(Calendar.HOUR_OF_DAY)
        val dayOfWeek = cal.get(Calendar.DAY_OF_WEEK)
        
        val patterns = mutableListOf<Pair<String, Int>>()
        
        // Check all stored patterns
        for ((key, freq) in prefs.all) {
            if (key.contains("_${hour}_$dayOfWeek") && freq is Int) {
                val action = key.split("_")[0]
                patterns.add(action to freq)
            }
        }
        
        // Get most frequent action
        val topPattern = patterns.maxByOrNull { it.second }
        
        return if (topPattern != null && topPattern.second >= 3) {
            val confidence = minOf(topPattern.second / 10f, 1f)
            Prediction(
                topPattern.first,
                confidence,
                "You usually do this at this time"
            )
        } else {
            null
        }
    }
    
    // Predict based on context
    fun predictFromContext(currentActivity: String): List<Prediction> {
        val predictions = mutableListOf<Prediction>()
        
        // Time-based predictions
        val cal = Calendar.getInstance()
        val hour = cal.get(Calendar.HOUR_OF_DAY)
        
        when (hour) {
            in 6..9 -> {
                predictions.add(Prediction("check weather", 0.8f, "Morning routine"))
                predictions.add(Prediction("read news", 0.7f, "Morning routine"))
                predictions.add(Prediction("check calendar", 0.6f, "Plan your day"))
            }
            in 12..14 -> {
                predictions.add(Prediction("set reminder", 0.5f, "Lunch time"))
            }
            in 17..19 -> {
                predictions.add(Prediction("call family", 0.6f, "Evening routine"))
                predictions.add(Prediction("check messages", 0.7f, "After work"))
            }
            in 21..23 -> {
                predictions.add(Prediction("set alarm", 0.8f, "Bedtime routine"))
                predictions.add(Prediction("check tomorrow's schedule", 0.6f, "Plan ahead"))
            }
        }
        
        // Context-based predictions
        when (currentActivity) {
            "driving" -> {
                predictions.add(Prediction("navigate", 0.9f, "You're driving"))
                predictions.add(Prediction("play music", 0.7f, "Entertainment while driving"))
            }
            "working" -> {
                predictions.add(Prediction("set focus mode", 0.8f, "Minimize distractions"))
                predictions.add(Prediction("silence notifications", 0.7f, "Stay focused"))
            }
        }
        
        return predictions
    }
    
    // Suggest proactive actions
    fun getProactiveSuggestions(): List<String> {
        val suggestions = mutableListOf<String>()
        val cal = Calendar.getInstance()
        val hour = cal.get(Calendar.HOUR_OF_DAY)
        val dayOfWeek = cal.get(Calendar.DAY_OF_WEEK)
        
        // Morning suggestions
        if (hour in 6..9) {
            suggestions.add("Good morning! Would you like to hear the weather and news?")
            suggestions.add("Don't forget to check your calendar for today")
        }
        
        // Afternoon suggestions
        if (hour in 12..14) {
            suggestions.add("It's lunch time. Want me to set a reminder for your afternoon tasks?")
        }
        
        // Evening suggestions
        if (hour in 18..20) {
            suggestions.add("Evening! Would you like a summary of your day?")
        }
        
        // Night suggestions
        if (hour in 21..23) {
            suggestions.add("Getting late. Should I set your alarm for tomorrow?")
            suggestions.add("Want me to activate night mode?")
        }
        
        // Check if user hasn't done usual action
        val prediction = predictNextAction()
        if (prediction != null && prediction.confidence > 0.7f) {
            val lastTime = prefs.getLong("last_${prediction.action}", 0)
            val hoursSince = (System.currentTimeMillis() - lastTime) / (1000 * 60 * 60)
            
            if (hoursSince > 24) {
                suggestions.add("You usually ${prediction.action} around this time. Want me to help?")
            }
        }
        
        return suggestions
    }
    
    // Predict user needs based on history
    fun predictUserNeeds(): String? {
        val memories = memoryManager.getRecentMemories(10)
        
        // Check for patterns in memories
        val actions = memories.map { it.type }
        val mostCommon = actions.groupingBy { it }.eachCount().maxByOrNull { it.value }
        
        return when (mostCommon?.key) {
            "call" -> "You've been making a lot of calls. Want me to check your contacts?"
            "app" -> "You've been opening many apps. Need help organizing?"
            "note" -> "You're taking lots of notes. Want me to summarize them?"
            else -> null
        }
    }
    
    // Smart reminders based on patterns
    fun suggestSmartReminder(): String? {
        val cal = Calendar.getInstance()
        val hour = cal.get(Calendar.HOUR_OF_DAY)
        
        // Check if user usually does something at this time
        val prediction = predictNextAction()
        if (prediction != null && prediction.confidence > 0.6f) {
            return "Reminder: You usually ${prediction.action} around this time"
        }
        
        return null
    }
    
    // Anticipate next command
    fun anticipateCommand(currentCommand: String): List<String> {
        val nextCommands = mutableListOf<String>()
        
        when {
            currentCommand.contains("call") -> {
                nextCommands.add("send message")
                nextCommands.add("add to contacts")
            }
            currentCommand.contains("weather") -> {
                nextCommands.add("check news")
                nextCommands.add("plan route")
            }
            currentCommand.contains("alarm") -> {
                nextCommands.add("check calendar")
                nextCommands.add("set reminder")
            }
            currentCommand.contains("music") -> {
                nextCommands.add("volume up")
                nextCommands.add("next song")
            }
        }
        
        return nextCommands
    }
}
