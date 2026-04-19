package com.example.passcard.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface PasswordDao {
    @Query("SELECT * FROM passwords ORDER BY updatedAt DESC")
    fun getAllPasswords(): Flow<List<PasswordEntity>>
    
    @Query("SELECT * FROM passwords WHERE id = :id")
    suspend fun getPasswordById(id: String): PasswordEntity?
    
    @Query("SELECT * FROM passwords WHERE category = :category ORDER BY updatedAt DESC")
    fun getPasswordsByCategory(category: String): Flow<List<PasswordEntity>>
    
    @Query("SELECT * FROM passwords WHERE name LIKE '%' || :query || '%' OR username LIKE '%' || :query || '%' OR email LIKE '%' || :query || '%' OR phone LIKE '%' || :query || '%' ORDER BY updatedAt DESC")
    fun searchPasswords(query: String): Flow<List<PasswordEntity>>
    
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
    
    @Query("SELECT COUNT(*) FROM passwords WHERE category = :category")
    fun getPasswordCountByCategory(category: String): Flow<Int>
}
