package com.example.coling.ui.screens

import android.widget.Toast
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
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
import androidx.compose.foundation.BorderStroke

data class ExportPreset(
    val id: String,
    val name: String,
    val resolution: String,
    val codec: String,
    val ratio: String
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeliverScreen() {
    val context = LocalContext.current

    val presets = listOf(
        ExportPreset("youtube", "YouTube / Vimeo", "1080p (FHD)", "H.264 (AVC)", "16:9 Landscape"),
        ExportPreset("tiktok", "TikTok / Social", "1080p (FHD)", "H.264 (AVC)", "9:16 Portrait"),
        ExportPreset("prores", "Master Archive", "4K (UHD)", "H.265 (HEVC)", "16:9 Landscape"),
        ExportPreset("custom", "Custom Render", "720p", "H.264 (AVC)", "1:1 Square")
    )

    var activePresetId by remember { mutableStateOf("youtube") }
    val activePreset = presets.first { it.id == activePresetId }

    var selectedResolution by remember { mutableStateOf(activePreset.resolution) }
    var selectedCodec by remember { mutableStateOf(activePreset.codec) }
    var selectedRatio by remember { mutableStateOf(activePreset.ratio) }
    var bitrateMbps by remember { mutableStateOf(16f) }

    var isExporting by remember { mutableStateOf(false) }
    var exportProgress by remember { mutableStateOf(0f) }
    var exportTaskJob by remember { mutableStateOf<Thread?>(null) }
    var currentPhaseText by remember { mutableStateOf("Ready to render") }

    // Sync state when preset changes
    LaunchedEffect(activePresetId) {
        val p = presets.first { it.id == activePresetId }
        selectedResolution = p.resolution
        selectedCodec = p.codec
        selectedRatio = p.ratio
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = "Export & Deliver",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )

        // 1. Presets Selection Row
        Text(
            text = "Delivery Presets",
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = TextSecondary
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            presets.forEach { preset ->
                val isSelected = preset.id == activePresetId
                Card(
                    modifier = Modifier
                        .weight(1f)
                        .height(70.dp)
                        .clickable { activePresetId = preset.id },
                    colors = CardDefaults.cardColors(
                        containerColor = if (isSelected) Color(0xFF1E293B) else DarkSurface
                    ),
                    border = BorderStroke(1.dp, if (isSelected) PrimaryAccent else BorderColor)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(6.dp),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = preset.name,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            maxLines = 1
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = preset.codec.split(" ")[0] + " | " + preset.resolution.split(" ")[0],
                            fontSize = 9.sp,
                            color = TextSecondary
                        )
                    }
                }
            }
        }

        // 2. Output Configurations
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = DarkSurface),
            border = BorderStroke(1.dp, BorderColor)
        ) {
            Column(
                modifier = Modifier.padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text("Export Profile Settings", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = PrimaryAccent)

                // Aspect ratio picker with graphical preview
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1.3f)) {
                        Text("Aspect Ratio", fontSize = 11.sp, color = TextSecondary)
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            val ratios = listOf("16:9 Landscape", "9:16 Portrait", "1:1 Square")
                            ratios.forEach { rt ->
                                FilterChip(
                                    selected = selectedRatio == rt,
                                    onClick = { selectedRatio = rt; activePresetId = "custom" },
                                    label = { Text(rt.split(" ")[0], fontSize = 10.sp) }
                                )
                            }
                        }
                    }

                    // Mini preview monitor representing chosen ratio shape
                    Canvas(
                        modifier = Modifier
                            .size(50.dp)
                            .background(Color.Black, RoundedCornerShape(4.dp))
                            .border(1.dp, BorderColor, RoundedCornerShape(4.dp))
                    ) {
                        val strokeW = 1.dp.toPx()
                        val w = size.width
                        val h = size.height
                        when {
                            selectedRatio.contains("16:9") -> {
                                drawRect(Color.White.copy(alpha = 0.3f), Offset(w * 0.1f, h * 0.25f), androidx.compose.ui.geometry.Size(w * 0.8f, h * 0.5f), style = Stroke(strokeW))
                            }
                            selectedRatio.contains("9:16") -> {
                                drawRect(Color.White.copy(alpha = 0.3f), Offset(w * 0.3f, h * 0.1f), androidx.compose.ui.geometry.Size(w * 0.4f, h * 0.8f), style = Stroke(strokeW))
                            }
                            else -> {
                                drawRect(Color.White.copy(alpha = 0.3f), Offset(w * 0.2f, h * 0.2f), androidx.compose.ui.geometry.Size(w * 0.6f, h * 0.6f), style = Stroke(strokeW))
                            }
                        }
                    }
                }

                Divider(color = BorderColor)

                // Resolution
                Column {
                    Text("Target Resolution", fontSize = 11.sp, color = TextSecondary)
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf("720p", "1080p (FHD)", "4K (UHD)").forEach { res ->
                            FilterChip(
                                selected = selectedResolution == res,
                                onClick = { selectedResolution = res; activePresetId = "custom" },
                                label = { Text(res, fontSize = 10.sp) }
                            )
                        }
                    }
                }

                // Bitrate
                Column {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Target Bitrate", fontSize = 11.sp, color = TextSecondary)
                        Text(
                            text = "${bitrateMbps.toInt()}\u00A0Mbps",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = SecondaryAccent,
                            style = TextStyle(fontFeatureSettings = "tnum")
                        )
                    }
                    Slider(
                        value = bitrateMbps,
                        onValueChange = { bitrateMbps = it },
                        valueRange = 2f..50f,
                        colors = SliderDefaults.colors(
                            thumbColor = PrimaryAccent,
                            activeTrackColor = PrimaryAccent
                        )
                    )
                }
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        // 3. Render Status & Controls
        if (isExporting) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, BorderColor, RoundedCornerShape(8.dp)),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A))
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = currentPhaseText,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = "${(exportProgress * 100).toInt()}%",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = PrimaryAccent,
                            style = TextStyle(fontFeatureSettings = "tnum")
                        )
                    }

                    LinearProgressIndicator(
                        progress = exportProgress,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp)),
                        color = PrimaryAccent,
                        trackColor = Color.Black
                    )

                    // Render speed & frame counting metrics
                    val framesDone = (exportProgress * 600).toInt()
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Frame:\u00A0$framesDone\u00A0/\u00A0600",
                            fontSize = 10.sp,
                            color = TextSecondary,
                            style = TextStyle(fontFeatureSettings = "tnum")
                        )
                        Text(
                            text = "Speed:\u00A024.5\u00A0fps",
                            fontSize = 10.sp,
                            color = TextSecondary,
                            style = TextStyle(fontFeatureSettings = "tnum")
                        )
                        Text(
                            text = "Est:\u00A0${((600 - framesDone) / 24)}s",
                            fontSize = 10.sp,
                            color = TextSecondary,
                            style = TextStyle(fontFeatureSettings = "tnum")
                        )
                    }

                    Button(
                        onClick = {
                            exportTaskJob?.interrupt()
                            isExporting = false
                            exportProgress = 0f
                            currentPhaseText = "Export cancelled"
                            Toast.makeText(context, "Export cancelled!", Toast.LENGTH_SHORT).show()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(38.dp),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text("Cancel Render", fontSize = 11.sp)
                    }
                }
            }
        } else {
            Button(
                onClick = {
                    isExporting = true
                    exportProgress = 0f
                    val thread = Thread {
                        try {
                            currentPhaseText = "Initializing rendering buffers…"
                            Thread.sleep(600)
                            
                            currentPhaseText = "Evaluating C++ grading nodes…"
                            for (p in 1..85) {
                                Thread.sleep(30)
                                exportProgress = p / 100f
                            }

                            currentPhaseText = "Muxing video containers…"
                            for (p in 86..100) {
                                Thread.sleep(40)
                                exportProgress = p / 100f
                            }
                            
                            isExporting = false
                            currentPhaseText = "Ready to render"
                            (context as? android.app.Activity)?.runOnUiThread {
                                Toast.makeText(context, "Render complete! Saved 14.5\u00A0MB to Movies folder", Toast.LENGTH_LONG).show()
                            }
                        } catch (e: InterruptedException) {
                            // Interrupted / Cancelled
                        }
                    }
                    exportTaskJob = thread
                    thread.start()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryAccent)
            ) {
                Text("Start Render Job", fontSize = 14.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

