package com.example.passcard.ui.screens

import org.junit.Assert.assertEquals
import org.junit.Test

class MainNavigationStateTest {
    @Test
    fun openingTopLevelPageMovesForward() {
        assertEquals(
            MainNavigationDirection.FORWARD,
            mainNavigationDirection(MainRoute.Tabs, MainRoute.AllPasswords)
        )
    }

    @Test
    fun returningToTabsMovesBackward() {
        assertEquals(
            MainNavigationDirection.BACKWARD,
            mainNavigationDirection(MainRoute.Help, MainRoute.Tabs)
        )
    }

    @Test
    fun openingEditorFromListMovesForward() {
        val editor = MainRoute.EditPassword(
            passwordId = "id",
            returnRoute = MainRoute.AllPasswords
        )

        assertEquals(
            MainNavigationDirection.FORWARD,
            mainNavigationDirection(MainRoute.AllPasswords, editor)
        )
    }

    @Test
    fun returningFromEditorToItsListMovesBackward() {
        val editor = MainRoute.EditPassword(
            passwordId = "id",
            returnRoute = MainRoute.WeakPasswords
        )

        assertEquals(
            MainNavigationDirection.BACKWARD,
            mainNavigationDirection(editor, MainRoute.WeakPasswords)
        )
    }

    @Test
    fun replacingPagesAtTheSameDepthUsesNeutralTransition() {
        assertEquals(
            MainNavigationDirection.REPLACE,
            mainNavigationDirection(MainRoute.Privacy, MainRoute.About)
        )
    }
}
