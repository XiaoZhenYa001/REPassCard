package com.example.passcard.data

import com.example.passcard.ui.screens.PasswordItem

internal fun PasswordEntity.toPasswordItem(): PasswordItem {
    return PasswordItem(
        id = id,
        name = name,
        username = username,
        phone = phone,
        email = email,
        password = password,
        category = category,
        note = note,
        iconType = iconType,
        iconValue = iconValue,
        createdAt = createdAt,
        updatedAt = updatedAt,
        revision = revision,
        deviceId = deviceId,
        deletedAt = deletedAt
    )
}

internal fun PasswordItem.toPasswordEntity(): PasswordEntity {
    return PasswordEntity(
        id = id,
        name = name,
        username = username,
        phone = phone,
        email = email,
        password = password,
        category = category,
        note = note,
        iconType = iconType,
        iconValue = iconValue,
        createdAt = createdAt,
        updatedAt = updatedAt,
        revision = revision,
        deviceId = deviceId,
        deletedAt = deletedAt
    )
}
