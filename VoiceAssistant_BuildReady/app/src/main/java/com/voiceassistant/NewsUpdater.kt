package com.voiceassistant

import android.content.Context
import okhttp3.*
import org.json.JSONObject
import java.io.IOException

class NewsUpdater(private val context: Context) {
    
    private val client = OkHttpClient()
    private val prefs = context.getSharedPreferences("news_settings", Context.MODE_PRIVATE)
    
    // Multiple news sources
    fun getTopHeadlines(category: String = "general", callback: (List<NewsArticle>) -> Unit) {
        val apiKey = prefs.getString("news_api_key", "") ?: ""
        
        if (apiKey.isEmpty()) {
            // Use free RSS feeds as fallback
            getRSSNews(callback)
            return
        }
        
        val url = "https://newsapi.org/v2/top-headlines?country=us&category=$category&apiKey=$apiKey"
        
        val request = Request.Builder().url(url).build()
        
        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                getRSSNews(callback)
            }
            
            override fun onResponse(call: Call, response: Response) {
                try {
                    val json = JSONObject(response.body?.string() ?: "{}")
                    val articles = json.optJSONArray("articles")
                    val newsList = mutableListOf<NewsArticle>()
                    
                    for (i in 0 until (articles?.length() ?: 0)) {
                        val article = articles?.getJSONObject(i)
                        newsList.add(NewsArticle(
                            title = article?.optString("title") ?: "",
                            description = article?.optString("description") ?: "",
                            url = article?.optString("url") ?: "",
                            source = article?.optJSONObject("source")?.optString("name") ?: "",
                            publishedAt = article?.optString("publishedAt") ?: "",
                            imageUrl = article?.optString("urlToImage") ?: ""
                        ))
                    }
                    callback(newsList)
                } catch (e: Exception) {
                    getRSSNews(callback)
                }
            }
        })
    }
    
    // Free RSS news (no API key needed)
    private fun getRSSNews(callback: (List<NewsArticle>) -> Unit) {
        val rssSources = listOf(
            "https://rss.nytimes.com/services/xml/rss/nyt/HomePage.xml",
            "http://feeds.bbci.co.uk/news/rss.xml",
            "https://www.reddit.com/r/worldnews/.rss"
        )
        
        // Simple RSS parsing
        val url = rssSources.first()
        val request = Request.Builder().url(url).build()
        
        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                callback(emptyList())
            }
            
            override fun onResponse(call: Call, response: Response) {
                val xml = response.body?.string() ?: ""
                val articles = parseRSS(xml)
                callback(articles)
            }
        })
    }
    
    private fun parseRSS(xml: String): List<NewsArticle> {
        val articles = mutableListOf<NewsArticle>()
        val items = xml.split("<item>")
        
        for (i in 1 until items.size) {
            val item = items[i]
            val title = extractTag(item, "title")
            val description = extractTag(item, "description")
            val link = extractTag(item, "link")
            val pubDate = extractTag(item, "pubDate")
            
            if (title.isNotEmpty()) {
                articles.add(NewsArticle(
                    title = title,
                    description = description,
                    url = link,
                    source = "RSS Feed",
                    publishedAt = pubDate,
                    imageUrl = ""
                ))
            }
        }
        
        return articles.take(10)
    }
    
    private fun extractTag(xml: String, tag: String): String {
        val start = xml.indexOf("<$tag>") + tag.length + 2
        val end = xml.indexOf("</$tag>")
        return if (start > 0 && end > start) {
            xml.substring(start, end).trim()
        } else ""
    }
    
    // Search news by keyword
    fun searchNews(query: String, callback: (List<NewsArticle>) -> Unit) {
        val apiKey = prefs.getString("news_api_key", "") ?: ""
        
        if (apiKey.isEmpty()) {
            callback(emptyList())
            return
        }
        
        val url = "https://newsapi.org/v2/everything?q=$query&sortBy=publishedAt&apiKey=$apiKey"
        val request = Request.Builder().url(url).build()
        
        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                callback(emptyList())
            }
            
            override fun onResponse(call: Call, response: Response) {
                try {
                    val json = JSONObject(response.body?.string() ?: "{}")
                    val articles = json.optJSONArray("articles")
                    val newsList = mutableListOf<NewsArticle>()
                    
                    for (i in 0 until minOf(10, articles?.length() ?: 0)) {
                        val article = articles?.getJSONObject(i)
                        newsList.add(NewsArticle(
                            title = article?.optString("title") ?: "",
                            description = article?.optString("description") ?: "",
                            url = article?.optString("url") ?: "",
                            source = article?.optJSONObject("source")?.optString("name") ?: "",
                            publishedAt = article?.optString("publishedAt") ?: "",
                            imageUrl = article?.optString("urlToImage") ?: ""
                        ))
                    }
                    callback(newsList)
                } catch (e: Exception) {
                    callback(emptyList())
                }
            }
        })
    }
    
    // Get news by category
    fun getNewsByCategory(category: String, callback: (List<NewsArticle>) -> Unit) {
        getTopHeadlines(category, callback)
    }
    
    // Get trending topics
    fun getTrendingTopics(callback: (List<String>) -> Unit) {
        // Use Google Trends or Twitter API
        val trends = listOf("Technology", "Politics", "Sports", "Entertainment", "Science")
        callback(trends)
    }
    
    // Save news preferences
    fun setNewsPreferences(categories: List<String>, sources: List<String>) {
        prefs.edit().apply {
            putString("preferred_categories", categories.joinToString(","))
            putString("preferred_sources", sources.joinToString(","))
            apply()
        }
    }
    
    // Get personalized news
    fun getPersonalizedNews(callback: (List<NewsArticle>) -> Unit) {
        val categories = prefs.getString("preferred_categories", "general")?.split(",") ?: listOf("general")
        getTopHeadlines(categories.first(), callback)
    }
    
    // Set News API key
    fun setAPIKey(key: String) {
        prefs.edit().putString("news_api_key", key).apply()
    }
    
    // Summarize news article
    fun summarizeArticle(article: NewsArticle, onlineAssistant: OnlineAssistant, callback: (String) -> Unit) {
        onlineAssistant.chatGPT("Summarize this news in 2 sentences: ${article.title}. ${article.description}") { summary ->
            callback(summary)
        }
    }
}

data class NewsArticle(
    val title: String,
    val description: String,
    val url: String,
    val source: String,
    val publishedAt: String,
    val imageUrl: String
)
