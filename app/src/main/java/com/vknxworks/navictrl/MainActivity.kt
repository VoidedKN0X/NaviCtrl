package com.vknxworks.navictrl

import android.Manifest
import android.os.Bundle
import com.vknxworks.navictrl.ui.lock.WindowPrivacy
import android.graphics.Color
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import android.view.WindowManager
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.clearAndSetSemantics
import com.vknxworks.navictrl.ui.components.NotificationPermission
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.vknxworks.navictrl.data.settings.ThemeChoice
import com.vknxworks.navictrl.ui.lock.AppLock
import com.vknxworks.navictrl.ui.lock.AppLockOverlay
import com.vknxworks.navictrl.ui.lock.ScreenPrivacy
import com.vknxworks.navictrl.ui.lock.lockCovering
import com.vknxworks.navictrl.ui.lock.lockLocked
import com.vknxworks.navictrl.ui.nav.AppRoot
import com.vknxworks.navictrl.ui.theme.NaviCtrlTheme

class MainActivity : ComponentActivity() {

    private val notificationPermission = registerForActivityResult(ActivityResultContracts.RequestPermission()) { }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        // Recents shows a blank card until the settings are read (then "Hide in recent apps" decides).
        setRecentsScreenshotEnabled(false)
        val c = container
        setContent {
            val settings by c.settings.settings.collectAsStateWithLifecycle()
            val dark = when (settings.theme) {
                ThemeChoice.SYSTEM -> isSystemInDarkTheme()
                ThemeChoice.LIGHT -> false
                ThemeChoice.DARK, ThemeChoice.BLACK -> true
            }
            // Keep status and navigation bar icons readable when the app theme differs from the system's.
            DisposableEffect(dark) {
                val style = if (dark) SystemBarStyle.dark(Color.TRANSPARENT)
                else SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT)
                enableEdgeToEdge(statusBarStyle = style, navigationBarStyle = style)
                onDispose { }
            }
            val stored by c.settings.stored.collectAsState(initial = null)
            // Until the settings are read, trust the synchronous read made at start-up.
            val lockSetting = stored?.appLock ?: c.lockAtStart
            val covered = lockCovering(lockSetting, c.vault)
            // "Hide in screenshots", screens where secrets are typed or shown and the lock and PIN screens keep the
            // window out of screenshots and recordings; "Hide in recent apps" blanks the recent-apps card.
            val secure = settings.hideScreenshots || covered || ScreenPrivacy.forced > 0
            // "Hide in recent apps" (off by default). With the app lock on, Recents always shows a blank
            // card: a preview there would show what's behind the PIN.
            WindowPrivacy(this, secure, hideRecents = settings.hideRecents || lockSetting || c.vault.enabled)
            // Locked (not just asked for the PIN again): the app's screens aren't composed at all. Dialogs, menus
            // and sheets are windows of their own that would stay above (and usable over) the lock screen, and a
            // hardware keyboard could still reach covered controls. Their state, including where you were, is
            // kept here and comes back on unlock. Music keeps playing: playback doesn't depend on these screens.
            val locked = lockLocked(lockSetting, c.vault)
            val savedRoot = rememberSaveableStateHolder()
            NaviCtrlTheme(settings.theme, settings.dynamicColor) {
                Box(Modifier.fillMaxSize()) {
                    // While the PIN is asked again, the app stays underneath (so nothing you typed is lost), hidden
                    // from TalkBack too.
                    if (!locked) {
                        Box(if (covered) Modifier.fillMaxSize().clearAndSetSemantics { } else Modifier.fillMaxSize()) {
                            savedRoot.SaveableStateProvider("root") { AppRoot() }
                        }
                    }
                    AppLockOverlay(lockSetting)
                }
            }
        }
    }

    /** Asked the first time something wants to show a notification (music playback). */
    fun ensureNotificationPermission() {
        // Only if Android's question was never shown: asked automatically at most once, so it never nags.
        if (!NotificationPermission.hasPermission(this) && !NotificationPermission.asked(this)) {
            NotificationPermission.markAsked(this)
            AppLock.expectReturn()
            notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }
}
