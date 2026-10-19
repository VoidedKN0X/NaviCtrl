package com.vknxworks.navictrl.ui.settings

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.vknxworks.navictrl.container
import kotlinx.coroutines.launch

/** What's shared, in the words used by the first-run question and the Settings footer. */
const val USAGE_STATS_DESCRIPTION =
    "Which screens you open, how you start music (an album, playlist, artist, shuffle or a mix), whether you " +
        "download songs, the app version, Android version, phone model and language. Never your server address, " +
        "username, or what you listen to or search for. Sent to VKNXWorks' own server, not to any analytics company."

/** Asked once, after signing in. Nothing is sent until the user says yes; they can change it in Settings. */
@Composable
fun UsageStatsPrompt() {
    val c = LocalContext.current.container
    if (!c.telemetry.available) return
    // The stored values, not the placeholder defaults, so the question doesn't flash for people who answered it.
    val stored by c.settings.stored.collectAsStateWithLifecycle(initialValue = null)
    val s = stored ?: return
    if (s.usageStatsAsked) return
    val scope = rememberCoroutineScope()
    fun answer(share: Boolean) {
        scope.launch { c.settings.update { it.copy(usageStats = share, usageStatsAsked = true) } }
    }
    AlertDialog(
        onDismissRequest = { },
        title = { Text("Help improve NaviCtrl?") },
        text = {
            Text(
                "Share anonymous usage stats so I can see which features people use and which Android " +
                    "versions to support.\n\n$USAGE_STATS_DESCRIPTION\n\nYou can change this any time in Settings.",
            )
        },
        confirmButton = { TextButton(onClick = { answer(true) }) { Text("Share stats") } },
        dismissButton = { TextButton(onClick = { answer(false) }) { Text("No thanks") } },
    )
}
