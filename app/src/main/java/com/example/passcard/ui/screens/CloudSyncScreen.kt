package com.example.passcard.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.passcard.ui.theme.*
import kotlinx.coroutines.delay

data class EncryptionMethod(
    val id: String,
    val title: String,
    val desc: String
)

@Composable
fun CloudSyncContent(
    currentLanguage: AppLanguage,
    themeColors: ThemeColors
) {
    val zh = currentLanguage == AppLanguage.CHINESE
    
    val title = if (zh) "安全同步" else "Secure Sync"
    val subtitle = if (zh) "您的密码库将执行端到端加密保护" else "Your vault is protected with end-to-end encryption"
    
    val methods = if (zh) listOf(
        EncryptionMethod("AES-256-GCM", "AES-256-GCM", "军用级对称加密"),
        EncryptionMethod("ChaCha20-Poly1305", "ChaCha20", "移动端高性能加密"),
        EncryptionMethod("RSA-4096", "RSA-4096", "超高强度非对称加密"),
        EncryptionMethod("SM4", "SM4 算法", "国密标准体制"),
        EncryptionMethod("PIN-Keypad", "数字按键", "自定义手势/PIN组合"),
        EncryptionMethod("Pattern-Lock", "图案密码", "九宫格手势连线")
    ) else listOf(
        EncryptionMethod("AES-256-GCM", "AES-256-GCM", "Military-grade symmetric encryption"),
        EncryptionMethod("ChaCha20-Poly1305", "ChaCha20", "High performance for mobile"),
        EncryptionMethod("RSA-4096", "RSA-4096", "Ultra-high strength asymmetric"),
        EncryptionMethod("SM4", "SM4 Algorithm", "Chinese national standard"),
        EncryptionMethod("PIN-Keypad", "PIN Code", "Custom numeric PIN"),
        EncryptionMethod("Pattern-Lock", "Pattern Lock", "9-grid gesture pattern")
    )
    
    var selectedMethod by remember { mutableStateOf("AES-256-GCM") }
    var showKeypad by remember { mutableStateOf(false) }
    var showPattern by remember { mutableStateOf(false) }
    var currentPin by remember { mutableStateOf("") }
    var patternPoints by remember { mutableStateOf<List<Pair<Float, Float>>>(emptyList()) }
    var patternDots by remember { mutableStateOf<Set<Int>>(emptySet()) }
    var isPatternDrawing by remember { mutableStateOf(false) }
    var pinConfirmed by remember { mutableStateOf(false) }
    var patternConfirmed by remember { mutableStateOf(false) }
    
    var consoleLines by remember { mutableStateOf(listOf<String>()) }
    
    val addLog: (String) -> Unit = { msg ->
        consoleLines = (listOf(msg) + consoleLines).take(8)
    }
    
    LaunchedEffect(Unit) {
        addLog(if (zh) "> 系统就绪。" else "> System ready.")
        addLog(if (zh) "> 当前通道: TLS 1.3" else "> Channel: TLS 1.3")
        addLog(if (zh) "> 请选择加密算法。" else "> Select encryption algorithm.")
    }
    
    fun selectMethod(id: String) {
        selectedMethod = id
        addLog(if (zh) "> 已切换: $id" else "> Switched: $id")
        showKeypad = id == "PIN-Keypad"
        showPattern = id == "Pattern-Lock"
        if (id != "PIN-Keypad") {
            currentPin = ""
            pinConfirmed = false
        }
        if (id != "Pattern-Lock") {
            patternPoints = emptyList()
            patternDots = emptySet()
            patternConfirmed = false
        }
    }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
    ) {
        Box(modifier = Modifier.fillMaxWidth().height(47.dp), contentAlignment = Alignment.Center) {
            Text(text = "9:41", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.W600), color = themeColors.onBackground)
        }
        
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp)
                .padding(bottom = 120.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            Text(text = title, style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold), color = themeColors.onBackground)
            Text(text = subtitle, style = MaterialTheme.typography.bodyMedium, color = themeColors.onSurfaceVariant)
            
            // 加密算法选择
            Text(text = if (zh) "选择加密体制" else "Select Encryption Method", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.W600), color = themeColors.onSurfaceVariant)
            
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                methods.chunked(2).forEach { row ->
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        row.forEach { method ->
                            val isSelected = selectedMethod == method.id
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(if (isSelected) Primary.copy(alpha = 0.1f) else themeColors.surface)
                                    .border(
                                        width = 2.dp,
                                        color = if (isSelected) Primary else themeColors.border,
                                        shape = RoundedCornerShape(12.dp)
                                    )
                                    .clickable { selectMethod(method.id) }
                                    .padding(14.dp)
                            ) {
                                Column {
                                    Text(
                                        text = method.title,
                                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                        color = if (isSelected) Primary else themeColors.onBackground
                                    )
                                    Text(
                                        text = method.desc,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = if (isSelected) Primary else themeColors.onSurfaceVariant
                                    )
                                }
                            }
                        }
                        if (row.size == 1) Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
            
            // 数字 PIN 键盘
            AnimatedVisibility(visible = showKeypad, enter = fadeIn(), exit = fadeOut()) {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Text(text = if (zh) "输入 PIN 码" else "Enter PIN Code", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.W600), color = themeColors.onSurfaceVariant)
                    
                    // PIN 显示
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (themeColors.isDark) Color(0xFF2A2A2A) else Color(0xFFF3F4F6))
                            .padding(horizontal = 16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        if (currentPin.isEmpty()) {
                            Text(text = if (zh) "请输入数字 PIN" else "Enter numeric PIN", style = MaterialTheme.typography.bodyMedium, color = themeColors.muted)
                        } else {
                            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                repeat(currentPin.length.coerceAtMost(8)) {
                                    Box(modifier = Modifier.size(12.dp).clip(CircleShape).background(if (isPatternDrawing) Primary else themeColors.onSurfaceVariant))
                                }
                            }
                        }
                    }
                    
                    // 键盘
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        listOf(listOf("1","2","3"), listOf("4","5","6"), listOf("7","8","9"), listOf("⌫","0","✓")).forEach { row ->
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                row.forEach { key ->
                                    val isAction = key == "⌫" || key == "✓"
                                    val bgColor = when {
                                        key == "✓" && pinConfirmed -> themeColors.success
                                        key == "✓" -> Primary
                                        else -> if (themeColors.isDark) Color(0xFF2A2A2A) else Color(0xFFF3F4F6)
                                    }
                                    val textColor = when {
                                        key == "✓" -> Color.White
                                        else -> themeColors.onBackground
                                    }
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .aspectRatio(1f)
                                            .clip(CircleShape)
                                            .background(bgColor)
                                            .clickable {
                                                when (key) {
                                                    "⌫" -> { if (currentPin.isNotEmpty()) currentPin = currentPin.dropLast(1) }
                                                    "✓" -> {
                                                        if (currentPin.length >= 4) {
                                                            pinConfirmed = true
                                                            showKeypad = false
                                                            addLog(if (zh) "> ✓ PIN 已确认！" else "> ✓ PIN confirmed!")
                                                        } else {
                                                            addLog(if (zh) "> ✗ PIN 至少需要4位" else "> ✗ PIN needs at least 4 digits")
                                                        }
                                                    }
                                                    else -> { if (currentPin.length < 8) currentPin += key }
                                                }
                                            },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        if (key == "⌫") {
                                            Icon(Icons.Outlined.Backspace, contentDescription = "Delete", tint = textColor, modifier = Modifier.size(24.dp))
                                        } else {
                                            Text(text = key, style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Medium), color = textColor)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
            
            // 图案密码区域
            AnimatedVisibility(visible = showPattern, enter = fadeIn(), exit = fadeOut()) {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Text(text = if (zh) "绘制图案密码" else "Draw Pattern", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.W600), color = themeColors.onSurfaceVariant)
                    
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (themeColors.isDark) Color(0xFF2A2A2A) else Color(0xFFF3F4F6))
                            .padding(horizontal = 16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (patternPoints.isEmpty()) (if (zh) "请绘制九宫格图案" else "Draw a 9-grid pattern") else (if (zh) "已连接 ${patternPoints.size} 个点" else "Connected ${patternPoints.size} dots"),
                            style = MaterialTheme.typography.bodyMedium,
                            color = themeColors.muted
                        )
                    }
                    
                    // 九宫格
                    val dotPositions = remember { listOf(0f to 0f, 0.5f to 0f, 1f to 0f, 0f to 0.5f, 0.5f to 0.5f, 1f to 0.5f, 0f to 1f, 0.5f to 1f, 1f to 1f) }
                    
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(1f)
                            .padding(16.dp)
                    ) {
                        Canvas(
                            modifier = Modifier.fillMaxSize()
                                .pointerInput(Unit) {
                                    detectTapGestures(
                                        onPress = { offset ->
                                            isPatternDrawing = true
                                            patternPoints = emptyList()
                                            patternDots = emptySet()
                                            val cellSize = size.width / 3f
                                            
                                            fun handleMove(x: Float, y: Float) {
                                                val col = (x / cellSize).toInt().coerceIn(0, 2)
                                                val row = (y / cellSize).toInt().coerceIn(0, 2)
                                                val dotIdx = row * 3 + col
                                                val cx = col * cellSize + cellSize / 2
                                                val cy = row * cellSize + cellSize / 2
                                                val dist = kotlin.math.hypot((x - cx).toDouble(), (y - cy).toDouble())
                                                if (dist < (cellSize * 0.6f).toDouble() && dotIdx !in patternDots) {
                                                    patternDots += dotIdx
                                                    patternPoints = patternPoints + (cx to cy)
                                                }
                                            }
                                            
                                            handleMove(offset.x, offset.y)
                                            tryAwaitRelease()
                                            isPatternDrawing = false
                                        }
                                    )
                                }
                        ) {
                            val cellSize = size.width / 3f
                            val dotRadius = size.width * 0.025f
                            val activeRadius = size.width * 0.04f
                            val lineWidth = size.width * 0.015f
                            
                            // 绘制线
                            if (patternPoints.size > 1) {
                                for (i in 0 until patternPoints.size - 1) {
                                    val start = Offset(patternPoints[i].first, patternPoints[i].second)
                                    val end = Offset(patternPoints[i + 1].first, patternPoints[i + 1].second)
                                    drawLine(
                                        color = Primary,
                                        start = start,
                                        end = end,
                                        strokeWidth = lineWidth
                                    )
                                }
                            }
                            
                            // 绘制点
                            for (i in 0 until 9) {
                                val col = i % 3
                                val row = i / 3
                                val cx = col * cellSize + cellSize / 2
                                val cy = row * cellSize + cellSize / 2
                                val isActive = i in patternDots
                                
                                drawCircle(
                                    color = if (isActive) Primary else (if (themeColors.isDark) Color(0xFF555) else Color(0xFFCCC)),
                                    radius = if (isActive) activeRadius else dotRadius,
                                    center = Offset(cx, cy)
                                )
                            }
                        }
                    }
                    
                    Button(
                        onClick = {
                            if (patternPoints.size >= 4) {
                                patternConfirmed = true
                                showPattern = false
                                addLog(if (zh) "> ✓ 图案已确认！" else "> ✓ Pattern confirmed!")
                            } else {
                                patternPoints = emptyList()
                                patternDots = emptySet()
                                addLog(if (zh) "> ✗ 至少需要4个点" else "> ✗ At least 4 dots required")
                            }
                        },
                        modifier = Modifier.fillMaxWidth().height(50.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Primary),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(text = if (zh) "确认图案" else "Confirm Pattern", fontWeight = FontWeight.Bold)
                    }
                }
            }
            
            // 状态控制台
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(if (themeColors.isDark) Color(0xFF1F2937) else Color(0xFF111827))
                    .padding(16.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    consoleLines.forEach { line ->
                        Text(
                            text = line,
                            style = MaterialTheme.typography.bodySmall.copy(fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace),
                            color = when {
                                line.contains("✓") -> themeColors.success
                                line.contains("✗") -> themeColors.error
                                line.contains(">") -> Color(0xFF10B981)
                                else -> Color(0xFF10B981)
                            }
                        )
                    }
                }
            }
            
            // 上传和下载按钮
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Button(
                    onClick = {
                        when {
                            selectedMethod == "PIN-Keypad" && !pinConfirmed -> {
                                addLog(if (zh) "> ✗ 请先确认 PIN" else "> ✗ Please confirm PIN first")
                                showKeypad = true
                            }
                            selectedMethod == "Pattern-Lock" && !patternConfirmed -> {
                                addLog(if (zh) "> ✗ 请先确认图案" else "> ✗ Please confirm pattern first")
                                showPattern = true
                            }
                            else -> {
                                consoleLines = emptyList()
                                addLog(if (zh) "> 开始上传流程..." else "> Starting upload...")
                                addLog(if (zh) "> 正在读取密码库... 📦" else "> Reading vault... 📦")
                            }
                        }
                    },
                    modifier = Modifier.weight(1f).height(52.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Primary),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Outlined.CloudUpload, contentDescription = null, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(text = if (zh) "加密至云端" else "Encrypt & Upload", fontWeight = FontWeight.Bold)
                }
                
                OutlinedButton(
                    onClick = {
                        when {
                            selectedMethod == "PIN-Keypad" && !pinConfirmed -> {
                                addLog(if (zh) "> ✗ 请先确认 PIN" else "> ✗ Please confirm PIN first")
                                showKeypad = true
                            }
                            selectedMethod == "Pattern-Lock" && !patternConfirmed -> {
                                addLog(if (zh) "> ✗ 请先确认图案" else "> ✗ Please confirm pattern first")
                                showPattern = true
                            }
                            else -> {
                                consoleLines = emptyList()
                                addLog(if (zh) "> 开始下载云端数据..." else "> Fetching from cloud...")
                                addLog(if (zh) "> 正在下载密文... 📥" else "> Downloading ciphertext... 📥")
                            }
                        }
                    },
                    modifier = Modifier.weight(1f).height(52.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = themeColors.onBackground)
                ) {
                    Icon(Icons.Outlined.CloudDownload, contentDescription = null, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(text = if (zh) "从云端获取" else "Fetch & Decrypt", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
