package com.example.passcard

import android.app.Application
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

class PassCardApp : Application() {
    val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
}
