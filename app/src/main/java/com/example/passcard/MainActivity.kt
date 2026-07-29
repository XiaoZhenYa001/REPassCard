package com.example.passcard

import android.os.Bundle
import android.os.SystemClock
import android.view.WindowManager
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
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
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.passcard.ui.MainViewModel
import com.example.passcard.ui.SessionStateViewModel
import com.example.passcard.ui.screens.LockScreen
import com.example.passcard.ui.screens.MainScreen
import com.example.passcard.ui.theme.PassCardTheme
import com.example.passcard.util.PreferencesManager

/**
 * 使用 FragmentActivity 以支持 BiometricPrompt
 */
class MainActivity : FragmentActivity() {
    private lateinit var preferencesManager: PreferencesManager
    private val sessionState: SessionStateViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
        enableEdgeToEdge()
        
        preferencesManager = PreferencesManager(this)
        sessionState.initialize(preferencesManager.hasMasterPassword)
        
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
            
            PassCardTheme(darkTheme = isDarkTheme) {
                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        color = MaterialTheme.colorScheme.background
                    ) {
                        if (!sessionState.isUnlocked && preferencesManager.hasMasterPassword) {
                            LockScreen(
                                preferencesManager = preferencesManager,
                                onUnlocked = {
                                    sessionState.unlockVault()
                                }
                            )
                        } else {
                            val viewModel: MainViewModel = viewModel(
                                viewModelStoreOwner = sessionState.vaultViewModelStoreOwner,
                                factory = ViewModelProvider.AndroidViewModelFactory.getInstance(application)
                            )
                            val passwords by viewModel.passwords.collectAsStateWithLifecycle()
                            val passwordCount by viewModel.passwordCount.collectAsStateWithLifecycle()
                            val securityStats by viewModel.securityStats.collectAsStateWithLifecycle()
                            val startupError by viewModel.startupError.collectAsStateWithLifecycle()

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
                                    weakPasswordsFlow = viewModel.weakPasswords,
                                    reusedPasswordGroupsFlow = viewModel.reusedPasswordGroups,
                                    loadAllPasswords = viewModel::getAllPasswordsSnapshot,
                                    onHomeSearchQueryChange = viewModel::setHomeSearchQuery,
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

    override fun onStart() {
        super.onStart()
        if (!this::preferencesManager.isInitialized) return

        sessionState.moveToForeground(
            hasMasterPassword = preferencesManager.hasMasterPassword,
            elapsedRealtime = SystemClock.elapsedRealtime(),
            timeoutSeconds = preferencesManager.autoLockDelaySeconds
        )
    }

    override fun onStop() {
        if (!isChangingConfigurations &&
            this::preferencesManager.isInitialized
        ) {
            sessionState.moveToBackground(
                hasMasterPassword = preferencesManager.hasMasterPassword,
                elapsedRealtime = SystemClock.elapsedRealtime(),
                timeoutSeconds = preferencesManager.autoLockDelaySeconds
            )
        }
        super.onStop()
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
