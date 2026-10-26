package com.example.passcard.data

import java.lang.reflect.Proxy
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test

class PasswordHealthRepositoryTest {
    @Test fun productionRepositoryUsesObservedRowsInsteadOfLegacySqlHeuristics() = runBlocking {
        val calls = mutableListOf<String>()
        val repository = PasswordRepository(dao(flowOf(listOf(entity("weak", "Abcdef1234"), entity("empty", ""))), calls))
        val snapshot = repository.healthSnapshot.first()
        assertEquals(1, snapshot.stats.weakCount)
        assertEquals(listOf("weak"), snapshot.weakPasswords.map { it.id })
        assertEquals(1, snapshot.stats.passwordlessCount)
        assertTrue("observeAllPasswords" in calls)
        assertFalse(calls.any { it in setOf("getWeakPasswords", "getWeakPasswordCount", "getReusedPasswords", "getReusedPasswordCount") })
    }

    @Test fun databaseUpdatesRefreshCountsAndListsTogether() = runBlocking {
        val firstDelivered = CompletableDeferred<Unit>()
        val rows = flow {
            emit(listOf(entity("a", "short"), entity("b", "short")))
            firstDelivered.await()
            emit(listOf(entity("a", "velvet sunrise birch copper"), entity("b", "")))
        }
        val snapshots = PasswordRepository(dao(rows, mutableListOf())).healthSnapshot
            .onEach { firstDelivered.complete(Unit) }.toList()
        assertEquals(2, snapshots.size)
        assertEquals(2, snapshots.first().stats.weakCount)
        assertEquals(2, snapshots.first().stats.reusedCount)
        assertEquals(0, snapshots.last().stats.weakCount)
        assertEquals(0, snapshots.last().stats.reusedCount)
        assertTrue(snapshots.last().weakPasswords.isEmpty())
        assertTrue(snapshots.last().reusedPasswordGroups.isEmpty())
        assertEquals(1, snapshots.last().stats.assessedCount)
    }

    private fun dao(rows: Flow<List<PasswordEntity>>, calls: MutableList<String>): PasswordDao =
        Proxy.newProxyInstance(PasswordDao::class.java.classLoader, arrayOf(PasswordDao::class.java)) { _, method, _ ->
            calls.add(method.name)
            when (method.name) {
                "observeAllPasswords" -> rows
                "getRecentPasswords" -> flowOf(emptyList<PasswordEntity>())
                "getPasswordCount" -> flowOf(0)
                "getCategories" -> flowOf(emptyList<String>())
                else -> throw AssertionError("Unexpected DAO call: ${method.name}")
            }
        } as PasswordDao

    private fun entity(id: String, password: String) = PasswordEntity(id = id, name = id, username = "", password = password)
}
