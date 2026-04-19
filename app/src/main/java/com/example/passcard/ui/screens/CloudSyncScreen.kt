package com.example.passcard.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
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
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.passcard.ui.theme.*

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
    var pinConfirmed by remember { mutableStateOf(false) }
    var patternConfirmed by remember { mutableStateOf(false) }
    
    var consoleLines by remember { mutableStateOf(listOf<String>()) }
    
    val addLog: (String) -> Unit = { msg ->
        consoleLines = (listOf(msg) + consoleLines).take(8)
    }
    
    // 初始化日志
    if (consoleLines.isEmpty()) {
        consoleLines = listOf(
            if (zh) "> 系统就绪。" else "> System ready.",
            if (zh) "> 当前通道: TLS 1.3" else "> Channel: TLS 1.3",
            if (zh) "> 请选择加密算法。" else "> Select encryption algorithm."
        )
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
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(text = if (zh) "输入 PIN 码" else "Enter PIN Code", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.W600), color = themeColors.onSurfaceVariant)
                    
                    // PIN 显示
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (themeColors.isDark) Color(0xFF2A2A2A) else Color(0xFFF3F4F6))
                            .padding(horizontal = 16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        if (currentPin.isEmpty()) {
                            Text(text = if (zh) "请输入数字 PIN" else "Enter numeric PIN", style = MaterialTheme.typography.bodyMedium, color = themeColors.muted)
                        } else {
                            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                repeat(currentPin.length.coerceAtMost(8)) {
                                    Box(modifier = Modifier.size(10.dp).clip(CircleShape).background(Primary))
                                }
                            }
                        }
                    }
                    
                    // 键盘 - 更紧凑的布局
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
                        listOf(listOf("1","2","3"), listOf("4","5","6"), listOf("7","8","9"), listOf("⌫","0","✓")).forEach { row ->
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                row.forEach { key ->
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
                                            .height(56.dp)
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
                                            Icon(Icons.Outlined.Backspace, contentDescription = "Delete", tint = textColor, modifier = Modifier.size(20.dp))
                                        } else {
                                            Text(text = key, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Medium), color = textColor)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
            
            // 图案密码区域 - 拖拽连线模式
            AnimatedVisibility(visible = showPattern, enter = fadeIn(), exit = fadeOut()) {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Text(text = if (zh) "绘制图案密码" else "Draw Pattern", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.W600), color = themeColors.onSurfaceVariant)
                    
                    // 状态显示
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
                            text = if (zh) "请拖动连接九宫格绘制图案" else "Drag to connect dots and draw pattern",
                            style = MaterialTheme.typography.bodyMedium,
                            color = themeColors.muted
                        )
                    }
                    
                    // 九宫格 Canvas - 拖拽连线
                    PatternLockView(
                        themeColors = themeColors,
                        onPatternComplete = { pattern ->
                            if (pattern.size >= 4) {
                                patternConfirmed = true
                                showPattern = false
                                addLog(if (zh) "> ✓ 图案已确认！连接了 ${pattern.size} 个点" else "> ✓ Pattern confirmed! ${pattern.size} dots connected")
                            } else {
                                addLog(if (zh) "> ✗ 至少需要4个点" else "> ✗ At least 4 dots required")
                            }
                        }
                    )
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

// 图案锁组件 - 拖拽连线模式（需确认）
@Composable
private fun PatternLockView(
    themeColors: ThemeColors,
    onPatternComplete: (List<Int>) -> Unit
) {
    var selectedDots by remember { mutableStateOf<List<Int>>(emptyList()) }
    var isDrawing by remember { mutableStateOf(false) }
    var currentPosition by remember { mutableStateOf(Offset.Zero) }
    
    // 计算九个点的位置（相对于 Canvas 尺寸的百分比）
    val dotPositions = remember {
        listOf(
            Offset(0.2f, 0.2f), Offset(0.5f, 0.2f), Offset(0.8f, 0.2f),
            Offset(0.2f, 0.5f), Offset(0.5f, 0.5f), Offset(0.8f, 0.5f),
            Offset(0.2f, 0.8f), Offset(0.5f, 0.8f), Offset(0.8f, 0.8f)
        )
    }
    
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        // 九宫格 Canvas - 拖拽连线
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .clip(RoundedCornerShape(16.dp))
                .background(if (themeColors.isDark) Color(0xFF1A1A1A) else Color(0xFFF0F0F0))
                .padding(16.dp)
        ) {
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(Unit) {
                        detectDragGestures(
                            onDragStart = { offset ->
                                // 开始新绘制 - 清除之前的
                                isDrawing = true
                                selectedDots = emptyList()
                                currentPosition = offset
                                
                                // 检查是否点中了某个点
                                val width = size.width.toFloat()
                                val height = size.height.toFloat()
                                dotPositions.forEachIndexed { index, pos ->
                                    val dotX = pos.x * width
                                    val dotY = pos.y * height
                                    val distance = kotlin.math.sqrt(
                                        (offset.x - dotX) * (offset.x - dotX) +
                                        (offset.y - dotY) * (offset.y - dotY)
                                    )
                                    val threshold = width * 0.15f
                                    if (distance < threshold && index !in selectedDots) {
                                        selectedDots = selectedDots + index
                                    }
                                }
                            },
                            onDrag = { change, _ ->
                                currentPosition = change.position
                                
                                // 检查是否经过某个点
                                val width = size.width.toFloat()
                                val height = size.height.toFloat()
                                dotPositions.forEachIndexed { index, pos ->
                                    val dotX = pos.x * width
                                    val dotY = pos.y * height
                                    val distance = kotlin.math.sqrt(
                                        (currentPosition.x - dotX) * (currentPosition.x - dotX) +
                                        (currentPosition.y - dotY) * (currentPosition.y - dotY)
                                    )
                                    val threshold = width * 0.15f
                                    if (distance < threshold && index !in selectedDots) {
                                        selectedDots = selectedDots + index
                                    }
                                }
                            },
                            onDragEnd = {
                                isDrawing = false
                                // 不自动确认，等用户点击"确定图案"按钮
                            }
                        )
                    }
            ) {
                val width = size.width
                val height = size.height
                
                // 绘制连线
                if (selectedDots.size > 1) {
                    for (i in 0 until selectedDots.size - 1) {
                        val startIndex = selectedDots[i]
                        val endIndex = selectedDots[i + 1]
                        val start = Offset(dotPositions[startIndex].x * width, dotPositions[startIndex].y * height)
                        val end = Offset(dotPositions[endIndex].x * width, dotPositions[endIndex].y * height)
                        
                        drawLine(
                            color = Primary,
                            start = start,
                            end = end,
                            strokeWidth = 8f,
                            cap = StrokeCap.Round
                        )
                    }
                }
                
                // 如果正在绘制，画到当前位置的线
                if (isDrawing && selectedDots.isNotEmpty()) {
                    val lastIndex = selectedDots.last()
                    val start = Offset(dotPositions[lastIndex].x * width, dotPositions[lastIndex].y * height)
                    drawLine(
                        color = Primary.copy(alpha = 0.5f),
                        start = start,
                        end = currentPosition,
                        strokeWidth = 8f,
                        cap = StrokeCap.Round
                    )
                }
                
                // 绘制九个点
                dotPositions.forEachIndexed { index, pos ->
                    val x = pos.x * width
                    val y = pos.y * height
                    val isSelected = index in selectedDots
                    val radius = if (isSelected) width * 0.06f else width * 0.04f
                    
                    // 外圈
                    drawCircle(
                        color = if (isSelected) Primary else (if (themeColors.isDark) Color(0xFF555555) else Color(0xFFCCCCCC)),
                        radius = radius,
                        center = Offset(x, y)
                    )
                    
                    // 内圈（选中时）
                    if (isSelected) {
                        drawCircle(
                            color = Primary.copy(alpha = 0.3f),
                            radius = radius * 1.8f,
                            center = Offset(x, y)
                        )
                    }
                }
            }
        }
        
        // 提示文字和确认按钮
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = if (selectedDots.isEmpty()) "请滑动绘制图案" else "已连接 ${selectedDots.size} 个点",
                style = MaterialTheme.typography.bodySmall,
                color = themeColors.onSurfaceVariant
            )
            
            if (selectedDots.isNotEmpty()) {
                Text(
                    text = "清除",
                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                    color = Primary,
                    modifier = Modifier.clickable { selectedDots = emptyList() }
                )
            }
        }
        
        // 确定图案按钮
        Button(
            onClick = {
                if (selectedDots.size >= 4) {
                    onPatternComplete(selectedDots)
                }
            },
            modifier = Modifier.fillMaxWidth().height(50.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = if (selectedDots.size >= 4) Primary else themeColors.muted
            ),
            shape = RoundedCornerShape(12.dp),
            enabled = selectedDots.size >= 4
        ) {
            Text(text = "确定图案", fontWeight = FontWeight.Bold)
        }
    }
}
