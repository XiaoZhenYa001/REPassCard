package com.example.passcard.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.example.passcard.data.AppDatabase
import com.example.passcard.data.PasswordRepository
import com.example.passcard.data.PasswordSecurityStats
import com.example.passcard.ui.screens.PasswordItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val repositoryState = MutableStateFlow<PasswordRepository?>(null)

    private val _startupError = MutableStateFlow<String?>(null)
    val startupError: StateFlow<String?> = _startupError.asStateFlow()

    private val _passwords = MutableStateFlow<List<PasswordItem>>(emptyList())
    val passwords: StateFlow<List<PasswordItem>> = _passwords.asStateFlow()

    private val _passwordCount = MutableStateFlow(0)
    val passwordCount: StateFlow<Int> = _passwordCount.asStateFlow()

    private val allPasswordsSearchQuery = MutableStateFlow("")

    private val debouncedAllPasswordsSearchQuery = allPasswordsSearchQuery
        .debounce(250)
        .distinctUntilChanged()

    val pagedPasswords: Flow<PagingData<PasswordItem>> = combine(
        debouncedAllPasswordsSearchQuery,
        repositoryState.filterNotNull()
    ) { query, repository -> query to repository }
        .flatMapLatest { (query, repository) -> repository.getPagedPasswords(query) }
        .catch { error ->
            _startupError.value = buildErrorMessage("Paging passwords failed", error)
            emit(PagingData.empty())
        }
        .cachedIn(viewModelScope)

    val securityStats: StateFlow<PasswordSecurityStats> = repositoryState
        .filterNotNull()
        .flatMapLatest { repository -> repository.securityStats }
        .catch { error ->
            _startupError.value = buildErrorMessage("Security stats failed", error)
            emit(PasswordSecurityStats())
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = PasswordSecurityStats()
        )

    init {
        loadData()
    }

    private fun loadData() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val repository = PasswordRepository(
                    AppDatabase.getInstance(getApplication()).passwordDao()
                )
                repositoryState.value = repository
            } catch (error: Throwable) {
                _startupError.value = buildErrorMessage("Database startup failed", error)
            }
        }

        viewModelScope.launch(Dispatchers.IO) {
            repositoryState
                .filterNotNull()
                .flatMapLatest { repository -> repository.recentPasswords }
                .catch { error ->
                    _startupError.value = buildErrorMessage("Recent passwords failed", error)
                    emit(emptyList())
                }
                .collectLatest { list ->
                    _passwords.value = list
                }
        }

        viewModelScope.launch(Dispatchers.IO) {
            repositoryState
                .filterNotNull()
                .flatMapLatest { repository -> repository.passwordCount }
                .catch { error ->
                    _startupError.value = buildErrorMessage("Password count failed", error)
                    emit(0)
                }
                .collectLatest { count ->
                    _passwordCount.value = count
                }
        }
    }

    fun setAllPasswordsSearchQuery(query: String) {
        allPasswordsSearchQuery.value = query
    }

    fun addPassword(item: PasswordItem) {
        viewModelScope.launch(Dispatchers.IO) {
            awaitRepository().insertPassword(item)
        }
    }

    fun updatePassword(item: PasswordItem) {
        viewModelScope.launch(Dispatchers.IO) {
            awaitRepository().updatePassword(item)
        }
    }

    fun importPasswords(items: List<PasswordItem>) {
        if (items.isEmpty()) return
        viewModelScope.launch(Dispatchers.IO) {
            awaitRepository().insertAllPasswords(items)
        }
    }

    fun replaceAllPasswords(items: List<PasswordItem>) {
        viewModelScope.launch(Dispatchers.IO) {
            val repository = awaitRepository()
            repository.deleteAllPasswords()
            repository.insertAllPasswords(items)
        }
    }

    fun deletePasswordById(id: String) {
        viewModelScope.launch(Dispatchers.IO) {
            awaitRepository().deletePasswordById(id)
        }
    }

    suspend fun getAllPasswordsSnapshot(): List<PasswordItem> {
        return awaitRepository().getAllPasswordItemsSnapshot()
    }

    private suspend fun awaitRepository(): PasswordRepository {
        return repositoryState.filterNotNull().first()
    }

    private fun buildErrorMessage(scope: String, error: Throwable): String {
        return "$scope: ${error.message ?: error::class.java.simpleName}"
    }
}
