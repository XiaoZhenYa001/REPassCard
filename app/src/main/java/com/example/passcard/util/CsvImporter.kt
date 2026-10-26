package com.example.passcard.util

import android.content.Context
import android.net.Uri
import java.io.InputStreamReader
import java.util.Locale

/**
 * CSV 导入工具
 * 支持格式: 服务,用户名,手机号,邮箱,密码,备注,分类
 */
data class PasswordEntryCsv(
    val service: String,
    val username: String,
    val phone: String,
    val email: String,
    val password: String,
    val note: String,
    val category: String = "",
    val sourceRow: Int,
    val iconType: String = "generated",
    val iconValue: String = ""
)

data class ImportIssue(
    val rowNumber: Int,
    val reason: String,
    val rawRow: String
)

data class ImportParseResult(
    val entries: List<PasswordEntryCsv>,
    val issues: List<ImportIssue>,
    val detectedDelimiter: Char,
    val headerDetected: Boolean,
    val totalRows: Int
)

object CsvImporter {

    private enum class CsvField {
        SERVICE,
        USERNAME,
        PHONE,
        EMAIL,
        PASSWORD,
        NOTE,
        CATEGORY,
        ICON_TYPE,
        ICON_VALUE
    }

    private val serviceHeaders = setOf("服务", "名称", "网站", "站点", "网址", "service", "name", "title", "url", "site", "domain")
    private val usernameHeaders = setOf("用户名", "账号", "账户", "user", "username", "login", "account")
    private val phoneHeaders = setOf("手机", "手机号", "电话", "phone", "tel", "mobile")
    private val emailHeaders = setOf("邮箱", "邮件", "email", "mail")
    private val passwordHeaders = setOf("密码", "口令", "pass", "password", "pwd")
    private val noteHeaders = setOf("备注", "说明", "note", "notes", "comment", "comments")
    private val iconTypeHeaders = setOf("icontype", "图标类型")
    private val iconValueHeaders = setOf("iconvalue", "图标值")
    private val nativeHeaders = listOf("服务", "用户名", "手机号", "邮箱", "密码", "备注", "分类")
    private val categoryHeaders = setOf("分类", "类别", "分组", "标签", "category", "group", "folder", "tag")
    
    /**
     * 从 URI 读取 CSV 文件
     * @param context Android 上下文
     * @param uri 文件 URI
     * @param charset 文件编码，默认 UTF-8
     * @return 解析后的密码条目列表
     */
    fun parseCsv(
        context: Context,
        uri: Uri,
        charset: String = "UTF-8"
    ): Result<ImportParseResult> {
        return try {
            val content = context.contentResolver.openInputStream(uri)?.use { inputStream ->
                InputStreamReader(inputStream, charset).readText()
            } ?: return Result.failure(Exception("无法读取文件"))

            Result.success(parseCsvContent(content))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    internal fun parseCsvContent(rawContent: String): ImportParseResult {
        val content = rawContent.removePrefix("\uFEFF")
        if (content.isBlank()) {
            throw IllegalArgumentException("文件为空")
        }

        val delimiter = detectDelimiter(content)
        val rows = parseCsvRows(content, delimiter)
        if (rows.isEmpty()) {
            throw IllegalArgumentException("文件中没有可解析的数据")
        }

        val headerMap = buildHeaderMap(rows.first())
        val headerDetected = headerMap.containsKey(CsvField.SERVICE) &&
            (headerMap.size > 1 || rows.first().size == 1)
        val preserveFields = headerDetected && rows.first().take(nativeHeaders.size) == nativeHeaders
        val dataRows = if (headerDetected) rows.drop(1) else rows
        val rowOffset = if (headerDetected) 2 else 1

        val issues = mutableListOf<ImportIssue>()
        val entries = mutableListOf<PasswordEntryCsv>()

        dataRows.forEachIndexed { index, row ->
            val rowNumber = rowOffset + index
            if (row.all { it.isBlank() }) {
                return@forEachIndexed
            }

            parseRowToEntry(row, rowNumber, headerMap, headerDetected, preserveFields).onSuccess {
                entries.add(it)
            }.onFailure { error ->
                issues.add(
                    ImportIssue(
                        rowNumber = rowNumber,
                        reason = error.message ?: "未知错误",
                        rawRow = row.joinToString(delimiter.toString())
                    )
                )
            }
        }

        return ImportParseResult(
            entries = entries,
            issues = issues,
            detectedDelimiter = delimiter,
            headerDetected = headerDetected,
            totalRows = dataRows.size
        )
    }
    
    /**
     * 解析 CSV 行
     * 支持格式: 服务,用户名,手机号,邮箱,密码,备注,分类(可选)
     */
    private fun parseRowToEntry(
        fields: List<String>,
        rowNumber: Int,
        headerMap: Map<CsvField, Int>,
        headerDetected: Boolean,
        preserveFields: Boolean
    ): Result<PasswordEntryCsv> {
        return runCatching {
            val service = getField(fields, CsvField.SERVICE, headerMap, 0, headerDetected, preserveFields)
            val username = getField(fields, CsvField.USERNAME, headerMap, 1, headerDetected, preserveFields)
            val phone = getField(fields, CsvField.PHONE, headerMap, 2, headerDetected, preserveFields)
            val email = getField(fields, CsvField.EMAIL, headerMap, 3, headerDetected, preserveFields)
            val password = getField(fields, CsvField.PASSWORD, headerMap, 4, headerDetected, preserveFields)
            val note = getField(fields, CsvField.NOTE, headerMap, 5, headerDetected, preserveFields)
            val category = getField(fields, CsvField.CATEGORY, headerMap, 6, headerDetected, preserveFields)

            if (service.isBlank()) {
                throw IllegalArgumentException("服务名称为空")
            }

            val iconType = getField(fields, CsvField.ICON_TYPE, headerMap, 7, headerDetected, preserveFields)
                .ifBlank { "generated" }
            val iconValue = getField(fields, CsvField.ICON_VALUE, headerMap, 8, headerDetected, preserveFields)

            PasswordEntryCsv(
                service = service,
                username = username,
                phone = phone,
                email = email,
                password = password,
                note = note,
                category = category,
                sourceRow = rowNumber,
                iconType = iconType,
                iconValue = iconValue
            )
        }
    }
    
    /**
     * 解析 CSV 字段
     * 处理带引号和逗号的情况
     */
    private fun parseCsvRows(content: String, delimiter: Char): List<List<String>> {
        val rows = mutableListOf<List<String>>()
        val currentRow = mutableListOf<String>()
        val currentField = StringBuilder()
        var inQuotes = false

        var i = 0
        while (i < content.length) {
            val char = content[i]
            when {
                char == '"' -> {
                    if (inQuotes && i + 1 < content.length && content[i + 1] == '"') {
                        currentField.append('"')
                        i++
                    } else {
                        inQuotes = !inQuotes
                    }
                }
                char == delimiter && !inQuotes -> {
                    currentRow.add(currentField.toString())
                    currentField.setLength(0)
                }
                (char == '\n' || char == '\r') && !inQuotes -> {
                    if (char == '\r' && i + 1 < content.length && content[i + 1] == '\n') {
                        i++
                    }
                    currentRow.add(currentField.toString())
                    currentField.setLength(0)
                    if (currentRow.any { it.isNotBlank() }) {
                        rows.add(currentRow.toList())
                    }
                    currentRow.clear()
                }
                else -> {
                    currentField.append(char)
                }
            }
            i++
        }

        if (currentField.isNotEmpty() || currentRow.isNotEmpty()) {
            currentRow.add(currentField.toString())
            if (currentRow.any { it.isNotBlank() }) {
                rows.add(currentRow.toList())
            }
        }

        return rows
    }

    private fun detectDelimiter(content: String): Char {
        val firstLine = content.lineSequence().firstOrNull { it.isNotBlank() } ?: return ','
        val candidates = listOf(',', ';', '\t')
        return candidates.maxByOrNull { candidate ->
            countDelimiterOutsideQuotes(firstLine, candidate)
        } ?: ','
    }

    private fun countDelimiterOutsideQuotes(line: String, delimiter: Char): Int {
        var inQuotes = false
        var count = 0
        var i = 0
        while (i < line.length) {
            val char = line[i]
            if (char == '"') {
                if (inQuotes && i + 1 < line.length && line[i + 1] == '"') {
                    i++
                } else {
                    inQuotes = !inQuotes
                }
            } else if (char == delimiter && !inQuotes) {
                count++
            }
            i++
        }
        return count
    }

    private fun buildHeaderMap(headers: List<String>): Map<CsvField, Int> {
        val map = mutableMapOf<CsvField, Int>()
        headers.forEachIndexed { index, header ->
            val key = normalizeHeader(header)
            when {
                key in serviceHeaders && CsvField.SERVICE !in map -> map[CsvField.SERVICE] = index
                key in usernameHeaders && CsvField.USERNAME !in map -> map[CsvField.USERNAME] = index
                key in phoneHeaders && CsvField.PHONE !in map -> map[CsvField.PHONE] = index
                key in emailHeaders && CsvField.EMAIL !in map -> map[CsvField.EMAIL] = index
                key in passwordHeaders && CsvField.PASSWORD !in map -> map[CsvField.PASSWORD] = index
                key in noteHeaders && CsvField.NOTE !in map -> map[CsvField.NOTE] = index
                key in categoryHeaders && CsvField.CATEGORY !in map -> map[CsvField.CATEGORY] = index
                key in iconTypeHeaders && CsvField.ICON_TYPE !in map -> map[CsvField.ICON_TYPE] = index
                key in iconValueHeaders && CsvField.ICON_VALUE !in map -> map[CsvField.ICON_VALUE] = index
            }
        }
        return map
    }

    private fun normalizeHeader(value: String): String {
        return value
            .trim()
            .lowercase(Locale.ROOT)
            .replace("_", "")
            .replace("-", "")
            .replace(" ", "")
    }

    private fun getField(
        fields: List<String>,
        target: CsvField,
        headerMap: Map<CsvField, Int>,
        fallbackIndex: Int,
        headerDetected: Boolean,
        preserveFields: Boolean
    ): String {
        val index = if (headerDetected) {
            headerMap[target] ?: return ""
        } else {
            fallbackIndex
        }
        val value = fields.getOrNull(index).orEmpty()
        return if (target == CsvField.PASSWORD || preserveFields) value else value.trim()
    }
    
    /**
     * 获取导入统计信息
     */
    fun getImportSummary(entries: List<PasswordEntryCsv>): ImportSummary {
        return ImportSummary(
            total = entries.size,
            withEmail = entries.count { it.email.isNotBlank() },
            withPhone = entries.count { it.phone.isNotBlank() },
            withNote = entries.count { it.note.isNotBlank() },
            withCategory = entries.count { it.category.isNotBlank() }
        )
    }
    
    data class ImportSummary(
        val total: Int,
        val withEmail: Int,
        val withPhone: Int,
        val withNote: Int,
        val withCategory: Int = 0
    )
}
