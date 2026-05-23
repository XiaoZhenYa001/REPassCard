package com.example.passcard.ui.components

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.passcard.util.PasswordIconStorage
import com.example.passcard.util.PasswordIconType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.absoluteValue

data class GeneratedIconOption(
    val value: String,
    val zhLabel: String,
    val enLabel: String
)

data class EmojiIconOption(
    val emoji: String,
    val zhKeywords: String,
    val enKeywords: String
)

val generatedIconOptions = listOf(
    GeneratedIconOption("", "自动", "Auto"),
    GeneratedIconOption("shield", "安全", "Secure"),
    GeneratedIconOption("key", "钥匙", "Key"),
    GeneratedIconOption("card", "卡片", "Card"),
    GeneratedIconOption("work", "工作", "Work"),
    GeneratedIconOption("cloud", "云端", "Cloud")
)

val emojiIconOptions = listOf(
    EmojiIconOption("🔐", "密码 安全 锁", "password secure lock"),
    EmojiIconOption("🔑", "钥匙 登录", "key login"),
    EmojiIconOption("🛡️", "盾牌 安全", "shield security"),
    EmojiIconOption("📧", "邮箱 邮件", "email mail"),
    EmojiIconOption("💬", "社交 聊天 微信", "social chat wechat"),
    EmojiIconOption("📱", "手机 应用", "mobile app"),
    EmojiIconOption("🏦", "银行 金融", "bank finance"),
    EmojiIconOption("💳", "支付 卡 金融", "payment card finance"),
    EmojiIconOption("🛒", "购物 商店", "shopping store"),
    EmojiIconOption("🛍️", "购物 商品", "shopping goods"),
    EmojiIconOption("🎮", "游戏", "game"),
    EmojiIconOption("💼", "工作 公司", "work company"),
    EmojiIconOption("🧾", "账单 发票", "bill invoice"),
    EmojiIconOption("☁️", "云 服务", "cloud service"),
    EmojiIconOption("🖥️", "电脑 服务器", "computer server"),
    EmojiIconOption("🗄️", "数据库 服务器", "database server"),
    EmojiIconOption("📚", "学习 文档", "study docs"),
    EmojiIconOption("🎓", "教育 学校", "education school"),
    EmojiIconOption("🎬", "视频 影视", "video movie"),
    EmojiIconOption("🎵", "音乐", "music"),
    EmojiIconOption("✈️", "旅行 出行", "travel"),
    EmojiIconOption("🚗", "汽车 出行", "car travel"),
    EmojiIconOption("🏠", "家庭 家", "home family"),
    EmojiIconOption("📌", "其他 标记", "other pin")
)

@Composable
fun PasswordIcon(
    label: String,
    iconType: String,
    iconValue: String,
    modifier: Modifier = Modifier,
    size: Dp = 40.dp,
    cornerRadius: Dp = 12.dp
) {
    when (iconType) {
        PasswordIconType.IMAGE -> ImagePasswordIcon(
            label = label,
            iconValue = iconValue,
            modifier = modifier,
            size = size,
            cornerRadius = cornerRadius
        )
        PasswordIconType.EMOJI -> EmojiPasswordIcon(
            emoji = iconValue.ifBlank { "🔐" },
            modifier = modifier,
            size = size,
            cornerRadius = cornerRadius
        )
        else -> GeneratedPasswordIcon(
            label = label,
            iconValue = iconValue,
            modifier = modifier,
            size = size,
            cornerRadius = cornerRadius
        )
    }
}

@Composable
private fun ImagePasswordIcon(
    label: String,
    iconValue: String,
    modifier: Modifier,
    size: Dp,
    cornerRadius: Dp
) {
    val context = LocalContext.current
    var bitmap by remember(iconValue) { mutableStateOf<Bitmap?>(null) }

    LaunchedEffect(iconValue) {
        bitmap = withContext(Dispatchers.IO) {
            PasswordIconStorage.decodeIconBitmap(context, iconValue, 192)
        }
    }

    val loaded = bitmap
    if (loaded != null) {
        Image(
            bitmap = loaded.asImageBitmap(),
            contentDescription = label,
            contentScale = ContentScale.Crop,
            modifier = modifier
                .size(size)
                .clip(RoundedCornerShape(cornerRadius))
        )
    } else {
        GeneratedPasswordIcon(
            label = label,
            iconValue = "",
            modifier = modifier,
            size = size,
            cornerRadius = cornerRadius
        )
    }
}

@Composable
private fun EmojiPasswordIcon(
    emoji: String,
    modifier: Modifier,
    size: Dp,
    cornerRadius: Dp
) {
    Box(
        modifier = modifier
            .size(size)
            .clip(RoundedCornerShape(cornerRadius))
            .background(Color(0xFFF2F6F9)),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = emoji,
            fontSize = (size.value * 0.46f).sp,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
fun GeneratedPasswordIcon(
    label: String,
    iconValue: String,
    modifier: Modifier = Modifier,
    size: Dp = 40.dp,
    cornerRadius: Dp = 12.dp
) {
    val colors = generatedColors(label + iconValue)
    Box(
        modifier = modifier
            .size(size)
            .clip(RoundedCornerShape(cornerRadius))
            .background(Brush.linearGradient(colors)),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = generatedSymbol(label, iconValue),
            style = MaterialTheme.typography.bodyLarge.copy(
                fontWeight = FontWeight.W800,
                fontSize = (size.value * 0.34f).sp
            ),
            color = Color.White,
            textAlign = TextAlign.Center
        )
    }
}

private fun generatedSymbol(label: String, iconValue: String): String {
    return when (iconValue) {
        "shield" -> "S"
        "key" -> "K"
        "card" -> "P"
        "work" -> "W"
        "cloud" -> "C"
        else -> label.trim().take(1).uppercase().ifBlank { "P" }
    }
}

private fun generatedColors(seed: String): List<Color> {
    val palettes = listOf(
        listOf(Color(0xFF0EA5E9), Color(0xFF14B8A6)),
        listOf(Color(0xFF2563EB), Color(0xFF7C3AED)),
        listOf(Color(0xFF059669), Color(0xFF84CC16)),
        listOf(Color(0xFFEA580C), Color(0xFFF59E0B)),
        listOf(Color(0xFFDB2777), Color(0xFF7C3AED)),
        listOf(Color(0xFF0F766E), Color(0xFF1D4ED8))
    )
    return palettes[seed.hashCode().absoluteValue % palettes.size]
}
