package com.example.coling.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.coling.ui.theme.BorderColor
import com.example.coling.ui.theme.DarkSurface
import com.example.coling.ui.theme.PrimaryAccent
import com.example.coling.ui.theme.SecondaryAccent
import com.example.coling.ui.theme.TextSecondary
import androidx.compose.runtime.collectAsState
import com.example.coling.data.ProjectViewModel
import com.example.coling.data.TimelineClipEntity
import androidx.compose.ui.graphics.toArgb
import android.graphics.Bitmap
import android.net.Uri
import com.example.coling.utils.extractThumbnail
import com.example.coling.NativeBridge
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.foundation.Image
import androidx.compose.ui.platform.LocalContext

data class TimelineClip(
    val id: String,
    val name: String,
    val type: ClipType,
    var startFrame: Int,
    var durationFrames: Int,
    val color: Color
)

enum class ClipType {
    VIDEO, AUDIO, TITLE
}


@Composable
fun EditScreen(viewModel: ProjectViewModel) {
    var isPlaying by remember { mutableStateOf(false) }
    var currentFrame by remember { mutableStateOf(120) }
    val totalFrames = 600 // 20 seconds at 30fps

    val clipsEntity by viewModel.timelineClips.collectAsState()
    
    // Map database entity to UI model helper
    fun TimelineClipEntity.toUIModel(): TimelineClip {
        val uiType = when (type) {
            "VIDEO" -> ClipType.VIDEO
            "AUDIO" -> ClipType.AUDIO
            else -> ClipType.TITLE
        }
        val uiColor = try {
            Color(android.graphics.Color.parseColor(colorHex))
        } catch (_: Exception) {
            PrimaryAccent
        }
        return TimelineClip(
            id = id,
            name = name,
            type = uiType,
            startFrame = startFrame,
            durationFrames = durationFrames,
            color = uiColor
        )
    }

    val clips = remember(clipsEntity) { clipsEntity.map { it.toUIModel() } }

    var selectedClipId by remember { mutableStateOf<String?>("v2") }

    val context = LocalContext.current
    val mediaList by viewModel.mediaAssets.collectAsState()
    val colorNodes by viewModel.colorNodes.collectAsState()
    val primariesNode = remember(colorNodes) { colorNodes.find { it.id == "node1" } }

    val activeVideoClip = remember(clips, currentFrame) {
        clips.firstOrNull {
            it.type == ClipType.VIDEO &&
            currentFrame >= it.startFrame &&
            currentFrame < (it.startFrame + it.durationFrames)
        }
    }

    var previewBitmap by remember { mutableStateOf<Bitmap?>(null) }

    LaunchedEffect(currentFrame, activeVideoClip, mediaList, primariesNode) {
        withContext(Dispatchers.IO) {
            val baseBitmap = if (activeVideoClip != null) {
                val matchingAsset = mediaList.find { it.fileName == activeVideoClip.name }
                if (matchingAsset != null) {
                    val clipLocalFrame = currentFrame - activeVideoClip.startFrame
                    val timeUs = (clipLocalFrame.toFloat() / 30f * 1_000_000f).toLong()
                    extractThumbnail(context, Uri.parse(matchingAsset.filePath), timeUs)
                        ?: createProceduralSunset(400, 225)
                } else {
                    createProceduralSunset(400, 225)
                }
            } else {
                createProceduralSunset(400, 225)
            }

            val mutableBitmap = baseBitmap.copy(Bitmap.Config.ARGB_8888, true)

            if (primariesNode != null) {
                val liftR = primariesNode.liftX * 0.2f
                val liftG = primariesNode.liftY * 0.2f
                val liftB = - (primariesNode.liftX + primariesNode.liftY) * 0.1f

                val gammaR = 1.0f + primariesNode.gammaX * 0.5f
                val gammaG = 1.0f + primariesNode.gammaY * 0.5f
                val gammaB = 1.0f - (primariesNode.gammaX + primariesNode.gammaY) * 0.25f

                val gainR = 1.0f + primariesNode.gainX * 0.5f
                val gainG = 1.0f + primariesNode.gainY * 0.5f
                val gainB = 1.0f - (primariesNode.gainX + primariesNode.gainY) * 0.25f

                NativeBridge.processBitmap(
                    mutableBitmap,
                    liftR = liftR, liftG = liftG, liftB = liftB,
                    gammaR = gammaR, gammaG = gammaG, gammaB = gammaB,
                    gainR = gainR, gainG = gainG, gainB = gainB,
                    contrast = 1.0f, saturation = 1.0f
                )
            }

            withContext(Dispatchers.Main) {
                previewBitmap = mutableBitmap
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(12.dp)
    ) {
        // 1. Preview Monitor
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(210.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(Color.Black)
                .border(1.dp, BorderColor, RoundedCornerShape(8.dp)),
            contentAlignment = Alignment.Center
        ) {
            // Real preview placeholder — will show actual frames when native pipeline is wired
            if (previewBitmap != null) {
                Image(
                    bitmap = previewBitmap!!.asImageBitmap(),
                    contentDescription = "Video Preview",
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Box(
                    modifier = Modifier.fillMaxSize().background(Color.Black),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Loading Frame...",
                        color = TextSecondary,
                        fontSize = 12.sp
                    )
                }
            }

            // Safe zone grid overlay (real feature, keep it)
            Canvas(modifier = Modifier.fillMaxSize()) {
                val w = size.width
                val h = size.height
                drawRect(
                    color = Color.White.copy(alpha = 0.15f),
                    topLeft = Offset(w * 0.05f, h * 0.05f),
                    size = androidx.compose.ui.geometry.Size(w * 0.9f, h * 0.9f),
                    style = Stroke(width = 1.dp.toPx())
                )
                drawRect(
                    color = Color.White.copy(alpha = 0.08f),
                    topLeft = Offset(w * 0.1f, h * 0.1f),
                    size = androidx.compose.ui.geometry.Size(w * 0.8f, h * 0.8f),
                    style = Stroke(width = 1.dp.toPx())
                )
            }

            // Title Overlay if current frame overlaps Title Card
            val activeTitle = clips.firstOrNull {
                it.type == ClipType.TITLE &&
                currentFrame >= it.startFrame &&
                currentFrame <= (it.startFrame + it.durationFrames)
            }
            if (activeTitle != null) {
                Text(
                    text = activeTitle.name,
                    color = Color.White,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 2.sp,
                    modifier = Modifier.align(Alignment.Center)
                )
            }

            // Bottom playhead metrics
            Text(
                text = "FRAME\u00A0${String.format("%04d", currentFrame)}\u00A0/\u00A0${String.format("%04d", totalFrames)}",
                color = Color.White,
                fontSize = 11.sp,
                style = TextStyle(fontFeatureSettings = "tnum"),
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 6.dp)
                    .background(Color.Black.copy(alpha = 0.7f), RoundedCornerShape(4.dp))
                    .padding(horizontal = 8.dp, vertical = 2.dp)
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        // 2. Playback Transport Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(DarkSurface, RoundedCornerShape(8.dp))
                .border(1.dp, BorderColor, RoundedCornerShape(8.dp))
                .padding(6.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = { currentFrame = 0 },
                colors = IconButtonDefaults.iconButtonColors(contentColor = Color.White)
            ) {
                Icon(Icons.Default.Refresh, contentDescription = "Rewind playhead to start")
            }
            IconButton(
                onClick = { if (currentFrame > 0) currentFrame-- },
                colors = IconButtonDefaults.iconButtonColors(contentColor = Color.White)
            ) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Step backward one frame")
            }

            Spacer(modifier = Modifier.width(8.dp))
            FilledIconButton(
                onClick = { isPlaying = !isPlaying },
                colors = IconButtonDefaults.filledIconButtonColors(
                    containerColor = if (isPlaying) Color(0xFFEF4444) else PrimaryAccent
                ),
                modifier = Modifier.size(44.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = if (isPlaying) "Pause video playback" else "Start video playback",
                    tint = Color.White
                )
            }
            Spacer(modifier = Modifier.width(8.dp))

            IconButton(
                onClick = { if (currentFrame < totalFrames) currentFrame++ },
                colors = IconButtonDefaults.iconButtonColors(contentColor = Color.White)
            ) {
                Icon(Icons.Default.ArrowForward, contentDescription = "Step forward one frame")
            }
            IconButton(
                onClick = { currentFrame = totalFrames },
                colors = IconButtonDefaults.iconButtonColors(contentColor = Color.White)
            ) {
                Icon(Icons.Default.ArrowForward, contentDescription = "Jump playhead to end", modifier = Modifier.padding(end = 4.dp))
            }
        }

        // Scrubbing timeline slider
        Slider(
            value = currentFrame.toFloat(),
            onValueChange = { currentFrame = it.toInt() },
            valueRange = 0f..totalFrames.toFloat(),
            modifier = Modifier.fillMaxWidth(),
            colors = SliderDefaults.colors(
                thumbColor = PrimaryAccent,
                activeTrackColor = PrimaryAccent,
                inactiveTrackColor = BorderColor
            )
        )

        // 3. Edit Action Bar
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Button(
                onClick = {
                    val selected = clips.find { it.id == selectedClipId }
                    if (selected != null) {
                        val playheadOffset = currentFrame - selected.startFrame
                        if (playheadOffset > 10 && playheadOffset < selected.durationFrames - 10) {
                            val newDuration = selected.durationFrames - playheadOffset
                            viewModel.updateClipPosition(selected.id, selected.startFrame, playheadOffset)
                            val colorHex = "#" + Integer.toHexString(selected.color.toArgb()).substring(2)
                            viewModel.addClipToTimeline(
                                name = selected.name.replace(".mp4", "") + " (Part 2)",
                                type = selected.type.name,
                                durationFrames = newDuration,
                                colorHex = colorHex
                            )
                        }
                    }
                },
                enabled = selectedClipId != null,
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF1E293B),
                    disabledContainerColor = Color(0xFF0F172A)
                ),
                shape = RoundedCornerShape(6.dp),
                contentPadding = PaddingValues(horizontal = 4.dp)
            ) {
                Text("Split", fontSize = 11.sp, color = if (selectedClipId != null) Color.White else TextSecondary)
            }
 
            Button(
                onClick = {
                    val selected = clips.find { it.id == selectedClipId }
                    if (selected != null && currentFrame > selected.startFrame && currentFrame < selected.startFrame + selected.durationFrames) {
                        val diff = currentFrame - selected.startFrame
                        viewModel.updateClipPosition(selected.id, currentFrame, selected.durationFrames - diff)
                    }
                },
                enabled = selectedClipId != null,
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF1E293B),
                    disabledContainerColor = Color(0xFF0F172A)
                ),
                shape = RoundedCornerShape(6.dp),
                contentPadding = PaddingValues(horizontal = 4.dp)
            ) {
                Text("Trim In", fontSize = 11.sp, color = if (selectedClipId != null) Color.White else TextSecondary)
            }
 
            Button(
                onClick = {
                    val selected = clips.find { it.id == selectedClipId }
                    if (selected != null && currentFrame > selected.startFrame && currentFrame < selected.startFrame + selected.durationFrames) {
                        viewModel.updateClipPosition(selected.id, selected.startFrame, currentFrame - selected.startFrame)
                    }
                },
                enabled = selectedClipId != null,
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF1E293B),
                    disabledContainerColor = Color(0xFF0F172A)
                ),
                shape = RoundedCornerShape(6.dp),
                contentPadding = PaddingValues(horizontal = 4.dp)
            ) {
                Text("Trim Out", fontSize = 11.sp, color = if (selectedClipId != null) Color.White else TextSecondary)
            }

            Button(
                onClick = {
                    selectedClipId?.let { clipId ->
                        viewModel.deleteClipFromTimeline(clipId)
                        selectedClipId = null
                    }
                },
                enabled = selectedClipId != null,
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFEF4444).copy(alpha = 0.8f),
                    disabledContainerColor = Color(0xFF0F172A)
                ),
                shape = RoundedCornerShape(6.dp),
                contentPadding = PaddingValues(horizontal = 4.dp)
            ) {
                Text("Delete", fontSize = 11.sp, color = if (selectedClipId != null) Color.White else TextSecondary)
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // 4. Multi-Track Timeline Visualizer with Timeline Ruler
        Text(
            text = "Timeline Tracks",
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )
        Spacer(modifier = Modifier.height(4.dp))

        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .background(Color(0xFF070B13))
                .border(1.dp, BorderColor, RoundedCornerShape(8.dp))
        ) {
            // Timeline Ruler
            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(24.dp)
                    .background(Color(0xFF0F172A))
            ) {
                val tickGap = size.width / 5

                for (i in 0..5) {
                    val x = i * tickGap
                    drawLine(Color(0xFF475569), Offset(x, size.height), Offset(x, size.height - 12.dp.toPx()), 1.dp.toPx())
                }
                // Playhead line
                val playheadX = (currentFrame.toFloat() / totalFrames) * size.width
                drawLine(Color.Red, Offset(playheadX, 0f), Offset(playheadX, size.height), 2.dp.toPx())
            }

            Divider(color = BorderColor)

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val tracks = listOf(
                    Pair("VIDEO (V1)", ClipType.VIDEO),
                    Pair("AUDIO (A1)", ClipType.AUDIO),
                    Pair("TITLES (T1)", ClipType.TITLE)
                )

                items(tracks) { (trackLabel, type) ->
                    val trackClips = clips.filter { it.type == type }
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFF0F172A), RoundedCornerShape(6.dp))
                            .border(1.dp, BorderColor, RoundedCornerShape(6.dp))
                            .padding(6.dp)
                    ) {
                        Text(
                            text = trackLabel,
                            fontSize = 9.sp,
                            color = TextSecondary,
                            fontWeight = FontWeight.Black
                        )
                        Spacer(modifier = Modifier.height(4.dp))

                        // Use BoxWithConstraints for correct percentage-based positioning
                        BoxWithConstraints(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(44.dp)
                                .background(Color.Black.copy(alpha = 0.4f))
                        ) {
                            val trackWidthPx = constraints.maxWidth
                            val density = LocalDensity.current

                            trackClips.forEach { clip ->
                                val leftFraction = clip.startFrame.toFloat() / totalFrames
                                val widthFraction = clip.durationFrames.toFloat() / totalFrames
                                val isSelected = selectedClipId == clip.id

                                val clipWidthDp = with(density) { (widthFraction * trackWidthPx).toDp() }
                                val clipOffsetX = (leftFraction * trackWidthPx).toInt()

                                Box(
                                    modifier = Modifier
                                        .offset { IntOffset(clipOffsetX, 0) }
                                        .width(clipWidthDp)
                                        .fillMaxHeight()
                                        .padding(vertical = 2.dp)
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(
                                            if (isSelected) clip.color.copy(alpha = 0.5f) else clip.color
                                        )
                                        .border(
                                            width = if (isSelected) 1.5.dp else 1.dp,
                                            color = if (isSelected) Color.White else clip.color.copy(alpha = 0.7f),
                                            shape = RoundedCornerShape(4.dp)
                                        )
                                        .clickable { selectedClipId = clip.id }
                                        .padding(horizontal = 6.dp),
                                    contentAlignment = Alignment.CenterStart
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxSize(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = clip.name,
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White,
                                            maxLines = 1
                                        )
                                        if (isSelected) {
                                            Box(
                                                modifier = Modifier
                                                    .width(4.dp)
                                                    .fillMaxHeight(0.7f)
                                                    .background(Color.White, RoundedCornerShape(1.dp))
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

fun createProceduralSunset(width: Int, height: Int): Bitmap {
    val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    val canvas = android.graphics.Canvas(bitmap)
    val paint = android.graphics.Paint()

    // sunset linear gradient background
    val shader = android.graphics.LinearGradient(
        0f, 0f, 0f, height.toFloat(),
        android.graphics.Color.parseColor("#1F1C2C"), // Dark Indigo
        android.graphics.Color.parseColor("#928DAB"), // Soft Lavender
        android.graphics.Shader.TileMode.CLAMP
    )
    paint.shader = shader
    canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), paint)

    // Draw a big warm sun in the middle
    paint.shader = null
    paint.color = android.graphics.Color.parseColor("#FF4E50")
    canvas.drawCircle(width / 2f, height * 0.6f, height * 0.3f, paint)

    // Draw horizontal scanlines/grid lines for the retro aesthetic
    paint.color = android.graphics.Color.argb(51, 0, 0, 0)
    paint.strokeWidth = 2f
    for (y in (height * 0.6f).toInt()..height step 12) {
        canvas.drawLine(0f, y.toFloat(), width.toFloat(), y.toFloat(), paint)
    }

    return bitmap
}
