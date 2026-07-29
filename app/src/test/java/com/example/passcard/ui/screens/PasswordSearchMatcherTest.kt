package com.example.passcard.ui.screens

import com.example.passcard.data.PasswordSearchSyntax
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PasswordSearchMatcherTest {
    private val item = PasswordItem(
        id = "1",
        name = "微信",
        username = "wechat_user",
        phone = "13800138000",
        email = "user@example.com",
        password = "Secure_123",
        category = "社交媒体",
        note = "工作账号"
    )

    @Test
    fun globalSearchMatchesEverySupportedField() {
        assertTrue(item.matchesPasswordSearch(PasswordSearchSyntax.parse("微信")))
        assertTrue(item.matchesPasswordSearch(PasswordSearchSyntax.parse("wechat_user")))
        assertTrue(item.matchesPasswordSearch(PasswordSearchSyntax.parse("1380013")))
        assertTrue(item.matchesPasswordSearch(PasswordSearchSyntax.parse("example.com")))
        assertTrue(item.matchesPasswordSearch(PasswordSearchSyntax.parse("Secure_")))
        assertTrue(item.matchesPasswordSearch(PasswordSearchSyntax.parse("社交媒体")))
        assertTrue(item.matchesPasswordSearch(PasswordSearchSyntax.parse("工作账号")))
    }

    @Test
    fun fieldSearchOnlyMatchesTheRequestedField() {
        assertTrue(item.matchesPasswordSearch(PasswordSearchSyntax.parse("/t 名称 微信")))
        assertFalse(item.matchesPasswordSearch(PasswordSearchSyntax.parse("/t 用户名 微信")))
        assertTrue(item.matchesPasswordSearch(PasswordSearchSyntax.parse("/t 备注 工作")))
        assertFalse(item.matchesPasswordSearch(PasswordSearchSyntax.parse("/t 邮箱 工作")))
    }

    @Test
    fun incompleteFieldSearchDoesNotReuseStaleResults() {
        assertFalse(item.matchesPasswordSearch(PasswordSearchSyntax.parse("/t 名称")))
    }
}
