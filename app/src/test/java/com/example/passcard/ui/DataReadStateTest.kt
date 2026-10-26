package com.example.passcard.ui

import java.io.IOException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test

class DataReadStateTest {
    @Test fun successfulEmptyListIsNotLoadingOrAnError() = runBlocking {
        val states = flowOf(emptyList<String>()).readStates().toList()
        assertTrue(states.first().isLoading)
        assertFalse(states.last().isLoading)
        assertEquals(emptyList<String>(), states.last().data)
        assertNull(states.last().errorMessage)
    }
    @Test fun failedReadKeepsLastResultsAndExposesRetryableError() = runBlocking {
        val states = flow { emit(listOf("existing")); throw IOException("read failed") }.readStates().toList()
        assertEquals(listOf("existing"), states.last().data)
        assertEquals("read failed", states.last().errorMessage)
        assertFalse(states.last().isLoading)
    }
    @Test fun completedReadWithoutAnEmissionCannotLeaveSpinnerRunning() = runBlocking {
        val state = emptyFlow<String>().readStates().toList().last()
        assertFalse(state.isLoading)
        assertNotNull(state.errorMessage)
    }
    @Test fun cancellationRemainsCancellation() = runBlocking {
        val error = CancellationException("cancelled")
        assertSame(error, runCatching { flow<String> { throw error }.readStates().toList() }.exceptionOrNull())
    }
    @Test fun revertingSearchBeforeDebounceUsesTheAlreadyLoadedResults() {
        val loaded = HomeReadState("A", null, DataReadState(emptyList(), false))
        assertTrue(loaded.isLoadingFor("B", null))
        assertFalse(loaded.isLoadingFor("A", null))
        assertTrue(loaded.isLoadingFor("A", "Work"))
    }
    @Test fun previousSearchErrorDoesNotApplyToNewSearch() {
        val failed = HomeReadState("A", null, DataReadState(isLoading = false, errorMessage = "failed"))
        assertEquals("failed", failed.errorFor("A", null))
        assertNull(failed.errorFor("B", null))
    }
    @Test fun initializationFailureFailsPendingActionsInsteadOfWaitingForever() = runBlocking {
        val error = IOException("database unavailable")
        assertSame(error, runCatching {
            awaitInitializedValue(MutableStateFlow<String?>(null), MutableStateFlow<Throwable?>(error))
        }.exceptionOrNull())
    }
    @Test fun initializedRepositoryCanProceed() = runBlocking {
        assertEquals("ready", awaitInitializedValue(MutableStateFlow<String?>("ready"), MutableStateFlow<Throwable?>(null)))
    }
}
