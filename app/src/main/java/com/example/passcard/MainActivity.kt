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
import androidx.compose.material3.Button
import androidx.compose.material3.TextButton
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
import com.example.passcard.util.ClipboardHelper

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
                            val categories by viewModel.categories.collectAsStateWithLifecycle()
                            val healthState by viewModel.healthState.collectAsStateWithLifecycle()
                            val startupError by viewModel.startupError.collectAsStateWithLifecycle()
                            val isHomeLoading by viewModel.isHomeLoading.collectAsStateWithLifecycle()
                            val homeErrorMessage by viewModel.homeErrorMessage.collectAsStateWithLifecycle()

                            if (startupError != null) {
                                StartupErrorContent(startupError.orEmpty(), languageKey == "CHINESE", viewModel::retryLoading)
                            } else {
                                MainScreen(
                                    navigationState = viewModel.navigationState,
                                    preferencesManager = preferencesManager,
                                    currentTheme = themeMode,
                                    onThemeChanged = refreshPreferencesState,
                                    languageKey = languageKey,
                                    passwords = passwords,
                                    passwordCount = passwordCount,
                                    isHomeLoading = isHomeLoading,
                                    pagedPasswords = viewModel.pagedPasswords,
                                    categories = categories,
                                    securityStats = healthState.data?.stats ?: com.example.passcard.data.PasswordSecurityStats(),
                                    healthSnapshot = healthState.data,
                                    isHealthLoading = healthState.isLoading,
                                    healthErrorMessage = healthState.errorMessage,
                                    homeErrorMessage = homeErrorMessage,
                                    onRetryData = viewModel::retryLoading,
                                    loadAllPasswords = viewModel::getAllPasswordsSnapshot,
                                    onHomeSearchQueryChange = viewModel::setHomeSearchQuery,
                                    onHomeCategoryChange = viewModel::setHomeCategory,
                                    onAllPasswordsSearchQueryChange = viewModel::setAllPasswordsSearchQuery,
                                    onAllPasswordsCategoryChange = viewModel::setAllPasswordsCategory,
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

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        ClipboardHelper.onWindowFocusChanged(this, hasFocus)
    }

    override fun onStop() {
        ClipboardHelper.onAppBackgrounded()
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
private fun StartupErrorContent(message: String, zh: Boolean, onRetry: () -> Unit) {
    var showDetails by remember(message) { mutableStateOf(false) }
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterVertically)
    ) {
        Text(if (zh) "暂时无法读取保险库" else "Unable to read your vault",
            style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onBackground)
        Text(if (zh) "请重试；如果仍无法读取，可展开详情检查原因。" else "Try again, or view the details to check what happened.",
            style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Button(onClick = onRetry) { Text(if (zh) "重新加载" else "Try again") }
        TextButton(onClick = { showDetails = !showDetails }) {
            Text(if (zh) { if (showDetails) "收起详情" else "查看详情" } else { if (showDetails) "Hide details" else "View details" })
        }
        if (showDetails) Text(message, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
    }
}
