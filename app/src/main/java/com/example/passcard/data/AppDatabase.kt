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
    version = 2,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun passwordDao(): PasswordDao

    companion object {
        private const val DATABASE_NAME = "passcard_database"

        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: buildEncryptedDatabase(context.applicationContext).also {
                    INSTANCE = it
                }
            }
        }

        private fun buildEncryptedDatabase(context: Context): AppDatabase {
            System.loadLibrary("sqlcipher")

            val passphrase = DatabasePassphraseManager.getOrCreatePassphrase(context)
            val plaintextMigration = PlaintextDatabaseMigrator.prepareIfNeeded(context, DATABASE_NAME)

            return try {
                val instance = Room.databaseBuilder(
                    context,
                    AppDatabase::class.java,
                    DATABASE_NAME
                )
                    .openHelperFactory(SupportOpenHelperFactory(passphrase.copyOf()))
                    .addMigrations(MIGRATION_1_2)
                    .fallbackToDestructiveMigration()
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
    }
}
