package com.example.coling.ui.screens

import android.content.Intent
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import kotlin.math.sin
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.coling.ui.theme.BorderColor
import com.example.coling.ui.theme.DarkSurface
import com.example.coling.ui.theme.PrimaryAccent
import com.example.coling.ui.theme.SecondaryAccent
import com.example.coling.ui.theme.TextSecondary
import com.example.coling.utils.getFileName
import com.example.coling.utils.probeMediaFromUri
import androidx.compose.runtime.collectAsState
import com.example.coling.data.ProjectViewModel
import com.example.coling.data.MediaAssetEntity
import java.io.File
import androidx.compose.ui.geometry.Offset
import androidx.compose.foundation.BorderStroke
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class MediaMetadata(
    val fileName: String,
    val filePath: String,
    val format: String,
    val duration: String,
    val size: String,
    val videoCodec: String,
    val audioCodec: String,
    val resolution: String
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MediaScreen(viewModel: ProjectViewModel) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val mediaList by viewModel.mediaAssets.collectAsState()
    var selectedMedia by remember { mutableStateOf<MediaAssetEntity?>(null) }
    var isLoading by remember { mutableStateOf(false) }
    var showAdvancedImport by remember { mutableStateOf(false) }
    var inputPath by remember { mutableStateOf("") }

    // SAF document picker
    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenMultipleDocuments()
    ) { uris ->
        if (uris.isNotEmpty()) {
            isLoading = true
            scope.launch {
                uris.forEach { uri ->
                    // Persist permission across process death
                    try {
                        context.contentResolver.takePersistableUriPermission(
                            uri, Intent.FLAG_GRANT_READ_URI_PERMISSION
                        )
                    } catch (_: Exception) { /* Some providers don't support persistable */ }

                    val fileName = withContext(Dispatchers.IO) {
                        getFileName(context, uri)
                    }
                    val meta = withContext(Dispatchers.IO) {
                        probeMediaFromUri(context, uri, fileName)
                    }
                    viewModel.importMedia(
                        fileName = meta.fileName,
                        filePath = meta.filePath,
                        format = meta.format,
                        duration = meta.duration,
                        size = meta.size,
                        videoCodec = meta.videoCodec,
                        audioCodec = meta.audioCodec,
                        resolution = meta.resolution
                    )
                    selectedMedia = MediaAssetEntity(
                        id = "",
                        projectId = "",
                        fileName = meta.fileName,
                        filePath = meta.filePath,
                        format = meta.format,
                        duration = meta.duration,
                        size = meta.size,
                        videoCodec = meta.videoCodec,
                        audioCodec = meta.audioCodec,
                        resolution = meta.resolution
                    )
                }
                isLoading = false
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "Media Pool",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 4.dp)
        )
        Text(
            text = "Import and inspect clips for your timeline",
            style = MaterialTheme.typography.bodySmall,
            color = TextSecondary,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        // Primary Import Button (SAF)
        Button(
            onClick = {
                importLauncher.launch(arrayOf("video/*", "image/*", "audio/*"))
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp),
            shape = RoundedCornerShape(8.dp),
            colors = ButtonDefaults.buttonColors(containerColor = PrimaryAccent)
        ) {
            Icon(Icons.Default.Add, contentDescription = "Import media")
            Spacer(modifier = Modifier.width(8.dp))
            Text("Import Media", fontWeight = FontWeight.Bold)
        }

        // Advanced: paste-a-path fallback (collapsible)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp)
                .clickable { showAdvancedImport = !showAdvancedImport },
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Advanced: paste file path",
                fontSize = 11.sp,
                color = TextSecondary
            )
            Icon(
                imageVector = if (showAdvancedImport) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                contentDescription = "Toggle advanced import",
                tint = TextSecondary,
                modifier = Modifier.size(16.dp)
            )
        }

        AnimatedVisibility(visible = showAdvancedImport) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp, bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = inputPath,
                    onValueChange = { inputPath = it },
                    label = { Text("File path") },
                    modifier = Modifier
                        .weight(1f)
                        .padding(end = 8.dp),
                    singleLine = true,
                    shape = RoundedCornerShape(8.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = PrimaryAccent,
                        unfocusedBorderColor = BorderColor,
                        focusedContainerColor = Color(0xFF0F172A),
                        unfocusedContainerColor = Color(0xFF0F172A)
                    )
                )

                Button(
                    onClick = {
                        if (inputPath.isBlank()) return@Button
                        isLoading = true
                        scope.launch {
                            val file = File(inputPath)
                            val meta = if (file.exists()) {
                                withContext(Dispatchers.IO) {
                                    try {
                                        val session = com.arthenica.ffmpegkit.FFprobeKit.getMediaInformation(inputPath)
                                        val info = session.mediaInformation
                                        if (info != null) {
                                            val videoStream = info.streams.firstOrNull { it.type == "video" }
                                            val audioStream = info.streams.firstOrNull { it.type == "audio" }
                                            MediaMetadata(
                                                fileName = file.name,
                                                filePath = inputPath,
                                                format = info.format ?: "unknown",
                                                duration = "${"%.2f".format((info.duration?.toDoubleOrNull() ?: 0.0))}s",
                                                size = "${"%.1f".format(file.length() / (1024.0 * 1024.0))}\u00A0MB",
                                                videoCodec = videoStream?.codec ?: "none",
                                                audioCodec = audioStream?.codec ?: "none",
                                                resolution = if (videoStream != null) "${videoStream.width}x${videoStream.height}" else "N/A"
                                            )
                                        } else null
                                    } catch (e: Exception) {
                                        null
                                    }
                                }
                            } else {
                                // File not found — add placeholder
                                MediaMetadata(
                                    fileName = file.name.ifEmpty { "unknown.mp4" },
                                    filePath = inputPath,
                                    format = "MPEG-4 / QuickTime",
                                    duration = "N/A",
                                    size = "N/A",
                                    videoCodec = "h264",
                                    audioCodec = "aac",
                                    resolution = "N/A"
                                )
                            }
                            if (meta != null) {
                                viewModel.importMedia(
                                    fileName = meta.fileName,
                                    filePath = meta.filePath,
                                    format = meta.format,
                                    duration = meta.duration,
                                    size = meta.size,
                                    videoCodec = meta.videoCodec,
                                    audioCodec = meta.audioCodec,
                                    resolution = meta.resolution
                                )
                                selectedMedia = MediaAssetEntity(
                                    id = "",
                                    projectId = "",
                                    fileName = meta.fileName,
                                    filePath = meta.filePath,
                                    format = meta.format,
                                    duration = meta.duration,
                                    size = meta.size,
                                    videoCodec = meta.videoCodec,
                                    audioCodec = meta.audioCodec,
                                    resolution = meta.resolution
                                )
                            }
                            isLoading = false
                        }
                    },
                    modifier = Modifier.height(56.dp),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E293B))
                ) {
                    Text("Add")
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        if (isLoading) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                LinearProgressIndicator(
                    modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(2.dp)),
                    color = PrimaryAccent,
                    trackColor = BorderColor
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Probing file metadata\u2026",
                    fontSize = 11.sp,
                    color = SecondaryAccent
                )
            }
        }

        // Empty state / List Split
        if (mediaList.isEmpty() && !isLoading) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFF070B13))
                    .border(1.dp, BorderColor, RoundedCornerShape(12.dp))
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Canvas(modifier = Modifier.size(80.dp)) {
                        val pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)
                        drawRoundRect(
                            color = BorderColor,
                            topLeft = Offset(10.dp.toPx(), 20.dp.toPx()),
                            size = this.size / 1.3f,
                            cornerRadius = androidx.compose.ui.geometry.CornerRadius(8.dp.toPx()),
                            style = Stroke(width = 2.dp.toPx(), pathEffect = pathEffect)
                        )
                        drawRoundRect(
                            color = BorderColor,
                            topLeft = Offset(15.dp.toPx(), 10.dp.toPx()),
                            size = androidx.compose.ui.geometry.Size(30.dp.toPx(), 12.dp.toPx()),
                            cornerRadius = androidx.compose.ui.geometry.CornerRadius(4.dp.toPx()),
                            style = Stroke(width = 2.dp.toPx(), pathEffect = pathEffect)
                        )
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "No clips imported yet",
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Tap Import Media to add clips from your device.",
                        fontSize = 12.sp,
                        color = TextSecondary
                    )
                }
            }
        } else if (mediaList.isNotEmpty()) {
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = "Imported Clips (${mediaList.size})",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                LazyColumn(
                    modifier = Modifier
                        .weight(1.3f)
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(DarkSurface)
                        .border(1.dp, BorderColor, RoundedCornerShape(8.dp))
                ) {
                    items(mediaList) { item ->
                        val isSelected = selectedMedia?.filePath == item.filePath
                        ListItem(
                            leadingContent = { MediaThumbnail(item.fileName) },
                            headlineContent = { Text(item.fileName, fontWeight = FontWeight.Bold) },
                            supportingContent = {
                                Text(
                                    text = if (item.filePath.startsWith("content://")) "SAF Import" else item.filePath,
                                    color = TextSecondary,
                                    fontSize = 11.sp,
                                    maxLines = 1
                                )
                            },
                            trailingContent = {
                                Text(
                                    text = item.resolution,
                                    color = SecondaryAccent,
                                    style = TextStyle(fontFeatureSettings = "tnum")
                                )
                            },
                            colors = ListItemDefaults.colors(
                                containerColor = if (isSelected) Color(0xFF1E293B) else Color.Transparent
                            ),
                            modifier = Modifier.clickable { selectedMedia = item }
                        )
                        Divider(color = BorderColor)
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                selectedMedia?.let { media ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .border(1.dp, BorderColor, RoundedCornerShape(8.dp)),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A))
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(12.dp),
                            verticalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(Icons.Default.Info, contentDescription = "Metadata", tint = PrimaryAccent, modifier = Modifier.size(16.dp))
                                    Text(
                                        text = "Clip Metadata Inspector",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                }
                                Text(
                                    text = "Delete",
                                    color = Color(0xFFEF4444),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier
                                        .clickable {
                                            val realAsset = mediaList.find { it.filePath == media.filePath }
                                            if (realAsset != null) {
                                                viewModel.deleteMedia(realAsset.id)
                                            }
                                            selectedMedia = null
                                        }
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                            Divider(color = BorderColor)

                            Row(modifier = Modifier.fillMaxWidth()) {
                                Column(modifier = Modifier.weight(1f)) {
                                    MetaField("Format", media.format)
                                    MetaField("Duration", media.duration, isTabular = true)
                                    MetaField("File Size", media.size, isTabular = true)
                                }
                                Column(modifier = Modifier.weight(1f)) {
                                    MetaField("Resolution", media.resolution, isTabular = true)
                                    MetaField("Video Stream", media.videoCodec)
                                    MetaField("Audio Stream", media.audioCodec)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun MetaField(label: String, value: String, isTabular: Boolean = false) {
    Column(modifier = Modifier.padding(vertical = 4.dp)) {
        Text(text = label, fontSize = 10.sp, color = TextSecondary)
        Text(
            text = value,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            color = Color.White,
            style = if (isTabular) TextStyle(fontFeatureSettings = "tnum") else TextStyle.Default
        )
    }
}

@Composable
fun MediaThumbnail(fileName: String, modifier: Modifier = Modifier) {
    val isAudio = fileName.endsWith(".wav", ignoreCase = true) ||
            fileName.endsWith(".mp3", ignoreCase = true) ||
            fileName.endsWith(".aac", ignoreCase = true) ||
            fileName.endsWith(".flac", ignoreCase = true)
    Canvas(
        modifier = modifier
            .size(width = 56.dp, height = 38.dp)
            .clip(RoundedCornerShape(4.dp))
            .background(Color(0xFF030712))
            .border(1.dp, BorderColor, RoundedCornerShape(4.dp))
    ) {
        val w = size.width
        val h = size.height

        if (isAudio) {
            val bars = 6
            val gap = 2.dp.toPx()
            val barW = (w - (bars - 1) * gap) / bars
            for (i in 0 until bars) {
                val valSin = sin(i * 1.2f)
                val barH = h * (0.2f + 0.6f * (if (valSin < 0) -valSin else valSin))
                val x = i * (barW + gap)
                val y = (h - barH) / 2f
                drawRect(
                    color = SecondaryAccent,
                    topLeft = Offset(x, y),
                    size = androidx.compose.ui.geometry.Size(barW, barH)
                )
            }
        } else {
            drawRect(
                brush = Brush.linearGradient(
                    colors = listOf(PrimaryAccent.copy(alpha = 0.6f), SecondaryAccent.copy(alpha = 0.6f)),
                    start = Offset(0f, 0f),
                    end = Offset(w, h)
                )
            )

            val sprockets = 5
            val spW = 3.dp.toPx()
            val spH = 2.dp.toPx()
            val spGap = (w - sprockets * spW) / (sprockets + 1)
            for (i in 0 until sprockets) {
                val x = spGap + i * (spW + spGap)
                drawRect(Color.Black.copy(alpha = 0.7f), Offset(x, 2.dp.toPx()), androidx.compose.ui.geometry.Size(spW, spH))
                drawRect(Color.Black.copy(alpha = 0.7f), Offset(x, h - spH - 2.dp.toPx()), androidx.compose.ui.geometry.Size(spW, spH))
            }

            val path = Path().apply {
                val cx = w / 2f
                val cy = h / 2f
                val size = 6.dp.toPx()
                moveTo(cx - size / 2f, cy - size)
                lineTo(cx + size, cy)
                lineTo(cx - size / 2f, cy + size)
                close()
            }
            drawPath(path, Color.White)
        }
    }
}
