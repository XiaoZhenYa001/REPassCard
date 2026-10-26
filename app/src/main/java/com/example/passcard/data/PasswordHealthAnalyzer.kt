package com.example.passcard.data

import com.example.passcard.ui.screens.PasswordItem
import java.text.Normalizer
import java.util.Locale

/** Local heuristics only: no breach lookup or estimate of time to crack. */
object PasswordHealthAnalyzer {
    const val RECOMMENDED_LENGTH = 15

    private val commonPasswords = setOf(
        "password", "passw0rd", "passwd", "pass", "123456", "123456789", "12345678",
        "1234567890", "12345", "1234", "111111", "000000", "abc123", "abcdef",
        "qwerty", "qwertyuiop", "asdfgh", "zxcvbn", "welcome", "admin", "administrator",
        "letmein", "iloveyou", "login", "changeme", "default", "test", "secret", "monkey",
        "dragon", "football", "sunshine", "princess", "trustno1", "passcard",
        // A publicly shared example is predictable even though it is a long passphrase.
        "correcthorsebatterystaple"
    )
    private val orderedSequences = listOf(
        "abcdefghijklmnopqrstuvwxyz", "01234567890", "qwertyuiop", "asdfghjkl", "zxcvbnm"
    ).flatMap { listOf(it, it.reversed()) }

    fun assessPassword(password: String): PasswordAssessment {
        if (password.isEmpty()) return PasswordAssessment(hasPassword = false)
        val folded = Normalizer.normalize(password, Normalizer.Form.NFKC).lowercase(Locale.ROOT)
        val reasons = buildList {
            if (password.codePointCount(0, password.length) < RECOMMENDED_LENGTH) add(PasswordWeakReason.TOO_SHORT)
            if (isCommonPassword(folded)) add(PasswordWeakReason.COMMON_PASSWORD)
            if (isRepeatedPattern(folded)) add(PasswordWeakReason.REPEATED_PATTERN)
            if (isMostlyPredictableSequence(folded)) add(PasswordWeakReason.PREDICTABLE_SEQUENCE)
        }
        return PasswordAssessment(hasPassword = true, reasons = reasons)
    }

    fun analyze(items: List<PasswordItem>): PasswordHealthSnapshot {
        val orderedItems = items.sortedWith(compareByDescending<PasswordItem> { it.updatedAt }.thenBy { it.id })
        val assessments = LinkedHashMap<String, PasswordAssessment>(orderedItems.size)
        val weakItems = ArrayList<PasswordItem>()
        val passwords = LinkedHashMap<String, MutableList<PasswordItem>>()
        val assessmentCache = HashMap<String, PasswordAssessment>()
        var passwordlessCount = 0
        orderedItems.forEach { item ->
            val assessment = assessmentCache.getOrPut(item.password) { assessPassword(item.password) }
            assessments[item.id] = assessment
            if (!assessment.hasPassword) {
                passwordlessCount++
            } else {
                if (assessment.isWeak) weakItems.add(item)
                passwords.getOrPut(item.password) { ArrayList() }.add(item)
            }
        }
        val reused = passwords.values.filter { it.size > 1 }.map { ReusedPasswordGroup(it.toList()) }
            .sortedWith(compareByDescending<ReusedPasswordGroup> { it.count }.thenBy { it.items.first().id })
        return PasswordHealthSnapshot(
            stats = PasswordSecurityStats(
                totalCount = orderedItems.size,
                weakCount = weakItems.size,
                reusedCount = reused.sumOf { it.count },
                passwordlessCount = passwordlessCount
            ),
            weakPasswords = weakItems,
            reusedPasswordGroups = reused,
            assessmentsById = assessments
        )
    }

    private fun isCommonPassword(folded: String): Boolean {
        val compact = folded.filterNot { it.isWhitespace() }
        if (compact in commonPasswords || compact.filter { it.isLetterOrDigit() } in commonPasswords) return true
        // Keep a second candidate so leading familiar substitutions such as @dm1n are not stripped.
        return listOf(compact.trim { !it.isLetter() }, compact.trimEnd { !it.isLetter() }).any { candidate ->
            val substituted = buildString(candidate.length) {
                candidate.forEach { char -> append(when (char) {
                    '@', '4' -> 'a'
                    '0' -> 'o'
                    '3' -> 'e'
                    '$', '5' -> 's'
                    '1', '!' -> 'i'
                    '7' -> 't'
                    else -> char
                }) }
            }.trim { !it.isLetter() }
            candidate in commonPasswords || substituted in commonPasswords
        }
    }

    private fun isRepeatedPattern(value: String): Boolean {
        if (value.isBlank()) return true
        if (value.length < 4) return false
        val occurrences = value.groupingBy { it }.eachCount()
        if ((occurrences.values.maxOrNull() ?: 0).toDouble() / value.length >= 0.75) return true
        val words = value.trim().split(Regex("\\s+"))
        if (words.size >= 3 && words.all { it == words.first() }) return true
        // Prefix-function keeps the check linear even for very long imported passwords.
        val prefix = IntArray(value.length)
        for (index in 1 until value.length) {
            var matched = prefix[index - 1]
            while (matched > 0 && value[index] != value[matched]) matched = prefix[matched - 1]
            if (value[index] == value[matched]) matched++
            prefix[index] = matched
        }
        val period = value.length - prefix.last()
        return period <= value.length / 2 && value.length % period == 0
    }

    private fun isMostlyPredictableSequence(value: String): Boolean {
        val compact = value.filter { it.isLetterOrDigit() }
        if (compact.length < 4) return false
        var covered = 0
        var index = 0
        while (index < compact.length) {
            var longest = 0
            orderedSequences.forEach { sequence ->
                sequence.indices.forEach { start ->
                    var count = 0
                    while (start + count < sequence.length && index + count < compact.length &&
                        sequence[start + count] == compact[index + count]) count++
                    if (count >= 3 && count > longest) longest = count
                }
            }
            if (longest >= 3) { covered += longest; index += longest } else index++
        }
        return covered.toDouble() / compact.length >= 0.75
    }
}

enum class PasswordWeakReason { TOO_SHORT, COMMON_PASSWORD, REPEATED_PATTERN, PREDICTABLE_SEQUENCE }

data class PasswordAssessment(val hasPassword: Boolean, val reasons: List<PasswordWeakReason> = emptyList()) {
    val isWeak: Boolean get() = hasPassword && reasons.isNotEmpty()
}

data class PasswordHealthSnapshot(
    val stats: PasswordSecurityStats = PasswordSecurityStats(),
    val weakPasswords: List<PasswordItem> = emptyList(),
    val reusedPasswordGroups: List<ReusedPasswordGroup> = emptyList(),
    val assessmentsById: Map<String, PasswordAssessment> = emptyMap()
)
