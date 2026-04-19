package com.example.passcard.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.passcard.PassCardApp
import com.example.passcard.data.PasswordEntity
import com.example.passcard.data.PasswordRepository
import com.example.passcard.ui.screens.PasswordItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainViewModel(application: Application) : AndroidViewModel(application) {
    
    private val _passwords = MutableStateFlow<List<PasswordItem>>(emptyList())
    val passwords: StateFlow<List<PasswordItem>> = _passwords.asStateFlow()
    
    private val _passwordCount = MutableStateFlow(0)
    val passwordCount: StateFlow<Int> = _passwordCount.asStateFlow()
    
    private var repository: PasswordRepository? = null
    private var initialized = false
    
    init {
        // 异步加载数据库数据
        loadData()
    }
    
    private fun loadData() {
        viewModelScope.launch(Dispatchers.IO) {
            if (initialized) return@launch
            
            val db = try {
                PassCardApp.getDatabase()
            } catch (e: Exception) {
                null
            }
            
            if (db == null) {
                initialized = true
                return@launch
            }
            
            repository = PasswordRepository(db.passwordDao())
            
            // 检查是否需要插入示例数据
            val count = repository?.passwordCount?.first() ?: 0
            if (count == 0) {
                insertSampleData()
            }
            
            // 加载数据
            repository?.allPasswords?.collectLatest { list ->
                _passwords.value = list
                _passwordCount.value = list.size
            }
            
            initialized = true
        }
    }
    
    private suspend fun insertSampleData() {
        val samplePasswords = listOf(
            PasswordEntity("1", "Google Account", "alex@gmail.com", "", "alex@gmail.com", "MySecretPassword123", "社交媒体", "主账号"),
            PasswordEntity("2", "Netflix", "alex@gmail.com", "", "alex@gmail.com", "NetflixPass456", "娱乐", ""),
            PasswordEntity("3", "Facebook", "alex.morgan", "", "alex@design.com", "FacebookPass789", "社交媒体", ""),
            PasswordEntity("4", "Twitter", "alex_twitter", "", "", "TwitterPass000", "", ""),
            PasswordEntity("5", "Amazon", "alex@amazon.com", "", "alex@amazon.com", "AmazonPass111", "购物", "Prime 会员")
        )
        samplePasswords.forEach { entity ->
            repository?.insertPassword(
                PasswordItem(entity.id, entity.name, entity.username, entity.phone, entity.email, entity.password, entity.category, entity.note)
            )
        }
    }
    
    fun addPassword(item: PasswordItem) {
        viewModelScope.launch(Dispatchers.IO) {
            repository?.insertPassword(item)
        }
    }
    
    fun updatePassword(item: PasswordItem) {
        viewModelScope.launch(Dispatchers.IO) {
            repository?.updatePassword(item)
        }
    }
    
    fun deletePasswordById(id: String) {
        viewModelScope.launch(Dispatchers.IO) {
            repository?.deletePasswordById(id)
        }
    }
}
