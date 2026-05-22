package com.example.passcard.data

import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.map
import com.example.passcard.ui.screens.PasswordItem
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map

class PasswordRepository(private val passwordDao: PasswordDao) {
    companion object {
        const val PASSWORD_PAGE_SIZE = 40
        const val HOME_RECENT_LIMIT = 30
    }
    
    val allPasswords: Flow<List<PasswordItem>> = passwordDao.getAllPasswords().map { entities ->
        entities.map { it.toPasswordItem() }
    }

    val recentPasswords: Flow<List<PasswordItem>> = passwordDao.getRecentPasswords(HOME_RECENT_LIMIT).map { entities ->
        entities.map { it.toPasswordItem() }
    }
    
    val passwordCount: Flow<Int> = passwordDao.getPasswordCount()

    val securityStats: Flow<PasswordSecurityStats> = combine(
        passwordDao.getPasswordCount(),
        passwordDao.getWeakPasswordCount(),
        passwordDao.getReusedPasswordCount()
    ) { total, weak, reused ->
        PasswordSecurityStats(
            totalCount = total,
            weakCount = weak,
            reusedCount = reused,
            compromisedCount = 0
        )
    }

    fun getPagedPasswords(query: String): Flow<PagingData<PasswordItem>> {
        val normalizedQuery = query.trim()
        return Pager(
            config = PagingConfig(
                pageSize = PASSWORD_PAGE_SIZE,
                prefetchDistance = 8,
                initialLoadSize = PASSWORD_PAGE_SIZE,
                enablePlaceholders = false
            ),
            pagingSourceFactory = { passwordDao.getPagedPasswords(normalizedQuery) }
        ).flow.map { pagingData ->
            pagingData.map { it.toPasswordItem() }
        }
    }
    
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

    suspend fun getPasswordsPage(limit: Int, offset: Int): List<PasswordItem> {
        return passwordDao.getPasswordsPage(limit, offset).map { it.toPasswordItem() }
    }

    suspend fun searchPasswordsPage(query: String, limit: Int, offset: Int): List<PasswordItem> {
        return passwordDao.searchPasswordsPage(query, limit, offset).map { it.toPasswordItem() }
    }

    suspend fun getSearchPasswordCount(query: String): Int {
        return passwordDao.getSearchPasswordCount(query)
    }
    
    suspend fun getPasswordById(id: String): PasswordItem? {
        return passwordDao.getPasswordById(id)?.toPasswordItem()
    }

    suspend fun getAllPasswordItemsSnapshot(): List<PasswordItem> {
        return passwordDao.getAllPasswordsSnapshot().map { it.toPasswordItem() }
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
