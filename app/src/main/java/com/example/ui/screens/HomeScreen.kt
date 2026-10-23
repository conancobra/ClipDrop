package com.example.ui.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.MainViewModel
import com.example.data.model.DownloadItem
import com.example.data.model.PlatformType
import com.example.data.repository.ActiveDownloadState
import com.example.ui.components.PlatformChipsRow
import com.example.ui.components.ThreeDimensionalDownloadCircle

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
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Platform Selection Pills (Filter / Indicator)
        PlatformChipsRow(
            selectedPlatform = detectedPlatform,
            onPlatformClick = { platform ->
                viewModel.onPlatformChipClicked(platform)
            }
        )

        // Center Piece: Large Paste & Start Circle Centered
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
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
    }
}
