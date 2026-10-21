package com.example.data.engine

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.util.concurrent.TimeUnit

sealed class ExtractionResult {
    data class Success(
        val directDownloadUrl: String,
        val filename: String,
        val title: String
    ) : ExtractionResult()

    data class Failure(
        val errorMessage: String
    ) : ExtractionResult()
}

class CobaltDownloaderEngine(
    private val context: Context,
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(45, TimeUnit.SECONDS)
        .writeTimeout(45, TimeUnit.SECONDS)
        .followRedirects(true)
        .followSslRedirects(true)
        .build()
) {
    companion object {
        const val PREFS_NAME = "clipdrop_prefs"
        const val KEY_CUSTOM_API_URL = "cobalt_custom_api_url"
        const val DEFAULT_PRIMARY_API = "https://api.cobalt.liubquanti.click/"
        
        // Verified community endpoints for Cobalt API
        val FALLBACK_APIS = listOf(
            "https://api.cobalt.liubquanti.click/",
            "https://api.urlvideodownload.com/",
            "https://cobalt.liubquanti.click/api/"
        )
    }

    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun getActiveApiUrl(): String {
        return prefs.getString(KEY_CUSTOM_API_URL, null)?.takeIf { it.isNotBlank() } ?: DEFAULT_PRIMARY_API
    }

    fun setCustomApiUrl(url: String?) {
        prefs.edit().putString(KEY_CUSTOM_API_URL, url?.trim()).apply()
    }

    suspend fun resolveRedirect(rawUrl: String): String = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder()
                .url(rawUrl)
                .head()
                .header("User-Agent", "Mozilla/5.0 (Linux; Android 14; Pixel 8) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Mobile Safari/537.36")
                .build()

            client.newCall(request).execute().use { response ->
                val finalUrl = response.request.url.toString()
                return@withContext finalUrl
            }
        } catch (e: Exception) {
            Log.e("CobaltEngine", "Error resolving redirect for $rawUrl: ${e.message}")
            return@withContext rawUrl
        }
    }

    private fun cleanUrl(rawUrl: String): String {
        var url = rawUrl.trim()
        // Strip trailing punctuation
        url = url.trimEnd('.', ',', '!', '?', ';', ')')
        
        // Remove tracking query params that confuse extractors, while keeping essential identifiers
        if (url.contains("instagram.com") && url.contains("?")) {
            url = url.substringBefore("?")
        } else if (url.contains("tiktok.com/@") && url.contains("?")) {
            url = url.substringBefore("?")
        } else if (url.contains("youtu.be/")) {
            url = url.substringBefore("&si=")
        }
        return url
    }

    suspend fun extractMedia(targetUrl: String, quality: String): ExtractionResult = withContext(Dispatchers.IO) {
        val cleaned = cleanUrl(targetUrl)
        val resolvedUrl = resolveRedirect(cleaned)
        val activeEndpoint = getActiveApiUrl()

        // List of endpoints to try: configured active endpoint first, then fallbacks
        val endpointsToTry = (listOf(activeEndpoint) + FALLBACK_APIS).distinct()

        var lastError = "Keine Verbindung zum Download-Server möglich."

        for (apiEndpoint in endpointsToTry) {
            try {
                val isAudioOnly = quality.equals("MP3", ignoreCase = true)
                val jsonPayload = JSONObject().apply {
                    put("url", resolvedUrl)
                    if (isAudioOnly) {
                        put("downloadMode", "audio")
                        put("audioFormat", "mp3")
                    } else {
                        val cobaltQuality = when (quality) {
                            "720p" -> "720"
                            "1080p" -> "1080"
                            else -> "max"
                        }
                        put("videoQuality", cobaltQuality)
                    }
                }

                val body = jsonPayload.toString().toRequestBody("application/json; charset=utf-8".toMediaType())
                val request = Request.Builder()
                    .url(apiEndpoint)
                    .addHeader("Accept", "application/json")
                    .addHeader("Content-Type", "application/json")
                    .addHeader("User-Agent", "ClipDrop/1.0 (Android; Linux)")
                    .post(body)
                    .build()

                val response = client.newCall(request).execute()
                val responseString = response.body?.string()

                if (!response.isSuccessful || responseString.isNullOrBlank()) {
                    lastError = "Server antwortete mit Fehlercode: ${response.code}"
                    continue
                }

                val jsonResponse = JSONObject(responseString)
                val status = jsonResponse.optString("status", "")

                if (status == "tunnel" || status == "redirect") {
                    val directUrl = jsonResponse.optString("url")
                    val filename = jsonResponse.optString("filename", "video.mp4")
                    val title = filename.substringBeforeLast(".").ifBlank { "Video" }
                    if (directUrl.isNotBlank()) {
                        return@withContext ExtractionResult.Success(
                            directDownloadUrl = directUrl,
                            filename = filename,
                            title = title
                        )
                    }
                } else if (status == "picker") {
                    // Multi-item / carousel post
                    val pickerArray = jsonResponse.optJSONArray("picker")
                    if (pickerArray != null && pickerArray.length() > 0) {
                        val firstItem = pickerArray.getJSONObject(0)
                        val directUrl = firstItem.optString("url")
                        val filename = "download_${System.currentTimeMillis()}.${if (isAudioOnly) "mp3" else "mp4"}"
                        if (directUrl.isNotBlank()) {
                            return@withContext ExtractionResult.Success(
                                directDownloadUrl = directUrl,
                                filename = filename,
                                title = "Media Download"
                            )
                        }
                    }
                } else if (status == "error") {
                    val errObj = jsonResponse.optJSONObject("error")
                    val errCode = errObj?.optString("code") ?: jsonResponse.optString("text")
                    
                    val userFriendlyMsg = when {
                        errCode.contains("unavailable") || errCode.contains("post.unavailable") -> 
                            "Dieses Video ist nicht verfügbar oder wurde entfernt."
                        errCode.contains("private") -> 
                            "Dieses Video ist auf einem privaten Konto und kann nicht geladen werden."
                        errCode.contains("age_restricted") -> 
                            "Dieses Video hat eine Altersbeschränkung."
                        errCode.contains("rate_exceeded") -> 
                            "Server-Limit vorübergehend erreicht. Versuche es gleich noch einmal."
                        else -> "Server-Meldung: $errCode"
                    }
                    
                    lastError = userFriendlyMsg
                    // If content is private or removed, trying other instances won't change that
                    if (errCode.contains("unavailable") || errCode.contains("private") || errCode.contains("age_restricted")) {
                        return@withContext ExtractionResult.Failure(userFriendlyMsg)
                    }
                }
            } catch (e: Exception) {
                Log.e("CobaltEngine", "Attempt failed on $apiEndpoint: ${e.message}")
                lastError = e.localizedMessage ?: "Netzwerkfehler beim Verbinden zum Server"
            }
        }

        return@withContext ExtractionResult.Failure(lastError)
    }
}
