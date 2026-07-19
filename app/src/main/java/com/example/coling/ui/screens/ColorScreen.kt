package com.example.coling.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.coling.ui.theme.BorderColor
import com.example.coling.ui.theme.DarkSurface
import com.example.coling.ui.theme.PrimaryAccent
import com.example.coling.ui.theme.SecondaryAccent
import com.example.coling.ui.theme.TextSecondary
import kotlin.math.cos
import kotlin.math.sin
import androidx.compose.foundation.BorderStroke
import androidx.compose.material3.RangeSlider
import androidx.compose.runtime.collectAsState
import com.example.coling.data.ProjectViewModel
import com.example.coling.data.ColorNodeEntity
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.ui.graphics.graphicsLayer

data class ColorNode(
    val id: String,
    val name: String,
    var type: NodeType,
    var isEnabled: Boolean = true
)

enum class NodeType {
    SERIAL, PARALLEL
}


@Composable
fun ColorScreen(viewModel: ProjectViewModel) {
    var liftVal by remember { mutableStateOf(Offset(0f, 0f)) }
    var gammaVal by remember { mutableStateOf(Offset(0f, 0f)) }
    var gainVal by remember { mutableStateOf(Offset(0f, 0f)) }

    // Secondary controls
    var tempGrade by remember { mutableStateOf(0f) } // -50 to 50
    var tintGrade by remember { mutableStateOf(0f) }
    var contrastGrade by remember { mutableStateOf(1.0f) } // 0.5 to 2.0
    var saturationGrade by remember { mutableStateOf(1.0f) }

    val colorNodesEntity by viewModel.colorNodes.collectAsState()
    
    val nodes = remember(colorNodesEntity) {
        colorNodesEntity.map { entity ->
            ColorNode(
                id = entity.id,
                name = entity.name,
                type = when (entity.type) {
                    "SERIAL" -> NodeType.SERIAL
                    else -> NodeType.PARALLEL
                },
                isEnabled = entity.isEnabled
            )
        }
    }

    var selectedNodeId by remember { mutableStateOf("") }
    
    LaunchedEffect(nodes) {
        if (selectedNodeId.isEmpty() && nodes.isNotEmpty()) {
            selectedNodeId = nodes.first().id
        }
    }

    val selectedNode = remember(colorNodesEntity, selectedNodeId) {
        colorNodesEntity.find { it.id == selectedNodeId }
    }

    LaunchedEffect(selectedNode) {
        selectedNode?.let {
            liftVal = Offset(it.liftX, it.liftY)
            gammaVal = Offset(it.gammaX, it.gammaY)
            gainVal = Offset(it.gainX, it.gainY)
        }
    }

    var scale by remember { mutableStateOf(1.0f) }
    var panOffset by remember { mutableStateOf(Offset.Zero) }
    
    // UI selections
    var activeTab by remember { mutableStateOf("Wheels") } 
    var activeCurveChannel by remember { mutableStateOf("Master") } // Master, Red, Green, Blue
    var activeScopeType by remember { mutableStateOf("Waveform") } // Waveform, Vectorscope, Histogram

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(8.dp)
    ) {
        // 1. Node Graph Panel
        Text(
            text = "Node Graph Editor",
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = TextSecondary,
            modifier = Modifier.padding(bottom = 4.dp, start = 4.dp)
        )

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(135.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(Color(0xFF070B13))
                .border(1.dp, BorderColor, RoundedCornerShape(8.dp))
                .pointerInput(Unit) {
                    detectTransformGestures { _, pan, zoom, _ ->
                        scale = (scale * zoom).coerceIn(0.5f, 2.0f)
                        panOffset += pan
                    }
                }
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer(
                        scaleX = scale,
                        scaleY = scale,
                        translationX = panOffset.x,
                        translationY = panOffset.y
                    )
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val nodeW = 120.dp.toPx()
                    val nodeH = 95.dp.toPx()
                    val y = size.height / 2

                    for (i in 0 until nodes.size - 1) {
                        val x1 = 16.dp.toPx() + (i * (nodeW + 30.dp.toPx())) + nodeW
                        val x2 = 16.dp.toPx() + ((i + 1) * (nodeW + 30.dp.toPx()))

                        val flowPath = Path().apply {
                            moveTo(x1, y)
                            cubicTo(x1 + 15.dp.toPx(), y, x2 - 15.dp.toPx(), y, x2, y)
                        }
                        drawPath(flowPath, SecondaryAccent.copy(alpha = 0.5f), style = Stroke(width = 2.dp.toPx()))
                        drawCircle(Color.Green, 3.dp.toPx(), Offset(x1, y))
                        drawCircle(Color.Green, 3.dp.toPx(), Offset(x2, y))
                    }
                }

                nodes.forEachIndexed { i, node ->
                    val isSelected = node.id == selectedNodeId
                    val nodeLeft = 16.dp + i * 150.dp // 120.dp width + 30.dp gap

                    Box(
                        modifier = Modifier
                            .offset(x = nodeLeft, y = 10.dp)
                    ) {
                        Card(
                            modifier = Modifier
                                .width(120.dp)
                                .height(95.dp)
                                .clickable { selectedNodeId = node.id },
                            colors = CardDefaults.cardColors(
                                containerColor = if (isSelected) Color(0xFF1E293B) else DarkSurface
                            ),
                            border = BorderStroke(1.dp, if (isSelected) Color.White else BorderColor)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(6.dp),
                                verticalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "0${node.id.takeLast(1)}",
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isSelected) Color.White else TextSecondary
                                    )
                                    Box(
                                        modifier = Modifier
                                            .size(6.dp)
                                            .background(if (node.isEnabled) Color.Green else Color.Red, CircleShape)
                                    )
                                }

                                Canvas(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(35.dp)
                                        .clip(RoundedCornerShape(2.dp))
                                ) {
                                    val nodeGrad = Brush.horizontalGradient(
                                        colors = if (node.id == "node1") {
                                            listOf(Color.DarkGray, Color.Gray)
                                        } else {
                                            listOf(Color(0xFF4338CA), Color(0xFFB91C1C), Color(0xFFF59E0B))
                                        }
                                    )
                                    drawRect(nodeGrad)
                                }

                                Text(
                                    text = node.name,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color.White,
                                    maxLines = 1
                                )
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // 2. Control Tabs
        TabRow(
            selectedTabIndex = when (activeTab) {
                "Wheels" -> 0
                "Curves" -> 1
                "Qualifier" -> 2
                "Scopes" -> 3
                else -> 0
            },
            containerColor = DarkSurface,
            contentColor = Color.White,
            modifier = Modifier.clip(RoundedCornerShape(6.dp))
        ) {
            val tabs = listOf("Wheels", "Curves", "Qualifier", "Scopes")
            tabs.forEach { tab ->
                Tab(
                    selected = activeTab == tab,
                    onClick = { activeTab = tab },
                    text = { Text(tab, fontSize = 11.sp, fontWeight = FontWeight.Bold) }
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // 3. Dynamic grading board
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .background(DarkSurface)
                .border(1.dp, BorderColor, RoundedCornerShape(8.dp))
                .padding(10.dp)
        ) {
            when (activeTab) {
                "Wheels" -> {
                    Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.SpaceBetween) {
                        // Lift/Gamma/Gain Row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            ColorWheelSection("Lift", liftVal) { 
                                liftVal = it 
                                selectedNode?.let { node ->
                                    viewModel.updateColorNodeOffset(node.id, it.x, it.y, gammaVal.x, gammaVal.y, gainVal.x, gainVal.y)
                                }
                            }
                            ColorWheelSection("Gamma", gammaVal) { 
                                gammaVal = it 
                                selectedNode?.let { node ->
                                    viewModel.updateColorNodeOffset(node.id, liftVal.x, liftVal.y, it.x, it.y, gainVal.x, gainVal.y)
                                }
                            }
                            ColorWheelSection("Gain", gainVal) { 
                                gainVal = it 
                                selectedNode?.let { node ->
                                    viewModel.updateColorNodeOffset(node.id, liftVal.x, liftVal.y, gammaVal.x, gammaVal.y, it.x, it.y)
                                }
                            }
                        }

                        Divider(color = BorderColor, modifier = Modifier.padding(vertical = 4.dp))

                        // Secondary Sliders (Contrast, Saturation, Temp)
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("Temp", modifier = Modifier.width(50.dp), fontSize = 10.sp, color = TextSecondary)
                                Slider(
                                    value = tempGrade,
                                    onValueChange = { tempGrade = it },
                                    valueRange = -50f..50f,
                                    modifier = Modifier.weight(1f)
                                )
                                Text(
                                    text = "${tempGrade.toInt()}",
                                    modifier = Modifier.width(30.dp),
                                    fontSize = 10.sp,
                                    color = Color.White
                                )
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("Contrast", modifier = Modifier.width(50.dp), fontSize = 10.sp, color = TextSecondary)
                                Slider(
                                    value = contrastGrade,
                                    onValueChange = { contrastGrade = it },
                                    valueRange = 0.5f..1.5f,
                                    modifier = Modifier.weight(1f)
                                )
                                Text(
                                    text = "%.2f".format(contrastGrade),
                                    modifier = Modifier.width(30.dp),
                                    fontSize = 10.sp,
                                    color = Color.White
                                )
                            }
                        }
                    }
                }
                "Curves" -> {
                    Column(modifier = Modifier.fillMaxSize()) {
                        // Curve Channels Selector
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color.Black.copy(alpha = 0.4f), RoundedCornerShape(4.dp))
                                .padding(4.dp),
                            horizontalArrangement = Arrangement.SpaceAround
                        ) {
                            val channels = listOf("Master", "Red", "Green", "Blue")
                            channels.forEach { ch ->
                                val active = activeCurveChannel == ch
                                Text(
                                    text = ch,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (active) {
                                        when (ch) {
                                            "Red" -> Color.Red
                                            "Green" -> Color.Green
                                            "Blue" -> Color.Blue
                                            else -> Color.White
                                        }
                                    } else TextSecondary,
                                    modifier = Modifier
                                        .clickable { activeCurveChannel = ch }
                                        .padding(horizontal = 12.dp, vertical = 4.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Grid and Drawing Canvas
                        Canvas(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth()
                                .background(Color.Black.copy(alpha = 0.5f))
                                .border(1.dp, BorderColor)
                        ) {
                            // Grid
                            val lines = 4
                            for (i in 1 until lines) {
                                val x = size.width * i / lines
                                val y = size.height * i / lines
                                drawLine(Color(0xFF334155), Offset(x, 0f), Offset(x, size.height), 1f)
                                drawLine(Color(0xFF334155), Offset(0f, y), Offset(size.width, y), 1f)
                            }

                            // Dynamic curve color
                            val curveColor = when (activeCurveChannel) {
                                "Red" -> Color.Red
                                "Green" -> Color.Green
                                "Blue" -> Color.Blue
                                else -> Color.White
                            }

                            // Diagonal Line representing spline
                            drawLine(
                                color = curveColor,
                                start = Offset(0f, size.height),
                                end = Offset(size.width, 0f),
                                strokeWidth = 2.dp.toPx()
                            )
                            // Anchor points
                            drawCircle(curveColor, 6.dp.toPx(), Offset(0f, size.height))
                            drawCircle(curveColor, 6.dp.toPx(), Offset(size.width / 2, size.height / 2))
                            drawCircle(curveColor, 6.dp.toPx(), Offset(size.width, 0f))
                        }
                    }
                }
                "Qualifier" -> {
                    Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("HSL Keyer qualifier", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color.White)
                        
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Button(onClick = {}, colors = ButtonDefaults.buttonColors(containerColor = PrimaryAccent)) {
                                Text("Eyedropper Pick")
                            }
                            Button(onClick = {}, colors = ButtonDefaults.buttonColors(containerColor = Color.DarkGray)) {
                                Text("Matte Overlay")
                            }
                        }

                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            QualifierSlider("Hue", 0.15f..0.25f)
                            QualifierSlider("Sat", 0.35f..0.75f)
                            QualifierSlider("Luma", 0.20f..0.80f)
                        }
                    }
                }
                "Scopes" -> {
                    Column(modifier = Modifier.fillMaxSize()) {
                        // Scope Type switcher
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 6.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            listOf("Waveform", "Vectorscope", "Histogram").forEach { sc ->
                                val selected = activeScopeType == sc
                                Text(
                                    text = sc,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (selected) SecondaryAccent else TextSecondary,
                                    modifier = Modifier
                                        .clickable { activeScopeType = sc }
                                        .background(if (selected) Color(0xFF1E293B) else Color.Transparent, RoundedCornerShape(4.dp))
                                        .padding(horizontal = 10.dp, vertical = 4.dp)
                                )
                            }
                        }

                        // Drawing active scope
                        Canvas(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth()
                                .background(Color.Black)
                                .border(1.dp, BorderColor)
                        ) {
                            val w = size.width
                            val h = size.height

                            when (activeScopeType) {
                                "Waveform" -> {
                                    // Three channel Parade parade outline
                                    val partition = w / 3
                                    drawLine(Color.DarkGray, Offset(partition, 0f), Offset(partition, h), 1f)
                                    drawLine(Color.DarkGray, Offset(partition * 2, 0f), Offset(partition * 2, h), 1f)
                                    
                                    // Render noise plots
                                    for (ch in 0..2) {
                                        val offset = ch * partition
                                        val color = when (ch) {
                                            0 -> Color.Red
                                            1 -> Color.Green
                                            else -> Color.Blue
                                        }
                                        for (i in 0..60) {
                                            val x = offset + (i.toFloat() / 60) * partition
                                            val y = h * (0.3f + 0.4f * sin(i.toFloat() / 3f) * cos(i.toFloat() / 7f))
                                            drawLine(color.copy(alpha = 0.4f), Offset(x, y - 5), Offset(x, y + 5), 1.5.dp.toPx())
                                        }
                                    }
                                }
                                "Vectorscope" -> {
                                    // Polar circle grid
                                    drawCircle(Color.DarkGray, h / 2.2f, Offset(w / 2, h / 2), style = Stroke(1f))
                                    drawCircle(Color.DarkGray, h / 4.4f, Offset(w / 2, h / 2), style = Stroke(1f))
                                    drawLine(Color.DarkGray, Offset(w / 2, 0f), Offset(w / 2, h))
                                    drawLine(Color.DarkGray, Offset(0f, h / 2), Offset(w, h / 2))

                                    // Color Targets
                                    val angles = listOf(0f, 60f, 120f, 180f, 240f, 300f)
                                    val labels = listOf("R", "Yl", "G", "Cy", "B", "Mg")
                                    angles.zip(labels).forEach { (ang, lbl) ->
                                        val rad = Math.toRadians(ang.toDouble())
                                        val cx = w / 2 + (h / 3) * cos(rad).toFloat()
                                        val cy = h / 2 + (h / 3) * sin(rad).toFloat()
                                        drawRect(Color.White.copy(alpha = 0.4f), Offset(cx - 6.dp.toPx(), cy - 6.dp.toPx()), androidx.compose.ui.geometry.Size(12.dp.toPx(), 12.dp.toPx()), style = Stroke(1f))
                                    }
                                }
                                "Histogram" -> {
                                    // Draw overlapping histograms
                                    val pR = Path().apply { moveTo(0f, h) }
                                    val pG = Path().apply { moveTo(0f, h) }
                                    val pB = Path().apply { moveTo(0f, h) }
                                    for (i in 0..100) {
                                        val x = (i.toFloat() / 100) * w
                                        pR.lineTo(x, h - h * 0.7f * sin(i / 10f) * cos(i / 15f))
                                        pG.lineTo(x, h - h * 0.6f * sin(i / 8f) * cos(i / 20f))
                                        pB.lineTo(x, h - h * 0.5f * sin(i / 12f) * cos(i / 10f))
                                    }
                                    pR.lineTo(w, h); pG.lineTo(w, h); pB.lineTo(w, h)
                                    drawPath(pR, Color.Red.copy(alpha = 0.25f))
                                    drawPath(pG, Color.Green.copy(alpha = 0.25f))
                                    drawPath(pB, Color.Blue.copy(alpha = 0.25f))
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
fun ColorWheelSection(
    title: String,
    value: Offset,
    onValueChange: (Offset) -> Unit
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = title.uppercase(),
            fontWeight = FontWeight.Black,
            fontSize = 9.sp,
            color = TextSecondary,
            letterSpacing = 1.sp
        )
        Spacer(modifier = Modifier.height(6.dp))
        
        val radiusDp = 38.dp
        Box(
            modifier = Modifier
                .size(radiusDp * 2)
                .clip(CircleShape)
                .background(DarkSurface)
                .border(1.dp, BorderColor, CircleShape)
        ) {
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(Unit) {
                        detectDragGestures(
                            onDragStart = { offset ->
                                val w = size.width.toFloat()
                                val h = size.height.toFloat()
                                val cx = w / 2f
                                val cy = h / 2f
                                val dx = offset.x - cx
                                val dy = offset.y - cy
                                val r = w / 2f
                                val dist = kotlin.math.sqrt(dx * dx + dy * dy)
                                val normX = dx / r
                                val normY = dy / r
                                if (dist <= r) {
                                    onValueChange(Offset(normX, normY))
                                } else {
                                    onValueChange(Offset(dx / dist, dy / dist))
                                }
                            },
                            onDrag = { change, dragAmount ->
                                change.consume()
                                val w = size.width.toFloat()
                                val h = size.height.toFloat()
                                val cx = w / 2f
                                val cy = h / 2f
                                val touch = change.position
                                val dx = touch.x - cx
                                val dy = touch.y - cy
                                val r = w / 2f
                                val dist = kotlin.math.sqrt(dx * dx + dy * dy)
                                if (dist <= r) {
                                    onValueChange(Offset(dx / r, dy / r))
                                } else {
                                    onValueChange(Offset(dx / dist, dy / dist))
                                }
                            }
                        )
                    }
            ) {
                val w = size.width.toFloat()
                val h = size.height.toFloat()
                val cx = w / 2f
                val cy = h / 2f
                val r = w / 2f

                // 1. Draw Sweep Gradient (Full Color Circle)
                drawCircle(
                    brush = Brush.sweepGradient(
                        colors = listOf(
                            Color.Red, Color.Yellow, Color.Green, Color.Cyan, Color.Blue, Color.Magenta, Color.Red
                        ),
                        center = Offset(cx, cy)
                    )
                )

                // 2. Draw Radial Gradient Overlay (White desaturation center)
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(Color.White, Color.Transparent),
                        center = Offset(cx, cy),
                        radius = r
                    )
                )

                // 3. Draw Axis Guideline Crosshairs (Resolve style)
                drawLine(
                    color = Color.Black.copy(alpha = 0.25f),
                    start = Offset(cx, 0f),
                    end = Offset(cx, h),
                    strokeWidth = 2.dp.toPx()
                )
                drawLine(
                    color = Color.Black.copy(alpha = 0.25f),
                    start = Offset(0f, cy),
                    end = Offset(w, cy),
                    strokeWidth = 2.dp.toPx()
                )
                
                drawLine(
                    color = Color.White.copy(alpha = 0.4f),
                    start = Offset(cx, 0f),
                    end = Offset(cx, h),
                    strokeWidth = 1.dp.toPx()
                )
                drawLine(
                    color = Color.White.copy(alpha = 0.4f),
                    start = Offset(0f, cy),
                    end = Offset(w, cy),
                    strokeWidth = 1.dp.toPx()
                )

                // Concentric circles for ticks
                drawCircle(
                    color = Color.White.copy(alpha = 0.15f),
                    radius = r * 0.5f,
                    style = Stroke(1.dp.toPx())
                )
                drawCircle(
                    color = Color.White.copy(alpha = 0.2f),
                    radius = r,
                    style = Stroke(1.5.dp.toPx())
                )

                // 4. Draw Interactive Handle/Selector
                val sx = cx + value.x * r
                val sy = cy + value.y * r
                
                // Draw handle shadow/outer rim
                drawCircle(
                    color = Color.Black.copy(alpha = 0.6f),
                    radius = 5.dp.toPx(),
                    center = Offset(sx, sy)
                )
                // Draw handle fill
                drawCircle(
                    color = Color.White,
                    radius = 4.dp.toPx(),
                    center = Offset(sx, sy)
                )
                // Center pin point
                drawCircle(
                    color = SecondaryAccent,
                    radius = 1.5.dp.toPx(),
                    center = Offset(sx, sy)
                )
            }
        }
    }
}

@Composable
fun QualifierSlider(name: String, range: ClosedFloatingPointRange<Float>) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(name, modifier = Modifier.width(45.dp), fontSize = 10.sp, color = TextSecondary)
        RangeSlider(
            value = range.start..range.endInclusive,
            onValueChange = {},
            valueRange = 0f..1f,
            modifier = Modifier.weight(1f),
            colors = SliderDefaults.colors(
                thumbColor = SecondaryAccent,
                activeTrackColor = SecondaryAccent
            )
        )
    }
}

