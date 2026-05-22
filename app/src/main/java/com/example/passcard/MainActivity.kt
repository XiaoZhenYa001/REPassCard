package com.example.passcard

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.passcard.ui.MainViewModel
import com.example.passcard.ui.screens.LockScreen
import com.example.passcard.ui.screens.MainScreen
import com.example.passcard.ui.theme.PassCardTheme
import com.example.passcard.ui.theme.LocalThemeColors
import com.example.passcard.ui.theme.buildThemeColors
import androidx.compose.runtime.CompositionLocalProvider
import com.example.passcard.util.PreferencesManager

/**
 * 使用 FragmentActivity 以支持 BiometricPrompt
 */
class MainActivity : FragmentActivity() {
    
    private lateinit var preferencesManager: PreferencesManager
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        preferencesManager = PreferencesManager(this)
        
        setContent {
            var themeMode by remember { mutableStateOf(preferencesManager.theme) }
            var languageKey by remember { mutableStateOf(preferencesManager.language) }

            val isDarkTheme = when (themeMode) {
                "DARK" -> true
                "LIGHT" -> false
                "SYSTEM" -> isSystemInDarkTheme()
                else -> false
            }

            val refreshPreferencesState = {
                themeMode = preferencesManager.theme
                languageKey = preferencesManager.language
            }
            
            // 是否已解锁（如果没有主密码则默认已解锁）
            var isUnlocked by remember {
                mutableStateOf(!preferencesManager.hasMasterPassword)
            }
            
            PassCardTheme(darkTheme = isDarkTheme) {
                val themeColors = buildThemeColors(isDarkTheme)
                
                CompositionLocalProvider(LocalThemeColors provides themeColors) {
                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        color = MaterialTheme.colorScheme.background
                    ) {
                        if (!isUnlocked && preferencesManager.hasMasterPassword) {
                            LockScreen(
                                preferencesManager = preferencesManager,
                                onUnlocked = { isUnlocked = true }
                            )
                        } else {
                            val viewModel: MainViewModel = viewModel()
                            val passwords by viewModel.passwords.collectAsState()
                            val passwordCount by viewModel.passwordCount.collectAsState()
                            val securityStats by viewModel.securityStats.collectAsState()
                            val startupError by viewModel.startupError.collectAsState()

                            if (startupError != null && passwords.isEmpty()) {
                                StartupErrorContent(startupError.orEmpty())
                            } else {
                                MainScreen(
                                    preferencesManager = preferencesManager,
                                    currentTheme = themeMode,
                                    onThemeChanged = refreshPreferencesState,
                                    languageKey = languageKey,
                                    passwords = passwords,
                                    passwordCount = passwordCount,
                                    pagedPasswords = viewModel.pagedPasswords,
                                    securityStats = securityStats,
                                    loadAllPasswords = viewModel::getAllPasswordsSnapshot,
                                    onAllPasswordsSearchQueryChange = viewModel::setAllPasswordsSearchQuery,
                                    onSavePassword = { item -> viewModel.addPassword(item) },
                                    onImportPasswords = { items -> viewModel.importPasswords(items) },
                                    onReplacePasswords = { items -> viewModel.replaceAllPasswords(items) },
                                    onDeletePassword = { id -> viewModel.deletePasswordById(id) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StartupErrorContent(message: String) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "应用启动数据失败",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onBackground
        )
        Text(
            text = message,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.error,
            modifier = Modifier.padding(top = 12.dp)
        )
    }
}
