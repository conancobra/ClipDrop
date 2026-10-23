package com.example.ui.screens

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
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.HighQuality
import androidx.compose.material.icons.filled.SdStorage
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.MainViewModel
import com.example.data.model.DownloadItem
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.DarkSurfaceCard
import com.example.ui.theme.DarkSurfaceElevated
import com.example.ui.theme.NeonCyan
import java.text.DecimalFormat

@Composable
fun SettingsScreen(
    viewModel: MainViewModel,
    downloads: List<DownloadItem>,
    selectedQuality: String,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()
    var showClearDialog by remember { mutableStateOf(false) }
    var showApiDialog by remember { mutableStateOf(false) }
    var customApiInput by remember { mutableStateOf(viewModel.getActiveApiUrl()) }

    val totalBytes = downloads.sumOf { if (it.fileSizeBytes > 1024) it.fileSizeBytes else 5_889_000L }
    val totalMb = DecimalFormat("#0.0").format(totalBytes / (1024.0 * 1024.0))

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // App Branding Card
        Card(
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = DarkSurfaceCard),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .clip(CircleShape)
                        .background(NeonCyan.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.HighQuality,
                        contentDescription = null,
                        tint = NeonCyan,
                        modifier = Modifier.size(26.dp)
                    )
                }

                Spacer(modifier = Modifier.width(14.dp))

                Column {
                    Text(
                        text = "ClipDrop Downloader",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                    Text(
                        text = "Version 1.2 • Schneller Video Saver",
                        color = Color(0xFF94A3B8),
                        fontSize = 12.sp
                    )
                }
            }
        }

        // Settings Section: Preferences
        Text(
            text = "EINSTELLUNGEN",
            color = Color(0xFF64748B),
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp
        )

        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = DarkSurfaceCard),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column {
                SettingsRow(
                    icon = Icons.Default.HighQuality,
                    title = "Standard-Qualität",
                    subtitle = selectedQuality,
                    onClick = { viewModel.setShowQualityDialog(true) }
                )

                Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(DarkSurfaceElevated))

                SettingsRow(
                    icon = Icons.Default.CloudDownload,
                    title = "Download-Server (Cobalt API)",
                    subtitle = viewModel.getActiveApiUrl(),
                    onClick = {
                        customApiInput = viewModel.getActiveApiUrl()
                        showApiDialog = true
                    }
                )

                Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(DarkSurfaceElevated))

                SettingsRow(
                    icon = Icons.Default.SdStorage,
                    title = "Speicherbelegung",
                    subtitle = "${downloads.size} Videos ($totalMb MB)",
                    onClick = {}
                )

                Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(DarkSurfaceElevated))

                SettingsRow(
                    icon = Icons.Default.CleaningServices,
                    title = "Mediathek leeren",
                    subtitle = "Alle Downloads von diesem Gerät entfernen",
                    titleColor = Color(0xFFFF5252),
                    onClick = { showClearDialog = true }
                )
            }
        }

        // How-To Guide
        Text(
            text = "ANLEITUNG",
            color = Color(0xFF64748B),
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp
        )

        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = DarkSurfaceCard),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                InstructionStep(
                    step = "1",
                    title = "Link kopieren",
                    desc = "Öffne Instagram, TikTok, YouTube oder Facebook und kopiere den Link zum Video oder Reel."
                )
                InstructionStep(
                    step = "2",
                    title = "ClipDrop öffnen",
                    desc = "Die App erkennt den Link in deiner Zwischenablage automatisch."
                )
                InstructionStep(
                    step = "3",
                    title = "3D-Kreis antippen",
                    desc = "Tippe auf den großen leuchtenden 3D-Kreis in der Mitte, um den Download sofort zu starten."
                )
                InstructionStep(
                    step = "4",
                    title = "Offline ansehen & teilen",
                    desc = "Dein Video wird in der Mediathek gespeichert und kann jederzeit abgespielt werden."
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))
    }

    if (showClearDialog) {
        AlertDialog(
            onDismissRequest = { showClearDialog = false },
            containerColor = DarkSurface,
            shape = RoundedCornerShape(20.dp),
            title = {
                Text("Mediathek leeren?", color = Color.White, fontWeight = FontWeight.Bold)
            },
            text = {
                Text(
                    "Möchtest du wirklich alle heruntergeladenen Videos löschen? Diese Aktion kann nicht rückgängig gemacht werden.",
                    color = Color(0xFF94A3B8)
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.clearAllHistory()
                        showClearDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF5252))
                ) {
                    Text("Alles löschen", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearDialog = false }) {
                    Text("Abbrechen", color = Color(0xFF94A3B8))
                }
            }
        )
    }

    if (showApiDialog) {
        AlertDialog(
            onDismissRequest = { showApiDialog = false },
            containerColor = DarkSurface,
            shape = RoundedCornerShape(20.dp),
            title = {
                Text("Cobalt API Server", color = Color.White, fontWeight = FontWeight.Bold)
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        "ClipDrop nutzt die Cobalt API (bekannt aus BlackHole) zum Herunterladen echter Videos von TikTok, Instagram, YouTube und Facebook.",
                        color = Color(0xFF94A3B8),
                        fontSize = 12.sp,
                        lineHeight = 16.sp
                    )
                    OutlinedTextField(
                        value = customApiInput,
                        onValueChange = { customApiInput = it },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = NeonCyan,
                            unfocusedBorderColor = Color(0xFF273142)
                        )
                    )
                    TextButton(
                        onClick = {
                            viewModel.resetApiUrl()
                            customApiInput = viewModel.getActiveApiUrl()
                            showApiDialog = false
                        }
                    ) {
                        Text("Standard-Server wiederherstellen", color = NeonCyan, fontSize = 12.sp)
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.setCustomApiUrl(customApiInput)
                        showApiDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = NeonCyan)
                ) {
                    Text("Speichern", color = Color(0xFF0C0F17), fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showApiDialog = false }) {
                    Text("Abbrechen", color = Color(0xFF94A3B8))
                }
            }
        )
    }
}

@Composable
private fun SettingsRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    titleColor: Color = Color.White,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(DarkSurfaceElevated),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = NeonCyan,
                modifier = Modifier.size(20.dp)
            )
        }

        Spacer(modifier = Modifier.width(14.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                color = titleColor,
                fontWeight = FontWeight.SemiBold,
                fontSize = 14.sp
            )
            Text(
                text = subtitle,
                color = Color(0xFF94A3B8),
                fontSize = 12.sp
            )
        }

        Icon(
            imageVector = Icons.Default.ChevronRight,
            contentDescription = null,
            tint = Color(0xFF64748B),
            modifier = Modifier.size(18.dp)
        )
    }
}

@Composable
private fun InstructionStep(
    step: String,
    title: String,
    desc: String
) {
    Row(modifier = Modifier.fillMaxWidth()) {
        Box(
            modifier = Modifier
                .size(26.dp)
                .clip(CircleShape)
                .background(NeonCyan.copy(alpha = 0.2f)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = step,
                color = NeonCyan,
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                color = Color.White,
                fontWeight = FontWeight.SemiBold,
                fontSize = 13.sp
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = desc,
                color = Color(0xFF94A3B8),
                fontSize = 12.sp,
                lineHeight = 16.sp
            )
        }
    }
}
