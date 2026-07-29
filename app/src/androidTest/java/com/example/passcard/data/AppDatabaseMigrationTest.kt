package com.example.passcard.data

import android.content.Context
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.SupportSQLiteOpenHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AppDatabaseMigrationTest {
    private lateinit var context: Context

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        context.deleteDatabase(TEST_DATABASE)
    }

    @After
    fun tearDown() {
        context.deleteDatabase(TEST_DATABASE)
    }

    @Test
    fun migrationTwoToThreePreservesDataAndCompletesLegacySchema() {
        val helper = createLegacyVersionTwoDatabase()
        val database = helper.writableDatabase

        AppDatabase.MIGRATION_2_3.migrate(database)

        val columns = database.query("PRAGMA table_info(passwords)").use { cursor ->
            val nameIndex = cursor.getColumnIndexOrThrow("name")
            buildSet {
                while (cursor.moveToNext()) add(cursor.getString(nameIndex))
            }
        }
        assertTrue(columns.containsAll(setOf("revision", "deviceId", "deletedAt")))

        val indices = database.query("PRAGMA index_list(passwords)").use { cursor ->
            val nameIndex = cursor.getColumnIndexOrThrow("name")
            buildSet {
                while (cursor.moveToNext()) add(cursor.getString(nameIndex))
            }
        }
        assertTrue(indices.contains("index_passwords_updatedAt"))
        assertTrue(indices.contains("index_passwords_category_updatedAt"))
        assertTrue(indices.contains("index_passwords_password"))

        database.query("SELECT name, password, revision, deviceId, deletedAt FROM passwords WHERE id = 'legacy-id'").use { cursor ->
            assertTrue(cursor.moveToFirst())
            assertEquals("Legacy", cursor.getString(0))
            assertEquals("secret", cursor.getString(1))
            assertEquals(0L, cursor.getLong(2))
            assertEquals("", cursor.getString(3))
            assertTrue(cursor.isNull(4))
        }

        helper.close()
    }

    private fun createLegacyVersionTwoDatabase(): SupportSQLiteOpenHelper {
        val configuration = SupportSQLiteOpenHelper.Configuration.builder(context)
            .name(TEST_DATABASE)
            .callback(
                object : SupportSQLiteOpenHelper.Callback(2) {
                    override fun onCreate(db: SupportSQLiteDatabase) {
                        db.execSQL(
                            """
                            CREATE TABLE passwords (
                                id TEXT NOT NULL PRIMARY KEY,
                                name TEXT NOT NULL,
                                username TEXT NOT NULL,
                                phone TEXT NOT NULL,
                                email TEXT NOT NULL,
                                password TEXT NOT NULL,
                                category TEXT NOT NULL,
                                note TEXT NOT NULL,
                                iconType TEXT NOT NULL,
                                iconValue TEXT NOT NULL,
                                createdAt INTEGER NOT NULL,
                                updatedAt INTEGER NOT NULL
                            )
                            """.trimIndent()
                        )
                        db.execSQL(
                            """
                            INSERT INTO passwords (
                                id, name, username, phone, email, password, category, note,
                                iconType, iconValue, createdAt, updatedAt
                            ) VALUES (
                                'legacy-id', 'Legacy', 'user', '', '', 'secret', 'Work', '',
                                'generated', '', 100, 200
                            )
                            """.trimIndent()
                        )
                    }

                    override fun onUpgrade(db: SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) = Unit
                }
            )
            .build()

        return FrameworkSQLiteOpenHelperFactory().create(configuration)
    }

    private companion object {
        const val TEST_DATABASE = "passcard-migration-test"
    }
}
