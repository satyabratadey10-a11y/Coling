package com.example.coling.ui.screens

import android.widget.Toast
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
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
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
import java.io.File
import androidx.compose.ui.geometry.Offset
import androidx.compose.foundation.BorderStroke

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
fun MediaScreen() {
    val context = LocalContext.current
    var inputPath by remember { mutableStateOf("/sdcard/Download/sample.mp4") }
    var mediaList by remember { mutableStateOf(listOf<MediaMetadata>()) }
    var selectedMedia by remember { mutableStateOf<MediaMetadata?>(null) }
    var isLoading by remember { mutableStateOf(false) }

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

        // Import Section
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = inputPath,
                onValueChange = { inputPath = it },
                label = { Text("Video file path") },
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
                    val file = File(inputPath)
                    if (!file.exists()) {
                        Toast.makeText(context, "File path not found! Adding mock clip.", Toast.LENGTH_SHORT).show()
                        val mockMeta = MediaMetadata(
                            fileName = file.name.ifEmpty { "camera_grade_test.mp4" },
                            filePath = inputPath,
                            format = "MPEG-4 / QuickTime",
                            duration = "24.50s",
                            size = "45.2 MB",
                            videoCodec = "h264 (High 10-bit)",
                            audioCodec = "aac (LC)",
                            resolution = "1920x1080"
                        )
                        mediaList = mediaList + mockMeta
                        selectedMedia = mockMeta
                    } else {
                        isLoading = true
                        Thread {
                            try {
                                val session = com.arthenica.ffmpegkit.FFprobeKit.getMediaInformation(inputPath)
                                val info = session.mediaInformation
                                if (info != null) {
                                    val videoStream = info.streams.firstOrNull { it.type == "video" }
                                    val audioStream = info.streams.firstOrNull { it.type == "audio" }
                                    val meta = MediaMetadata(
                                        fileName = file.name,
                                        filePath = inputPath,
                                        format = info.format ?: "unknown",
                                        duration = "${"%.2f".format((info.duration?.toDoubleOrNull() ?: 0.0))}s",
                                        size = "${"%.1f".format(file.length() / (1024.0 * 1024.0))}\u00A0MB",
                                        videoCodec = videoStream?.codec ?: "none",
                                        audioCodec = audioStream?.codec ?: "none",
                                        resolution = if (videoStream != null) "${videoStream.width}x${videoStream.height}" else "N/A"
                                    )
                                    mediaList = mediaList + meta
                                    selectedMedia = meta
                                }
                            } catch (e: Exception) {
                                e.printStackTrace()
                            } finally {
                                isLoading = false
                            }
                        }.start()
                    }
                },
                modifier = Modifier.height(56.dp),
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryAccent)
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add clip")
                Spacer(modifier = Modifier.width(4.dp))
                Text("Add Clip")
            }
        }

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
                    text = "Probing file metadata…",
                    fontSize = 11.sp,
                    color = SecondaryAccent
                )
            }
        }

        // Empty state / List Split
        if (mediaList.isEmpty()) {
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
                        // Draw empty media folder shape
                        val pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)
                        drawRoundRect(
                            color = BorderColor,
                            topLeft = Offset(10.dp.toPx(), 20.dp.toPx()),
                            size = this.size / 1.3f,
                            cornerRadius = androidx.compose.ui.geometry.CornerRadius(8.dp.toPx()),
                            style = Stroke(width = 2.dp.toPx(), pathEffect = pathEffect)
                        )
                        // Folder tab
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
                        text = "Enter a file path above and tap Add Clip to start.",
                        fontSize = 12.sp,
                        color = TextSecondary
                    )
                }
            }
        } else {
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
                        val isSelected = selectedMedia == item
                        ListItem(
                            headlineContent = { Text(item.fileName, fontWeight = FontWeight.Bold) },
                            supportingContent = { Text(item.filePath, color = TextSecondary, fontSize = 11.sp) },
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

