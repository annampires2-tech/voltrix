package com.voiceassistant

import android.content.Context
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.IOException

class OnlineAssistant(private val context: Context) {
    
    private val client = OkHttpClient()
    private val prefs = context.getSharedPreferences("ai_settings", Context.MODE_PRIVATE)
    
    // Get AI provider (openai, ollama, groq, or local)
    private fun getAIProvider(): String {
        return prefs.getString("ai_provider", "groq") ?: "groq"
    }
    
    private fun getAPIKey(): String {
        return prefs.getString("api_key", "") ?: ""
    }
    
    private fun getAPIUrl(): String {
        return when (getAIProvider()) {
            "openai" -> "https://api.openai.com/v1/chat/completions"
            "groq" -> "https://api.groq.com/openai/v1/chat/completions"
            "ollama" -> prefs.getString("ollama_url", "http://localhost:11434/api/chat") ?: "http://localhost:11434/api/chat"
            "local" -> prefs.getString("local_url", "http://localhost:8080/v1/chat/completions") ?: "http://localhost:8080/v1/chat/completions"
            else -> "https://api.groq.com/openai/v1/chat/completions"
        }
    }
    
    private fun getModel(): String {
        return when (getAIProvider()) {
            "openai" -> "gpt-3.5-turbo"
            "groq" -> "llama-3.1-70b-versatile"
            "ollama" -> "llama3"
            "local" -> "local-model"
            else -> "llama-3.1-70b-versatile"
        }
    }
    
    // OpenAI-compatible API (works with OpenAI, Groq, Ollama, local LLMs)
    fun chatGPT(prompt: String, callback: (String) -> Unit) {
        val provider = getAIProvider()
        
        if (provider == "ollama") {
            chatOllama(prompt, callback)
            return
        }
        
        val json = JSONObject().apply {
            put("model", getModel())
            put("messages", org.json.JSONArray().apply {
                put(JSONObject().apply {
                    put("role", "user")
                    put("content", prompt)
                })
            })
        }
        
        val requestBuilder = Request.Builder()
            .url(getAPIUrl())
            .post(json.toString().toRequestBody("application/json".toMediaType()))
        
        // Add auth header if API key exists
        val apiKey = getAPIKey()
        if (apiKey.isNotEmpty()) {
            requestBuilder.addHeader("Authorization", "Bearer $apiKey")
        }
        
        client.newCall(requestBuilder.build()).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                callback("Network error: ${e.message}")
            }
            
            override fun onResponse(call: Call, response: Response) {
                try {
                    val result = JSONObject(response.body?.string() ?: "{}")
                    val message = result.optJSONArray("choices")
                        ?.optJSONObject(0)
                        ?.optJSONObject("message")
                        ?.optString("content") ?: "No response"
                    callback(message)
                } catch (e: Exception) {
                    callback("Error parsing response")
                }
            }
        })
    }
    
    // Ollama-specific API
    private fun chatOllama(prompt: String, callback: (String) -> Unit) {
        val json = JSONObject().apply {
            put("model", getModel())
            put("prompt", prompt)
            put("stream", false)
        }
        
        val request = Request.Builder()
            .url(getAPIUrl())
            .post(json.toString().toRequestBody("application/json".toMediaType()))
            .build()
        
        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                callback("Ollama error: ${e.message}")
            }
            
            override fun onResponse(call: Call, response: Response) {
                try {
                    val result = JSONObject(response.body?.string() ?: "{}")
                    val message = result.optString("response", "No response")
                    callback(message)
                } catch (e: Exception) {
                    callback("Error parsing Ollama response")
                }
            }
        })
    }
    
    // Set AI provider
    fun setAIProvider(provider: String, apiKey: String = "", customUrl: String = "") {
        prefs.edit().apply {
            putString("ai_provider", provider)
            if (apiKey.isNotEmpty()) {
                putString("api_key", apiKey)
            }
            if (customUrl.isNotEmpty()) {
                when (provider) {
                    "ollama" -> putString("ollama_url", customUrl)
                    "local" -> putString("local_url", customUrl)
                }
            }
            apply()
        }
    }
    
    // Weather API
    fun getWeather(location: String, callback: (String) -> Unit) {
        val request = Request.Builder()
            .url("https://wttr.in/$location?format=j1")
            .build()
        
        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                callback("Cannot get weather")
            }
            
            override fun onResponse(call: Call, response: Response) {
                val json = JSONObject(response.body?.string() ?: "{}")
                val current = json.optJSONArray("current_condition")?.optJSONObject(0)
                val temp = current?.optString("temp_C")
                val desc = current?.optJSONArray("weatherDesc")?.optJSONObject(0)?.optString("value")
                callback("Temperature is $temp degrees, $desc")
            }
        })
    }
    
    // News API
    fun getNews(callback: (String) -> Unit) {
        val request = Request.Builder()
            .url("https://newsapi.org/v2/top-headlines?country=us&apiKey=YOUR_API_KEY")
            .build()
        
        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                callback("Cannot get news")
            }
            
            override fun onResponse(call: Call, response: Response) {
                val json = JSONObject(response.body?.string() ?: "{}")
                val articles = json.optJSONArray("articles")
                val headlines = mutableListOf<String>()
                for (i in 0 until minOf(3, articles?.length() ?: 0)) {
                    headlines.add(articles?.optJSONObject(i)?.optString("title") ?: "")
                }
                callback("Top news: ${headlines.joinToString(". ")}")
            }
        })
    }
    
    // Wikipedia search
    fun searchWikipedia(query: String, callback: (String) -> Unit) {
        val request = Request.Builder()
            .url("https://en.wikipedia.org/api/rest_v1/page/summary/$query")
            .build()
        
        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                callback("Cannot search Wikipedia")
            }
            
            override fun onResponse(call: Call, response: Response) {
                val json = JSONObject(response.body?.string() ?: "{}")
                val extract = json.optString("extract")
                callback(extract.take(200))
            }
        })
    }
    
    // Translate text
    fun translate(text: String, targetLang: String, callback: (String) -> Unit) {
        val request = Request.Builder()
            .url("https://translate.googleapis.com/translate_a/single?client=gtx&sl=auto&tl=$targetLang&dt=t&q=$text")
            .build()
        
        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                callback("Cannot translate")
            }
            
            override fun onResponse(call: Call, response: Response) {
                val result = response.body?.string() ?: "[]"
                callback("Translation: $result")
            }
        })
    }
}
