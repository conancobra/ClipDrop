package com.example.data.model

import androidx.compose.ui.graphics.Color
import com.example.ui.theme.FacebookBlue
import com.example.ui.theme.InstagramPurple
import com.example.ui.theme.TikTokPink
import com.example.ui.theme.YouTubeRed

enum class PlatformType(
    val displayName: String,
    val primaryColor: Color,
    val badgeIconName: String,
    val sampleDomain: String
) {
    YOUTUBE("YouTube", YouTubeRed, "play_arrow", "youtube.com"),
    INSTAGRAM("Instagram", InstagramPurple, "camera_alt", "instagram.com"),
    TIKTOK("TikTok", TikTokPink, "music_note", "tiktok.com"),
    FACEBOOK("Facebook", FacebookBlue, "thumb_up", "facebook.com"),
    TWITTER("X / Twitter", Color(0xFF1DA1F2), "share", "x.com"),
    REDDIT("Reddit", Color(0xFFFF4500), "forum", "reddit.com"),
    PINTEREST("Pinterest", Color(0xFFE60023), "push_pin", "pinterest.com"),
    OTHER("Web Video", Color(0xFF00E5FF), "movie", "web");

    companion object {
        private val URL_PATTERN = java.util.regex.Pattern.compile("""https?://[^\s<>"']+""", java.util.regex.Pattern.CASE_INSENSITIVE)

        fun extractUrlFromText(text: String?): String? {
            if (text.isNullOrBlank()) return null
            val matcher = URL_PATTERN.matcher(text)
            if (matcher.find()) {
                val found = matcher.group(0)?.trim() ?: ""
                return found.trimEnd('.', ',', ';', '!', '?', ')', ']', '}', '"', '\'')
                    .takeIf { it.length > 10 }
            }
            val trimmed = text.trim()
            return if (trimmed.startsWith("http://") || trimmed.startsWith("https://")) {
                trimmed.trimEnd('.', ',', ';', '!', '?', ')', ']', '}', '"', '\'')
                    .takeIf { it.length > 10 }
            } else null
        }

        fun fromUrl(url: String?): PlatformType {
            val cleanUrl = extractUrlFromText(url) ?: url
            if (cleanUrl.isNullOrBlank()) return OTHER
            val lower = cleanUrl.lowercase().trim()
            return when {
                lower.contains("youtube.com") || lower.contains("youtu.be") -> YOUTUBE
                lower.contains("instagram.com") || lower.contains("instagr.am") -> INSTAGRAM
                lower.contains("tiktok.com") || lower.contains("vm.tiktok.com") || lower.contains("vt.tiktok.com") -> TIKTOK
                lower.contains("facebook.com") || lower.contains("fb.watch") || lower.contains("fb.com") || lower.contains("fb.gg") -> FACEBOOK
                lower.contains("twitter.com") || lower.contains("x.com") -> TWITTER
                lower.contains("reddit.com") || lower.contains("redd.it") -> REDDIT
                lower.contains("pinterest.com") || lower.contains("pin.it") -> PINTEREST
                else -> OTHER
            }
        }

        fun isValidVideoUrl(url: String?): Boolean {
            val cleanUrl = extractUrlFromText(url) ?: url
            if (cleanUrl.isNullOrBlank()) return false
            val lower = cleanUrl.lowercase().trim()
            val isKnownPlatform = lower.contains("youtube.com") ||
                    lower.contains("youtu.be") ||
                    lower.contains("instagram.com") ||
                    lower.contains("instagr.am") ||
                    lower.contains("tiktok.com") ||
                    lower.contains("facebook.com") ||
                    lower.contains("fb.watch") ||
                    lower.contains("fb.com") ||
                    lower.contains("twitter.com") ||
                    lower.contains("x.com") ||
                    lower.contains("reddit.com") ||
                    lower.contains("redd.it") ||
                    lower.contains("pinterest.com") ||
                    lower.contains("pin.it")

            val isGeneralMedia = lower.startsWith("http://") || lower.startsWith("https://")
            return isKnownPlatform || (isGeneralMedia && (
                lower.endsWith(".mp4") ||
                lower.endsWith(".webm") ||
                lower.endsWith(".m4a") ||
                lower.endsWith(".mov") ||
                lower.contains("video")
            ))
        }
    }
}

enum class DownloadStatus {
    PENDING,
    ANALYZING,
    DOWNLOADING,
    COMPLETED,
    FAILED,
    CANCELLED
}
