package com.example.passcard.util

import kotlin.random.Random

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
        const val DEFAULT_LENGTH = 14
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
    private const val UPPERCASE = "ABCDEFGHJKLMNPQRSTUVWXYZ"
    private const val LOWERCASE = "abcdefghijkmnopqrstuvwxyz"
    private const val NUMBERS = "23456789"
    private const val SYMBOLS = "!@#$%^&*+-_=?."

    fun generate(spec: RandomPasswordSpec, random: Random = Random.Default): String {
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
                labelZh = "强",
                labelEn = "Strong",
                hintZh = "长度和字符类型都比较均衡。",
                hintEn = "Length and character variety are well balanced."
            )
            score >= 52 -> PasswordStrength(
                score = score,
                labelZh = "中等",
                labelEn = "Good",
                hintZh = "适合普通账户，重要账户建议更长。",
                hintEn = "Good for regular accounts. Use longer passwords for important ones."
            )
            else -> PasswordStrength(
                score = score,
                labelZh = "偏弱",
                labelEn = "Weak",
                hintZh = "建议增加长度并启用更多字符类型。",
                hintEn = "Increase length and enable more character types."
            )
        }
    }
}
