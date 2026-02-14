package com.voiceassistant

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.util.*

data class Memory(
    val timestamp: Long,
    val type: String,
    val content: String,
    val context: String = ""
)

data class Conversation(
    val timestamp: Long,
    val userInput: String,
    val assistantResponse: String
)

class MemoryManager(private val context: Context) {
    
    private val prefs: SharedPreferences = context.getSharedPreferences("assistant_memory", Context.MODE_PRIVATE)
    private val gson = Gson()
    
    // Save memory
    fun remember(type: String, content: String, context: String = "") {
        val memories = getMemories().toMutableList()
        memories.add(Memory(System.currentTimeMillis(), type, content, context))
        
        // Keep only last 100 memories
        if (memories.size > 100) {
            memories.removeAt(0)
        }
        
        val json = gson.toJson(memories)
        prefs.edit().putString("memories", json).apply()
    }
    
    // Retrieve memories
    fun getMemories(): List<Memory> {
        val json = prefs.getString("memories", "[]") ?: "[]"
        val type = object : TypeToken<List<Memory>>() {}.type
        return gson.fromJson(json, type)
    }
    
    // Search memories
    fun searchMemories(query: String): List<Memory> {
        return getMemories().filter { 
            it.content.contains(query, ignoreCase = true) ||
            it.context.contains(query, ignoreCase = true)
        }
    }
    
    // Get recent memories
    fun getRecentMemories(count: Int = 5): List<Memory> {
        return getMemories().takeLast(count)
    }
    
    // Save conversation
    fun saveConversation(userInput: String, response: String) {
        val conversations = getConversations().toMutableList()
        conversations.add(Conversation(System.currentTimeMillis(), userInput, response))
        
        // Keep only last 50 conversations
        if (conversations.size > 50) {
            conversations.removeAt(0)
        }
        
        val json = gson.toJson(conversations)
        prefs.edit().putString("conversations", json).apply()
    }
    
    // Get conversations
    fun getConversations(): List<Conversation> {
        val json = prefs.getString("conversations", "[]") ?: "[]"
        val type = object : TypeToken<List<Conversation>>() {}.type
        return gson.fromJson(json, type)
    }
    
    // Get context from recent conversations
    fun getConversationContext(): String {
        val recent = getConversations().takeLast(3)
        return recent.joinToString("\n") { 
            "User: ${it.userInput}\nAssistant: ${it.assistantResponse}" 
        }
    }
    
    // Save user preference
    fun savePreference(key: String, value: String) {
        prefs.edit().putString("pref_$key", value).apply()
    }
    
    // Get user preference
    fun getPreference(key: String): String? {
        return prefs.getString("pref_$key", null)
    }
    
    // Learn from user
    fun learn(topic: String, information: String) {
        remember("learning", information, topic)
    }
    
    // Recall learned information
    fun recall(topic: String): String? {
        val memories = searchMemories(topic).filter { it.type == "learning" }
        return memories.lastOrNull()?.content
    }
    
    // Save important dates
    fun saveDate(name: String, date: String) {
        prefs.edit().putString("date_$name", date).apply()
    }
    
    // Get important date
    fun getDate(name: String): String? {
        return prefs.getString("date_$name", null)
    }
    
    // Clear old memories
    fun clearOldMemories(daysOld: Int = 30) {
        val cutoff = System.currentTimeMillis() - (daysOld * 24 * 60 * 60 * 1000L)
        val memories = getMemories().filter { it.timestamp > cutoff }
        val json = gson.toJson(memories)
        prefs.edit().putString("memories", json).apply()
    }
}
