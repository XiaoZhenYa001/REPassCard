package com.example.passcard.data

import com.example.passcard.ui.screens.PasswordItem
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class PasswordRepository(private val passwordDao: PasswordDao) {
    
    val allPasswords: Flow<List<PasswordItem>> = passwordDao.getAllPasswords().map { entities ->
        entities.map { it.toPasswordItem() }
    }
    
    val passwordCount: Flow<Int> = passwordDao.getPasswordCount()
    
    fun getPasswordsByCategory(category: String): Flow<List<PasswordItem>> {
        return passwordDao.getPasswordsByCategory(category).map { entities ->
            entities.map { it.toPasswordItem() }
        }
    }
    
    fun searchPasswords(query: String): Flow<List<PasswordItem>> {
        return passwordDao.searchPasswords(query).map { entities ->
            entities.map { it.toPasswordItem() }
        }
    }
    
    suspend fun getPasswordById(id: String): PasswordItem? {
        return passwordDao.getPasswordById(id)?.toPasswordItem()
    }
    
    suspend fun insertPassword(item: PasswordItem) {
        passwordDao.insertPassword(item.toEntity())
    }
    
    suspend fun insertAllPasswords(items: List<PasswordItem>) {
        passwordDao.insertAllPasswords(items.map { it.toEntity() })
    }
    
    suspend fun updatePassword(item: PasswordItem) {
        passwordDao.updatePassword(item.toEntity())
    }
    
    suspend fun deletePassword(item: PasswordItem) {
        passwordDao.deletePassword(item.toEntity())
    }
    
    suspend fun deletePasswordById(id: String) {
        passwordDao.deletePasswordById(id)
    }
    
    suspend fun deleteAllPasswords() {
        passwordDao.deleteAllPasswords()
    }
}

private fun PasswordEntity.toPasswordItem(): PasswordItem {
    return PasswordItem(
        id = id,
        name = name,
        username = username,
        phone = phone,
        email = email,
        password = password,
        category = category,
        note = note
    )
}

private fun PasswordItem.toEntity(): PasswordEntity {
    return PasswordEntity(
        id = id,
        name = name,
        username = username,
        phone = phone,
        email = email,
        password = password,
        category = category,
        note = note
    )
}
