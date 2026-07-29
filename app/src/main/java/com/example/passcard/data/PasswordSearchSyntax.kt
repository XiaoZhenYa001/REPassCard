package com.example.passcard.data

data class PasswordSearch(
    val keyword: String,
    val field: PasswordSearchField? = null
) {
    private val escapedKeyword = keyword
        .replace("\\", "\\\\")
        .replace("%", "\\%")
        .replace("_", "\\_")

    val prefix: String = "$escapedKeyword%"
    val contains: String = "%$escapedKeyword%"
}

enum class PasswordSearchField(val column: String) {
    NAME("name"),
    USERNAME("username"),
    PHONE("phone"),
    EMAIL("email"),
    PASSWORD("password"),
    CATEGORY("category"),
    NOTE("note")
}

object PasswordSearchSyntax {
    fun parse(rawQuery: String): PasswordSearch {
        val trimmed = rawQuery.trim()
        val hasTypeCommand = trimmed.startsWith("/t", ignoreCase = true) &&
            (trimmed.length == 2 || trimmed[2].isWhitespace())
        if (!hasTypeCommand) {
            return PasswordSearch(keyword = trimmed)
        }

        val body = trimmed.drop(2).trim()
        val parts = body.split(Regex("\\s+"), limit = 2)
        if (parts.size < 2) {
            return PasswordSearch(keyword = "")
        }

        val field = parseField(parts[0]) ?: return PasswordSearch(keyword = body)
        return PasswordSearch(keyword = parts[1].trim(), field = field)
    }

    private fun parseField(token: String): PasswordSearchField? {
        return when (token.lowercase()) {
            "名称", "名字", "标题", "name", "title" -> PasswordSearchField.NAME
            "用户名", "账号", "账户", "user", "username", "account" -> PasswordSearchField.USERNAME
            "手机号", "手机", "电话", "phone", "mobile", "tel" -> PasswordSearchField.PHONE
            "邮箱", "邮件", "email", "mail" -> PasswordSearchField.EMAIL
            "密码", "password", "pwd" -> PasswordSearchField.PASSWORD
            "分类", "类别", "category", "tag" -> PasswordSearchField.CATEGORY
            "备注", "笔记", "note", "memo", "remark" -> PasswordSearchField.NOTE
            else -> null
        }
    }
}
