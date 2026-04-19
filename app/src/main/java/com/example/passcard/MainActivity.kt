package com.example.passcard

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.passcard.ui.MainViewModel
import com.example.passcard.ui.screens.MainScreen
import com.example.passcard.ui.theme.PassCardTheme
import com.example.passcard.util.PreferencesManager

class MainActivity : ComponentActivity() {
    
    private lateinit var preferencesManager: PreferencesManager
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        preferencesManager = PreferencesManager(this)
        
        setContent {
            val savedTheme = preferencesManager.theme
            val isDarkTheme = when (savedTheme) {
                "DARK" -> true
                "LIGHT" -> false
                "SYSTEM" -> isSystemInDarkTheme()
                else -> false
            }
            
            val languageKey by remember { mutableStateOf(preferencesManager.language) }
            
            val viewModel: MainViewModel = viewModel()
            val passwords by viewModel.passwords.collectAsState()
            
            PassCardTheme(darkTheme = isDarkTheme) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    MainScreen(
                        preferencesManager = preferencesManager,
                        onThemeChanged = { recreate() },
                        languageKey = languageKey,
                        passwords = passwords,
                        onSavePassword = { item -> viewModel.addPassword(item) },
                        onDeletePassword = { id -> viewModel.deletePasswordById(id) }
                    )
                }
            }
        }
    }
}
