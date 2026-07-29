package com.example.passcard.data

import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.map
import com.example.passcard.ui.screens.PasswordItem
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map

class PasswordRepository(private val passwordDao: PasswordDao) {
    companion object {
        const val PASSWORD_PAGE_SIZE = 40
        const val HOME_RECENT_LIMIT = 30
    }
    
    val recentPasswords: Flow<List<PasswordItem>> = passwordDao.getRecentPasswords(HOME_RECENT_LIMIT).map { entities ->
        entities.map { it.toPasswordItem() }
    }

    val weakPasswords: Flow<List<PasswordItem>> = passwordDao.getWeakPasswords().map { entities ->
        entities.map { it.toPasswordItem() }
    }

    val reusedPasswordGroups: Flow<List<ReusedPasswordGroup>> = passwordDao.getReusedPasswords().map { entities ->
        entities
            .map { it.toPasswordItem() }
            .groupBy { it.password }
            .filterValues { it.size > 1 }
            .values
            .map { group -> ReusedPasswordGroup(items = group) }
            .sortedWith(compareByDescending<ReusedPasswordGroup> { it.items.size }.thenBy { it.label })
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
        val parsedSearch = PasswordSearchSyntax.parse(normalizedQuery)
        return Pager(
            config = PagingConfig(
                pageSize = PASSWORD_PAGE_SIZE,
                prefetchDistance = 8,
                initialLoadSize = PASSWORD_PAGE_SIZE,
                enablePlaceholders = false
            ),
            pagingSourceFactory = {
                when {
                    normalizedQuery.isBlank() -> passwordDao.getPagedPasswords("", "", "%%")
                    parsedSearch.keyword.isBlank() -> passwordDao.getEmptyPagedPasswords()
                    parsedSearch.field != null -> passwordDao.getPagedPasswordsByField(
                        field = parsedSearch.field.column,
                        query = parsedSearch.keyword,
                        prefix = parsedSearch.prefix,
                        contains = parsedSearch.contains
                    )
                    else -> passwordDao.getPagedPasswords(
                        query = parsedSearch.keyword,
                        prefix = parsedSearch.prefix,
                        contains = parsedSearch.contains
                    )
                }
            }
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
        val parsedSearch = PasswordSearchSyntax.parse(query)
        if (parsedSearch.keyword.isBlank()) return flowOf(emptyList())

        val source = if (parsedSearch.field != null) {
            passwordDao.searchPasswordsByField(
                field = parsedSearch.field.column,
                query = parsedSearch.keyword,
                prefix = parsedSearch.prefix,
                contains = parsedSearch.contains
            )
        } else {
            passwordDao.searchPasswords(
                query = parsedSearch.keyword,
                prefix = parsedSearch.prefix,
                contains = parsedSearch.contains
            )
        }
        return source.map { entities ->
            entities.map { it.toPasswordItem() }
        }
    }

    suspend fun getPasswordById(id: String): PasswordItem? {
        return passwordDao.getPasswordById(id)?.toPasswordItem()
    }

    suspend fun getAllPasswordItemsSnapshot(): List<PasswordItem> {
        return passwordDao.getAllPasswordsSnapshot().map { it.toPasswordItem() }
    }
    
    suspend fun insertPassword(item: PasswordItem) {
        passwordDao.insertPassword(item.toPasswordEntity())
    }
    
    suspend fun insertAllPasswords(items: List<PasswordItem>) {
        passwordDao.insertAllPasswords(items.map { it.toPasswordEntity() })
    }

    suspend fun replaceAllPasswords(items: List<PasswordItem>) {
        passwordDao.replaceAllPasswords(items.map { it.toPasswordEntity() })
    }
    
    suspend fun updatePassword(item: PasswordItem) {
        passwordDao.updatePassword(item.toPasswordEntity())
    }
    
    suspend fun deletePassword(item: PasswordItem) {
        passwordDao.deletePassword(item.toPasswordEntity())
    }
    
    suspend fun deletePasswordById(id: String) {
        passwordDao.deletePasswordById(id)
    }
    
    suspend fun deleteAllPasswords() {
        passwordDao.deleteAllPasswords()
    }
}

data class ReusedPasswordGroup(
    val items: List<PasswordItem>
) {
    val label: String = items.firstOrNull()?.password.orEmpty()
    val count: Int = items.size
}
