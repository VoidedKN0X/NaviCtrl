package com.vknxworks.navictrl.ui.lock

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue

/**
 * Screens that show or take secrets (passwords, API keys, server logs) keep the window out of screenshots,
 * screen recordings and the recent-apps preview while they're shown, whatever "Hide in screenshots and
 * recent apps" is set to. MainActivity applies it (FLAG_SECURE).
 */
object ScreenPrivacy {
    var forced by mutableIntStateOf(0)
        private set

    internal fun add() { forced++ }
    internal fun remove() { forced = (forced - 1).coerceAtLeast(0) }
}

/** Keeps the window secure while this is shown (and [active]). */
@Composable
fun SecureScreen(active: Boolean = true) {
    DisposableEffect(active) {
        if (active) ScreenPrivacy.add()
        onDispose { if (active) ScreenPrivacy.remove() }
    }
}
