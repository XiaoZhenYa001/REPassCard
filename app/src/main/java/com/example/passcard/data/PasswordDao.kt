package com.example.passcard.data

import androidx.room.*
import androidx.paging.PagingSource
import kotlinx.coroutines.flow.Flow

@Dao
interface PasswordDao {
    @Query("SELECT * FROM passwords ORDER BY updatedAt DESC LIMIT :limit")
    fun getRecentPasswords(limit: Int): Flow<List<PasswordEntity>>

    @Query("SELECT * FROM passwords ORDER BY updatedAt DESC")
    suspend fun getAllPasswordsSnapshot(): List<PasswordEntity>

    @Query("SELECT * FROM passwords WHERE 0")
    fun getEmptyPagedPasswords(): PagingSource<Int, PasswordEntity>

    @Query("""
        SELECT * FROM passwords
        WHERE :query = ''
            OR name LIKE :contains ESCAPE '\'
            OR username LIKE :contains ESCAPE '\'
            OR phone LIKE :contains ESCAPE '\'
            OR email LIKE :contains ESCAPE '\'
            OR password LIKE :contains ESCAPE '\'
            OR category LIKE :contains ESCAPE '\'
            OR note LIKE :contains ESCAPE '\'
        ORDER BY
            CASE
                WHEN :query = '' THEN 0
                WHEN name = :query COLLATE NOCASE THEN 0
                WHEN name LIKE :prefix ESCAPE '\' THEN 1
                WHEN name LIKE :contains ESCAPE '\' THEN 2
                WHEN username = :query COLLATE NOCASE THEN 10
                WHEN username LIKE :prefix ESCAPE '\' THEN 11
                WHEN username LIKE :contains ESCAPE '\' THEN 12
                WHEN phone = :query THEN 20
                WHEN phone LIKE :prefix ESCAPE '\' THEN 21
                WHEN phone LIKE :contains ESCAPE '\' THEN 22
                WHEN email = :query COLLATE NOCASE THEN 30
                WHEN email LIKE :prefix ESCAPE '\' THEN 31
                WHEN email LIKE :contains ESCAPE '\' THEN 32
                WHEN password = :query THEN 40
                WHEN password LIKE :prefix ESCAPE '\' THEN 41
                WHEN password LIKE :contains ESCAPE '\' THEN 42
                WHEN category = :query COLLATE NOCASE THEN 50
                WHEN category LIKE :prefix ESCAPE '\' THEN 51
                WHEN category LIKE :contains ESCAPE '\' THEN 52
                WHEN note = :query COLLATE NOCASE THEN 60
                WHEN note LIKE :prefix ESCAPE '\' THEN 61
                WHEN note LIKE :contains ESCAPE '\' THEN 62
                ELSE 99
            END,
            updatedAt DESC
    """)
    fun getPagedPasswords(query: String, prefix: String, contains: String): PagingSource<Int, PasswordEntity>

    @Query("""
        SELECT * FROM passwords
        WHERE CASE :field
            WHEN 'name' THEN name LIKE :contains ESCAPE '\'
            WHEN 'username' THEN username LIKE :contains ESCAPE '\'
            WHEN 'phone' THEN phone LIKE :contains ESCAPE '\'
            WHEN 'email' THEN email LIKE :contains ESCAPE '\'
            WHEN 'password' THEN password LIKE :contains ESCAPE '\'
            WHEN 'category' THEN category LIKE :contains ESCAPE '\'
            WHEN 'note' THEN note LIKE :contains ESCAPE '\'
            ELSE 0
        END
        ORDER BY
            CASE :field
                WHEN 'name' THEN CASE WHEN name = :query COLLATE NOCASE THEN 0 WHEN name LIKE :prefix ESCAPE '\' THEN 1 ELSE 2 END
                WHEN 'username' THEN CASE WHEN username = :query COLLATE NOCASE THEN 0 WHEN username LIKE :prefix ESCAPE '\' THEN 1 ELSE 2 END
                WHEN 'phone' THEN CASE WHEN phone = :query THEN 0 WHEN phone LIKE :prefix ESCAPE '\' THEN 1 ELSE 2 END
                WHEN 'email' THEN CASE WHEN email = :query COLLATE NOCASE THEN 0 WHEN email LIKE :prefix ESCAPE '\' THEN 1 ELSE 2 END
                WHEN 'password' THEN CASE WHEN password = :query THEN 0 WHEN password LIKE :prefix ESCAPE '\' THEN 1 ELSE 2 END
                WHEN 'category' THEN CASE WHEN category = :query COLLATE NOCASE THEN 0 WHEN category LIKE :prefix ESCAPE '\' THEN 1 ELSE 2 END
                WHEN 'note' THEN CASE WHEN note = :query COLLATE NOCASE THEN 0 WHEN note LIKE :prefix ESCAPE '\' THEN 1 ELSE 2 END
                ELSE 99
            END,
            updatedAt DESC
    """)
    fun getPagedPasswordsByField(field: String, query: String, prefix: String, contains: String): PagingSource<Int, PasswordEntity>

    @Query("SELECT * FROM passwords WHERE id = :id")
    suspend fun getPasswordById(id: String): PasswordEntity?
    
    @Query("SELECT * FROM passwords WHERE category = :category ORDER BY updatedAt DESC")
    fun getPasswordsByCategory(category: String): Flow<List<PasswordEntity>>
    
    @Query("""
        SELECT * FROM passwords
        WHERE name LIKE :contains ESCAPE '\'
            OR username LIKE :contains ESCAPE '\'
            OR phone LIKE :contains ESCAPE '\'
            OR email LIKE :contains ESCAPE '\'
            OR password LIKE :contains ESCAPE '\'
            OR category LIKE :contains ESCAPE '\'
            OR note LIKE :contains ESCAPE '\'
        ORDER BY
            CASE
                WHEN name = :query COLLATE NOCASE THEN 0
                WHEN name LIKE :prefix ESCAPE '\' THEN 1
                WHEN name LIKE :contains ESCAPE '\' THEN 2
                WHEN username = :query COLLATE NOCASE THEN 10
                WHEN username LIKE :prefix ESCAPE '\' THEN 11
                WHEN username LIKE :contains ESCAPE '\' THEN 12
                WHEN phone = :query THEN 20
                WHEN phone LIKE :prefix ESCAPE '\' THEN 21
                WHEN phone LIKE :contains ESCAPE '\' THEN 22
                WHEN email = :query COLLATE NOCASE THEN 30
                WHEN email LIKE :prefix ESCAPE '\' THEN 31
                WHEN email LIKE :contains ESCAPE '\' THEN 32
                WHEN password = :query THEN 40
                WHEN password LIKE :prefix ESCAPE '\' THEN 41
                WHEN password LIKE :contains ESCAPE '\' THEN 42
                WHEN category = :query COLLATE NOCASE THEN 50
                WHEN category LIKE :prefix ESCAPE '\' THEN 51
                WHEN category LIKE :contains ESCAPE '\' THEN 52
                WHEN note = :query COLLATE NOCASE THEN 60
                WHEN note LIKE :prefix ESCAPE '\' THEN 61
                WHEN note LIKE :contains ESCAPE '\' THEN 62
                ELSE 99
            END,
            updatedAt DESC
    """)
    fun searchPasswords(query: String, prefix: String, contains: String): Flow<List<PasswordEntity>>

    @Query("""
        SELECT * FROM passwords
        WHERE CASE :field
            WHEN 'name' THEN name LIKE :contains ESCAPE '\'
            WHEN 'username' THEN username LIKE :contains ESCAPE '\'
            WHEN 'phone' THEN phone LIKE :contains ESCAPE '\'
            WHEN 'email' THEN email LIKE :contains ESCAPE '\'
            WHEN 'password' THEN password LIKE :contains ESCAPE '\'
            WHEN 'category' THEN category LIKE :contains ESCAPE '\'
            WHEN 'note' THEN note LIKE :contains ESCAPE '\'
            ELSE 0
        END
        ORDER BY
            CASE :field
                WHEN 'name' THEN CASE WHEN name = :query COLLATE NOCASE THEN 0 WHEN name LIKE :prefix ESCAPE '\' THEN 1 ELSE 2 END
                WHEN 'username' THEN CASE WHEN username = :query COLLATE NOCASE THEN 0 WHEN username LIKE :prefix ESCAPE '\' THEN 1 ELSE 2 END
                WHEN 'phone' THEN CASE WHEN phone = :query THEN 0 WHEN phone LIKE :prefix ESCAPE '\' THEN 1 ELSE 2 END
                WHEN 'email' THEN CASE WHEN email = :query COLLATE NOCASE THEN 0 WHEN email LIKE :prefix ESCAPE '\' THEN 1 ELSE 2 END
                WHEN 'password' THEN CASE WHEN password = :query THEN 0 WHEN password LIKE :prefix ESCAPE '\' THEN 1 ELSE 2 END
                WHEN 'category' THEN CASE WHEN category = :query COLLATE NOCASE THEN 0 WHEN category LIKE :prefix ESCAPE '\' THEN 1 ELSE 2 END
                WHEN 'note' THEN CASE WHEN note = :query COLLATE NOCASE THEN 0 WHEN note LIKE :prefix ESCAPE '\' THEN 1 ELSE 2 END
                ELSE 99
            END,
            updatedAt DESC
    """)
    fun searchPasswordsByField(field: String, query: String, prefix: String, contains: String): Flow<List<PasswordEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPassword(password: PasswordEntity)
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllPasswords(passwords: List<PasswordEntity>)

    @Transaction
    suspend fun replaceAllPasswords(passwords: List<PasswordEntity>) {
        deleteAllPasswords()
        if (passwords.isNotEmpty()) {
            insertAllPasswords(passwords)
        }
    }
    
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

    @Query("SELECT COUNT(*) FROM passwords WHERE category = :category")
    fun getPasswordCountByCategory(category: String): Flow<Int>
}
