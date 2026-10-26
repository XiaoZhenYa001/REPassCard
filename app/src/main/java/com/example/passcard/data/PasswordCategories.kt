package com.example.passcard.data

/** Stable category values shared by filtering and editing. Legacy labels remain readable. */
object PasswordCategories {
    private val common = linkedMapOf(
        "Social Media" to "社交媒体",
        "Work" to "工作",
        "Finance" to "金融",
        "Shopping" to "购物",
        "Entertainment" to "娱乐",
        "AI" to "AI",
        "Gaming" to "游戏",
        "Education" to "教育",
        "Other" to "其他"
    )

    val commonKeys: List<String> = common.keys.toList()

    fun key(value: String): String {
        val trimmed = value.trim()
        return common.entries.firstOrNull {
            it.key.equals(trimmed, ignoreCase = true) || it.value == trimmed
        }?.key ?: trimmed
    }

    fun label(value: String, isChinese: Boolean): String {
        val key = key(value)
        if (key.isEmpty()) return if (isChinese) "未分类" else "Uncategorized"
        if (isChinese && key == "全部") return "全部（分类）"
        if (!isChinese && key == "All") return "All (category)"
        return if (isChinese) common[key] ?: key else key
    }

    fun aliases(value: String?): List<String> {
        if (value == null) return listOf("")
        val key = key(value)
        return listOfNotNull(key, common[key]).distinct()
    }

    fun searchAliases(keyword: String): List<String> = (listOf(keyword) + common.entries
        .filter { it.key.contains(keyword, ignoreCase = true) || it.value.contains(keyword) }
        .flatMap { listOf(it.key, it.value) }).distinct()

    fun availableKeys(values: List<String>): List<String> {
        val keys = values.map(::key).distinctBy(::comparisonKey)
        return commonKeys.filter { it in keys } +
            keys.filter { it.isNotEmpty() && it !in common }.sorted() +
            keys.filter { it.isEmpty() }
    }

    fun matches(value: String, selected: String): Boolean = comparisonKey(value) == comparisonKey(selected)

    // Match SQLite NOCASE, which folds ASCII letters rather than arbitrary Unicode.
    private fun comparisonKey(value: String): String = key(value)
        .map { if (it in 'A'..'Z') it.lowercaseChar() else it }.joinToString("")
}
