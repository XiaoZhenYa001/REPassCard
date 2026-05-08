package com.example.passcard.data

import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import java.io.File

object PlaintextDatabaseMigrator {
    private const val SQLITE_HEADER = "SQLite format 3\u0000"

    data class Result(
        val passwords: List<PasswordEntity>,
        val movedFiles: List<MovedFile>
    ) {
        val shouldMigrate: Boolean = movedFiles.isNotEmpty()
    }

    data class MovedFile(
        val original: File,
        val backup: File
    )

    fun prepareIfNeeded(context: Context, databaseName: String): Result {
        val databaseFile = context.getDatabasePath(databaseName)
        if (!isPlaintextSqlite(databaseFile)) {
            return Result(emptyList(), emptyList())
        }

        val passwords = readPasswords(databaseFile)
        val movedFiles = moveDatabaseFiles(databaseFile)
        return Result(passwords, movedFiles)
    }

    fun complete(result: Result) {
        result.movedFiles.forEach { it.backup.delete() }
    }

    fun rollback(result: Result) {
        result.movedFiles.forEach { it.original.delete() }
        result.movedFiles.asReversed().forEach { movedFile ->
            if (movedFile.backup.exists()) {
                movedFile.backup.renameTo(movedFile.original)
            }
        }
    }

    private fun isPlaintextSqlite(databaseFile: File): Boolean {
        if (!databaseFile.exists() || databaseFile.length() < SQLITE_HEADER.length) {
            return false
        }

        val header = ByteArray(SQLITE_HEADER.length)
        databaseFile.inputStream().use { input ->
            if (input.read(header) != header.size) return false
        }
        return String(header, Charsets.US_ASCII) == SQLITE_HEADER
    }

    private fun readPasswords(databaseFile: File): List<PasswordEntity> {
        val database = SQLiteDatabase.openDatabase(
            databaseFile.absolutePath,
            null,
            SQLiteDatabase.OPEN_READONLY
        )

        return database.use { db ->
            db.rawQuery("SELECT * FROM passwords", null).use { cursor ->
                buildList {
                    while (cursor.moveToNext()) {
                        add(cursor.toPasswordEntity())
                    }
                }
            }
        }
    }

    private fun moveDatabaseFiles(databaseFile: File): List<MovedFile> {
        val parent = databaseFile.parentFile ?: return emptyList()
        val timestamp = System.currentTimeMillis()
        val companionNames = listOf(
            databaseFile.name,
            "${databaseFile.name}-wal",
            "${databaseFile.name}-shm",
            "${databaseFile.name}-journal"
        )

        val movedFiles = mutableListOf<MovedFile>()
        try {
            companionNames.forEach { fileName ->
                val original = File(parent, fileName)
                if (!original.exists()) return@forEach

                val backup = File(parent, "$fileName.plaintext-backup-$timestamp")
                if (!original.renameTo(backup)) {
                    throw IllegalStateException("Unable to move plaintext database file: ${original.absolutePath}")
                }
                movedFiles += MovedFile(original, backup)
            }
            return movedFiles
        } catch (e: Exception) {
            rollback(Result(emptyList(), movedFiles))
            throw e
        }
    }

    private fun Cursor.toPasswordEntity(): PasswordEntity {
        return PasswordEntity(
            id = getString("id"),
            name = getString("name"),
            username = getString("username"),
            phone = getStringOrDefault("phone", ""),
            email = getStringOrDefault("email", ""),
            password = getString("password"),
            category = getStringOrDefault("category", ""),
            note = getStringOrDefault("note", ""),
            createdAt = getLongOrDefault("createdAt", System.currentTimeMillis()),
            updatedAt = getLongOrDefault("updatedAt", System.currentTimeMillis())
        )
    }

    private fun Cursor.getString(columnName: String): String {
        val index = getColumnIndexOrThrow(columnName)
        return getString(index) ?: ""
    }

    private fun Cursor.getStringOrDefault(columnName: String, defaultValue: String): String {
        val index = getColumnIndex(columnName)
        return if (index >= 0) getString(index) ?: defaultValue else defaultValue
    }

    private fun Cursor.getLongOrDefault(columnName: String, defaultValue: Long): Long {
        val index = getColumnIndex(columnName)
        return if (index >= 0 && !isNull(index)) getLong(index) else defaultValue
    }
}
