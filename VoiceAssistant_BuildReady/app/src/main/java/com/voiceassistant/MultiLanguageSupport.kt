package com.voiceassistant

import android.content.Context
import android.speech.tts.TextToSpeech
import java.util.*

class MultiLanguageSupport(private val context: Context, private val tts: TextToSpeech) {
    
    private val prefs = context.getSharedPreferences("language_settings", Context.MODE_PRIVATE)
    
    // Supported languages with Vosk models
    val supportedLanguages = mapOf(
        "en" to Language("English", "en-US", "vosk-model-small-en-us-0.15"),
        "es" to Language("Spanish", "es-ES", "vosk-model-small-es-0.42"),
        "fr" to Language("French", "fr-FR", "vosk-model-small-fr-0.22"),
        "de" to Language("German", "de-DE", "vosk-model-small-de-0.15"),
        "zh" to Language("Chinese", "zh-CN", "vosk-model-small-cn-0.22"),
        "hi" to Language("Hindi", "hi-IN", "vosk-model-small-hi-0.22"),
        "ar" to Language("Arabic", "ar-AR", "vosk-model-small-ar-0.22"),
        "pt" to Language("Portuguese", "pt-BR", "vosk-model-small-pt-0.3"),
        "ru" to Language("Russian", "ru-RU", "vosk-model-small-ru-0.22"),
        "ja" to Language("Japanese", "ja-JP", "vosk-model-small-ja-0.22"),
        "ko" to Language("Korean", "ko-KR", "vosk-model-small-ko-0.22"),
        "it" to Language("Italian", "it-IT", "vosk-model-small-it-0.22"),
        "nl" to Language("Dutch", "nl-NL", "vosk-model-small-nl-0.22"),
        "tr" to Language("Turkish", "tr-TR", "vosk-model-small-tr-0.3")
    )
    
    // Get current language
    fun getCurrentLanguage(): String {
        return prefs.getString("current_language", "en") ?: "en"
    }
    
    // Set language
    fun setLanguage(languageCode: String): Boolean {
        if (!supportedLanguages.containsKey(languageCode)) {
            return false
        }
        
        prefs.edit().putString("current_language", languageCode).apply()
        
        // Update TTS language
        val locale = getLocale(languageCode)
        tts.language = locale
        
        return true
    }
    
    // Get locale for language code
    fun getLocale(languageCode: String): Locale {
        return when (languageCode) {
            "en" -> Locale.US
            "es" -> Locale("es", "ES")
            "fr" -> Locale.FRENCH
            "de" -> Locale.GERMAN
            "zh" -> Locale.CHINESE
            "hi" -> Locale("hi", "IN")
            "ar" -> Locale("ar", "AR")
            "pt" -> Locale("pt", "BR")
            "ru" -> Locale("ru", "RU")
            "ja" -> Locale.JAPANESE
            "ko" -> Locale.KOREAN
            "it" -> Locale.ITALIAN
            "nl" -> Locale("nl", "NL")
            "tr" -> Locale("tr", "TR")
            else -> Locale.US
        }
    }
    
    // Detect language from text
    fun detectLanguage(text: String): String {
        // Simple detection based on character sets
        return when {
            text.matches(Regex(".*[\\u4e00-\\u9fa5].*")) -> "zh" // Chinese
            text.matches(Regex(".*[\\u0600-\\u06FF].*")) -> "ar" // Arabic
            text.matches(Regex(".*[\\u0900-\\u097F].*")) -> "hi" // Hindi
            text.matches(Regex(".*[\\u3040-\\u309F].*")) -> "ja" // Japanese
            text.matches(Regex(".*[\\uAC00-\\uD7AF].*")) -> "ko" // Korean
            text.matches(Regex(".*[\\u0400-\\u04FF].*")) -> "ru" // Russian
            else -> "en" // Default to English
        }
    }
    
    // Translate command keywords to English
    fun translateCommand(command: String, fromLang: String): String {
        val translations = mapOf(
            // Spanish
            "es" to mapOf(
                "llamar" to "call",
                "abrir" to "open",
                "tiempo" to "time",
                "noticias" to "news",
                "clima" to "weather",
                "música" to "music",
                "volumen" to "volume",
                "linterna" to "flashlight"
            ),
            // French
            "fr" to mapOf(
                "appeler" to "call",
                "ouvrir" to "open",
                "temps" to "time",
                "nouvelles" to "news",
                "météo" to "weather",
                "musique" to "music",
                "volume" to "volume",
                "lampe" to "flashlight"
            ),
            // German
            "de" to mapOf(
                "anrufen" to "call",
                "öffnen" to "open",
                "zeit" to "time",
                "nachrichten" to "news",
                "wetter" to "weather",
                "musik" to "music",
                "lautstärke" to "volume",
                "taschenlampe" to "flashlight"
            ),
            // Portuguese
            "pt" to mapOf(
                "ligar" to "call",
                "abrir" to "open",
                "tempo" to "time",
                "notícias" to "news",
                "clima" to "weather",
                "música" to "music",
                "volume" to "volume",
                "lanterna" to "flashlight"
            ),
            // Hindi
            "hi" to mapOf(
                "कॉल" to "call",
                "खोलें" to "open",
                "समय" to "time",
                "समाचार" to "news",
                "मौसम" to "weather",
                "संगीत" to "music",
                "वॉल्यूम" to "volume",
                "टॉर्च" to "flashlight"
            )
        )
        
        var translated = command.lowercase()
        translations[fromLang]?.forEach { (from, to) ->
            translated = translated.replace(from, to)
        }
        
        return translated
    }
    
    // Get localized response
    fun getLocalizedResponse(key: String, language: String): String {
        val responses = mapOf(
            "greeting" to mapOf(
                "en" to "Hello! How can I help you?",
                "es" to "¡Hola! ¿Cómo puedo ayudarte?",
                "fr" to "Bonjour! Comment puis-je vous aider?",
                "de" to "Hallo! Wie kann ich Ihnen helfen?",
                "zh" to "你好！我能帮你什么？",
                "hi" to "नमस्ते! मैं आपकी कैसे मदद कर सकता हूं?",
                "ar" to "مرحبا! كيف يمكنني مساعدتك؟",
                "pt" to "Olá! Como posso ajudá-lo?",
                "ru" to "Привет! Чем могу помочь?",
                "ja" to "こんにちは！どうすればいいですか？",
                "ko" to "안녕하세요! 어떻게 도와드릴까요?"
            ),
            "activated" to mapOf(
                "en" to "Voice assistant activated",
                "es" to "Asistente de voz activado",
                "fr" to "Assistant vocal activé",
                "de" to "Sprachassistent aktiviert",
                "zh" to "语音助手已激活",
                "hi" to "वॉयस असिस्टेंट सक्रिय",
                "ar" to "تم تنشيط المساعد الصوتي",
                "pt" to "Assistente de voz ativado",
                "ru" to "Голосовой помощник активирован",
                "ja" to "音声アシスタントが有効になりました",
                "ko" to "음성 비서가 활성화되었습니다"
            ),
            "error" to mapOf(
                "en" to "Sorry, I didn't understand that",
                "es" to "Lo siento, no entendí eso",
                "fr" to "Désolé, je n'ai pas compris",
                "de" to "Entschuldigung, das habe ich nicht verstanden",
                "zh" to "对不起，我不明白",
                "hi" to "क्षमा करें, मुझे समझ नहीं आया",
                "ar" to "آسف، لم أفهم ذلك",
                "pt" to "Desculpe, não entendi",
                "ru" to "Извините, я не понял",
                "ja" to "すみません、理解できませんでした",
                "ko" to "죄송합니다. 이해하지 못했습니다"
            )
        )
        
        return responses[key]?.get(language) ?: responses[key]?.get("en") ?: ""
    }
    
    // Get Vosk model URL for language
    fun getModelUrl(languageCode: String): String {
        val model = supportedLanguages[languageCode]?.voskModel ?: return ""
        return "https://alphacephei.com/vosk/models/$model.zip"
    }
}

data class Language(
    val name: String,
    val locale: String,
    val voskModel: String
)
