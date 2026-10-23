package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.ThumbUp
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.PlatformType
import com.example.ui.theme.DarkSurfaceElevated
import com.example.ui.theme.FacebookBlue
import com.example.ui.theme.InstagramPurple
import com.example.ui.theme.TikTokPink
import com.example.ui.theme.YouTubeRed

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Forum
import androidx.compose.material.icons.filled.Share

@Composable
fun PlatformChipsRow(
    selectedPlatform: PlatformType,
    onPlatformClick: (PlatformType) -> Unit,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()

    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(scrollState)
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        val platforms = listOf(
            PlatformItem(PlatformType.YOUTUBE, "YouTube", YouTubeRed, Icons.Default.PlayArrow),
            PlatformItem(PlatformType.INSTAGRAM, "Instagram", InstagramPurple, Icons.Default.CameraAlt),
            PlatformItem(PlatformType.TIKTOK, "TikTok", TikTokPink, Icons.Default.MusicNote),
            PlatformItem(PlatformType.FACEBOOK, "Facebook", FacebookBlue, Icons.Default.ThumbUp),
            PlatformItem(PlatformType.TWITTER, "X / Twitter", Color(0xFF1DA1F2), Icons.Default.Share),
            PlatformItem(PlatformType.REDDIT, "Reddit", Color(0xFFFF4500), Icons.Default.Forum),
            PlatformItem(PlatformType.PINTEREST, "Pinterest", Color(0xFFE60023), Icons.Default.Bookmark)
        )

        platforms.forEach { item ->
            val isSelected = selectedPlatform == item.type
            val bgColor by animateColorAsState(
                targetValue = if (isSelected) item.brandColor.copy(alpha = 0.22f) else DarkSurfaceElevated,
                animationSpec = tween(250),
                label = "chipBg"
            )
            val borderColor by animateColorAsState(
                targetValue = if (isSelected) item.brandColor else Color.Transparent,
                animationSpec = tween(250),
                label = "chipBorder"
            )

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .clip(RoundedCornerShape(14.dp))
                    .clickable { onPlatformClick(item.type) }
                    .background(bgColor)
                    .border(1.5.dp, borderColor, RoundedCornerShape(14.dp))
                    .padding(horizontal = 10.dp, vertical = 8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(item.brandColor.copy(alpha = if (isSelected) 0.95f else 0.4f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = item.icon,
                        contentDescription = item.name,
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = item.name,
                    color = if (isSelected) Color.White else Color(0xFF94A3B8),
                    fontSize = 11.sp,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                )
            }
        }
    }
}

private data class PlatformItem(
    val type: PlatformType,
    val name: String,
    val brandColor: Color,
    val icon: ImageVector
)
