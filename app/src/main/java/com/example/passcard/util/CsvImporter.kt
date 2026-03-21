package com.example.passcard.util

import android.content.Context
import android.net.Uri
import java.io.BufferedReader
import java.io.InputStreamReader

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
    val category: String = ""
)

object CsvImporter {
    
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
    ): Result<List<PasswordEntryCsv>> {
        return try {
            val entries = mutableListOf<PasswordEntryCsv>()
            
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                BufferedReader(InputStreamReader(inputStream, charset)).use { reader ->
                    // 跳过 BOM 和标题行
                    var firstLine = reader.readLine() ?: return Result.failure(Exception("文件为空"))
                    
                    // 跳过 UTF-8 BOM
                    if (firstLine.startsWith("\uFEFF")) {
                        firstLine = firstLine.substring(1)
                    }
                    
                    // 如果第一行是标题，跳过它
                    val isFirstLineHeader = firstLine.contains("服务") || 
                                            firstLine.contains("service") ||
                                            firstLine.contains("名称") ||
                                            firstLine.contains("name") ||
                                            firstLine.contains("category") ||
                                            firstLine.contains("分类")
                    
                    if (!isFirstLineHeader) {
                        parseLine(firstLine)?.let { entries.add(it) }
                    }
                    
                    // 解析剩余行
                    reader.forEachLine { line ->
                        if (line.isNotBlank()) {
                            parseLine(line)?.let { entries.add(it) }
                        }
                    }
                }
            }
            
            Result.success(entries)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * 解析 CSV 行
     * 支持格式: 服务,用户名,手机号,邮箱,密码,备注,分类(可选)
     */
    private fun parseLine(line: String): PasswordEntryCsv? {
        val fields = parseCsvFields(line)
        
        if (fields.size < 5) {
            return null
        }
        
        return PasswordEntryCsv(
            service = fields.getOrNull(0)?.trim() ?: "",
            username = fields.getOrNull(1)?.trim() ?: "",
            phone = fields.getOrNull(2)?.trim() ?: "",
            email = fields.getOrNull(3)?.trim() ?: "",
            password = fields.getOrNull(4)?.trim() ?: "",
            note = fields.getOrNull(5)?.trim() ?: "",
            category = fields.getOrNull(6)?.trim() ?: ""  // 分类字段，可选
        )
    }
    
    /**
     * 解析 CSV 字段
     * 处理带引号和逗号的情况
     */
    private fun parseCsvFields(line: String): List<String> {
        val fields = mutableListOf<String>()
        var current = StringBuilder()
        var inQuotes = false
        
        for (char in line) {
            when {
                char == '"' -> {
                    inQuotes = !inQuotes
                }
                char == ',' && !inQuotes -> {
                    fields.add(current.toString())
                    current = StringBuilder()
                }
                else -> {
                    current.append(char)
                }
            }
        }
        fields.add(current.toString())
        
        return fields
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
