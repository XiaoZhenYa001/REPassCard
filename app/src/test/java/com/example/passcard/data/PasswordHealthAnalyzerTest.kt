package com.example.passcard.data

import com.example.passcard.ui.screens.PasswordItem
import org.junit.Assert.*
import org.junit.Test

class PasswordHealthAnalyzerTest {
    @Test fun mixedCharacterSequenceIsStillWeak() {
        val assessment = PasswordHealthAnalyzer.assessPassword("Abcdef1234")
        assertTrue(assessment.isWeak)
        assertTrue(PasswordWeakReason.PREDICTABLE_SEQUENCE in assessment.reasons)
    }

    @Test fun familiarPasswordWithLeetAndSuffixIsWeak() {
        assertTrue(PasswordWeakReason.COMMON_PASSWORD in PasswordHealthAnalyzer.assessPassword("P@ssw0rd2026!").reasons)
        assertTrue(PasswordWeakReason.COMMON_PASSWORD in PasswordHealthAnalyzer.assessPassword("Welcome2026!!!!").reasons)
    }

    @Test fun familiarLeadingSubstitutionsAndLongSuffixesRemainPredictable() {
        assertTrue(PasswordWeakReason.COMMON_PASSWORD in PasswordHealthAnalyzer.assessPassword("@dm1n202620262026!").reasons)
        assertTrue(PasswordWeakReason.COMMON_PASSWORD in PasswordHealthAnalyzer.assessPassword("1L0v3you202620262026!").reasons)
        assertTrue(PasswordWeakReason.COMMON_PASSWORD in PasswordHealthAnalyzer.assessPassword("2026Password20262026!").reasons)
    }

    @Test fun separatorsCannotMakeThePublicExamplePassphraseUnpredictable() {
        assertTrue(PasswordWeakReason.COMMON_PASSWORD in PasswordHealthAnalyzer.assessPassword("correct-horse-battery-staple").reasons)
    }

    @Test fun longRepeatedValueIsStillWeak() {
        assertTrue(PasswordWeakReason.REPEATED_PATTERN in PasswordHealthAnalyzer.assessPassword("abC!abC!abC!abC!abC!").reasons)
        assertTrue(PasswordWeakReason.REPEATED_PATTERN in PasswordHealthAnalyzer.assessPassword("aaaaaaaaaaaaaaaaaaaa").reasons)
    }

    @Test fun aLongRepeatedValueWithASimpleSuffixIsStillWeak() {
        assertTrue(PasswordWeakReason.REPEATED_PATTERN in PasswordHealthAnalyzer.assessPassword("aaaaaaaaaaaaaaaa1!").reasons)
        assertTrue(PasswordWeakReason.REPEATED_PATTERN in PasswordHealthAnalyzer.assessPassword("birch birch birch birch").reasons)
    }

    @Test fun longKeyboardAndAlphabetSequencesAreWeak() {
        assertTrue(PasswordWeakReason.PREDICTABLE_SEQUENCE in PasswordHealthAnalyzer.assessPassword("qwertyuiopasdfghjkl").reasons)
        assertTrue(PasswordWeakReason.PREDICTABLE_SEQUENCE in PasswordHealthAnalyzer.assessPassword("abcdefghijklmnop").reasons)
        assertTrue(PasswordWeakReason.PREDICTABLE_SEQUENCE in PasswordHealthAnalyzer.assessPassword("9876543210987654").reasons)
    }

    @Test fun longPassphraseDoesNotRequireUppercaseOrNumbers() {
        assertFalse(PasswordHealthAnalyzer.assessPassword("velvet sunrise birch copper").isWeak)
        assertFalse(PasswordHealthAnalyzer.assessPassword("灯塔旁的银杏树正在秋风里轻轻摇曳").isWeak)
    }

    @Test fun commonExamplePassphraseIsNotGrantedFullHealth() {
        assertTrue(PasswordWeakReason.COMMON_PASSWORD in PasswordHealthAnalyzer.assessPassword("correct horse battery staple").reasons)
    }

    @Test fun onlyTrulyEmptyPasswordsAreExcluded() {
        val snapshot = PasswordHealthAnalyzer.analyze(listOf(item("a", ""), item("b", "")))
        assertEquals(2, snapshot.stats.passwordlessCount)
        assertEquals(0, snapshot.stats.assessedCount)
        assertEquals(0, snapshot.stats.weakCount)
        assertEquals(0, snapshot.stats.reusedCount)
        assertFalse(snapshot.stats.hasScore)
        assertTrue(snapshot.weakPasswords.isEmpty())
        assertTrue(snapshot.reusedPasswordGroups.isEmpty())
    }

    @Test fun savedWhitespacePasswordsAreWeakAndReusedWithoutBeingTrimmed() {
        val snapshot = PasswordHealthAnalyzer.analyze(listOf(item("a", ""), item("b", "  "), item("c", "  "), item("d", "\t")))
        assertEquals(1, snapshot.stats.passwordlessCount)
        assertEquals(3, snapshot.stats.assessedCount)
        assertEquals(3, snapshot.stats.weakCount)
        assertEquals(2, snapshot.stats.reusedCount)
        assertTrue(snapshot.stats.hasScore)
        assertEquals("  ", snapshot.reusedPasswordGroups.single().label)
        assertTrue(PasswordWeakReason.REPEATED_PATTERN in PasswordHealthAnalyzer.assessPassword(" ".repeat(30)).reasons)
    }

    @Test fun emptyVaultHasNoScore() {
        val snapshot = PasswordHealthAnalyzer.analyze(emptyList())
        assertFalse(snapshot.stats.hasScore)
        assertEquals(0, snapshot.stats.score)
    }

    @Test fun passwordlessRecordsDoNotDilutePenalties() {
        val items = (1..30).map { item("blank$it", "") } + item("weak", "short")
        val snapshot = PasswordHealthAnalyzer.analyze(items)
        assertEquals(31, snapshot.stats.totalCount)
        assertEquals(30, snapshot.stats.passwordlessCount)
        assertEquals(1, snapshot.stats.assessedCount)
        assertEquals(45, snapshot.stats.score)
    }

    @Test fun countsListsAndGroupsComeFromTheSameSnapshot() {
        val snapshot = PasswordHealthAnalyzer.analyze(listOf(item("a", "Abcdef1234"), item("b", "Abcdef1234"), item("c", "velvet sunrise birch copper"), item("d", "")))
        assertEquals(snapshot.weakPasswords.size, snapshot.stats.weakCount)
        assertEquals(snapshot.reusedPasswordGroups.sumOf { it.count }, snapshot.stats.reusedCount)
        assertEquals(setOf("a", "b"), snapshot.weakPasswords.map { it.id }.toSet())
        assertEquals(setOf("a", "b", "c", "d"), snapshot.assessmentsById.keys)
        assertEquals(4, snapshot.stats.totalCount)
        assertEquals(3, snapshot.stats.assessedCount)
    }

    @Test fun passwordsAreComparedExactlyWithoutTrimmingOrCaseFolding() {
        val snapshot = PasswordHealthAnalyzer.analyze(listOf(item("a", "velvet sunrise birch copper"), item("b", " velvet sunrise birch copper "), item("c", "Velvet sunrise birch copper")))
        assertEquals(0, snapshot.stats.reusedCount)
        assertTrue(snapshot.reusedPasswordGroups.isEmpty())
    }

    @Test fun removingAnIssueRecomputesAllDerivedValues() {
        val before = PasswordHealthAnalyzer.analyze(listOf(item("a", "short"), item("b", "short")))
        val after = PasswordHealthAnalyzer.analyze(listOf(item("a", "short"), item("b", "velvet sunrise birch copper")))
        assertEquals(2, before.stats.reusedCount)
        assertEquals(0, after.stats.reusedCount)
        assertEquals(1, after.weakPasswords.size)
        assertEquals(1, after.stats.weakCount)
    }

    @Test fun unicodeLengthCountsCharactersRatherThanUtf16Units() {
        val assessment = PasswordHealthAnalyzer.assessPassword("😀😃😄😁😆😅😂🤣😊")
        assertTrue(PasswordWeakReason.TOO_SHORT in assessment.reasons)
    }

    @Test fun fullWidthCommonPasswordCannotBypassTheLocalCheck() {
        assertTrue(PasswordWeakReason.COMMON_PASSWORD in PasswordHealthAnalyzer.assessPassword("Ｐａｓｓｗｏｒｄ１２３！").reasons)
    }

    @Test fun aSmallSequenceInsideALongPassphraseIsNotAutomaticallyWeak() {
        assertFalse(PasswordHealthAnalyzer.assessPassword("birch velvet 1234 river copper").isWeak)
    }

    @Test fun resultsAreStableAcrossInputOrdering() {
        val a = item("a", "shared birch sunrise password", updatedAt = 1)
        val b = item("b", "shared birch sunrise password", updatedAt = 2)
        val first = PasswordHealthAnalyzer.analyze(listOf(a, b))
        val second = PasswordHealthAnalyzer.analyze(listOf(b, a))
        assertEquals(first.stats, second.stats)
        assertEquals(first.reusedPasswordGroups, second.reusedPasswordGroups)
    }

    @Test fun largeVaultKeepsTotalsAndReuseGroupsConsistent() {
        val snapshot = PasswordHealthAnalyzer.analyze((1..4_000).map { item("item$it", if (it % 2 == 0) "" else "velvet sunrise birch copper") })
        assertEquals(4_000, snapshot.stats.totalCount)
        assertEquals(2_000, snapshot.stats.passwordlessCount)
        assertEquals(2_000, snapshot.stats.assessedCount)
        assertEquals(0, snapshot.stats.weakCount)
        assertEquals(2_000, snapshot.stats.reusedCount)
        assertEquals(1, snapshot.reusedPasswordGroups.size)
        assertEquals(2_000, snapshot.reusedPasswordGroups.single().count)
    }

    private fun item(id: String, password: String, updatedAt: Long = 1) = PasswordItem(id = id, name = id, username = "", password = password, updatedAt = updatedAt)
}
