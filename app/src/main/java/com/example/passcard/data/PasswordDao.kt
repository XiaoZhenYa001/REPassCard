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

    @Query("SELECT * FROM passwords WHERE :query = '' OR name LIKE '%' || :query || '%' OR username LIKE '%' || :query || '%' OR email LIKE '%' || :query || '%' OR phone LIKE '%' || :query || '%' OR note LIKE '%' || :query || '%' ORDER BY updatedAt DESC")
    fun getPagedPasswords(query: String): PagingSource<Int, PasswordEntity>

    @Query("SELECT * FROM passwords ORDER BY updatedAt DESC LIMIT :limit OFFSET :offset")
    suspend fun getPasswordsPage(limit: Int, offset: Int): List<PasswordEntity>
    
    @Query("SELECT * FROM passwords WHERE id = :id")
    suspend fun getPasswordById(id: String): PasswordEntity?
    
    @Query("SELECT * FROM passwords WHERE category = :category ORDER BY updatedAt DESC")
    fun getPasswordsByCategory(category: String): Flow<List<PasswordEntity>>
    
    @Query("SELECT * FROM passwords WHERE name LIKE '%' || :query || '%' OR username LIKE '%' || :query || '%' OR email LIKE '%' || :query || '%' OR phone LIKE '%' || :query || '%' ORDER BY updatedAt DESC")
    fun searchPasswords(query: String): Flow<List<PasswordEntity>>

    @Query("SELECT * FROM passwords WHERE name LIKE '%' || :query || '%' OR username LIKE '%' || :query || '%' OR email LIKE '%' || :query || '%' OR phone LIKE '%' || :query || '%' OR note LIKE '%' || :query || '%' ORDER BY updatedAt DESC LIMIT :limit OFFSET :offset")
    suspend fun searchPasswordsPage(query: String, limit: Int, offset: Int): List<PasswordEntity>
    
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

    @Query("SELECT COUNT(*) FROM passwords WHERE password != '' AND password IN (SELECT password FROM passwords WHERE password != '' GROUP BY password HAVING COUNT(*) > 1)")
    fun getReusedPasswordCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM passwords WHERE name LIKE '%' || :query || '%' OR username LIKE '%' || :query || '%' OR email LIKE '%' || :query || '%' OR phone LIKE '%' || :query || '%' OR note LIKE '%' || :query || '%'")
    suspend fun getSearchPasswordCount(query: String): Int
    
    @Query("SELECT COUNT(*) FROM passwords WHERE category = :category")
    fun getPasswordCountByCategory(category: String): Flow<Int>
}
