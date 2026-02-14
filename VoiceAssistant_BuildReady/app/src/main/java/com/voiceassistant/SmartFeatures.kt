package com.voiceassistant

import android.content.Context
import android.content.SharedPreferences
import android.database.Cursor
import android.provider.ContactsContract

class SmartFeatures(private val context: Context) {
    
    private val prefs: SharedPreferences = context.getSharedPreferences("assistant_prefs", Context.MODE_PRIVATE)
    
    // Contact name to number
    fun getContactNumber(name: String): String? {
        val cursor: Cursor? = context.contentResolver.query(
            ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
            null,
            "${ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME} LIKE ?",
            arrayOf("%$name%"),
            null
        )
        
        cursor?.use {
            if (it.moveToFirst()) {
                val numberIndex = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
                return it.getString(numberIndex)
            }
        }
        return null
    }
    
    // Save voice note
    fun saveVoiceNote(note: String) {
        val notes = prefs.getStringSet("voice_notes", mutableSetOf()) ?: mutableSetOf()
        notes.add("${System.currentTimeMillis()}: $note")
        prefs.edit().putStringSet("voice_notes", notes).apply()
    }
    
    // Get voice notes
    fun getVoiceNotes(): List<String> {
        return prefs.getStringSet("voice_notes", mutableSetOf())?.toList() ?: emptyList()
    }
    
    // Save routine
    fun saveRoutine(name: String, commands: List<String>) {
        prefs.edit().putString("routine_$name", commands.joinToString("|")).apply()
    }
    
    // Get routine
    fun getRoutine(name: String): List<String> {
        return prefs.getString("routine_$name", "")?.split("|") ?: emptyList()
    }
    
    // Custom wake word
    fun setWakeWord(word: String) {
        prefs.edit().putString("wake_word", word.lowercase()).apply()
    }
    
    fun getWakeWord(): String {
        return prefs.getString("wake_word", "assistant") ?: "assistant"
    }
    
    // Conversation mode
    fun setConversationMode(enabled: Boolean) {
        prefs.edit().putBoolean("conversation_mode", enabled).apply()
    }
    
    fun isConversationMode(): Boolean {
        return prefs.getBoolean("conversation_mode", false)
    }
    
    // Auto-reply message
    fun setAutoReply(message: String) {
        prefs.edit().putString("auto_reply", message).apply()
    }
    
    fun getAutoReply(): String {
        return prefs.getString("auto_reply", "I'm busy right now") ?: "I'm busy right now"
    }
}
