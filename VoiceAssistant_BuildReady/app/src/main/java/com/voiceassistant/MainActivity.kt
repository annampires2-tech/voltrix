package com.voiceassistant

import android.Manifest
import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Build
import android.speech.tts.TextToSpeech
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import java.util.*

class MainActivity : AppCompatActivity(), TextToSpeech.OnInitListener {
    
    private lateinit var tts: TextToSpeech
    private lateinit var statusText: TextView
    private lateinit var commandText: TextView
    private lateinit var assistantIcon: ImageView
    private lateinit var glowOuter: View
    private lateinit var glowMiddle: View
    private var isListening = false
    
    private var pulseAnimator: ValueAnimator? = null
    private var glowAnimator: ValueAnimator? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        statusText = findViewById(R.id.statusText)
        commandText = findViewById(R.id.commandText)
        assistantIcon = findViewById(R.id.assistantIcon)
        glowOuter = findViewById(R.id.glowOuter)
        glowMiddle = findViewById(R.id.glowMiddle)
        
        tts = TextToSpeech(this, this)
        
        requestPermissions()
        
        assistantIcon.setOnClickListener {
            if (isListening) {
                stopVoiceService()
            } else {
                startVoiceService()
            }
        }
        
        // Auto-start
        startVoiceService()
    }

    private fun requestPermissions() {
        val permissions = arrayOf(
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.CALL_PHONE,
            Manifest.permission.SEND_SMS,
            Manifest.permission.READ_CONTACTS
        )
        
        val notGranted = permissions.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }
        
        if (notGranted.isNotEmpty()) {
            ActivityCompat.requestPermissions(this, notGranted.toTypedArray(), 100)
        }
    }

    private fun startVoiceService() {
        val intent = Intent(this, VoiceService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent)
        } else {
            startService(intent)
        }
        isListening = true
        statusText.text = "Listening..."
        commandText.text = "Say 'Hey Assistant'"
        startListeningAnimation()
    }

    private fun stopVoiceService() {
        val intent = Intent(this, VoiceService::class.java)
        stopService(intent)
        isListening = false
        statusText.text = "Voice Assistant"
        commandText.text = "Tap to start"
        stopAllAnimations()
    }
    
    private fun startListeningAnimation() {
        // Pulse icon
        pulseAnimator = ValueAnimator.ofFloat(1f, 1.1f, 1f).apply {
            duration = 1500
            repeatCount = ValueAnimator.INFINITE
            interpolator = AccelerateDecelerateInterpolator()
            addUpdateListener { animation ->
                val scale = animation.animatedValue as Float
                assistantIcon.scaleX = scale
                assistantIcon.scaleY = scale
            }
            start()
        }
        
        // Glow effect
        glowAnimator = ValueAnimator.ofFloat(0f, 0.7f, 0f).apply {
            duration = 2000
            repeatCount = ValueAnimator.INFINITE
            addUpdateListener { animation ->
                val alpha = animation.animatedValue as Float
                glowOuter.alpha = alpha * 0.5f
                glowMiddle.alpha = alpha
            }
            start()
        }
    }
    
    fun startSpeakingAnimation() {
        runOnUiThread {
            stopAllAnimations()
            
            // Faster pulse when speaking
            pulseAnimator = ValueAnimator.ofFloat(1f, 1.2f, 1f).apply {
                duration = 500
                repeatCount = ValueAnimator.INFINITE
                addUpdateListener { animation ->
                    val scale = animation.animatedValue as Float
                    assistantIcon.scaleX = scale
                    assistantIcon.scaleY = scale
                }
                start()
            }
            
            // Bright glow
            glowAnimator = ValueAnimator.ofFloat(0.3f, 1f, 0.3f).apply {
                duration = 800
                repeatCount = ValueAnimator.INFINITE
                addUpdateListener { animation ->
                    val alpha = animation.animatedValue as Float
                    glowOuter.alpha = alpha
                    glowMiddle.alpha = alpha
                }
                start()
            }
        }
    }
    
    private fun stopAllAnimations() {
        pulseAnimator?.cancel()
        glowAnimator?.cancel()
        assistantIcon.scaleX = 1f
        assistantIcon.scaleY = 1f
        glowOuter.alpha = 0f
        glowMiddle.alpha = 0f
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            tts.language = Locale.US
        }
    }

    fun updateCommandText(text: String) {
        runOnUiThread {
            commandText.text = text
        }
    }

    override fun onDestroy() {
        tts.shutdown()
        stopAllAnimations()
        super.onDestroy()
    }
}
