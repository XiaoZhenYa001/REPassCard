package com.example.passcard.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "passwords")
data class PasswordEntity(
    @PrimaryKey
    val id: String,
    val name: String,
    val username: String,
    val phone: String = "",
    val email: String = "",
    val password: String,
    val category: String = "",
    val note: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
