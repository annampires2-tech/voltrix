package com.voiceassistant

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast

class WhatsAppAutomation(private val context: Context) {
    
    // Send WhatsApp message
    fun sendMessage(phoneNumber: String, message: String) {
        try {
            val url = "https://api.whatsapp.com/send?phone=$phoneNumber&text=${Uri.encode(message)}"
            val intent = Intent(Intent.ACTION_VIEW)
            intent.data = Uri.parse(url)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
            context.startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(context, "WhatsApp not installed", Toast.LENGTH_SHORT).show()
        }
    }
    
    // Send to contact by name
    fun sendToContact(contactName: String, message: String, smartFeatures: SmartFeatures) {
        val number = smartFeatures.getContactNumber(contactName)
        if (number != null) {
            sendMessage(number, message)
        }
    }
    
    // Open WhatsApp chat
    fun openChat(phoneNumber: String) {
        try {
            val intent = Intent(Intent.ACTION_VIEW)
            intent.data = Uri.parse("https://api.whatsapp.com/send?phone=$phoneNumber")
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
            context.startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(context, "Cannot open WhatsApp", Toast.LENGTH_SHORT).show()
        }
    }
    
    // Send to WhatsApp group
    fun sendToGroup(groupId: String, message: String) {
        try {
            val intent = Intent(Intent.ACTION_VIEW)
            intent.data = Uri.parse("https://api.whatsapp.com/send?text=${Uri.encode(message)}")
            intent.setPackage("com.whatsapp")
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
            context.startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(context, "Cannot send to group", Toast.LENGTH_SHORT).show()
        }
    }
}
