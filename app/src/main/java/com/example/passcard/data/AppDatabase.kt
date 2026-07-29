package com.example.passcard.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.runBlocking
import net.zetetic.database.sqlcipher.SupportOpenHelperFactory
import java.util.Arrays

@Database(
    entities = [PasswordEntity::class],
    version = 3,
    exportSchema = true
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun passwordDao(): PasswordDao

    companion object {
        private const val DATABASE_NAME = "passcard_database"

        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return synchronized(this) {
                INSTANCE ?: buildEncryptedDatabase(context.applicationContext).also {
                    INSTANCE = it
                }
            }
        }

        fun closeInstance() {
            synchronized(this) {
                INSTANCE?.close()
                INSTANCE = null
            }
        }

        private fun buildEncryptedDatabase(context: Context): AppDatabase {
            System.loadLibrary("sqlcipher")

            val plaintextMigration = PlaintextDatabaseMigrator.prepareIfNeeded(context, DATABASE_NAME)
            val passphrase = try {
                DatabasePassphraseManager.getOrCreatePassphrase(
                    context = context,
                    hasExistingDatabase = context.getDatabasePath(DATABASE_NAME).exists()
                )
            } catch (error: Exception) {
                PlaintextDatabaseMigrator.rollback(plaintextMigration)
                throw error
            }

            return try {
                val instance = Room.databaseBuilder(
                    context,
                    AppDatabase::class.java,
                    DATABASE_NAME
                )
                    .openHelperFactory(SupportOpenHelperFactory(passphrase.copyOf()))
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
                    .build()

                if (plaintextMigration.shouldMigrate && plaintextMigration.passwords.isNotEmpty()) {
                    runBlocking {
                        instance.passwordDao().insertAllPasswords(plaintextMigration.passwords)
                    }
                }

                PlaintextDatabaseMigrator.complete(plaintextMigration)
                instance
            } catch (e: Exception) {
                PlaintextDatabaseMigrator.rollback(plaintextMigration)
                throw e
            } finally {
                Arrays.fill(passphrase, 0)
            }
        }

        fun getAppDataDir(context: Context): java.io.File? {
            return context.getExternalFilesDir(null)
        }

        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE passwords ADD COLUMN iconType TEXT NOT NULL DEFAULT 'generated'")
                db.execSQL("ALTER TABLE passwords ADD COLUMN iconValue TEXT NOT NULL DEFAULT ''")
            }
        }

        internal val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                val columns = db.query("PRAGMA table_info(passwords)").use { cursor ->
                    val nameIndex = cursor.getColumnIndexOrThrow("name")
                    buildSet {
                        while (cursor.moveToNext()) {
                            add(cursor.getString(nameIndex))
                        }
                    }
                }

                if ("revision" !in columns) {
                    db.execSQL("ALTER TABLE passwords ADD COLUMN revision INTEGER NOT NULL DEFAULT 0")
                }
                if ("deviceId" !in columns) {
                    db.execSQL("ALTER TABLE passwords ADD COLUMN deviceId TEXT NOT NULL DEFAULT ''")
                }
                if ("deletedAt" !in columns) {
                    db.execSQL("ALTER TABLE passwords ADD COLUMN deletedAt INTEGER DEFAULT NULL")
                }

                db.execSQL("CREATE INDEX IF NOT EXISTS index_passwords_updatedAt ON passwords(updatedAt)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_passwords_category_updatedAt ON passwords(category, updatedAt)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_passwords_password ON passwords(password)")
            }
        }
    }
}
