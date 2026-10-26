package com.example.passcard.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.example.passcard.data.AppDatabase
import com.example.passcard.data.PasswordRepository
import com.example.passcard.data.PasswordHealthSnapshot
import com.example.passcard.data.PasswordSecurityStats
import com.example.passcard.data.ReusedPasswordGroup
import com.example.passcard.ui.screens.PasswordItem
import com.example.passcard.ui.screens.MainNavigationState
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
class MainViewModel(application: Application) : AndroidViewModel(application) {
    val navigationState = MainNavigationState()
    private val repositoryState = MutableStateFlow<PasswordRepository?>(null)

    private var initializationJob: Job? = null
    private val repositoryFailure = MutableStateFlow<Throwable?>(null)
    private val _dataReadError = MutableStateFlow<String?>(null)

    private val _startupError = MutableStateFlow<String?>(null)
    val startupError: StateFlow<String?> = _startupError.asStateFlow()

    private val homeSearchQuery = MutableStateFlow("")
    private val allPasswordsSearchQuery = MutableStateFlow("")
    private val homeCategory = MutableStateFlow<String?>(null)
    private val allPasswordsCategory = MutableStateFlow<String?>(null)

    private val debouncedHomeSearchQuery = homeSearchQuery
        .debounce { query -> if (query.isBlank()) 0L else 250L }
        .distinctUntilChanged()

    private val debouncedAllPasswordsSearchQuery = allPasswordsSearchQuery
        .debounce { query -> if (query.isBlank()) 0L else 250L }
        .distinctUntilChanged()

    val homeReadState: StateFlow<HomeReadState> = combine(
        debouncedHomeSearchQuery, homeCategory, repositoryState.filterNotNull()
    ) { query, category, repository -> Triple(query, category, repository) }
        .flatMapLatest { (query, category, repository) ->
            repository.getHomePasswords(query, category).readStates().map { HomeReadState(query, category, it) }
        }.flowOn(Dispatchers.Default)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeReadState())

    val passwords: StateFlow<List<PasswordItem>> = homeReadState.map { it.result.data.orEmpty() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val isHomeLoading: StateFlow<Boolean> = combine(homeReadState, homeSearchQuery, homeCategory, repositoryState) { state, query, category, repository ->
        repository == null || state.isLoadingFor(query, category)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), true)
    val homeErrorMessage: StateFlow<String?> = combine(homeReadState, homeSearchQuery, homeCategory, _dataReadError, repositoryState) { state, query, category, error, repository ->
        if (repository == null) null else state.errorFor(query, category) ?: error
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val passwordCount: StateFlow<Int> = observeRepository("Password count failed", 0) { it.passwordCount }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)

    val pagedPasswords: Flow<PagingData<PasswordItem>> = combine(
        debouncedAllPasswordsSearchQuery, allPasswordsCategory, repositoryState.filterNotNull()
    ) { query, category, repository -> Triple(query, category, repository) }
        .flatMapLatest { (query, category, repository) ->
            repository.getPagedPasswords(query, category).catch { error ->
                _dataReadError.value = buildErrorMessage("Paging passwords failed", error)
                emit(PagingData.empty())
            }
        }
        .flowOn(Dispatchers.Default)
        .cachedIn(viewModelScope)

    val categories: StateFlow<List<String>> = observeRepository("Categories failed", emptyList<String>()) { it.categories }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val healthState: StateFlow<DataReadState<PasswordHealthSnapshot>> = combine(repositoryState, repositoryFailure) { repository, error -> repository to error }
        .flatMapLatest { (repository, error) ->
            if (repository != null) repository.healthSnapshot.readStates()
            else kotlinx.coroutines.flow.flowOf(DataReadState<PasswordHealthSnapshot>(isLoading = error == null, errorMessage = error?.message))
        }.flowOn(Dispatchers.Default)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), DataReadState())

    val securityStats: StateFlow<PasswordSecurityStats> = healthState.map { it.data?.stats ?: PasswordSecurityStats() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), PasswordSecurityStats())
    val weakPasswords: StateFlow<List<PasswordItem>> = healthState.map { it.data?.weakPasswords.orEmpty() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val reusedPasswordGroups: StateFlow<List<ReusedPasswordGroup>> = healthState.map { it.data?.reusedPasswordGroups.orEmpty() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private fun <T> observeRepository(scope: String, fallback: T, read: (PasswordRepository) -> Flow<T>): Flow<T> =
        repositoryState.filterNotNull().flatMapLatest { repository ->
            read(repository).catch { error ->
                _dataReadError.value = buildErrorMessage(scope, error)
                emit(fallback)
            }
        }.flowOn(Dispatchers.Default)

    init {
        loadData()
    }

    private fun loadData() {
        if (initializationJob?.isActive == true) return
        _startupError.value = null
        _dataReadError.value = null
        repositoryFailure.value = null
        repositoryState.value = null
        initializationJob = viewModelScope.launch(Dispatchers.IO) {
            try {
                val repository = PasswordRepository(
                    AppDatabase.getInstance(getApplication()).passwordDao()
                )
                repositoryState.value = repository
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Throwable) {
                repositoryFailure.value = error
                _startupError.value = buildErrorMessage("Database startup failed", error)
            }
        }

    }

    fun retryLoading() = loadData()

    fun setAllPasswordsSearchQuery(query: String) {
        allPasswordsSearchQuery.value = query
    }

    fun setHomeSearchQuery(query: String) {
        homeSearchQuery.value = query
    }

    fun setHomeCategory(category: String?) {
        homeCategory.value = category
    }

    fun setAllPasswordsCategory(category: String?) {
        allPasswordsCategory.value = category
    }

    suspend fun addPassword(item: PasswordItem) {
        withContext(Dispatchers.IO) {
            awaitRepository().insertPassword(item)
        }
    }

    fun updatePassword(item: PasswordItem) {
        viewModelScope.launch(Dispatchers.IO) {
            awaitRepository().updatePassword(item)
        }
    }

    suspend fun importPasswords(items: List<PasswordItem>) {
        if (items.isEmpty()) return
        withContext(Dispatchers.IO) {
            awaitRepository().insertAllPasswords(items)
        }
    }

    suspend fun replaceAllPasswords(items: List<PasswordItem>) {
        withContext(Dispatchers.IO) {
            awaitRepository().replaceAllPasswords(items)
        }
    }

    suspend fun deletePasswordById(id: String) {
        withContext(Dispatchers.IO) {
            awaitRepository().deletePasswordById(id)
        }
    }

    suspend fun getAllPasswordsSnapshot(): List<PasswordItem> {
        return awaitRepository().getAllPasswordItemsSnapshot()
    }

    override fun onCleared() {
        navigationState.clear()
        repositoryState.value = null
        _startupError.value = null
        super.onCleared()
    }

    private suspend fun awaitRepository(): PasswordRepository {
        return awaitInitializedValue(repositoryState, repositoryFailure)
    }

    private fun buildErrorMessage(scope: String, error: Throwable): String {
        return "$scope: ${error.message ?: error::class.java.simpleName}"
    }
}
