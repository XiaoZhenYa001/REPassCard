package com.example.passcard.util

import android.view.SoundEffectConstants
import android.view.View

fun View.playPassCardClick() {
    if (PreferencesManager(context).soundEnabled) playSoundEffect(SoundEffectConstants.CLICK)
}
