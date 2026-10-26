package com.example.passcard.ui.screens

import com.example.passcard.util.ImportParseResult
import java.util.Locale

private fun formatImportDuration(durationMillis: Long): String {
    val seconds = durationMillis.coerceAtLeast(1) / 1000.0
    return String.format(Locale.ROOT, "%.1fs", seconds)
}

fun buildParseReceipt(
    parseResult: ImportParseResult,
    duplicateCount: Int,
    fileName: String,
    durationMillis: Long,
    language: AppLanguage
): ImportReceiptUi {
    val isZh = language == AppLanguage.CHINESE
    val validCount = parseResult.entries.size
    val issueCount = parseResult.issues.size
    val hasRisk = issueCount > 0 || duplicateCount > 0
    val level = when {
        validCount == 0 -> ImportReceiptLevel.ERROR
        hasRisk -> ImportReceiptLevel.WARNING
        else -> ImportReceiptLevel.SUCCESS
    }

    val feed = mutableListOf<ImportReceiptFeedItem>()
    feed.add(
        ImportReceiptFeedItem(
            title = if (isZh) "来源文件" else "Source File",
            description = if (isZh) {
                "已读取 $fileName，分隔符识别为 '${parseResult.detectedDelimiter}'"
            } else {
                "Loaded $fileName. Detected delimiter '${parseResult.detectedDelimiter}'."
            },
            tag = if (isZh) "${parseResult.totalRows} 行" else "${parseResult.totalRows} rows",
            tone = ImportReceiptFeedTone.INFO
        )
    )
    if (duplicateCount > 0) {
        feed.add(
            ImportReceiptFeedItem(
                title = if (isZh) "发现重复项" else "Duplicates Found",
                description = if (isZh) {
                    "$duplicateCount 条记录的全部内容与保险库或本批次完全相同，默认跳过；内容不同的记录仍可导入。"
                } else {
                    "$duplicateCount records have identical content in the vault or this batch and are skipped by default. Records with different content remain importable."
                },
                tag = if (isZh) "重复" else "duplicate",
                tone = ImportReceiptFeedTone.WARNING
            )
        )
    }
    if (issueCount > 0) {
        feed.add(
            ImportReceiptFeedItem(
                title = if (isZh) "发现格式问题" else "Format Issues",
                description = if (isZh) {
                    "有 $issueCount 行格式不完整，已标记到异常列表。"
                } else {
                    "$issueCount rows are incomplete and have been marked as issues."
                },
                tag = if (isZh) "待修复" else "fix",
                tone = ImportReceiptFeedTone.ERROR
            )
        )
    }
    if (!hasRisk && validCount > 0) {
        feed.add(
            ImportReceiptFeedItem(
                title = if (isZh) "解析完成" else "Ready",
                description = if (isZh) "字段校验通过，可以开始导入。" else "Validation passed. Ready to import.",
                tag = if (isZh) "就绪" else "ready",
                tone = ImportReceiptFeedTone.SUCCESS
            )
        )
    }

    val statusLabel: String
    val title: String
    val description: String
    val primaryAction: ImportReceiptActionType
    val primaryActionText: String
    val secondaryAction: ImportReceiptActionType
    val secondaryActionText: String

    when (level) {
        ImportReceiptLevel.SUCCESS -> {
            statusLabel = if (isZh) "全部就绪" else "All Set"
            title = if (isZh) "$validCount 条记录可直接导入" else "$validCount records are ready to import"
            description = if (isZh) "没有发现重复或格式异常。" else "No duplicates or format errors found."
            primaryAction = ImportReceiptActionType.START_IMPORT
            primaryActionText = if (isZh) "开始导入" else "Import Now"
            secondaryAction = ImportReceiptActionType.CLOSE_PREVIEW
            secondaryActionText = if (isZh) "稍后处理" else "Later"
        }
        ImportReceiptLevel.WARNING -> {
            statusLabel = if (isZh) "部分待处理" else "Needs Attention"
            title = if (isZh) "$validCount 条可导入，存在风险项" else "$validCount importable records with potential risks"
            description = if (isZh) "建议先查看异常和重复记录，再执行导入。" else "Review duplicates/issues before importing."
            primaryAction = ImportReceiptActionType.START_IMPORT
            primaryActionText = if (isZh) "仅导入可用项" else "Import Valid Only"
            secondaryAction = ImportReceiptActionType.SHOW_ISSUES
            secondaryActionText = if (isZh) "查看明细" else "View Details"
        }
        ImportReceiptLevel.ERROR -> {
            statusLabel = if (isZh) "导入失败" else "Import Failed"
            title = if (isZh) "当前文件无法完成导入" else "Cannot import this file"
            description = if (isZh) "请修复编码或字段格式后重试。" else "Fix encoding or field format, then retry."
            primaryAction = ImportReceiptActionType.PICK_FILE
            primaryActionText = if (isZh) "重新选择文件" else "Pick Another File"
            secondaryAction = ImportReceiptActionType.SHOW_ISSUES
            secondaryActionText = if (isZh) "查看原因" else "See Why"
        }
    }

    return ImportReceiptUi(
        level = level,
        statusLabel = statusLabel,
        title = title,
        description = description,
        primaryValue = validCount.toString(),
        primaryLabel = if (isZh) "可导入" else "Importable",
        secondaryValue = (issueCount + duplicateCount).toString(),
        secondaryLabel = if (isZh) "待处理" else "Pending",
        durationText = formatImportDuration(durationMillis),
        primaryActionText = primaryActionText,
        secondaryActionText = secondaryActionText,
        primaryAction = primaryAction,
        secondaryAction = secondaryAction,
        feedItems = feed
    )
}

fun buildParseFailureReceipt(reason: String, language: AppLanguage): ImportReceiptUi {
    val isZh = language == AppLanguage.CHINESE
    return ImportReceiptUi(
        level = ImportReceiptLevel.ERROR,
        statusLabel = if (isZh) "导入失败" else "Import Failed",
        title = if (isZh) "无法解析当前文件" else "Failed to parse file",
        description = reason,
        primaryValue = "0",
        primaryLabel = if (isZh) "可导入" else "Importable",
        secondaryValue = "-",
        secondaryLabel = if (isZh) "待处理" else "Pending",
        durationText = "0.0s",
        primaryActionText = if (isZh) "重新选择文件" else "Pick Another File",
        secondaryActionText = if (isZh) "查看原因" else "See Why",
        primaryAction = ImportReceiptActionType.PICK_FILE,
        secondaryAction = ImportReceiptActionType.SHOW_ISSUES,
        feedItems = listOf(
            ImportReceiptFeedItem(
                title = if (isZh) "建议" else "Suggestion",
                description = if (isZh) {
                    "请确认文件为 UTF-8 编码并包含服务、用户名、密码等字段。"
                } else {
                    "Make sure the file is UTF-8 encoded and includes service, username, and password fields."
                },
                tag = if (isZh) "修复" else "fix",
                tone = ImportReceiptFeedTone.WARNING
            )
        )
    )
}

fun buildNoSelectionReceipt(language: AppLanguage): ImportReceiptUi {
    val isZh = language == AppLanguage.CHINESE
    return ImportReceiptUi(
        level = ImportReceiptLevel.WARNING,
        statusLabel = if (isZh) "尚未选择" else "No Selection",
        title = if (isZh) "请先勾选至少一条记录" else "Select at least one record",
        description = if (isZh) "可以逐条选择后再导入，避免误操作。" else "Select specific records before import.",
        primaryValue = "0",
        primaryLabel = if (isZh) "已选中" else "Selected",
        secondaryValue = "-",
        secondaryLabel = if (isZh) "待导入" else "To Import",
        durationText = "0.0s",
        primaryActionText = if (isZh) "查看列表" else "Back to List",
        secondaryActionText = if (isZh) "关闭" else "Close",
        primaryAction = ImportReceiptActionType.SHOW_ISSUES,
        secondaryAction = ImportReceiptActionType.CLOSE_PREVIEW,
        feedItems = listOf(
            ImportReceiptFeedItem(
                title = if (isZh) "提示" else "Tip",
                description = if (isZh) {
                    "支持全选或取消选择，也可仅导入需要的条目。"
                } else {
                    "Use select all, unselect all, or only import what you need."
                },
                tag = if (isZh) "操作建议" else "hint",
                tone = ImportReceiptFeedTone.INFO
            )
        )
    )
}

fun buildImportDoneReceipt(
    importedCount: Int,
    duplicateSkipped: Int,
    parseIssueCount: Int,
    selectedCount: Int,
    durationMillis: Long,
    language: AppLanguage
): ImportReceiptUi {
    val isZh = language == AppLanguage.CHINESE
    if (importedCount == 0 && selectedCount > 0 && duplicateSkipped == selectedCount) {
        return buildAlreadyPresentReceipt(duplicateSkipped, parseIssueCount, durationMillis, language)
    }
    val unresolved = duplicateSkipped + parseIssueCount
    val level = when {
        importedCount == 0 -> ImportReceiptLevel.ERROR
        unresolved > 0 -> ImportReceiptLevel.WARNING
        else -> ImportReceiptLevel.SUCCESS
    }

    val feed = mutableListOf<ImportReceiptFeedItem>()
    feed.add(
        ImportReceiptFeedItem(
            title = if (isZh) "导入结果" else "Import Result",
            description = if (isZh) {
                "本次选择 $selectedCount 条，成功写入 $importedCount 条。"
            } else {
                "$importedCount out of $selectedCount selected records were imported."
            },
            tag = "$importedCount/$selectedCount",
            tone = if (importedCount > 0) ImportReceiptFeedTone.SUCCESS else ImportReceiptFeedTone.ERROR
        )
    )
    if (duplicateSkipped > 0) {
        feed.add(
            ImportReceiptFeedItem(
                title = if (isZh) "重复项已跳过" else "Duplicates Skipped",
                description = if (isZh) {
                    "已跳过 $duplicateSkipped 条内容完全相同的记录；不同账户、密码或备注的记录会保留。"
                } else {
                    "$duplicateSkipped identical records were skipped. Records with different accounts, passwords, or notes are kept."
                },
                tag = if (isZh) "跳过" else "skip",
                tone = ImportReceiptFeedTone.WARNING
            )
        )
    }
    if (parseIssueCount > 0) {
        feed.add(
            ImportReceiptFeedItem(
                title = if (isZh) "存在异常行" else "Invalid Rows",
                description = if (isZh) {
                    "$parseIssueCount 行未通过格式校验，建议修复后重新导入。"
                } else {
                    "$parseIssueCount rows failed validation. Please fix and retry."
                },
                tag = if (isZh) "异常" else "invalid",
                tone = ImportReceiptFeedTone.ERROR
            )
        )
    }
    feed.add(
        ImportReceiptFeedItem(
            title = if (isZh) "安全建议" else "Security Advice",
            description = if (isZh) {
                "确认导入完成后，请妥善处理包含明文数据的 CSV / JSON 源文件。"
            } else {
                "After confirming the import, handle the source CSV / JSON file carefully because it contains plaintext data."
            },
            tag = if (isZh) "重要" else "important",
            tone = ImportReceiptFeedTone.INFO
        )
    )

    return ImportReceiptUi(
        level = level,
        statusLabel = if (level == ImportReceiptLevel.SUCCESS) {
            if (isZh) "导入完成" else "Import Completed"
        } else if (level == ImportReceiptLevel.WARNING) {
            if (isZh) "导入部分完成" else "Partially Completed"
        } else {
            if (isZh) "未导入成功" else "Import Not Completed"
        },
        title = if (level == ImportReceiptLevel.ERROR) {
            if (isZh) "本次没有导入成功" else "No records imported this time"
        } else {
            if (isZh) "$importedCount 条密码已写入保险库" else "$importedCount passwords imported into vault"
        },
        description = if (level == ImportReceiptLevel.ERROR) {
            if (isZh) "请检查异常项后重试。" else "Please review issues and retry."
        } else {
            if (isZh) "你可以继续处理剩余异常或直接返回。" else "You can review remaining issues or return now."
        },
        primaryValue = importedCount.toString(),
        primaryLabel = if (isZh) "成功导入" else "Imported",
        secondaryValue = unresolved.toString(),
        secondaryLabel = if (isZh) "待处理" else "Pending",
        durationText = formatImportDuration(durationMillis),
        primaryActionText = if (level == ImportReceiptLevel.ERROR) {
            if (isZh) "重新选择文件" else "Pick Another File"
        } else {
            if (isZh) "完成" else "Done"
        },
        secondaryActionText = if (isZh) "查看明细" else "View Details",
        primaryAction = if (level == ImportReceiptLevel.ERROR) ImportReceiptActionType.PICK_FILE else ImportReceiptActionType.CLOSE_PREVIEW,
        secondaryAction = ImportReceiptActionType.SHOW_ISSUES,
        feedItems = feed
    )
}

private fun buildAlreadyPresentReceipt(
    existingCount: Int,
    parseIssueCount: Int,
    durationMillis: Long,
    language: AppLanguage
): ImportReceiptUi {
    val isZh = language == AppLanguage.CHINESE
    val feed = mutableListOf(
        ImportReceiptFeedItem(
            title = if (isZh) "记录已存在" else "Records already exist",
            description = if (isZh) {
                "所选 $existingCount 条记录与保险库已有内容完全相同，已保留原记录。"
            } else {
                "All $existingCount selected records match the vault exactly. Existing records were kept."
            },
            tag = if (isZh) "已保留" else "kept",
            tone = ImportReceiptFeedTone.INFO
        )
    )
    if (parseIssueCount > 0) {
        feed.add(
            ImportReceiptFeedItem(
                title = if (isZh) "存在异常行" else "Invalid rows",
                description = if (isZh) {
                    "$parseIssueCount 行未通过格式校验，可返回预览查看原因。"
                } else {
                    "$parseIssueCount rows failed validation. Return to the preview for details."
                },
                tag = if (isZh) "待处理" else "pending",
                tone = ImportReceiptFeedTone.WARNING
            )
        )
    }
    return ImportReceiptUi(
        level = if (parseIssueCount > 0) ImportReceiptLevel.WARNING else ImportReceiptLevel.SUCCESS,
        statusLabel = if (isZh) "已全部存在" else "Already in vault",
        title = if (isZh) "所选记录已全部在保险库中" else "All selected records are already in the vault",
        description = if (isZh) "无需重复导入，可以返回预览查看记录。" else "No duplicate import is needed. You can return to the preview to review the records.",
        primaryValue = existingCount.toString(),
        primaryLabel = if (isZh) "已存在" else "Already present",
        secondaryValue = parseIssueCount.toString(),
        secondaryLabel = if (isZh) "待处理" else "Pending",
        durationText = formatImportDuration(durationMillis),
        primaryActionText = if (isZh) "查看预览" else "View preview",
        secondaryActionText = if (isZh) "完成" else "Done",
        primaryAction = ImportReceiptActionType.SHOW_ISSUES,
        secondaryAction = ImportReceiptActionType.CLOSE_PREVIEW,
        feedItems = feed
    )
}
