package com.example.passcard.util

/** Exact user-visible content, excluding generated IDs and local sync timestamps. */
data class ImportRecordContent(
    val service: String,
    val username: String,
    val phone: String = "",
    val email: String = "",
    val password: String,
    val note: String = "",
    val category: String = "",
    val iconType: String = "generated",
    val iconValue: String = ""
)

object ImportDeduplication {
    /** Only skip identical records; different account or secret data must remain importable. */
    fun duplicateFlags(
        existing: Iterable<ImportRecordContent>,
        incoming: Iterable<ImportRecordContent>
    ): List<Boolean> {
        val seen = existing.toMutableSet()
        return incoming.map { !seen.add(it) }
    }
}
