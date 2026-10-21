package com.vknxworks.navictrl.ui.settings

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.material.icons.outlined.Fingerprint
import androidx.compose.material.icons.outlined.Layers
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Password
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.ui.res.stringResource
import com.vknxworks.navictrl.R
import com.vknxworks.navictrl.data.lock.LockVault
import com.vknxworks.navictrl.data.settings.LockTimeout
import com.vknxworks.navictrl.ui.lock.AppLock
import com.vknxworks.navictrl.ui.lock.PinRequest
import com.vknxworks.navictrl.ui.lock.findActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.DownloadForOffline
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.vknxworks.navictrl.container
import com.vknxworks.navictrl.data.download.DownloadStatus
import com.vknxworks.navictrl.data.settings.AppSettings
import com.vknxworks.navictrl.data.settings.ThemeChoice
import com.vknxworks.navictrl.data.settings.TranscodeFormat
import com.vknxworks.navictrl.ui.components.ConfirmDialog
import com.vknxworks.navictrl.ui.components.Group
import com.vknxworks.navictrl.ui.components.GroupChoice
import com.vknxworks.navictrl.ui.components.GroupRow
import com.vknxworks.navictrl.ui.components.GroupSwitch
import com.vknxworks.navictrl.ui.components.LocalBottomClearance
import com.vknxworks.navictrl.ui.components.MessageView
import com.vknxworks.navictrl.ui.components.PlayShuffleRow
import com.vknxworks.navictrl.ui.components.ScreenTopBar
import com.vknxworks.navictrl.ui.components.SongRow
import com.vknxworks.navictrl.ui.components.formatBytes
import com.vknxworks.navictrl.ui.nav.AddAccountRoute
import com.vknxworks.navictrl.ui.nav.LocalNavigator
import com.vknxworks.navictrl.ui.theme.Gutter
import kotlinx.coroutines.launch

private val STREAM_QUALITY = listOf(
    0 to "Original files", 320 to "320 kbps", 256 to "256 kbps", 192 to "192 kbps", 128 to "128 kbps", 96 to "96 kbps",
)
private val CACHE_SIZES = listOf(0 to "Off", 512 to "512 MB", 1024 to "1 GB", 2048 to "2 GB", 4096 to "4 GB", 8192 to "8 GB")

@Composable
fun SettingsScreen() {
    val c = LocalContext.current.container
    val scope = rememberCoroutineScope()
    val s by c.settings.settings.collectAsStateWithLifecycle()
    fun update(t: (AppSettings) -> AppSettings) { scope.launch { c.settings.update(t) } }

    Scaffold(
        contentWindowInsets = WindowInsets(0),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = { ScreenTopBar("Settings") },
    ) { padding ->
        Column(Modifier.padding(padding).fillMaxSize().verticalScroll(rememberScrollState()).padding(bottom = 32.dp)) {
            Group("Appearance") {
                GroupChoice("Theme", ThemeChoice.entries.map { it to it.label }, s.theme) { v -> update { it.copy(theme = v) } }
                GroupSwitch("Use wallpaper colours", "Replaces the default colours with ones from your wallpaper", s.dynamicColor) { v ->
                    update { it.copy(dynamicColor = v) }
                }
            }

            PrivacyGroup(s) { t -> update(t) }

            Group("Streaming", footer = "Lower quality is converted on your server, so it needs a transcoder set up in Navidrome (it has one by default).") {
                GroupChoice("Quality on Wi-Fi", STREAM_QUALITY, s.wifiBitrate) { v -> update { it.copy(wifiBitrate = v) } }
                GroupChoice("Quality on mobile data", STREAM_QUALITY, s.mobileBitrate) { v -> update { it.copy(mobileBitrate = v) } }
                GroupChoice("Format for lower quality", TranscodeFormat.entries.map { it to it.label }, s.format) { v -> update { it.copy(format = v) } }
                GroupChoice("Keep played music on the phone", CACHE_SIZES, s.cacheMb) { v -> update { it.copy(cacheMb = v) } }
            }
            Text(
                "A change to the cache size applies the next time the app starts.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = Gutter + 4.dp),
            )

            Group("Downloads") {
                GroupChoice("Download quality", STREAM_QUALITY, s.downloadBitrate) { v -> update { it.copy(downloadBitrate = v) } }
                GroupSwitch("Download on Wi-Fi only", checked = s.downloadWifiOnly) { v -> update { it.copy(downloadWifiOnly = v) } }
            }

            Group("Listening history", footer = "Navidrome passes plays on to Last.fm or ListenBrainz if you've linked them there.") {
                GroupSwitch("Report what I play", "Updates play counts, recently played and scrobbles", s.scrobble) { v -> update { it.copy(scrobble = v) } }
            }

        }
    }
}

/** Privacy and security: the shared Ctrl series app lock and screenshot protection. */
@Composable
private fun PrivacyGroup(s: AppSettings, update: ((AppSettings) -> AppSettings) -> Unit) {
    val context = LocalContext.current
    val c = context.container
    val bioAvailable = remember { AppLock.biometricAvailable(context) }
    val lockOn = c.vault.enabled
    Group(
        stringResource(R.string.group_privacy_security),
        footer = if (lockOn) stringResource(R.string.app_lock_footer) else null,
    ) {
        GroupSwitch(
            stringResource(R.string.app_lock),
            stringResource(R.string.app_lock_subtitle),
            lockOn,
            icon = Icons.Outlined.Lock,
        ) { on -> AppLock.request = if (on) PinRequest.ENABLE else PinRequest.DISABLE }
        if (lockOn) {
            GroupRow(
                stringResource(R.string.lock_change_pin),
                stringResource(R.string.lock_change_pin_subtitle, LockVault.PIN_MIN),
                icon = Icons.Outlined.Password,
                showChevron = true,
                onClick = { AppLock.request = PinRequest.CHANGE },
            )
            GroupSwitch(
                stringResource(R.string.lock_fingerprint),
                stringResource(if (bioAvailable || c.vault.biometricEnabled) R.string.lock_fingerprint_subtitle else R.string.lock_fingerprint_unavailable),
                c.vault.biometricEnabled,
                icon = Icons.Outlined.Fingerprint,
            ) { on ->
                if (!on) {
                    c.vault.disableBiometric()
                } else if (bioAvailable) {
                    val activity = context.findActivity()
                    val cipher = c.vault.biometricEnrolCipher()
                    if (activity == null || cipher == null) {
                        Toast.makeText(context, context.getString(R.string.lock_fingerprint_setup_failed), Toast.LENGTH_SHORT).show()
                    } else {
                        AppLock.biometricPrompt(
                            activity,
                            context.getString(R.string.lock_fingerprint_enable_title),
                            context.getString(R.string.lock_fingerprint_enable_subtitle),
                            cipher,
                        ) { unlocked ->
                            if (!c.vault.finishBiometricEnrol(unlocked)) {
                                Toast.makeText(context, context.getString(R.string.lock_fingerprint_setup_failed), Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                }
            }
            GroupChoice(
                stringResource(R.string.lock_again),
                LockTimeout.entries.map { it to stringResource(it.label) },
                s.lockTimeout,
                icon = Icons.Outlined.Timer,
            ) { v -> update { it.copy(lockTimeout = v) } }
        }
        GroupSwitch(
            stringResource(R.string.hide_screenshots),
            stringResource(R.string.hide_screenshots_subtitle),
            s.hideScreenshots,
            icon = Icons.Outlined.VisibilityOff,
        ) { v -> update { it.copy(hideScreenshots = v) } }
        GroupSwitch(
            stringResource(R.string.hide_recents),
            stringResource(R.string.hide_recents_subtitle),
            s.hideRecents,
            icon = Icons.Outlined.Layers,
        ) { v -> update { it.copy(hideRecents = v) } }
    }
}

@Composable
fun AccountsScreen() {
    val c = LocalContext.current.container
    val nav = LocalNavigator.current
    val scope = rememberCoroutineScope()
    val accounts by c.sessions.accounts.collectAsStateWithLifecycle()
    val active by c.sessions.active.collectAsStateWithLifecycle()
    var removing by remember { mutableStateOf<String?>(null) }

    Scaffold(
        contentWindowInsets = WindowInsets(0),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = { ScreenTopBar("Servers and accounts") },
    ) { padding ->
        Column(Modifier.padding(padding).fillMaxSize().verticalScroll(rememberScrollState())) {
            Group(footer = "Tap an account to switch to it. Playback stops when you switch.") {
                accounts.forEach { a ->
                    val isActive = a.key == active?.account?.key
                    GroupRow(
                        a.username,
                        if (a.isEncrypted) a.serverUrl else "${a.serverUrl}  ·  Not encrypted (HTTP)",
                        onClick = if (isActive) null else {
                            {
                                c.music.reset()
                                scope.launch { c.sessions.switchTo(a.key) }
                                Unit
                            }
                        },
                        trailing = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                if (isActive) Icon(Icons.Filled.CheckCircle, "Active", tint = MaterialTheme.colorScheme.primary)
                                IconButton(onClick = { removing = a.key }) { Icon(Icons.AutoMirrored.Filled.Logout, "Sign out of this account") }
                            }
                        },
                    )
                }
            }
            Group {
                GroupRow("Add a server or account", icon = Icons.Filled.Add, showChevron = true, onClick = { nav.navigate(AddAccountRoute) })
            }
        }
    }

    removing?.let { key ->
        val acc = accounts.firstOrNull { it.key == key }
        ConfirmDialog(
            "Sign out of ${acc?.serverName ?: "this server"}?",
            "You can sign in again any time.",
            "Sign out",
            onDismiss = { removing = null },
        ) {
            removing = null
            if (key == active?.account?.key) c.music.reset()
            c.appScope.launch { c.signOut(key) }
        }
    }
}

@Composable
fun DownloadsScreen() {
    val c = LocalContext.current.container
    val all by c.downloads.entries.collectAsStateWithLifecycle()
    var clearing by remember { mutableStateOf(false) }
    val mine = c.downloads.forActive(all)
    val done = mine.filter { it.status == DownloadStatus.DONE }
    val pending = mine.filter { it.status != DownloadStatus.DONE }

    Scaffold(
        contentWindowInsets = WindowInsets(0),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            ScreenTopBar("Downloads") {
                if (mine.isNotEmpty()) TextButton(onClick = { clearing = true }) { Text("Remove all") }
            }
        },
    ) { padding ->
        if (mine.isEmpty()) {
            Box(Modifier.padding(padding).fillMaxSize()) {
                MessageView(
                    Icons.Outlined.DownloadForOffline,
                    "No downloads",
                    "Download songs, albums or playlists from their menus to listen without a connection.",
                )
            }
            return@Scaffold
        }
        LazyColumn(Modifier.padding(padding).fillMaxSize(), contentPadding = PaddingValues(bottom = 24.dp + LocalBottomClearance.current)) {
            item(key = "summary") {
                Text(
                    "${done.size} songs  ·  ${formatBytes(c.downloads.usedBytes(all))}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = Gutter),
                )
            }
            if (pending.isNotEmpty()) {
                item(key = "pending-h") {
                    Text("In progress", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(start = Gutter, top = 16.dp, bottom = 4.dp))
                }
                items(pending, key = { "p-${it.song.id}" }) { e ->
                    Row(Modifier.fillMaxWidth().padding(start = Gutter, end = 4.dp, top = 6.dp, bottom = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            Modifier.size(40.dp).clip(CircleShape).background(MaterialTheme.colorScheme.surfaceContainerHigh),
                            contentAlignment = Alignment.Center,
                        ) {
                            when (e.status) {
                                DownloadStatus.FAILED -> Icon(Icons.Filled.ErrorOutline, "Failed", tint = MaterialTheme.colorScheme.error)
                                DownloadStatus.RUNNING -> CircularProgressIndicator(progress = { e.progress }, modifier = Modifier.size(24.dp), strokeWidth = 3.dp)
                                else -> CircularProgressIndicator(Modifier.size(24.dp), strokeWidth = 2.dp)
                            }
                        }
                        Spacer(Modifier.width(14.dp))
                        Column(Modifier.weight(1f)) {
                            Text(e.song.title, style = MaterialTheme.typography.bodyLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text(
                                when (e.status) {
                                    DownloadStatus.FAILED -> e.error ?: "Failed"
                                    DownloadStatus.RUNNING -> "${(e.progress * 100).toInt()}%"
                                    else -> if (c.settings.settings.value.downloadWifiOnly) "Waiting for Wi-Fi" else "Waiting"
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = if (e.status == DownloadStatus.FAILED) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            if (e.status == DownloadStatus.RUNNING) LinearProgressIndicator(progress = { e.progress }, modifier = Modifier.fillMaxWidth().padding(top = 4.dp))
                        }
                        if (e.status == DownloadStatus.FAILED) IconButton(onClick = { c.downloads.retry(e) }) { Icon(Icons.Filled.Refresh, "Retry") }
                        IconButton(onClick = { c.downloads.remove(listOf(e.song.id)) }) { Icon(Icons.Outlined.Delete, "Cancel") }
                    }
                }
            }
            if (done.isNotEmpty()) {
                val songs = done.map { it.song }
                item(key = "done-h") {
                    Text("On this phone", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(start = Gutter, top = 16.dp, bottom = 4.dp))
                    PlayShuffleRow(onPlay = { c.music.play(songs) }, onShuffle = { c.music.shuffle(songs) })
                }
                items(songs.size, key = { "d-${songs[it].id}" }) { i ->
                    SongRow(songs[i], onClick = { c.music.play(songs, i) })
                }
            }
        }
    }

    if (clearing) {
        ConfirmDialog("Remove all downloads?", "Frees ${formatBytes(c.downloads.usedBytes(all))} on this phone.", "Remove", onDismiss = { clearing = false }) {
            clearing = false
            c.downloads.removeAll()
        }
    }
}
