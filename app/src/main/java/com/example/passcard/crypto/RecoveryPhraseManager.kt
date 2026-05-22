package com.example.passcard.crypto

import android.content.Context
import java.security.SecureRandom
import java.util.Locale
import java.util.concurrent.atomic.AtomicReference

object RecoveryPhraseManager {
    private data class Wordbook(val list: List<String>, val set: Set<String>)

    private val bookRef = AtomicReference<Wordbook?>(null)
    private val random = SecureRandom()

    fun init(context: Context) {
        if (bookRef.get() != null) return
        synchronized(this) {
            if (bookRef.get() != null) return
            val lines = context.applicationContext.assets.open("bip39_english.txt")
                .bufferedReader()
                .use { it.readLines() }
            val list = lines.map { it.trim().lowercase(Locale.US) }.filter { it.isNotEmpty() }
            require(list.size == 2048) { "bip39_english.txt must contain 2048 words" }
            bookRef.set(Wordbook(list, list.toHashSet()))
        }
    }

    private fun book(): Wordbook = bookRef.get() ?: error("RecoveryPhraseManager.init(Context) was not called")

    fun generateWordCount(count: Int = 24): List<String> {
        val w = book().list
        return List(count) { w[random.nextInt(w.size)] }
    }

    fun generatePhrase(count: Int = 24): String = generateWordCount(count).joinToString(" ")

    fun normalize(phrase: String): String = phrase.trim().lowercase(Locale.US).replace(Regex("\\s+"), " ")

    fun isValidFormat(phrase: String, count: Int = 24): Boolean {
        return normalize(phrase).split(" ").filter { it.isNotBlank() }.size == count
    }

    fun isValidWords(phrase: String, count: Int = 24): Boolean {
        val s = book().set
        val normalized = normalize(phrase)
        val list = normalized.split(" ").filter { it.isNotBlank() }
        return list.size == count && list.all { it in s }
    }

    /** 1-based positions，例如 3、8、17 */
    fun wordsAtOneBasedPositions(phrase: String, positions: IntArray): List<String> {
        val list = normalize(phrase).split(" ").filter { it.isNotBlank() }
        return positions.map { idx ->
            require(idx >= 1) { "positions are 1-based" }
            list[idx - 1]
        }
    }
}
