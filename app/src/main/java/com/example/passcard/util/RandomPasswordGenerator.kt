package com.example.passcard.util

import java.security.SecureRandom
import kotlin.math.log2
import kotlin.math.roundToInt
import kotlin.random.Random
import kotlin.random.asKotlinRandom

enum class PasswordCharacterGroup(val availableCharacters: String) {
    Uppercase("ABCDEFGHIJKLMNOPQRSTUVWXYZ"),
    Lowercase("abcdefghijklmnopqrstuvwxyz"),
    Numbers("0123456789"),
    Symbols("!@#$%^&*+-_=?.")
}

data class RandomPasswordSpec(
    val length: Int = DEFAULT_LENGTH,
    val includeUppercase: Boolean = true,
    val includeLowercase: Boolean = true,
    val includeNumbers: Boolean = true,
    val includeSymbols: Boolean = true,
    val uppercaseCharacters: String = PasswordCharacterGroup.Uppercase.availableCharacters,
    val lowercaseCharacters: String = PasswordCharacterGroup.Lowercase.availableCharacters,
    val numberCharacters: String = PasswordCharacterGroup.Numbers.availableCharacters,
    val symbolCharacters: String = PasswordCharacterGroup.Symbols.availableCharacters
) {
    fun selectedCharacters(group: PasswordCharacterGroup): String {
        val selection = when (group) {
            PasswordCharacterGroup.Uppercase -> if (includeUppercase) uppercaseCharacters else ""
            PasswordCharacterGroup.Lowercase -> if (includeLowercase) lowercaseCharacters else ""
            PasswordCharacterGroup.Numbers -> if (includeNumbers) numberCharacters else ""
            PasswordCharacterGroup.Symbols -> if (includeSymbols) symbolCharacters else ""
        }
        return group.availableCharacters.filter { it in selection }
    }

    fun withSelectedCharacters(group: PasswordCharacterGroup, characters: String): RandomPasswordSpec {
        val selection = group.availableCharacters.filter { it in characters }
        return when (group) {
            PasswordCharacterGroup.Uppercase -> copy(uppercaseCharacters = selection, includeUppercase = selection.isNotEmpty())
            PasswordCharacterGroup.Lowercase -> copy(lowercaseCharacters = selection, includeLowercase = selection.isNotEmpty())
            PasswordCharacterGroup.Numbers -> copy(numberCharacters = selection, includeNumbers = selection.isNotEmpty())
            PasswordCharacterGroup.Symbols -> copy(symbolCharacters = selection, includeSymbols = selection.isNotEmpty())
        }
    }

    val hasSelectedCharacters: Boolean
        get() = PasswordCharacterGroup.entries.any { selectedCharacters(it).isNotEmpty() }

    fun normalized(): RandomPasswordSpec {
        val uppercase = PasswordCharacterGroup.Uppercase.availableCharacters.filter { it in uppercaseCharacters }
        val lowercase = PasswordCharacterGroup.Lowercase.availableCharacters.filter { it in lowercaseCharacters }
        val numbers = PasswordCharacterGroup.Numbers.availableCharacters.filter { it in numberCharacters }
        val symbols = PasswordCharacterGroup.Symbols.availableCharacters.filter { it in symbolCharacters }
        val cleaned = copy(
            length = length.coerceIn(MIN_LENGTH, MAX_LENGTH),
            includeUppercase = includeUppercase && uppercase.isNotEmpty(),
            includeLowercase = includeLowercase && lowercase.isNotEmpty(),
            includeNumbers = includeNumbers && numbers.isNotEmpty(),
            includeSymbols = includeSymbols && symbols.isNotEmpty(),
            uppercaseCharacters = uppercase,
            lowercaseCharacters = lowercase,
            numberCharacters = numbers,
            symbolCharacters = symbols
        )
        return if (cleaned.hasSelectedCharacters) cleaned else cleaned.copy(
            includeLowercase = true,
            lowercaseCharacters = PasswordCharacterGroup.Lowercase.availableCharacters
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

    fun generate(spec: RandomPasswordSpec, random: Random = secureRandom): String {
        val normalized = spec.normalized()
        val groups = PasswordCharacterGroup.entries.map { normalized.selectedCharacters(it) }.filter(String::isNotEmpty)
        val pool = groups.joinToString("")
        val required = groups.map { group -> group.random(random) }
        val rest = List((normalized.length - required.size).coerceAtLeast(0)) {
            pool.random(random)
        }
        return (required + rest).shuffled(random).joinToString("")
    }

    fun strength(spec: RandomPasswordSpec): PasswordStrength {
        val normalized = spec.normalized()
        val poolSize = PasswordCharacterGroup.entries.flatMap { normalized.selectedCharacters(it).toList() }.toSet().size
        // Forcing one character per active group is not uniform whole-pool sampling.
        // This size-sensitive score is a rough configuration reference, not exact entropy.
        val score = (normalized.length * log2(poolSize.toDouble())).roundToInt().coerceIn(0, 100)
        if (poolSize == 1) return PasswordStrength(
            score = 0,
            labelZh = "参考值较低",
            labelEn = "Lower estimate",
            hintZh = "仅选择了 1 个字符，生成结果固定。请增加可选字符数量；此分数仅供配置参考。",
            hintEn = "Only one character is selected, so the result is fixed. Select more characters; this score is only a configuration reference."
        )
        return when {
            score >= 76 -> PasswordStrength(
                score = score,
                labelZh = "参考值较高",
                labelEn = "Higher estimate",
                hintZh = "仅按长度与所选字符数量粗略提供配置参考；不保证具体密码的安全，也不检查常见模式或重复使用。",
                hintEn = "A rough configuration reference from length and selected-character count, not a safety guarantee. It does not check predictable patterns or reuse."
            )
            score >= 52 -> PasswordStrength(
                score = score,
                labelZh = "参考值中等",
                labelEn = "Moderate estimate",
                hintZh = "可增加生成长度或所选字符数量。此粗略配置参考不等于具体密码的健康评分。",
                hintEn = "Consider increasing length or the selected-character count. This rough configuration reference is not a health score for the actual password."
            )
            else -> PasswordStrength(
                score = score,
                labelZh = "参考值较低",
                labelEn = "Lower estimate",
                hintZh = "建议增加长度及所选字符数量；此粗略参考不代表具体密码的安全程度。",
                hintEn = "Increase length and the selected-character count. This rough reference does not establish how safe the actual password is."
            )
        }
    }
}
