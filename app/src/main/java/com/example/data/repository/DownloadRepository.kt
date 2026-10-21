package com.example.data.repository

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import androidx.core.content.FileProvider
import com.example.data.engine.CobaltDownloaderEngine
import com.example.data.engine.ExtractionResult
import com.example.data.engine.VideoMetadataExtractor
import com.example.data.local.DownloadDao
import com.example.data.model.DownloadItem
import com.example.data.model.DownloadStatus
import com.example.data.model.PlatformType
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.text.DecimalFormat
import java.util.concurrent.TimeUnit

data class ActiveDownloadState(
    val isRunning: Boolean = false,
    val downloadId: Long = 0,
    val url: String = "",
    val title: String = "",
    val platform: PlatformType = PlatformType.OTHER,
    val progress: Float = 0f,
    val speed: String = "",
    val status: DownloadStatus = DownloadStatus.PENDING,
    val statusText: String = "",
    val localFilePath: String? = null
)

class DownloadRepository(
    private val context: Context,
    private val dao: DownloadDao,
    private val extractor: VideoMetadataExtractor = VideoMetadataExtractor(),
    val cobaltEngine: CobaltDownloaderEngine = CobaltDownloaderEngine(context),
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.IO)
) {
    val allDownloads: Flow<List<DownloadItem>> = dao.getAllDownloads()

    private val _activeDownload = MutableStateFlow(ActiveDownloadState())
    val activeDownload = _activeDownload.asStateFlow()

    private var activeJob: Job? = null
    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(120, TimeUnit.SECONDS)
        .writeTimeout(120, TimeUnit.SECONDS)
        .build()

    init {
        // Automatically publish any previously downloaded videos to the public Gallery & MediaStore
        scope.launch {
            migrateExistingDownloadsToMediaStore()
        }
    }

    fun startDownload(url: String, quality: String = "1080p") {
        if (_activeDownload.value.isRunning) return

        activeJob?.cancel()
        activeJob = scope.launch {
            val cleanUrl = PlatformType.extractUrlFromText(url) ?: url.trim()
            val platform = PlatformType.fromUrl(cleanUrl)
            _activeDownload.value = ActiveDownloadState(
                isRunning = true,
                url = cleanUrl,
                title = "Analysiere Video-Link...",
                platform = platform,
                progress = 0.05f,
                speed = "Initialisiere",
                status = DownloadStatus.ANALYZING,
                statusText = "Extrahiere Metadaten..."
            )

            // Step 1: Extract Preview Metadata (title, author, thumbnail)
            val info = try {
                extractor.extractInfo(cleanUrl)
            } catch (_: Exception) {
                null
            }

            var finalTitle = info?.title ?: "Video von ${platform.displayName}"
            val finalAuthor = info?.author ?: platform.displayName
            val finalThumb = info?.thumbnailUrl ?: ""

            val initialItem = DownloadItem(
                url = cleanUrl,
                title = finalTitle,
                author = finalAuthor,
                platform = platform.name,
                thumbnailUrl = finalThumb,
                quality = quality,
                status = DownloadStatus.ANALYZING.name,
                progress = 0.1f,
                fileSizeBytes = 0L,
                timestamp = System.currentTimeMillis()
            )

            val insertedId = dao.insertDownload(initialItem)

            _activeDownload.value = _activeDownload.value.copy(
                downloadId = insertedId,
                title = finalTitle,
                status = DownloadStatus.ANALYZING,
                statusText = "Löse echten Video-Stream auf..."
            )

            try {
                // Step 2: Resolve Direct Download Stream using Cobalt Engine (BlackHole Architecture)
                var directDownloadUrl: String? = null
                var directFilename: String? = null

                val isDirectMedia = cleanUrl.lowercase().endsWith(".mp4") ||
                        cleanUrl.lowercase().endsWith(".webm") ||
                        cleanUrl.lowercase().endsWith(".mp3") ||
                        cleanUrl.lowercase().endsWith(".m4a")

                if (isDirectMedia) {
                    directDownloadUrl = cleanUrl
                    directFilename = cleanUrl.substringAfterLast("/").substringBefore("?")
                } else {
                    // Extract real direct stream from platform link
                    val extraction = cobaltEngine.extractMedia(cleanUrl, quality)
                    when (extraction) {
                        is ExtractionResult.Success -> {
                            directDownloadUrl = extraction.directDownloadUrl
                            directFilename = extraction.filename
                            if (extraction.title.isNotBlank()) {
                                finalTitle = extraction.title
                            }
                        }
                        is ExtractionResult.Failure -> {
                            // Professional handling: notify user of real issue instead of downloading wrong file!
                            val errorMsg = extraction.errorMessage
                            dao.updateDownload(initialItem.copy(id = insertedId, status = DownloadStatus.FAILED.name))
                            _activeDownload.value = ActiveDownloadState(
                                isRunning = false,
                                status = DownloadStatus.FAILED,
                                statusText = errorMsg
                            )
                            return@launch
                        }
                    }
                }

                if (directDownloadUrl.isNullOrBlank()) {
                    dao.updateDownload(initialItem.copy(id = insertedId, status = DownloadStatus.FAILED.name))
                    _activeDownload.value = ActiveDownloadState(
                        isRunning = false,
                        status = DownloadStatus.FAILED,
                        statusText = "Download-URL konnte nicht ermittelt werden."
                    )
                    return@launch
                }

                // Step 3: Stream download the real file directly from the resolved URL
                val moviesDir = context.getExternalFilesDir(Environment.DIRECTORY_MOVIES)
                    ?: File(context.filesDir, "movies")
                if (!moviesDir.exists()) moviesDir.mkdirs()

                val extension = if (quality.equals("MP3", ignoreCase = true) || directFilename?.endsWith(".mp3") == true) "mp3" else "mp4"
                val cleanTitle = finalTitle.replace(Regex("[^a-zA-Z0-9.-]"), "_").take(35)
                val targetFile = File(moviesDir, "${cleanTitle}_${System.currentTimeMillis()}.$extension")

                _activeDownload.value = _activeDownload.value.copy(
                    status = DownloadStatus.DOWNLOADING,
                    statusText = "Lade echtes Video herunter..."
                )

                val downloadRequest = Request.Builder()
                    .url(directDownloadUrl)
                    .addHeader("User-Agent", "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36")
                    .build()

                val response = httpClient.newCall(downloadRequest).execute()
                if (!response.isSuccessful) {
                    throw IOException("Server antwortete mit Statuscode: ${response.code}")
                }

                val body = response.body ?: throw IOException("Server-Antwort ist leer.")
                val totalBytes = body.contentLength()

                body.byteStream().use { input ->
                    FileOutputStream(targetFile).use { output ->
                        val buffer = ByteArray(32768)
                        var bytesRead: Int
                        var downloadedBytes = 0L
                        val startTime = System.currentTimeMillis()

                        while (input.read(buffer).also { bytesRead = it } != -1) {
                            output.write(buffer, 0, bytesRead)
                            downloadedBytes += bytesRead

                            val progressFraction = if (totalBytes > 0) {
                                (downloadedBytes.toFloat() / totalBytes).coerceIn(0.05f, 0.99f)
                            } else {
                                0.5f
                            }

                            val elapsedSec = ((System.currentTimeMillis() - startTime) / 1000.0).coerceAtLeast(0.1)
                            val mbps = (downloadedBytes / (1024.0 * 1024.0)) / elapsedSec
                            val speedStr = "${DecimalFormat("#0.0").format(mbps)} MB/s"

                            val downloadedMB = downloadedBytes / (1024.0 * 1024.0)
                            val statusMsg = if (totalBytes > 0) {
                                val totalMB = totalBytes / (1024.0 * 1024.0)
                                "${DecimalFormat("#0.0").format(downloadedMB)} MB / ${DecimalFormat("#0.0").format(totalMB)} MB"
                            } else {
                                "${DecimalFormat("#0.0").format(downloadedMB)} MB geladen"
                            }

                            _activeDownload.value = _activeDownload.value.copy(
                                progress = progressFraction,
                                speed = speedStr,
                                statusText = statusMsg
                            )
                        }
                    }
                }

                val finalSizeBytes = targetFile.length()
                if (finalSizeBytes <= 0) {
                    throw IOException("Heruntergeladene Datei ist leer.")
                }

                val isAudio = quality.equals("MP3", ignoreCase = true) || directFilename?.endsWith(".mp3") == true
                // Save directly to public MediaStore (Filme/ClipDrop or Musik/ClipDrop) so it's instantly in Gallery & File Manager
                val publicLocation = exportToPublicGallery(targetFile, finalTitle, isAudio)

                // Finalize Download with real video file
                val updatedItem = initialItem.copy(
                    id = insertedId,
                    title = finalTitle,
                    status = DownloadStatus.COMPLETED.name,
                    progress = 1.0f,
                    localFilePath = targetFile.absolutePath,
                    fileSizeBytes = finalSizeBytes,
                    downloadSpeed = "Fertig"
                )
                dao.updateDownload(updatedItem)

                _activeDownload.value = _activeDownload.value.copy(
                    isRunning = false,
                    progress = 1.0f,
                    status = DownloadStatus.COMPLETED,
                    statusText = "Gespeichert in Galerie & Filme/ClipDrop!",
                    speed = "Fertig",
                    localFilePath = targetFile.absolutePath
                )

                // Brief visual confirmation of completion, then automatically reset sphere back to ready for next download
                kotlinx.coroutines.delay(2500)
                if (_activeDownload.value.status == DownloadStatus.COMPLETED) {
                    _activeDownload.value = ActiveDownloadState(
                        isRunning = false,
                        status = DownloadStatus.PENDING,
                        statusText = ""
                    )
                }

            } catch (e: CancellationException) {
                dao.updateDownload(initialItem.copy(id = insertedId, status = DownloadStatus.CANCELLED.name))
                _activeDownload.value = ActiveDownloadState(isRunning = false, status = DownloadStatus.CANCELLED)
                kotlinx.coroutines.delay(2500)
                if (_activeDownload.value.status == DownloadStatus.CANCELLED) {
                    resetActiveDownload()
                }
            } catch (e: Exception) {
                dao.updateDownload(initialItem.copy(id = insertedId, status = DownloadStatus.FAILED.name))
                _activeDownload.value = ActiveDownloadState(
                    isRunning = false,
                    status = DownloadStatus.FAILED,
                    statusText = "Download-Fehler: ${e.localizedMessage ?: "Unbekannt"}"
                )
                kotlinx.coroutines.delay(3500)
                if (_activeDownload.value.status == DownloadStatus.FAILED) {
                    resetActiveDownload()
                }
            }
        }
    }

    fun resetActiveDownload() {
        _activeDownload.value = ActiveDownloadState(
            isRunning = false,
            status = DownloadStatus.PENDING,
            statusText = ""
        )
    }

    fun cancelActiveDownload() {
        activeJob?.cancel()
        _activeDownload.value = ActiveDownloadState(isRunning = false, status = DownloadStatus.CANCELLED)
    }

    suspend fun deleteDownload(item: DownloadItem) = withContext(Dispatchers.IO) {
        if (!item.localFilePath.isNullOrBlank()) {
            val file = File(item.localFilePath)
            if (file.exists()) {
                file.delete()
            }
        }
        dao.deleteDownload(item)
    }

    suspend fun clearHistory() = withContext(Dispatchers.IO) {
        dao.clearAll()
    }

    fun openVideo(item: DownloadItem) {
        if (item.localFilePath.isNullOrBlank()) return
        val file = File(item.localFilePath)
        if (!file.exists()) return

        try {
            val uri: Uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )
            val mimeType = if (file.name.endsWith(".mp3")) "audio/*" else "video/*"
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, mimeType)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (_: Exception) {
            // Fallback
        }
    }

    fun openGallery() {
        try {
            val intent = Intent(Intent.ACTION_VIEW).apply {
                type = "video/*"
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(Intent.createChooser(intent, "Galerie öffnen").apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            })
        } catch (_: Exception) {
            try {
                val intent = Intent(Intent.ACTION_MAIN).apply {
                    addCategory(Intent.CATEGORY_APP_GALLERY)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(intent)
            } catch (_: Exception) {}
        }
    }

    fun openDownloadsFolder() {
        try {
            val intent = Intent(android.app.DownloadManager.ACTION_VIEW_DOWNLOADS).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (_: Exception) {
            openGallery()
        }
    }

    fun exportToPublicGallery(file: File, title: String, isAudio: Boolean): String {
        val extension = if (isAudio) "mp3" else "mp4"
        val mimeType = if (isAudio) "audio/mpeg" else "video/mp4"
        val cleanTitle = title.replace(Regex("[^a-zA-Z0-9.-]"), "_").take(35)
        val fileName = "${cleanTitle}_${System.currentTimeMillis()}.$extension"

        // Android 10+ (API 29+) MediaStore API
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            try {
                val contentValues = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                    put(MediaStore.MediaColumns.MIME_TYPE, mimeType)
                    put(
                        MediaStore.MediaColumns.RELATIVE_PATH,
                        if (isAudio) "${Environment.DIRECTORY_MUSIC}/ClipDrop" else "${Environment.DIRECTORY_MOVIES}/ClipDrop"
                    )
                    put(MediaStore.MediaColumns.IS_PENDING, 1)
                }

                val collectionUri = if (isAudio) {
                    MediaStore.Audio.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
                } else {
                    MediaStore.Video.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
                }

                val itemUri = context.contentResolver.insert(collectionUri, contentValues)
                if (itemUri != null) {
                    context.contentResolver.openOutputStream(itemUri)?.use { output ->
                        file.inputStream().use { input ->
                            input.copyTo(output)
                        }
                    }
                    contentValues.clear()
                    contentValues.put(MediaStore.MediaColumns.IS_PENDING, 0)
                    context.contentResolver.update(itemUri, contentValues, null, null)

                    Log.d("DownloadRepo", "Exported to MediaStore: $itemUri")
                    return "${if (isAudio) "Musik" else "Filme"}/ClipDrop/$fileName"
                }
            } catch (e: Exception) {
                Log.e("DownloadRepo", "Failed to export via MediaStore: ${e.message}")
            }
        }

        // Fallback for Android 9 or direct filesystem:
        try {
            val publicDir = File(
                Environment.getExternalStoragePublicDirectory(
                    if (isAudio) Environment.DIRECTORY_MUSIC else Environment.DIRECTORY_MOVIES
                ),
                "ClipDrop"
            )
            if (!publicDir.exists()) publicDir.mkdirs()
            val destFile = File(publicDir, fileName)
            file.copyTo(destFile, overwrite = true)
            MediaScannerConnection.scanFile(
                context,
                arrayOf(destFile.absolutePath),
                arrayOf(mimeType),
                null
            )
            return destFile.absolutePath
        } catch (e: Exception) {
            Log.e("DownloadRepo", "Failed to copy to public dir: ${e.message}")
            return file.absolutePath
        }
    }

    private suspend fun migrateExistingDownloadsToMediaStore() = withContext(Dispatchers.IO) {
        try {
            val list = dao.getAllDownloadsList()
            for (item in list) {
                if (!item.localFilePath.isNullOrBlank()) {
                    val file = File(item.localFilePath)
                    if (file.exists() && item.status == DownloadStatus.COMPLETED.name) {
                        val isAudio = item.quality.equals("MP3", ignoreCase = true) || file.name.endsWith(".mp3")
                        exportToPublicGallery(file, item.title, isAudio)
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("DownloadRepo", "Migration error: ${e.message}")
        }
    }

    fun shareVideo(item: DownloadItem) {
        if (item.localFilePath.isNullOrBlank()) {
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, item.url)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(Intent.createChooser(shareIntent, "Video-Link teilen").apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            })
            return
        }

        val file = File(item.localFilePath)
        if (!file.exists()) return

        try {
            val uri: Uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )
            val mimeType = if (file.name.endsWith(".mp3")) "audio/*" else "video/*"
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = mimeType
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, item.title)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(Intent.createChooser(shareIntent, "Datei teilen").apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            })
        } catch (_: Exception) {
            // Fallback
        }
    }
}
