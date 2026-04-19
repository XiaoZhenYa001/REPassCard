package com.example.passcard.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.passcard.data.AppDatabase
import com.example.passcard.data.PasswordEntity
import com.example.passcard.data.PasswordRepository
import com.example.passcard.ui.screens.AppLanguage
import com.example.passcard.ui.screens.PasswordItem
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class MainViewModel(application: Application) : AndroidViewModel(application) {
    
    private val database = AppDatabase.getInstance(application)
    private val repository = PasswordRepository(database.passwordDao())
    
    // 从数据库加载密码
    val passwords: StateFlow<List<PasswordItem>> = repository.allPasswords
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    
    // 密码数量
    val passwordCount: StateFlow<Int> = repository.passwordCount
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)
    
    init {
        // 首次启动时插入示例数据
        viewModelScope.launch {
            val count = repository.passwordCount.first()
            if (count == 0) {
                insertSampleData()
            }
        }
    }
    
    private suspend fun insertSampleData() {
        val samplePasswords = listOf(
            PasswordEntity(
                id = "1",
                name = "Google Account",
                username = "alex@gmail.com",
                email = "alex@gmail.com",
                password = "MySecretPassword123",
                category = "社交媒体",
                note = "主账号"
            ),
            PasswordEntity(
                id = "2",
                name = "Netflix",
                username = "alex@gmail.com",
                email = "alex@gmail.com",
                password = "NetflixPass456",
                category = "娱乐",
                note = ""
            ),
            PasswordEntity(
                id = "3",
                name = "Facebook",
                username = "alex.morgan",
                email = "alex@design.com",
                password = "FacebookPass789",
                category = "社交媒体",
                note = ""
            ),
            PasswordEntity(
                id = "4",
                name = "Twitter",
                username = "alex_twitter",
                email = "",
                password = "TwitterPass000",
                category = "",
                note = ""
            ),
            PasswordEntity(
                id = "5",
                name = "Amazon",
                username = "alex@amazon.com",
                email = "alex@amazon.com",
                password = "AmazonPass111",
                category = "购物",
                note = "Prime 会员"
            )
        )
        repository.insertAllPasswords(samplePasswords.map {
            PasswordItem(it.id, it.name, it.username, it.phone, it.email, it.password, it.category, it.note)
        })
    }
    
    fun addPassword(item: PasswordItem) {
        viewModelScope.launch {
            repository.insertPassword(item)
        }
    }
    
    fun updatePassword(item: PasswordItem) {
        viewModelScope.launch {
            repository.updatePassword(item)
        }
    }
    
    fun deletePassword(item: PasswordItem) {
        viewModelScope.launch {
            repository.deletePassword(item)
        }
    }
    
    fun deletePasswordById(id: String) {
        viewModelScope.launch {
            repository.deletePasswordById(id)
        }
    }
}
