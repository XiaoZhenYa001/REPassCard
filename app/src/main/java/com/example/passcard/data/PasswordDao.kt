package com.example.passcard.data

import androidx.room.*
import androidx.paging.PagingSource
import kotlinx.coroutines.flow.Flow

@Dao
interface PasswordDao {
    @Query("SELECT * FROM passwords ORDER BY updatedAt DESC")
    fun getAllPasswords(): Flow<List<PasswordEntity>>

    @Query("SELECT * FROM passwords ORDER BY updatedAt DESC LIMIT :limit")
    fun getRecentPasswords(limit: Int): Flow<List<PasswordEntity>>

    @Query("SELECT * FROM passwords ORDER BY updatedAt DESC")
    suspend fun getAllPasswordsSnapshot(): List<PasswordEntity>

    @Query("SELECT * FROM passwords WHERE 0")
    fun getEmptyPagedPasswords(): PagingSource<Int, PasswordEntity>

    @Query("""
        SELECT * FROM passwords
        WHERE :query = ''
            OR name LIKE :contains
            OR username LIKE :contains
            OR phone LIKE :contains
            OR email LIKE :contains
            OR password LIKE :contains
            OR category LIKE :contains
            OR note LIKE :contains
        ORDER BY
            CASE
                WHEN :query = '' THEN 0
                WHEN name = :query COLLATE NOCASE THEN 0
                WHEN name LIKE :prefix THEN 1
                WHEN name LIKE :contains THEN 2
                WHEN username = :query COLLATE NOCASE THEN 10
                WHEN username LIKE :prefix THEN 11
                WHEN username LIKE :contains THEN 12
                WHEN phone = :query THEN 20
                WHEN phone LIKE :prefix THEN 21
                WHEN phone LIKE :contains THEN 22
                WHEN email = :query COLLATE NOCASE THEN 30
                WHEN email LIKE :prefix THEN 31
                WHEN email LIKE :contains THEN 32
                WHEN password = :query THEN 40
                WHEN password LIKE :prefix THEN 41
                WHEN password LIKE :contains THEN 42
                WHEN category = :query COLLATE NOCASE THEN 50
                WHEN category LIKE :prefix THEN 51
                WHEN category LIKE :contains THEN 52
                WHEN note = :query COLLATE NOCASE THEN 60
                WHEN note LIKE :prefix THEN 61
                WHEN note LIKE :contains THEN 62
                ELSE 99
            END,
            updatedAt DESC
    """)
    fun getPagedPasswords(query: String, prefix: String, contains: String): PagingSource<Int, PasswordEntity>

    @Query("""
        SELECT * FROM passwords
        WHERE CASE :field
            WHEN 'name' THEN name LIKE :contains
            WHEN 'username' THEN username LIKE :contains
            WHEN 'phone' THEN phone LIKE :contains
            WHEN 'email' THEN email LIKE :contains
            WHEN 'password' THEN password LIKE :contains
            WHEN 'category' THEN category LIKE :contains
            WHEN 'note' THEN note LIKE :contains
            ELSE 0
        END
        ORDER BY
            CASE :field
                WHEN 'name' THEN CASE WHEN name = :query COLLATE NOCASE THEN 0 WHEN name LIKE :prefix THEN 1 ELSE 2 END
                WHEN 'username' THEN CASE WHEN username = :query COLLATE NOCASE THEN 0 WHEN username LIKE :prefix THEN 1 ELSE 2 END
                WHEN 'phone' THEN CASE WHEN phone = :query THEN 0 WHEN phone LIKE :prefix THEN 1 ELSE 2 END
                WHEN 'email' THEN CASE WHEN email = :query COLLATE NOCASE THEN 0 WHEN email LIKE :prefix THEN 1 ELSE 2 END
                WHEN 'password' THEN CASE WHEN password = :query THEN 0 WHEN password LIKE :prefix THEN 1 ELSE 2 END
                WHEN 'category' THEN CASE WHEN category = :query COLLATE NOCASE THEN 0 WHEN category LIKE :prefix THEN 1 ELSE 2 END
                WHEN 'note' THEN CASE WHEN note = :query COLLATE NOCASE THEN 0 WHEN note LIKE :prefix THEN 1 ELSE 2 END
                ELSE 99
            END,
            updatedAt DESC
    """)
    fun getPagedPasswordsByField(field: String, query: String, prefix: String, contains: String): PagingSource<Int, PasswordEntity>

    @Query("SELECT * FROM passwords ORDER BY updatedAt DESC LIMIT :limit OFFSET :offset")
    suspend fun getPasswordsPage(limit: Int, offset: Int): List<PasswordEntity>
    
    @Query("SELECT * FROM passwords WHERE id = :id")
    suspend fun getPasswordById(id: String): PasswordEntity?
    
    @Query("SELECT * FROM passwords WHERE category = :category ORDER BY updatedAt DESC")
    fun getPasswordsByCategory(category: String): Flow<List<PasswordEntity>>
    
    @Query("""
        SELECT * FROM passwords
        WHERE name LIKE :contains
            OR username LIKE :contains
            OR phone LIKE :contains
            OR email LIKE :contains
            OR password LIKE :contains
            OR category LIKE :contains
            OR note LIKE :contains
        ORDER BY
            CASE
                WHEN name = :query COLLATE NOCASE THEN 0
                WHEN name LIKE :prefix THEN 1
                WHEN name LIKE :contains THEN 2
                WHEN username = :query COLLATE NOCASE THEN 10
                WHEN username LIKE :prefix THEN 11
                WHEN username LIKE :contains THEN 12
                WHEN phone = :query THEN 20
                WHEN phone LIKE :prefix THEN 21
                WHEN phone LIKE :contains THEN 22
                WHEN email = :query COLLATE NOCASE THEN 30
                WHEN email LIKE :prefix THEN 31
                WHEN email LIKE :contains THEN 32
                WHEN password = :query THEN 40
                WHEN password LIKE :prefix THEN 41
                WHEN password LIKE :contains THEN 42
                WHEN category = :query COLLATE NOCASE THEN 50
                WHEN category LIKE :prefix THEN 51
                WHEN category LIKE :contains THEN 52
                WHEN note = :query COLLATE NOCASE THEN 60
                WHEN note LIKE :prefix THEN 61
                WHEN note LIKE :contains THEN 62
                ELSE 99
            END,
            updatedAt DESC
    """)
    fun searchPasswords(query: String, prefix: String, contains: String): Flow<List<PasswordEntity>>

    @Query("""
        SELECT * FROM passwords
        WHERE CASE :field
            WHEN 'name' THEN name LIKE :contains
            WHEN 'username' THEN username LIKE :contains
            WHEN 'phone' THEN phone LIKE :contains
            WHEN 'email' THEN email LIKE :contains
            WHEN 'password' THEN password LIKE :contains
            WHEN 'category' THEN category LIKE :contains
            WHEN 'note' THEN note LIKE :contains
            ELSE 0
        END
        ORDER BY
            CASE :field
                WHEN 'name' THEN CASE WHEN name = :query COLLATE NOCASE THEN 0 WHEN name LIKE :prefix THEN 1 ELSE 2 END
                WHEN 'username' THEN CASE WHEN username = :query COLLATE NOCASE THEN 0 WHEN username LIKE :prefix THEN 1 ELSE 2 END
                WHEN 'phone' THEN CASE WHEN phone = :query THEN 0 WHEN phone LIKE :prefix THEN 1 ELSE 2 END
                WHEN 'email' THEN CASE WHEN email = :query COLLATE NOCASE THEN 0 WHEN email LIKE :prefix THEN 1 ELSE 2 END
                WHEN 'password' THEN CASE WHEN password = :query THEN 0 WHEN password LIKE :prefix THEN 1 ELSE 2 END
                WHEN 'category' THEN CASE WHEN category = :query COLLATE NOCASE THEN 0 WHEN category LIKE :prefix THEN 1 ELSE 2 END
                WHEN 'note' THEN CASE WHEN note = :query COLLATE NOCASE THEN 0 WHEN note LIKE :prefix THEN 1 ELSE 2 END
                ELSE 99
            END,
            updatedAt DESC
    """)
    fun searchPasswordsByField(field: String, query: String, prefix: String, contains: String): Flow<List<PasswordEntity>>

    @Query("""
        SELECT * FROM passwords
        WHERE name LIKE :contains
            OR username LIKE :contains
            OR phone LIKE :contains
            OR email LIKE :contains
            OR password LIKE :contains
            OR category LIKE :contains
            OR note LIKE :contains
        ORDER BY updatedAt DESC LIMIT :limit OFFSET :offset
    """)
    suspend fun searchPasswordsPage(contains: String, limit: Int, offset: Int): List<PasswordEntity>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPassword(password: PasswordEntity)
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllPasswords(passwords: List<PasswordEntity>)
    
    @Update
    suspend fun updatePassword(password: PasswordEntity)
    
    @Delete
    suspend fun deletePassword(password: PasswordEntity)
    
    @Query("DELETE FROM passwords WHERE id = :id")
    suspend fun deletePasswordById(id: String)
    
    @Query("DELETE FROM passwords")
    suspend fun deleteAllPasswords()
    
    @Query("SELECT COUNT(*) FROM passwords")
    fun getPasswordCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM passwords WHERE length(password) < 10 OR password NOT GLOB '*[0-9]*' OR password NOT GLOB '*[A-Z]*' OR password NOT GLOB '*[a-z]*'")
    fun getWeakPasswordCount(): Flow<Int>

    @Query("SELECT * FROM passwords WHERE length(password) < 10 OR password NOT GLOB '*[0-9]*' OR password NOT GLOB '*[A-Z]*' OR password NOT GLOB '*[a-z]*' ORDER BY updatedAt DESC")
    fun getWeakPasswords(): Flow<List<PasswordEntity>>

    @Query("SELECT COUNT(*) FROM passwords WHERE password != '' AND password IN (SELECT password FROM passwords WHERE password != '' GROUP BY password HAVING COUNT(*) > 1)")
    fun getReusedPasswordCount(): Flow<Int>

    @Query("SELECT * FROM passwords WHERE password != '' AND password IN (SELECT password FROM passwords WHERE password != '' GROUP BY password HAVING COUNT(*) > 1) ORDER BY password ASC, updatedAt DESC")
    fun getReusedPasswords(): Flow<List<PasswordEntity>>

    @Query("SELECT COUNT(*) FROM passwords WHERE name LIKE :contains OR username LIKE :contains OR phone LIKE :contains OR email LIKE :contains OR password LIKE :contains OR category LIKE :contains OR note LIKE :contains")
    suspend fun getSearchPasswordCount(contains: String): Int
    
    @Query("SELECT COUNT(*) FROM passwords WHERE category = :category")
    fun getPasswordCountByCategory(category: String): Flow<Int>
}
