package com.example.passcard.benchmark

import androidx.test.uiautomator.By
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.Until
import java.util.regex.Pattern

internal const val TARGET_PACKAGE = "com.example.passcard"
private const val UI_TIMEOUT_MS = 5_000L

internal fun exerciseMainUi(device: UiDevice) {
    val homeVisible = device.wait(
        Until.hasObject(By.text(Pattern.compile("我的保险库|My Vault"))),
        UI_TIMEOUT_MS
    )
    if (!homeVisible) return

    val centerX = device.displayWidth / 2
    device.swipe(centerX, device.displayHeight * 3 / 4, centerX, device.displayHeight / 3, 12)
    device.waitForIdle()

    device.findObject(By.text(Pattern.compile("设置|Settings")))?.click()
    device.wait(Until.hasObject(By.text(Pattern.compile("主题外观|Theme"))), UI_TIMEOUT_MS)
    device.findObject(By.text(Pattern.compile("主题外观|Theme")))?.click()
    device.wait(Until.hasObject(By.text(Pattern.compile("浅色|Light"))), UI_TIMEOUT_MS)
    device.pressBack()
}
