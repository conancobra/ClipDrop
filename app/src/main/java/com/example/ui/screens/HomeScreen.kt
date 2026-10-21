package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.ElectricBolt
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.MainViewModel
import com.example.data.model.DownloadItem
import com.example.data.model.DownloadStatus
import com.example.data.model.PlatformType
import com.example.data.repository.ActiveDownloadState
import com.example.ui.components.ClipboardInputBar
import com.example.ui.components.PlatformChipsRow
import com.example.ui.components.ThreeDimensionalDownloadCircle
import com.example.ui.theme.DarkSurfaceCard
import com.example.ui.theme.DarkSurfaceElevated
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.NeonGreen

@Composable
fun HomeScreen(
    viewModel: MainViewModel,
    activeDownload: ActiveDownloadState,
    urlInput: String,
    detectedPlatform: PlatformType,
    selectedQuality: String,
    recentDownloads: List<DownloadItem>,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Platform Selection Pills (Filter / Indicator)
        PlatformChipsRow(
            selectedPlatform = detectedPlatform,
            onPlatformClick = { platform ->
                viewModel.onPlatformChipClicked(platform)
            }
        )

        // Center Piece: Large 3D Circle
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            contentAlignment = Alignment.Center
        ) {
            ThreeDimensionalDownloadCircle(
                activeState = activeDownload,
                hasClipboardLink = urlInput.isNotBlank(),
                detectedPlatform = detectedPlatform,
                onCircleClick = { viewModel.onCircleTapped() },
                onCancelClick = { viewModel.cancelActiveDownload() },
                onOpenCompleted = { viewModel.openCompletedVideo() },
                onResetToReady = { viewModel.resetActiveDownload() }
            )
        }

        // Subtitle guidance under 3D Circle
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(horizontal = 24.dp)
        ) {
            Text(
                text = when {
                    activeDownload.isRunning -> activeDownload.statusText
                    activeDownload.status == DownloadStatus.COMPLETED -> "Download abgeschlossen! 🎉"
                    urlInput.isNotBlank() -> "Bereit zum Herunterladen"
                    else -> "Tippe auf den 3D-Kreis"
                },
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 17.sp,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(3.dp))
            Text(
                text = when {
                    activeDownload.isRunning -> activeDownload.title
                    activeDownload.status == DownloadStatus.COMPLETED -> "In Galerie & Filme/ClipDrop gespeichert • Kugel bereit"
                    urlInput.isNotBlank() -> "${detectedPlatform.displayName}-Video: ${urlInput.take(45)}..."
                    else -> "Kopiere einen Link aus Instagram, TikTok, YouTube oder Facebook"
                },
                color = if (urlInput.isNotBlank()) NeonCyan else Color(0xFF94A3B8),
                fontSize = 12.sp,
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            // When download is completed, offer 1-tap view and 1-tap reset buttons
            if (activeDownload.status == DownloadStatus.COMPLETED) {
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Button(
                        onClick = { viewModel.openCompletedVideo() },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = NeonGreen.copy(alpha = 0.22f),
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = null,
                            tint = NeonGreen,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Video ansehen", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }

                    OutlinedButton(
                        onClick = { viewModel.resetActiveDownload() },
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = Color.White
                        )
                    ) {
                        Text("Bereit für neuen Link", fontSize = 12.sp)
                    }
                }
            }
        }

        // Input & Paste Control Bar
        ClipboardInputBar(
            urlText = urlInput,
            onUrlChange = { viewModel.updateUrl(it) },
            detectedPlatform = detectedPlatform,
            selectedQuality = selectedQuality,
            onPasteClick = { viewModel.pasteFromClipboard() },
            onQualityClick = { viewModel.setShowQualityDialog(true) },
            onClearClick = { viewModel.clearUrl() },
            onSubmit = { viewModel.onCircleTapped() }
        )

        // Information / Feature Guide Card
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = DarkSurfaceCard),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.ElectricBolt,
                        contentDescription = null,
                        tint = NeonCyan,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "1-Klick Video-Download",
                        color = Color.White,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 13.sp
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Kopiere einfach einen Link in TikTok, Instagram, YouTube oder Facebook. Beim Öffnen von ClipDrop oder Tippen auf den 3D-Kreis wird genau dieses Video in voller Qualität heruntergeladen.",
                    color = Color(0xFF94A3B8),
                    fontSize = 12.sp,
                    lineHeight = 16.sp
                )
            }
        }

        // Recent Download preview if any
        if (recentDownloads.isNotEmpty()) {
            val latest = recentDownloads.first()
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = DarkSurfaceElevated),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .clickable { viewModel.setPreviewVideo(latest) }
                    .testTag("recent_download_card")
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(NeonGreen.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = NeonGreen,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Zuletzt heruntergeladen:",
                            color = Color(0xFF94A3B8),
                            fontSize = 11.sp
                        )
                        Text(
                            text = latest.title,
                            color = Color.White,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 13.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Icon(
                        imageVector = Icons.Default.ArrowForward,
                        contentDescription = "Öffnen",
                        tint = Color(0xFF94A3B8),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}
