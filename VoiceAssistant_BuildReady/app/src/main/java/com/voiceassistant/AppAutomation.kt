package com.voiceassistant

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager

class AppAutomation(private val context: Context) {
    
    // Common app package names
    private val appPackages = mapOf(
        "whatsapp" to "com.whatsapp",
        "facebook" to "com.facebook.katana",
        "instagram" to "com.instagram.android",
        "twitter" to "com.twitter.android",
        "youtube" to "com.google.android.youtube",
        "gmail" to "com.google.android.gm",
        "chrome" to "com.android.chrome",
        "maps" to "com.google.android.apps.maps",
        "spotify" to "com.spotify.music",
        "netflix" to "com.netflix.mediaclient",
        "telegram" to "org.telegram.messenger",
        "tiktok" to "com.zhiliaoapp.musically",
        "snapchat" to "com.snapchat.android",
        "messenger" to "com.facebook.orca",
        "uber" to "com.ubercab",
        "camera" to "com.android.camera2",
        "gallery" to "com.google.android.apps.photos",
        "settings" to "com.android.settings",
        "calculator" to "com.google.android.calculator",
        "calendar" to "com.google.android.calendar",
        "clock" to "com.google.android.deskclock",
        "contacts" to "com.android.contacts"
    )
    
    // Open app by name
    fun openApp(appName: String): Boolean {
        val packageName = appPackages[appName.lowercase()] ?: appName
        
        return try {
            val intent = context.packageManager.getLaunchIntentForPackage(packageName)
            if (intent != null) {
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                context.startActivity(intent)
                true
            } else {
                false
            }
        } catch (e: Exception) {
            false
        }
    }
    
    // Open app with specific action
    fun openAppWithAction(appName: String, action: String, data: String? = null): Boolean {
        val packageName = appPackages[appName.lowercase()] ?: appName
        
        return try {
            val intent = Intent(action)
            intent.setPackage(packageName)
            if (data != null) {
                intent.data = android.net.Uri.parse(data)
            }
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
            context.startActivity(intent)
            true
        } catch (e: Exception) {
            false
        }
    }
    
    // Get list of installed apps
    fun getInstalledApps(): List<String> {
        val pm = context.packageManager
        val apps = pm.getInstalledApplications(PackageManager.GET_META_DATA)
        return apps.map { it.packageName }
    }
    
    // Search for app by partial name
    fun findApp(partialName: String): String? {
        val pm = context.packageManager
        val apps = pm.getInstalledApplications(PackageManager.GET_META_DATA)
        
        for (app in apps) {
            val appName = pm.getApplicationLabel(app).toString().lowercase()
            if (appName.contains(partialName.lowercase())) {
                return app.packageName
            }
        }
        return null
    }
    
    // Close app (requires root or accessibility)
    fun closeApp(packageName: String) {
        // This requires system permissions
        // For now, just go to home screen
        val intent = Intent(Intent.ACTION_MAIN)
        intent.addCategory(Intent.CATEGORY_HOME)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
        context.startActivity(intent)
    }
}
