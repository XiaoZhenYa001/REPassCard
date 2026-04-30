package com.example.passcard.util

import android.content.Context
import android.net.Uri
import org.json.JSONArray
import org.json.JSONObject
import java.io.InputStreamReader
import java.util.Locale

/**
 * JSON 导入工具
 * 支持智能字段匹配，兼容 REPassCard / Bitwarden / Chrome / 1Password / LastPass 等格式
 */
object JsonImporter {

    // ---- 字段名映射（全部小写匹配） ----
    private val serviceKeys = setOf(
        "service", "name", "title", "site", "url", "domain", "hostname",
        "服务", "名称", "网站", "站点", "网址"
    )
    private val usernameKeys = setOf(
        "username", "user", "login", "account", "login_username",
        "用户名", "账号", "账户"
    )
    private val phoneKeys = setOf(
        "phone", "tel", "mobile", "telephone",
        "手机", "手机号", "电话"
    )
    private val emailKeys = setOf(
        "email", "mail", "e-mail",
        "邮箱", "邮件"
    )
    private val passwordKeys = setOf(
        "password", "pass", "pwd", "login_password",
        "密码", "口令"
    )
    private val noteKeys = setOf(
        "note", "notes", "comment", "comments", "extra", "memo",
        "备注", "说明"
    )
    private val categoryKeys = setOf(
        "category", "group", "folder", "tag", "type", "grouping",
        "分类", "类别", "分组", "标签"
    )
    private val urlKeys = setOf(
        "url", "uri", "website", "href", "login_uri",
        "网址", "链接"
    )

    /**
     * 从 URI 读取并解析 JSON 文件
     */
    fun parseJson(
        context: Context,
        uri: Uri,
        charset: String = "UTF-8"
    ): Result<ImportParseResult> {
        return try {
            val content = context.contentResolver.openInputStream(uri)?.use { inputStream ->
                InputStreamReader(inputStream, charset).readText()
            } ?: return Result.failure(Exception("无法读取文件"))

            Result.success(parseJsonContent(content))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * 解析 JSON 字符串内容
     */
    fun parseJsonContent(content: String): ImportParseResult {
        val trimmed = content.trim().removePrefix("\uFEFF")
        if (trimmed.isBlank()) {
            throw IllegalArgumentException("文件为空")
        }

        val items: JSONArray = when {
            // REPassCard 格式: { "passwords": [...] }
            trimmed.startsWith("{") -> {
                val root = JSONObject(trimmed)
                extractPasswordArray(root)
            }
            // 纯数组格式: [...]
            trimmed.startsWith("[") -> {
                JSONArray(trimmed)
            }
            else -> throw IllegalArgumentException("不是有效的 JSON 格式")
        }

        val entries = mutableListOf<PasswordEntryCsv>()
        val issues = mutableListOf<ImportIssue>()

        for (i in 0 until items.length()) {
            val rowNumber = i + 1
            try {
                val obj = items.getJSONObject(i)
                val entry = mapJsonObjectToEntry(obj, rowNumber)
                if (entry.service.isBlank()) {
                    issues.add(ImportIssue(rowNumber, "服务名称为空", obj.toString()))
                    continue
                }
                if (entry.password.isBlank()) {
                    issues.add(ImportIssue(rowNumber, "密码为空", obj.toString()))
                    continue
                }
                entries.add(entry)
            } catch (e: Exception) {
                issues.add(
                    ImportIssue(
                        rowNumber = rowNumber,
                        reason = e.message ?: "JSON 条目解析失败",
                        rawRow = try { items.get(i).toString() } catch (_: Exception) { "" }
                    )
                )
            }
        }

        return ImportParseResult(
            entries = entries,
            issues = issues,
            detectedDelimiter = ' ',  // JSON 无分隔符，用占位
            headerDetected = true,
            totalRows = items.length()
        )
    }

    /**
     * 从 JSON 根对象中提取密码数组
     * 支持多种结构：
     * - REPassCard: { "passwords": [...] }
     * - Bitwarden: { "items": [...] }
     * - LastPass: { "accounts": [...] }
     * - 通用: { "entries": [...] }, { "data": [...] }, { "records": [...] }
     */
    private fun extractPasswordArray(root: JSONObject): JSONArray {
        val candidateKeys = listOf(
            "passwords", "items", "accounts", "entries",
            "data", "records", "logins", "credentials"
        )
        for (key in candidateKeys) {
            if (root.has(key)) {
                val value = root.get(key)
                if (value is JSONArray) return value
            }
        }
        // 如果没找到已知的数组字段，尝试找第一个数组类型的值
        val keys = root.keys()
        while (keys.hasNext()) {
            val key = keys.next()
            val value = root.opt(key)
            if (value is JSONArray && value.length() > 0) {
                // 检查数组元素是否为对象
                val first = value.opt(0)
                if (first is JSONObject) return value
            }
        }
        throw IllegalArgumentException("JSON 中未找到密码数据数组")
    }

    /**
     * 将 JSON 对象映射为密码条目
     * 使用智能字段名匹配
     */
    private fun mapJsonObjectToEntry(obj: JSONObject, rowNumber: Int): PasswordEntryCsv {
        // 扁平化嵌套结构（如 Bitwarden 的 login.username）
        val flatMap = flattenJsonObject(obj)

        val service = findField(flatMap, serviceKeys)
            .ifBlank { findField(flatMap, urlKeys) }  // fallback: 用 URL 作为服务名
        val username = findField(flatMap, usernameKeys)
        val phone = findField(flatMap, phoneKeys)
        val email = findField(flatMap, emailKeys)
        val password = findField(flatMap, passwordKeys)
        val note = findField(flatMap, noteKeys)
        val category = findField(flatMap, categoryKeys)

        return PasswordEntryCsv(
            service = service,
            username = username,
            phone = phone,
            email = email,
            password = password,
            note = note,
            category = category,
            sourceRow = rowNumber
        )
    }

    /**
     * 将嵌套的 JSON 对象扁平化为 key-value Map
     * 例如 {"login": {"username": "foo"}} → {"login_username": "foo", "username": "foo"}
     */
    private fun flattenJsonObject(obj: JSONObject, prefix: String = ""): Map<String, String> {
        val map = mutableMapOf<String, String>()
        val keys = obj.keys()
        while (keys.hasNext()) {
            val key = keys.next()
            val value = obj.opt(key) ?: continue
            val fullKey = if (prefix.isEmpty()) key else "${prefix}_$key"

            when (value) {
                is JSONObject -> {
                    map.putAll(flattenJsonObject(value, fullKey))
                    // 也把嵌套的字段直接加到顶层（不带前缀），便于匹配
                    flattenJsonObject(value, "").forEach { (k, v) ->
                        if (k !in map) map[k] = v
                    }
                }
                is JSONArray -> {
                    // 处理 Bitwarden 的 uris 数组
                    if (value.length() > 0) {
                        val first = value.opt(0)
                        if (first is JSONObject) {
                            // 取第一个对象的值
                            val nested = flattenJsonObject(first, fullKey)
                            map.putAll(nested)
                            flattenJsonObject(first, "").forEach { (k, v) ->
                                if (k !in map) map[k] = v
                            }
                        } else if (first is String) {
                            map[fullKey] = first
                            map[key] = first
                        }
                    }
                }
                else -> {
                    val strVal = value.toString()
                    if (strVal != "null") {
                        map[fullKey] = strVal
                        if (prefix.isNotEmpty()) {
                            if (key !in map) map[key] = strVal
                        }
                    }
                }
            }
        }
        return map
    }

    /**
     * 在扁平化的 Map 中查找匹配的字段值
     */
    private fun findField(flatMap: Map<String, String>, candidates: Set<String>): String {
        // 先精确匹配（小写）
        for (candidate in candidates) {
            flatMap.entries.firstOrNull { it.key.lowercase(Locale.ROOT) == candidate }
                ?.let { return it.value }
        }
        // 再模糊匹配（包含）
        for (candidate in candidates) {
            flatMap.entries.firstOrNull { it.key.lowercase(Locale.ROOT).contains(candidate) }
                ?.let { return it.value }
        }
        return ""
    }
}

/**
 * 文件格式自动检测工具
 */
object FileFormatDetector {

    enum class FileFormat { CSV, JSON }

    /**
     * 根据文件内容自动检测格式
     */
    fun detect(content: String): FileFormat {
        val trimmed = content.trim().removePrefix("\uFEFF")
        return if (trimmed.startsWith("{") || trimmed.startsWith("[")) {
            FileFormat.JSON
        } else {
            FileFormat.CSV
        }
    }

    /**
     * 从 URI 读取内容并检测格式
     */
    fun detectFromUri(context: android.content.Context, uri: android.net.Uri): Pair<FileFormat, String> {
        val content = context.contentResolver.openInputStream(uri)?.use { inputStream ->
            InputStreamReader(inputStream, "UTF-8").readText()
        } ?: throw IllegalArgumentException("无法读取文件")
        return Pair(detect(content), content)
    }
}
