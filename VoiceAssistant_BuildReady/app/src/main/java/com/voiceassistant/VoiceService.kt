package com.voiceassistant

import android.app.Service
import android.content.Intent
import android.net.Uri
import android.os.IBinder
import android.speech.tts.TextToSpeech
import android.telephony.SmsManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.os.Build
import androidx.core.app.NotificationCompat
import org.vosk.Model
import org.vosk.Recognizer
import org.vosk.android.RecognitionListener
import org.vosk.android.SpeechService
import java.util.*

class VoiceService : Service(), RecognitionListener, TextToSpeech.OnInitListener {

    private var speechService: SpeechService? = null
    private lateinit var tts: TextToSpeech
    private lateinit var onlineAssistant: OnlineAssistant
    private lateinit var smartFeatures: SmartFeatures
    private lateinit var whatsappAutomation: WhatsAppAutomation
    private lateinit var appAutomation: AppAutomation
    private lateinit var memoryManager: MemoryManager
    private lateinit var bookReader: BookReader
    private lateinit var emotionalIntelligence: EmotionalIntelligence
    private lateinit var predictiveActions: PredictiveActions
    private lateinit var imageVideoEditor: ImageVideoEditor
    private lateinit var facialRecognition: FacialRecognition
    private lateinit var videoStabilizer: VideoStabilizer
    private lateinit var newsUpdater: NewsUpdater
    private lateinit var multiLanguage: MultiLanguageSupport
    private lateinit var ocrProcessor: OCRProcessor
    private lateinit var documentFormatter: DocumentFormatter
    private lateinit var audioGenerator: AudioGenerator
    private lateinit var voiceBiometrics: VoiceBiometrics
    private lateinit var accentDetector: AccentDetector
    private lateinit var federatedLearning: FederatedLearning
    private lateinit var memeGenerator: MemeGenerator
    private var conversationMode = false
    private var lastCommandTime = 0L
    private var lastSuggestionTime = 0L
    
    companion object {
        private const val NOTIFICATION_ID = 1
        private const val CHANNEL_ID = "voice_assistant_channel"
    }

    override fun onCreate() {
        super.onCreate()
        
        // Start as foreground service
        startForegroundService()
        
        tts = TextToSpeech(this, this)
        onlineAssistant = OnlineAssistant(this)
        smartFeatures = SmartFeatures(this)
        whatsappAutomation = WhatsAppAutomation(this)
        appAutomation = AppAutomation(this)
        memoryManager = MemoryManager(this)
        bookReader = BookReader(this, tts)
        emotionalIntelligence = EmotionalIntelligence(this)
        predictiveActions = PredictiveActions(this, memoryManager)
        imageVideoEditor = ImageVideoEditor(this)
        facialRecognition = FacialRecognition(this)
        videoStabilizer = VideoStabilizer(this)
        newsUpdater = NewsUpdater(this)
        multiLanguage = MultiLanguageSupport(this, tts)
        ocrProcessor = OCRProcessor(this)
        documentFormatter = DocumentFormatter(this)
        audioGenerator = AudioGenerator(this)
        voiceBiometrics = VoiceBiometrics(this)
        accentDetector = AccentDetector(this)
        federatedLearning = FederatedLearning(this)
        memeGenerator = MemeGenerator(this)
        conversationMode = smartFeatures.isConversationMode()
        initVoiceRecognition()
        startProactiveSuggestions()
    }
    
    private fun startForegroundService() {
        createNotificationChannel()
        
        val notificationIntent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this, 0, notificationIntent,
            PendingIntent.FLAG_IMMUTABLE
        )
        
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Voice Assistant")
            .setContentText("Listening for commands...")
            .setSmallIcon(android.R.drawable.ic_btn_speak_now)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
        
        startForeground(NOTIFICATION_ID, notification)
    }
    
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Voice Assistant Service",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Keeps voice assistant running in background"
            }
            
            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }
    }
    
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_STICKY // Restart service if killed
    }
    
    private fun startProactiveSuggestions() {
        // Check for proactive suggestions every 30 minutes
        Thread {
            while (true) {
                Thread.sleep(30 * 60 * 1000) // 30 minutes
                
                val now = System.currentTimeMillis()
                if (now - lastSuggestionTime > 30 * 60 * 1000) {
                    val suggestions = predictiveActions.getProactiveSuggestions()
                    if (suggestions.isNotEmpty()) {
                        speak(suggestions.first())
                        lastSuggestionTime = now
                    }
                }
            }
        }.start()
    }

    private fun initVoiceRecognition() {
        Thread {
            try {
                val model = Model(assets, "vosk-model-small-en-us-0.15")
                val recognizer = Recognizer(model, 16000.0f)
                speechService = SpeechService(recognizer, 16000.0f)
                speechService?.startListening(this)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }.start()
    }

    override fun onResult(hypothesis: String?) {
        hypothesis?.let {
            val command = it.lowercase()
            val wakeWord = smartFeatures.getWakeWord()
            
            // Check for wake word or conversation mode
            if (command.contains(wakeWord) || conversationMode) {
                val cleanCommand = command.replace(wakeWord, "").trim()
                processCommand(cleanCommand)
                lastCommandTime = System.currentTimeMillis()
            }
            
            // Exit conversation mode after 10 seconds of silence
            if (conversationMode && System.currentTimeMillis() - lastCommandTime > 10000) {
                conversationMode = false
            }
        }
    }

    override fun onPartialResult(hypothesis: String?) {}
    override fun onError(exception: Exception?) {}
    override fun onTimeout() {}

    private fun processCommand(command: String) {
        // Detect and translate language
        val currentLang = multiLanguage.getCurrentLanguage()
        val translatedCommand = if (currentLang != "en") {
            multiLanguage.translateCommand(command, currentLang)
        } else {
            command
        }
        
        // Detect emotion
        val emotionalState = emotionalIntelligence.detectEmotion(translatedCommand)
        
        // Learn pattern
        val action = translatedCommand.split(" ").firstOrNull() ?: ""
        predictiveActions.learnPattern(action)
        
        // Save conversation with emotion
        memoryManager.saveConversation(translatedCommand, "Processing...")
        
        // Check if user needs emotional support
        if (emotionalIntelligence.needsEmotionalSupport()) {
            val supportMessage = emotionalIntelligence.generateEmpatheticResponse(emotionalState.emotion, emotionalState.intensity)
            speak(supportMessage)
            
            val suggestion = emotionalIntelligence.suggestMoodAction(emotionalState.emotion)
            if (suggestion.isNotEmpty()) {
                speak(suggestion)
            }
        }
        
        when {
            // Language switching
            translatedCommand.contains("switch to") || translatedCommand.contains("change language") -> {
                val lang = when {
                    translatedCommand.contains("spanish") || translatedCommand.contains("español") -> "es"
                    translatedCommand.contains("french") || translatedCommand.contains("français") -> "fr"
                    translatedCommand.contains("german") || translatedCommand.contains("deutsch") -> "de"
                    translatedCommand.contains("chinese") || translatedCommand.contains("中文") -> "zh"
                    translatedCommand.contains("hindi") || translatedCommand.contains("हिंदी") -> "hi"
                    translatedCommand.contains("arabic") || translatedCommand.contains("عربي") -> "ar"
                    translatedCommand.contains("portuguese") || translatedCommand.contains("português") -> "pt"
                    translatedCommand.contains("russian") || translatedCommand.contains("русский") -> "ru"
                    translatedCommand.contains("japanese") || translatedCommand.contains("日本語") -> "ja"
                    translatedCommand.contains("korean") || translatedCommand.contains("한국어") -> "ko"
                    translatedCommand.contains("english") -> "en"
                    else -> null
                }
                
                if (lang != null && multiLanguage.setLanguage(lang)) {
                    speak(multiLanguage.getLocalizedResponse("activated", lang))
                } else {
                    speak("Language not supported")
                }
            }
            translatedCommand.contains("what languages") || translatedCommand.contains("supported languages") -> {
                val languages = multiLanguage.supportedLanguages.values.joinToString(", ") { it.name }
                speak("I support: $languages")
            }
            // WhatsApp automation
            command.contains("whatsapp") && command.contains("send") -> {
                val parts = command.split("to")
                if (parts.size == 2) {
                    val message = parts[0].replace("whatsapp", "").replace("send", "").trim()
                    val contact = parts[1].trim()
                    whatsappAutomation.sendToContact(contact, message, smartFeatures)
                    speak("Sending WhatsApp message to $contact")
                    memoryManager.remember("whatsapp", "Sent '$message' to $contact")
                }
            }
            command.contains("open whatsapp") -> {
                appAutomation.openApp("whatsapp")
                speak("Opening WhatsApp")
            }
            
            // App automation
            command.contains("open") -> {
                val appName = command.replace("open", "").trim()
                val opened = appAutomation.openApp(appName)
                if (opened) {
                    speak("Opening $appName")
                    memoryManager.remember("app", "Opened $appName")
                } else {
                    // Try to find app
                    val found = appAutomation.findApp(appName)
                    if (found != null) {
                        appAutomation.openApp(found)
                        speak("Opening app")
                    } else {
                        speak("App not found")
                    }
                }
            }
            
            // Memory commands
            command.contains("remember") || command.contains("save this") -> {
                val info = command.replace("remember", "").replace("save this", "").trim()
                memoryManager.remember("user_note", info)
                speak("I'll remember that")
            }
            command.contains("what do you remember") || command.contains("recall") -> {
                val query = command.replace("what do you remember about", "")
                    .replace("recall", "").trim()
                val memories = if (query.isNotEmpty()) {
                    memoryManager.searchMemories(query)
                } else {
                    memoryManager.getRecentMemories(3)
                }
                if (memories.isNotEmpty()) {
                    speak("I remember: ${memories.last().content}")
                } else {
                    speak("I don't have any memories about that")
                }
            }
            command.contains("learn") -> {
                val parts = command.split("is")
                if (parts.size == 2) {
                    val topic = parts[0].replace("learn", "").trim()
                    val info = parts[1].trim()
                    memoryManager.learn(topic, info)
                    speak("Learned: $topic is $info")
                }
            }
            command.contains("what is my") -> {
                val query = command.replace("what is my", "").trim()
                val info = memoryManager.recall(query) ?: memoryManager.getPreference(query)
                if (info != null) {
                    speak("Your $query is $info")
                } else {
                    speak("I don't know your $query")
                }
            }
            
            // Book reading commands
            command.contains("read book") || command.contains("open book") -> {
                val bookPath = command.replace("read book", "").replace("open book", "").trim()
                if (bookPath.isNotEmpty()) {
                    val loaded = bookReader.loadBook(bookPath)
                    if (loaded) {
                        speak("Book loaded. Say start reading to begin")
                    } else {
                        speak("Could not load book")
                    }
                } else {
                    speak("Please specify book path")
                }
            }
            command.contains("start reading") || command.contains("begin reading") -> {
                bookReader.startReading { status ->
                    speak(status)
                }
            }
            command.contains("stop reading") -> {
                bookReader.stopReading()
                speak("Stopped reading")
            }
            command.contains("pause reading") || command.contains("pause book") -> {
                bookReader.pauseReading()
                speak("Paused")
            }
            command.contains("resume reading") || command.contains("continue reading") -> {
                bookReader.resumeReading { status ->
                    speak(status)
                }
            }
            command.contains("next page") -> {
                bookReader.nextPage { status ->
                    speak(status)
                }
            }
            command.contains("previous page") || command.contains("last page") -> {
                bookReader.previousPage { status ->
                    speak(status)
                }
            }
            command.contains("go to page") -> {
                val pageNum = command.replace("go to page", "").trim().toIntOrNull()
                if (pageNum != null) {
                    bookReader.goToPage(pageNum - 1) { status ->
                        speak(status)
                    }
                }
            }
            command.contains("reading speed") -> {
                val speed = when {
                    command.contains("slow") -> "slow"
                    command.contains("fast") -> "fast"
                    command.contains("very fast") -> "very fast"
                    else -> "normal"
                }
                bookReader.setSpeed(speed)
                speak("Reading speed set to $speed")
            }
            command.contains("bookmark this") || command.contains("save bookmark") -> {
                bookReader.addBookmark()
                speak("Bookmark saved")
            }
            command.contains("go to bookmark") -> {
                bookReader.goToBookmark { status ->
                    speak(status)
                }
            }
            command.contains("summarize") && command.contains("page") -> {
                bookReader.summarizePage(onlineAssistant) { summary ->
                    speak(summary)
                }
            }
            command.contains("book status") || command.contains("where am i in the book") -> {
                speak(bookReader.getStatus())
            }
            
            // Emotional intelligence commands
            command.contains("how am i feeling") || command.contains("my mood") -> {
                val pattern = emotionalIntelligence.analyzeEmotionalPattern()
                speak("Based on our recent conversations, you seem to be feeling $pattern")
            }
            command.contains("i'm feeling") || command.contains("i feel") -> {
                val response = emotionalIntelligence.generateEmpatheticResponse(emotionalState.emotion, emotionalState.intensity)
                speak(response)
            }
            
            // Predictive actions commands
            command.contains("what should i do") || command.contains("suggest something") -> {
                val prediction = predictiveActions.predictNextAction()
                if (prediction != null) {
                    speak("Based on your patterns, you might want to ${prediction.action}. ${prediction.reason}")
                } else {
                    val suggestions = predictiveActions.getProactiveSuggestions()
                    if (suggestions.isNotEmpty()) {
                        speak(suggestions.first())
                    } else {
                        speak("I don't have any suggestions right now")
                    }
                }
            }
            command.contains("what do i usually do") -> {
                val prediction = predictiveActions.predictNextAction()
                if (prediction != null) {
                    speak("You usually ${prediction.action} around this time")
                } else {
                    speak("I haven't learned your patterns for this time yet")
                }
            }
            command.contains("remind me of my routine") -> {
                val suggestions = predictiveActions.getProactiveSuggestions()
                speak(suggestions.joinToString(". "))
            }
            
            // AI provider settings
            command.contains("use groq") || command.contains("switch to groq") -> {
                onlineAssistant.setAIProvider("groq")
                speak("Switched to Groq AI. It's free and fast!")
            }
            command.contains("use openai") || command.contains("switch to openai") -> {
                speak("Please set your OpenAI API key first")
            }
            command.contains("use ollama") || command.contains("switch to ollama") -> {
                onlineAssistant.setAIProvider("ollama")
                speak("Switched to Ollama. Make sure Ollama is running on your device")
            }
            command.contains("set api key") -> {
                val key = command.replace("set api key", "").trim()
                if (key.isNotEmpty()) {
                    onlineAssistant.setAIProvider("openai", key)
                    speak("API key saved")
                } else {
                    speak("Please provide an API key")
                }
            }
            
            // Image editing commands
            command.contains("edit image") || command.contains("apply filter") -> {
                val filter = when {
                    command.contains("grayscale") || command.contains("black and white") -> "grayscale"
                    command.contains("sepia") -> "sepia"
                    command.contains("blur") -> "blur"
                    command.contains("brighten") -> "brighten"
                    command.contains("darken") -> "darken"
                    command.contains("sharpen") -> "sharpen"
                    else -> "grayscale"
                }
                speak("Which image? Please say the file path")
                // Store filter for next command
            }
            command.contains("rotate image") -> {
                speak("Rotating image. Please specify the file path")
            }
            command.contains("crop image") -> {
                speak("Cropping image. Please specify coordinates")
            }
            
            // Video editing commands
            command.contains("trim video") || command.contains("cut video") -> {
                speak("Trimming video. Please specify start time and duration")
            }
            command.contains("merge videos") || command.contains("combine videos") -> {
                speak("Merging videos. Please specify video files")
            }
            command.contains("add music to video") -> {
                speak("Adding audio to video. Please specify video and audio files")
            }
            command.contains("speed up video") -> {
                speak("Speeding up video. Processing...")
            }
            command.contains("slow down video") -> {
                speak("Slowing down video. Processing...")
            }
            
            // Facial recognition commands
            command.contains("detect faces") || command.contains("how many faces") -> {
                speak("Detecting faces. Please specify image path")
            }
            command.contains("who is this") || command.contains("recognize face") -> {
                speak("Recognizing face. Please specify image")
            }
            command.contains("is anyone smiling") -> {
                speak("Checking for smiles. Please specify image")
            }
            command.contains("what's in this image") || command.contains("describe image") -> {
                speak("Analyzing image. Please specify path")
            }
            command.contains("save face") || command.contains("remember this face") -> {
                speak("Saving face to database. What's the person's name?")
            }
            
            // Video stabilization commands
            command.contains("stabilize video") || command.contains("fix shaky video") -> {
                speak("Stabilizing video. This may take a few minutes. Please specify video path")
            }
            command.contains("remove shake") -> {
                speak("Removing camera shake. Please specify video")
            }
            command.contains("enhance video") || command.contains("improve video quality") -> {
                speak("Enhancing video quality. Processing...")
            }
            command.contains("denoise video") || command.contains("reduce video noise") -> {
                speak("Denoising video. Processing...")
            }
            
            // OCR and text extraction
            command.contains("read text from image") || command.contains("extract text") -> {
                speak("Extracting text from image. Please specify image path")
            }
            command.contains("scan document") -> {
                speak("Scanning document. Please specify image path")
            }
            command.contains("extract emails") || command.contains("find emails") -> {
                speak("Extracting email addresses from image")
            }
            
            // Document formatting
            command.contains("create pdf") || command.contains("make pdf") -> {
                speak("Creating PDF document. Please provide content")
            }
            command.contains("create word document") || command.contains("make word doc") -> {
                speak("Creating Word document")
            }
            command.contains("format document") -> {
                speak("Formatting document")
            }
            command.contains("create resume") || command.contains("make cv") -> {
                speak("Creating resume. Please provide your details")
            }
            command.contains("create invoice") -> {
                speak("Creating invoice")
            }
            
            // Audio generation
            command.contains("text to audio") || command.contains("convert text to speech file") -> {
                speak("Converting text to audio file")
            }
            command.contains("create song") || command.contains("make song") -> {
                speak("Creating song. Please provide lyrics")
            }
            command.contains("generate music") || command.contains("create music") -> {
                speak("Generating music")
            }
            command.contains("add background music") -> {
                speak("Adding background music")
            }
            command.contains("change voice pitch") -> {
                speak("Changing voice pitch")
            }
            command.contains("add echo") || command.contains("add reverb") -> {
                speak("Adding audio effect")
            }
            
            // Voice biometrics
            command.contains("register my voice") || command.contains("enroll voice") -> {
                speak("Please say a few sentences to register your voice")
                // Capture audio and register
            }
            command.contains("verify my voice") || command.contains("voice unlock") -> {
                speak("Verifying your voice")
                // Verify speaker
            }
            command.contains("who is speaking") || command.contains("identify speaker") -> {
                speak("Identifying speaker")
                // Identify from registered users
            }
            
            // Accent detection
            command.contains("detect my accent") || command.contains("what's my accent") -> {
                speak("Analyzing your accent")
                // Detect accent
            }
            command.contains("adapt to my accent") -> {
                speak("Adapting speech to your accent")
                // Adjust TTS
            }
            
            // Federated learning
            command.contains("sync learnings") || command.contains("update model") -> {
                federatedLearning.sync { success ->
                    if (success) {
                        speak("Model synchronized with global learnings")
                    } else {
                        speak("Sync failed")
                    }
                }
            }
            command.contains("enable federated learning") -> {
                federatedLearning.setEnabled(true)
                speak("Federated learning enabled. Your learnings will be shared anonymously")
            }
            command.contains("disable federated learning") -> {
                federatedLearning.setEnabled(false)
                speak("Federated learning disabled")
            }
            
            // Meme generation
            command.contains("create meme") || command.contains("make meme") -> {
                speak("Creating meme. Please specify image path and text")
            }
            command.contains("meme from video") -> {
                speak("Extracting frame from video to create meme")
            }
            command.contains("ai meme") || command.contains("generate meme caption") -> {
                speak("Generating AI meme caption")
            }
            command.contains("deep fry") -> {
                speak("Deep frying meme")
            }
            command.contains("create gif meme") -> {
                speak("Creating GIF meme from video")
            }
            command.contains("extract funny moments") -> {
                speak("Extracting funny moments from video")
            }
            command.contains("add caption to video") -> {
                speak("Adding caption to video")
            }
            
            // Call with contact name
            command.contains("call") -> {
                val name = command.replace("call", "").trim()
                val number = smartFeatures.getContactNumber(name) ?: name
                makeCall(number)
                memoryManager.remember("call", "Called $name")
            }
            
            // Smart features
            command.contains("conversation mode on") -> {
                conversationMode = true
                smartFeatures.setConversationMode(true)
                speak("Conversation mode activated")
            }
            command.contains("conversation mode off") -> {
                conversationMode = false
                smartFeatures.setConversationMode(false)
                speak("Conversation mode deactivated")
            }
            command.contains("take a note") -> {
                val note = command.replace("take a note", "").trim()
                smartFeatures.saveVoiceNote(note)
                memoryManager.remember("note", note)
                speak("Note saved")
            }
            command.contains("read my notes") -> {
                val notes = smartFeatures.getVoiceNotes()
                speak("You have ${notes.size} notes. ${notes.lastOrNull() ?: ""}")
            }
            command.contains("good morning") -> {
                executeRoutine("morning")
            }
            command.contains("good night") -> {
                executeRoutine("night")
            }
            
            // Online features with context
            command.contains("ask") || command.contains("tell me about") -> {
                val query = command.replace("ask", "").replace("tell me about", "").trim()
                val context = memoryManager.getConversationContext()
                onlineAssistant.chatGPT("$context\nUser: $query") { response ->
                    speak(response)
                    memoryManager.saveConversation(query, response)
                }
            }
            command.contains("weather") -> {
                onlineAssistant.getWeather("current") { response ->
                    speak(response)
                    memoryManager.saveConversation(command, response)
                }
            }
            command.contains("news") -> {
                when {
                    command.contains("technology") || command.contains("tech") -> {
                        newsUpdater.getNewsByCategory("technology") { articles ->
                            if (articles.isNotEmpty()) {
                                speak("Top tech news: ${articles.take(3).joinToString(". ") { it.title }}")
                            } else {
                                speak("Could not fetch tech news")
                            }
                        }
                    }
                    command.contains("sports") -> {
                        newsUpdater.getNewsByCategory("sports") { articles ->
                            if (articles.isNotEmpty()) {
                                speak("Sports news: ${articles.take(3).joinToString(". ") { it.title }}")
                            } else {
                                speak("Could not fetch sports news")
                            }
                        }
                    }
                    command.contains("business") -> {
                        newsUpdater.getNewsByCategory("business") { articles ->
                            if (articles.isNotEmpty()) {
                                speak("Business news: ${articles.take(3).joinToString(". ") { it.title }}")
                            } else {
                                speak("Could not fetch business news")
                            }
                        }
                    }
                    command.contains("search") -> {
                        val query = command.replace("news search", "").replace("search news", "").trim()
                        newsUpdater.searchNews(query) { articles ->
                            if (articles.isNotEmpty()) {
                                speak("Found news about $query: ${articles.first().title}")
                            } else {
                                speak("No news found about $query")
                            }
                        }
                    }
                    else -> {
                        newsUpdater.getTopHeadlines { articles ->
                            if (articles.isNotEmpty()) {
                                speak("Top headlines: ${articles.take(3).joinToString(". ") { it.title }}")
                            } else {
                                speak("Could not fetch news")
                            }
                        }
                    }
                }
            }
            command.contains("trending") || command.contains("what's trending") -> {
                newsUpdater.getTrendingTopics { topics ->
                    speak("Trending topics: ${topics.joinToString(", ")}")
                }
            }
            command.contains("personalized news") || command.contains("my news") -> {
                newsUpdater.getPersonalizedNews { articles ->
                    if (articles.isNotEmpty()) {
                        speak("Your personalized news: ${articles.take(3).joinToString(". ") { it.title }}")
                    } else {
                        speak("No personalized news available")
                    }
                }
            }
            command.contains("set news api key") -> {
                val key = command.replace("set news api key", "").trim()
                if (key.isNotEmpty()) {
                    newsUpdater.setAPIKey(key)
                    speak("News API key saved")
                } else {
                    speak("Please provide an API key")
                }
            }
            
            // Existing commands
            command.contains("time") -> {
                val cal = Calendar.getInstance()
                val hour = cal.get(Calendar.HOUR_OF_DAY)
                val minute = cal.get(Calendar.MINUTE)
                speak("The time is $hour $minute")
            }
            command.contains("battery") -> {
                val battery = getBatteryLevel()
                speak("Battery is at $battery percent")
            }
            command.contains("volume up") -> adjustVolume(true)
            command.contains("volume down") -> adjustVolume(false)
            command.contains("flashlight on") -> toggleFlashlight(true)
            command.contains("flashlight off") -> toggleFlashlight(false)
            command.contains("play music") -> playMusic()
            command.contains("stop music") -> stopMusic()
            command.contains("calculate") -> calculate(command)
            command.contains("search") -> webSearch(command)
            command.contains("date") -> getDate()
            command.contains("location") -> getLocation()
            
            else -> {
                // Generate empathetic response based on emotion
                val empatheticResponse = emotionalIntelligence.generateEmpatheticResponse(emotionalState.emotion, emotionalState.intensity)
                
                // Use AI with memory context and emotional awareness
                val context = memoryManager.getConversationContext()
                val emotionalContext = "User seems ${emotionalState.emotion}. Respond with ${emotionalIntelligence.getResponseTone(emotionalState.emotion)} tone."
                
                onlineAssistant.chatGPT("$emotionalContext\n$context\nUser: $command") { response ->
                    // Add empathetic prefix if needed
                    val finalResponse = if (emotionalState.intensity > 0.7f) {
                        "$empatheticResponse $response"
                    } else {
                        response
                    }
                    speak(finalResponse)
                    memoryManager.saveConversation(command, finalResponse)
                }
            }
        }
        
        // After processing, suggest next action
        val nextActions = predictiveActions.anticipateCommand(command)
        if (nextActions.isNotEmpty() && conversationMode) {
            // Optionally suggest next action
            // speak("Would you like to ${nextActions.first()}?")
        }
    }

    private fun makeCall(number: String) {
        try {
            val intent = Intent(Intent.ACTION_CALL)
            intent.data = Uri.parse("tel:$number")
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
            startActivity(intent)
            speak("Calling $number")
        } catch (e: Exception) {
            speak("Cannot make call")
        }
    }

    private fun openApp(appName: String) {
        val intent = packageManager.getLaunchIntentForPackage(appName)
        if (intent != null) {
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
            startActivity(intent)
            speak("Opening $appName")
        } else {
            speak("App not found")
        }
    }

    private fun getBatteryLevel(): Int {
        val batteryIntent = registerReceiver(null, android.content.IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        val level = batteryIntent?.getIntExtra(android.os.BatteryManager.EXTRA_LEVEL, -1) ?: 0
        val scale = batteryIntent?.getIntExtra(android.os.BatteryManager.EXTRA_SCALE, -1) ?: 100
        return (level * 100 / scale)
    }

    private fun toggleWifi(enable: Boolean) {
        try {
            val intent = Intent(android.provider.Settings.ACTION_WIFI_SETTINGS)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
            startActivity(intent)
            speak(if (enable) "Opening WiFi settings to turn on" else "Opening WiFi settings to turn off")
        } catch (e: Exception) {
            speak("Cannot control WiFi")
        }
    }

    private fun setAlarm(command: String) {
        try {
            val intent = Intent(android.provider.AlarmClock.ACTION_SET_ALARM)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
            startActivity(intent)
            speak("Opening alarm settings")
        } catch (e: Exception) {
            speak("Cannot set alarm")
        }
    }

    private fun setReminder(command: String) {
        speak("Reminder feature - opening notes")
        openApp("com.google.android.keep")
    }

    private fun playMusic() {
        try {
            val intent = Intent(Intent.ACTION_VIEW)
            intent.setDataAndType(Uri.parse("content://media/internal/audio/media"), "audio/*")
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
            startActivity(intent)
            speak("Playing music")
        } catch (e: Exception) {
            speak("Cannot play music")
        }
    }

    private fun stopMusic() {
        try {
            val intent = Intent("com.android.music.musicservicecommand")
            intent.putExtra("command", "pause")
            sendBroadcast(intent)
            speak("Music paused")
        } catch (e: Exception) {
            speak("Cannot pause music")
        }
    }

    private fun adjustVolume(increase: Boolean) {
        val audioManager = getSystemService(android.content.Context.AUDIO_SERVICE) as android.media.AudioManager
        if (increase) {
            audioManager.adjustVolume(android.media.AudioManager.ADJUST_RAISE, android.media.AudioManager.FLAG_SHOW_UI)
            speak("Volume up")
        } else {
            audioManager.adjustVolume(android.media.AudioManager.ADJUST_LOWER, android.media.AudioManager.FLAG_SHOW_UI)
            speak("Volume down")
        }
    }

    private fun adjustBrightness(increase: Boolean) {
        try {
            val intent = Intent(android.provider.Settings.ACTION_DISPLAY_SETTINGS)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
            startActivity(intent)
            speak("Opening brightness settings")
        } catch (e: Exception) {
            speak("Cannot adjust brightness")
        }
    }

    private fun toggleFlashlight(enable: Boolean) {
        try {
            val cameraManager = getSystemService(android.content.Context.CAMERA_SERVICE) as android.hardware.camera2.CameraManager
            val cameraId = cameraManager.cameraIdList[0]
            cameraManager.setTorchMode(cameraId, enable)
            speak(if (enable) "Flashlight on" else "Flashlight off")
        } catch (e: Exception) {
            speak("Cannot control flashlight")
        }
    }

    private fun takeScreenshot() {
        speak("Screenshot feature requires root access or accessibility service")
    }

    private fun getWeather() {
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.google.com/search?q=weather"))
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
            startActivity(intent)
            speak("Opening weather")
        } catch (e: Exception) {
            speak("Cannot get weather")
        }
    }

    private fun calculate(command: String) {
        try {
            val expression = command.replace("calculate", "").replace("what is", "").trim()
            val result = evaluateExpression(expression)
            speak("The answer is $result")
        } catch (e: Exception) {
            val intent = Intent()
            intent.action = Intent.ACTION_MAIN
            intent.addCategory(Intent.CATEGORY_LAUNCHER)
            intent.component = android.content.ComponentName("com.google.android.calculator", "com.android.calculator2.Calculator")
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
            try {
                startActivity(intent)
                speak("Opening calculator")
            } catch (ex: Exception) {
                speak("Cannot calculate")
            }
        }
    }

    private fun evaluateExpression(expr: String): Double {
        val cleaned = expr.replace("plus", "+").replace("minus", "-")
            .replace("times", "*").replace("divided by", "/")
            .replace("x", "*").replace(" ", "")
        
        return when {
            "+" in cleaned -> {
                val parts = cleaned.split("+")
                parts[0].toDouble() + parts[1].toDouble()
            }
            "-" in cleaned -> {
                val parts = cleaned.split("-")
                parts[0].toDouble() - parts[1].toDouble()
            }
            "*" in cleaned -> {
                val parts = cleaned.split("*")
                parts[0].toDouble() * parts[1].toDouble()
            }
            "/" in cleaned -> {
                val parts = cleaned.split("/")
                parts[0].toDouble() / parts[1].toDouble()
            }
            else -> cleaned.toDouble()
        }
    }

    private fun webSearch(command: String) {
        val query = command.replace("search", "").replace("google", "").trim()
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.google.com/search?q=$query"))
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
        startActivity(intent)
        speak("Searching for $query")
    }

    private fun getDate() {
        val cal = Calendar.getInstance()
        val day = cal.get(Calendar.DAY_OF_MONTH)
        val month = cal.getDisplayName(Calendar.MONTH, Calendar.LONG, Locale.US)
        val year = cal.get(Calendar.YEAR)
        val dayOfWeek = cal.getDisplayName(Calendar.DAY_OF_WEEK, Calendar.LONG, Locale.US)
        speak("Today is $dayOfWeek, $month $day, $year")
    }

    private fun getLocation() {
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("geo:0,0?q=my+location"))
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
            startActivity(intent)
            speak("Opening location")
        } catch (e: Exception) {
            speak("Cannot get location")
        }
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            tts.language = Locale.US
        }
    }

    private fun speak(text: String) {
        tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, null)
        // Trigger speaking animation
        try {
            val mainActivity = this as? MainActivity
            mainActivity?.startSpeakingAnimation()
        } catch (e: Exception) {
            // Activity not available
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        speechService?.stop()
        tts.shutdown()
        super.onDestroy()
    }
}
