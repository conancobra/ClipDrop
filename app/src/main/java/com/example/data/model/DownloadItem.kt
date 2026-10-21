package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "downloads")
data class DownloadItem(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val url: String,
    val title: String,
    val author: String = "",
    val platform: String, // from PlatformType.name
    val thumbnailUrl: String = "",
    val localFilePath: String? = null,
    val fileSizeBytes: Long = 0,
    val durationSec: Int = 0,
    val quality: String = "1080p",
    val status: String = DownloadStatus.PENDING.name,
    val progress: Float = 0f,
    val downloadSpeed: String = "",
    val timestamp: Long = System.currentTimeMillis()
)
