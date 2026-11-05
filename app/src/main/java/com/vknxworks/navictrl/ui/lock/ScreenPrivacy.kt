package com.vknxworks.navictrl.ui.lock

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.os.Build
import android.view.View
import android.view.Window
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.window.DialogWindowProvider
import java.util.WeakHashMap

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

/** Keeps the window secure (and [ShieldScreen]) while this is shown (and [active]). */
@Composable
fun SecureScreen(active: Boolean = true) {
    DisposableEffect(active) {
        if (active) ScreenPrivacy.add()
        onDispose { if (active) ScreenPrivacy.remove() }
    }
    if (active) ShieldScreen()
}

/**
 * Applies "Hide in screenshots" ([secure]: FLAG_SECURE, which also blanks the recent-apps preview) and
 * "Hide in recent apps" ([hideRecents]: a blank card in Recents, screenshots still work) to [activity]'s
 * window, and again whenever they change, so a switch works at once in every open screen. Callers pass the
 * app lock being on as part of [hideRecents]: Android takes the Recents snapshot when you leave, before the
 * app locks, so a preview would otherwise show what's behind the PIN.
 */
@Composable
fun WindowPrivacy(activity: android.app.Activity, secure: Boolean, hideRecents: Boolean) {
    androidx.compose.runtime.LaunchedEffect(secure) {
        if (secure) activity.window.addFlags(android.view.WindowManager.LayoutParams.FLAG_SECURE)
        else activity.window.clearFlags(android.view.WindowManager.LayoutParams.FLAG_SECURE)
    }
    androidx.compose.runtime.LaunchedEffect(hideRecents) { activity.setRecentsScreenshotEnabled(!hideRecents) }
}

/**
 * While a PIN, lock, sign-in or destructive confirmation screen is showing:
 * - other apps' floating windows (chat heads, "draw over other apps" overlays) are hidden, so nothing can
 *   cover or imitate the screen to trick a tap or capture the PIN (Android 12+);
 * - with [sensitiveData], the screen's contents are only shared with real accessibility tools such as
 *   screen readers, not with other apps that use the accessibility API to read the screen (Android 14+).
 * Counted per window, so screens that overlap during a transition can't turn it off early; everything
 * goes back to normal when the last one closes. Other screens of the app aren't affected.
 */
@Composable
fun ShieldScreen(sensitiveData: Boolean = true) {
    val view = LocalView.current
    DisposableEffect(view, sensitiveData) {
        val window = (view.parent as? DialogWindowProvider)?.window ?: view.context.shieldActivity()?.window
        val root = view.rootView
        if (window != null) Shields.hideOverlays(window, true)
        if (sensitiveData) Shields.sensitive(root, true)
        onDispose {
            if (window != null) Shields.hideOverlays(window, false)
            if (sensitiveData) Shields.sensitive(root, false)
        }
    }
}

private object Shields {
    private val overlayCounts = WeakHashMap<Window, Int>()
    private val sensitiveCounts = WeakHashMap<View, Int>()

    fun hideOverlays(window: Window, on: Boolean) {
        val n = (overlayCounts[window] ?: 0) + if (on) 1 else -1
        overlayCounts[window] = n.coerceAtLeast(0)
        if ((on && n == 1) || (!on && n <= 0)) runCatching { window.setHideOverlayWindows(on) }
    }

    fun sensitive(view: View, on: Boolean) {
        if (Build.VERSION.SDK_INT < 34) return
        val n = (sensitiveCounts[view] ?: 0) + if (on) 1 else -1
        sensitiveCounts[view] = n.coerceAtLeast(0)
        if ((on && n == 1) || (!on && n <= 0)) runCatching {
            view.setAccessibilityDataSensitive(
                if (on) View.ACCESSIBILITY_DATA_SENSITIVE_YES else View.ACCESSIBILITY_DATA_SENSITIVE_AUTO,
            )
        }
    }
}

private tailrec fun Context.shieldActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.shieldActivity()
    else -> null
}
