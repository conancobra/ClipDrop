package com.example

import android.app.Application
import android.content.ClipboardManager
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.AppDatabase
import com.example.data.model.DownloadItem
import com.example.data.model.DownloadStatus
import com.example.data.model.PlatformType
import com.example.data.repository.ActiveDownloadState
import com.example.data.repository.DownloadRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getDatabase(application)
    private val repository = DownloadRepository(application, db.downloadDao())
    private val clipboardManager = application.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager

    val allDownloads: StateFlow<List<DownloadItem>> = repository.allDownloads.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val activeDownload: StateFlow<ActiveDownloadState> = repository.activeDownload

    private val _urlInput = MutableStateFlow("")
    val urlInput: StateFlow<String> = _urlInput.asStateFlow()

    private val _clipboardDetectedUrl = MutableStateFlow<String?>(null)
    val clipboardDetectedUrl: StateFlow<String?> = _clipboardDetectedUrl.asStateFlow()

    private val _detectedPlatform = MutableStateFlow(PlatformType.OTHER)
    val detectedPlatform: StateFlow<PlatformType> = _detectedPlatform.asStateFlow()

    private val _selectedQuality = MutableStateFlow("1080p")
    val selectedQuality: StateFlow<String> = _selectedQuality.asStateFlow()

    private val _currentTab = MutableStateFlow(0) // 0: Downloader, 1: Mediathek, 2: Einstellungen
    val currentTab: StateFlow<Int> = _currentTab.asStateFlow()

    private val _selectedVideoForPreview = MutableStateFlow<DownloadItem?>(null)
    val selectedVideoForPreview: StateFlow<DownloadItem?> = _selectedVideoForPreview.asStateFlow()

    private val _showQualityDialog = MutableStateFlow(false)
    val showQualityDialog: StateFlow<Boolean> = _showQualityDialog.asStateFlow()

    private val _snackbarMessage = MutableStateFlow<String?>(null)
    val snackbarMessage: StateFlow<String?> = _snackbarMessage.asStateFlow()

    // Tracks last processed link to avoid re-triggering the same finished video
    private var lastProcessedUrl: String? = null

    init {
        checkClipboard()

        // Observe download state: when a download completes, clear input so next copied link is fresh
        viewModelScope.launch {
            activeDownload.collect { state ->
                if (state.status == DownloadStatus.COMPLETED && state.url.isNotBlank()) {
                    lastProcessedUrl = state.url
                    // Clear the input so next clipboard copy immediately populates
                    if (_urlInput.value == state.url) {
                        _urlInput.value = ""
                    }
                }
            }
        }
    }

    /**
     * Inspects the system clipboard (Black Hole behavior).
     * Extracts pure video URLs from shared text and updates input if appropriate.
     */
    fun checkClipboard() {
        try {
            val clipData = clipboardManager?.primaryClip
            if (clipData != null && clipData.itemCount > 0) {
                val clipText = clipData.getItemAt(0)?.text?.toString()?.trim()
                if (!clipText.isNullOrBlank()) {
                    val cleanUrl = PlatformType.extractUrlFromText(clipText) ?: clipText
                    if (PlatformType.isValidVideoUrl(cleanUrl)) {
                        _clipboardDetectedUrl.value = cleanUrl
                        val platform = PlatformType.fromUrl(cleanUrl)

                        // If input is currently empty, or holds the previously downloaded link:
                        if (_urlInput.value.isBlank() || _urlInput.value == lastProcessedUrl) {
                            _urlInput.value = cleanUrl
                            _detectedPlatform.value = platform
                            if (activeDownload.value.status == DownloadStatus.COMPLETED) {
                                repository.resetActiveDownload()
                            }
                        } else if (_urlInput.value != cleanUrl) {
                            // User has a different link in input but a fresh one in clipboard
                            _detectedPlatform.value = platform
                        }
                    }
                }
            }
        } catch (_: Exception) {}
    }

    fun pasteFromClipboard() {
        try {
            val clipData = clipboardManager?.primaryClip
            if (clipData != null && clipData.itemCount > 0) {
                val clipText = clipData.getItemAt(0)?.text?.toString()?.trim()
                if (!clipText.isNullOrBlank()) {
                    val cleanUrl = PlatformType.extractUrlFromText(clipText) ?: clipText
                    if (PlatformType.isValidVideoUrl(cleanUrl)) {
                        updateUrl(cleanUrl)
                        val platform = PlatformType.fromUrl(cleanUrl)
                        _snackbarMessage.value = "Link von ${platform.displayName} eingefügt!"
                    } else {
                        updateUrl(cleanUrl)
                        _snackbarMessage.value = "Link eingefügt"
                    }
                    if (activeDownload.value.status == DownloadStatus.COMPLETED) {
                        repository.resetActiveDownload()
                    }
                } else {
                    _snackbarMessage.value = "Zwischenablage ist leer. Bitte kopiere zuerst einen Video-Link."
                }
            } else {
                _snackbarMessage.value = "Zwischenablage ist leer. Bitte kopiere zuerst einen Video-Link."
            }
        } catch (_: Exception) {
            _snackbarMessage.value = "Konnte Zwischenablage nicht lesen"
        }
    }

    fun updateUrl(newUrl: String) {
        val clean = PlatformType.extractUrlFromText(newUrl) ?: newUrl.trim()
        _urlInput.value = clean
        _detectedPlatform.value = PlatformType.fromUrl(clean)
        if (activeDownload.value.status == DownloadStatus.COMPLETED) {
            repository.resetActiveDownload()
        }
    }

    fun clearUrl() {
        _urlInput.value = ""
        _detectedPlatform.value = PlatformType.OTHER
        if (activeDownload.value.status == DownloadStatus.COMPLETED) {
            repository.resetActiveDownload()
        }
    }

    fun handleSharedText(sharedText: String, autoStart: Boolean = true) {
        val extractedUrl = PlatformType.extractUrlFromText(sharedText) ?: sharedText.trim()
        if (extractedUrl.isNotBlank()) {
            repository.resetActiveDownload()
            _urlInput.value = extractedUrl
            val platform = PlatformType.fromUrl(extractedUrl)
            _detectedPlatform.value = platform
            _currentTab.value = 0 // Switch to Downloader tab
            if (autoStart && PlatformType.isValidVideoUrl(extractedUrl)) {
                repository.startDownload(extractedUrl, _selectedQuality.value)
                _snackbarMessage.value = "${platform.displayName}-Download gestartet!"
            } else {
                _snackbarMessage.value = "Link von ${platform.displayName} empfangen! Tippe auf den 3D-Kreis."
            }
        }
    }

    fun resetActiveDownload() {
        repository.resetActiveDownload()
    }

    fun onCircleTapped() {
        // Read fresh clipboard if available
        checkClipboard()

        var targetUrl = _urlInput.value.trim()
        if (targetUrl.isBlank() || targetUrl == lastProcessedUrl) {
            targetUrl = _clipboardDetectedUrl.value?.trim() ?: ""
        }

        // If circle is in COMPLETED state:
        if (activeDownload.value.status == DownloadStatus.COMPLETED) {
            // If user has a new link ready, immediately start downloading!
            if (targetUrl.isNotBlank() && targetUrl != lastProcessedUrl) {
                val cleanUrl = PlatformType.extractUrlFromText(targetUrl) ?: targetUrl
                if (PlatformType.isValidVideoUrl(cleanUrl)) {
                    updateUrl(cleanUrl)
                    repository.resetActiveDownload()
                    repository.startDownload(cleanUrl, _selectedQuality.value)
                    return
                }
            }
            // Otherwise reset circle to ready so user can paste or type a new link
            repository.resetActiveDownload()
            _snackbarMessage.value = "Kugel bereit! Füge einen Video-Link ein."
            return
        }

        if (targetUrl.isBlank()) {
            _snackbarMessage.value = "Zwischenablage ist leer. Kopiere einen Video-Link (YouTube, Instagram, TikTok, Facebook, X, Reddit) und tippe erneut!"
            return
        }

        val cleanUrl = PlatformType.extractUrlFromText(targetUrl) ?: targetUrl
        if (!PlatformType.isValidVideoUrl(cleanUrl)) {
            _snackbarMessage.value = "Bitte einen gültigen Video-Link verwenden."
            return
        }

        updateUrl(cleanUrl)
        repository.startDownload(cleanUrl, _selectedQuality.value)
    }

    fun onPlatformChipClicked(platform: PlatformType) {
        if (activeDownload.value.status == DownloadStatus.COMPLETED) {
            repository.resetActiveDownload()
        }
        // Platform chips are filters / indicators, do not overwrite user's URL with fake demo videos
        if (_urlInput.value.isBlank()) {
            _detectedPlatform.value = platform
            _snackbarMessage.value = "${platform.displayName} ausgewählt. Kopiere einen Link und tippe auf den 3D-Kreis."
        } else {
            _detectedPlatform.value = platform
        }
    }

    fun cancelActiveDownload() {
        repository.cancelActiveDownload()
        _snackbarMessage.value = "Download abgebrochen"
    }

    fun openCompletedVideo() {
        val active = activeDownload.value
        val localPath = active.localFilePath
        if (!localPath.isNullOrBlank()) {
            val item = DownloadItem(
                url = active.url,
                title = active.title,
                platform = active.platform.name,
                localFilePath = localPath
            )
            repository.openVideo(item)
        }
    }

    fun openGallery() {
        repository.openGallery()
    }

    fun openDownloadsFolder() {
        repository.openDownloadsFolder()
    }

    fun openVideoItem(item: DownloadItem) {
        repository.openVideo(item)
    }

    fun shareVideoItem(item: DownloadItem) {
        repository.shareVideo(item)
    }

    fun deleteVideoItem(item: DownloadItem) {
        viewModelScope.launch {
            repository.deleteDownload(item)
            _snackbarMessage.value = "Video gelöscht"
        }
    }

    fun clearAllHistory() {
        viewModelScope.launch {
            repository.clearHistory()
            _snackbarMessage.value = "Mediathek geleert"
        }
    }

    fun setQuality(quality: String) {
        _selectedQuality.value = quality
    }

    fun setShowQualityDialog(show: Boolean) {
        _showQualityDialog.value = show
    }

    fun setPreviewVideo(item: DownloadItem?) {
        _selectedVideoForPreview.value = item
    }

    fun selectTab(tabIndex: Int) {
        _currentTab.value = tabIndex
    }

    fun getActiveApiUrl(): String = repository.cobaltEngine.getActiveApiUrl()

    fun setCustomApiUrl(url: String?) {
        repository.cobaltEngine.setCustomApiUrl(url)
        _snackbarMessage.value = "Server-URL aktualisiert"
    }

    fun resetApiUrl() {
        repository.cobaltEngine.setCustomApiUrl(null)
        _snackbarMessage.value = "Standard-Server wiederhergestellt"
    }

    fun dismissSnackbar() {
        _snackbarMessage.value = null
    }
}
