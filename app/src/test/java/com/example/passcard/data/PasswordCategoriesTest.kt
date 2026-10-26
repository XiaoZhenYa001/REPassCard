package com.example.passcard.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PasswordCategoriesTest {
    @Test
    fun translatedAndLegacyCategoriesKeepTheSameIdentity() {
        assertEquals("Work", PasswordCategories.key("工作"))
        assertEquals("Work", PasswordCategories.key(" work "))
        assertEquals("工作", PasswordCategories.label("Work", true))
        assertEquals("Work", PasswordCategories.label("工作", false))
        assertEquals(listOf("Work", "工作"), PasswordCategories.aliases("工作"))
    }

    @Test
    fun uncategorizedIsSeparateFromAllAndCustomAllLabels() {
        assertEquals("", PasswordCategories.key("  "))
        assertEquals("未分类", PasswordCategories.label("", true))
        assertEquals("All", PasswordCategories.key("All"))
        assertEquals("全部", PasswordCategories.key("全部"))
        assertEquals(listOf(""), PasswordCategories.aliases(""))
    }

    @Test
    fun categoryOptionsIncludeOnlyUsedCategoriesAndPreserveCustomValues() {
        assertEquals(
            listOf("Work", "Finance", "自定义", ""),
            PasswordCategories.availableKeys(listOf("工作", " Work ", "金融", "自定义", ""))
        )
        assertFalse("Social Media" in PasswordCategories.availableKeys(listOf("工作")))
        assertEquals(emptyList<String>(), PasswordCategories.availableKeys(emptyList()))
    }

    @Test
    fun changingLanguageNeverChangesCustomCategories() {
        assertEquals("我的账户", PasswordCategories.label("我的账户", true))
        assertEquals("我的账户", PasswordCategories.label("我的账户", false))
        assertEquals(listOf("我的账户"), PasswordCategories.aliases("我的账户"))
    }

    @Test
    fun customCategoryIdentityFollowsSqliteWithoutChangingItsLabel() {
        assertEquals(listOf("Development"), PasswordCategories.availableKeys(listOf("Development", "development")))
        assertTrue(PasswordCategories.matches("Development", "development"))
        assertFalse(PasswordCategories.matches("École", "école"))
        assertEquals("Development", PasswordCategories.label("Development", false))
    }
}
