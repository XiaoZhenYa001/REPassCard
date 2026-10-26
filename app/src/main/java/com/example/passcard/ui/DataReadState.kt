package com.example.passcard.ui

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.onStart
import com.example.passcard.ui.screens.PasswordItem

/** Empty successful results and failed reads must be distinguishable from an unfinished read. */
data class DataReadState<T>(val data: T? = null, val isLoading: Boolean = true, val errorMessage: String? = null)

internal fun <T> Flow<T>.readStates(): Flow<DataReadState<T>> = flow {
    var lastData: T? = null
    var emittedValue = false
    this@readStates.map { value ->
        lastData = value
        emittedValue = true
        DataReadState(data = value, isLoading = false)
    }.onStart { emit(DataReadState()) }
        .onCompletion { cause ->
            if (cause == null && !emittedValue) emit(DataReadState(isLoading = false, errorMessage = "读取未返回结果，请重试。"))
        }.catch { error ->
            if (error is CancellationException) throw error
            emit(DataReadState(lastData, false, error.message ?: "读取失败，请重试。"))
        }.collect { emit(it) }
}

data class HomeReadState(
    val query: String = "", val category: String? = null,
    val result: DataReadState<List<PasswordItem>> = DataReadState()
) {
    fun isLoadingFor(query: String, category: String?): Boolean =
        result.isLoading || this.query != query || this.category != category
    fun errorFor(query: String, category: String?): String? =
        if (this.query == query && this.category == category) result.errorMessage else null
}

internal suspend fun <T> awaitInitializedValue(value: StateFlow<T?>, failure: StateFlow<Throwable?>): T {
    val (ready, error) = combine(value, failure) { v, e -> v to e }.first { (v, e) -> v != null || e != null }
    return ready ?: throw checkNotNull(error)
}
