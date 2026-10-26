package com.example.passcard.util

import java.security.SecureRandom
import kotlin.random.Random
import kotlin.random.asKotlinRandom

data class RandomPasswordSpec(
    val length: Int = DEFAULT_LENGTH,
    val includeUppercase: Boolean = true,
    val includeLowercase: Boolean = true,
    val includeNumbers: Boolean = true,
    val includeSymbols: Boolean = true
) {
    fun normalized(): RandomPasswordSpec {
        val hasCharset = includeUppercase || includeLowercase || includeNumbers || includeSymbols
        return copy(
            length = length.coerceIn(MIN_LENGTH, MAX_LENGTH),
            includeLowercase = includeLowercase || !hasCharset
        )
    }

    companion object {
        const val MIN_LENGTH = 4
        const val MAX_LENGTH = 18
        const val DEFAULT_LENGTH = 16
    }
}

data class PasswordStrength(
    val score: Int,
    val labelZh: String,
    val labelEn: String,
    val hintZh: String,
    val hintEn: String
)

object RandomPasswordGenerator {
    private val secureRandom: Random = SecureRandom().asKotlinRandom()

    private const val UPPERCASE = "ABCDEFGHJKLMNPQRSTUVWXYZ"
    private const val LOWERCASE = "abcdefghijkmnopqrstuvwxyz"
    private const val NUMBERS = "23456789"
    private const val SYMBOLS = "!@#$%^&*+-_=?."

    fun generate(spec: RandomPasswordSpec, random: Random = secureRandom): String {
        val normalized = spec.normalized()
        val groups = buildList {
            if (normalized.includeUppercase) add(UPPERCASE)
            if (normalized.includeLowercase) add(LOWERCASE)
            if (normalized.includeNumbers) add(NUMBERS)
            if (normalized.includeSymbols) add(SYMBOLS)
        }
        val pool = groups.joinToString("")
        val required = groups.map { group -> group.random(random) }
        val rest = List((normalized.length - required.size).coerceAtLeast(0)) {
            pool.random(random)
        }
        return (required + rest).shuffled(random).joinToString("")
    }

    fun strength(spec: RandomPasswordSpec): PasswordStrength {
        val normalized = spec.normalized()
        val charsetCount = listOf(
            normalized.includeUppercase,
            normalized.includeLowercase,
            normalized.includeNumbers,
            normalized.includeSymbols
        ).count { it }
        val score = (normalized.length * 4 + charsetCount * 12).coerceIn(0, 100)
        return when {
            score >= 76 -> PasswordStrength(
                score = score,
                labelZh = "较高",
                labelEn = "Higher",
                hintZh = "仅根据生成长度与字符类型提供配置参考；不检查具体密码的常见模式或重复使用。",
                hintEn = "A configuration reference based on length and character types. It does not check the actual password for predictable patterns or reuse."
            )
            score >= 52 -> PasswordStrength(
                score = score,
                labelZh = "中等",
                labelEn = "Moderate",
                hintZh = "可增加生成长度。此配置参考不等于具体密码的健康评分。",
                hintEn = "Consider increasing length. This configuration reference is not a health score for the actual password."
            )
            else -> PasswordStrength(
                score = score,
                labelZh = "较低",
                labelEn = "Lower",
                hintZh = "建议优先增加长度；此参考不代表具体密码的安全程度。",
                hintEn = "Increase length first. This reference does not establish how safe the actual password is."
            )
        }
    }
}
