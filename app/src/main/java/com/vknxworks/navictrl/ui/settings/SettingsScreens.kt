package com.vknxworks.navictrl.ui.settings

import androidx.compose.material3.ButtonDefaults
import com.vknxworks.navictrl.ui.components.CtrlLabel
import com.vknxworks.navictrl.ui.components.CtrlProgressBar
import com.vknxworks.navictrl.ui.components.StatusDot
import com.vknxworks.navictrl.ui.components.topDivider
import com.vknxworks.navictrl.ui.theme.CtrlColors
import com.vknxworks.navictrl.ui.theme.CtrlShapes
import com.vknxworks.navictrl.ui.theme.CtrlType
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
import com.vknxworks.navictrl.ui.components.openNotificationSettings
import com.vknxworks.navictrl.ui.components.rememberNotificationAccess
import androidx.compose.material.icons.outlined.NotificationsOff
import androidx.compose.material.icons.outlined.Notifications
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
import androidx.compose.ui.res.pluralStringResource

private val STREAM_QUALITY: List<Pair<Int, String>>
    @Composable get() = listOf(0 to stringResource(R.string.original_files)) +
        listOf(320, 256, 192, 128, 96).map { it to stringResource(R.string.x_kbps, it) }
private val CACHE_SIZES: List<Pair<Int, String>>
    @Composable get() = listOf(0 to stringResource(R.string.off), 512 to stringResource(R.string.x_mb, 512)) +
        listOf(1, 2, 4, 8).map { it * 1024 to stringResource(R.string.x_gb, it) }

@Composable
fun SettingsScreen() {
    val context = LocalContext.current
    val c = context.container
    val scope = rememberCoroutineScope()
    val s by c.settings.settings.collectAsStateWithLifecycle()
    fun update(t: (AppSettings) -> AppSettings) { scope.launch { c.settings.update(t) } }

    Scaffold(
        contentWindowInsets = WindowInsets(0),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = { ScreenTopBar(stringResource(R.string.settings)) },
    ) { padding ->
        Column(Modifier.padding(padding).fillMaxSize().verticalScroll(rememberScrollState()).padding(bottom = 32.dp)) {
            PrivacyGroup(s) { t -> update(t) }

            // Music controls live in a notification: say so when Android blocks it. Checked again on every return.
            val notifications = rememberNotificationAccess()
            Group(stringResource(R.string.notifications), footer = stringResource(R.string.notifications_footer)) {
                if (notifications.granted) {
                    GroupRow(
                        stringResource(R.string.notifications_allowed),
                        stringResource(R.string.notifications_allowed_summary),
                        Icons.Outlined.Notifications,
                        showChevron = true,
                        onClick = { openNotificationSettings(context) },
                    )
                } else {
                    GroupRow(
                        stringResource(R.string.notifications_are_blocked),
                        stringResource(R.string.notifications_are_blocked_summary),
                        Icons.Outlined.NotificationsOff,
                        iconTint = MaterialTheme.colorScheme.error,
                        onClick = { notifications.request() },
                        trailing = { TextButton(onClick = { notifications.request() }, colors = ButtonDefaults.textButtonColors(contentColor = CtrlColors.Accent)) { Text(stringResource(R.string.allow)) } },
                    )
                }
            }

            Group(stringResource(R.string.streaming), footer = stringResource(R.string.lower_quality_is_converted_on_your)) {
                GroupChoice(stringResource(R.string.quality_on_wi_fi), STREAM_QUALITY, s.wifiBitrate) { v -> update { it.copy(wifiBitrate = v) } }
                GroupChoice(stringResource(R.string.quality_on_mobile_data), STREAM_QUALITY, s.mobileBitrate) { v -> update { it.copy(mobileBitrate = v) } }
                GroupChoice(stringResource(R.string.format_for_lower_quality), TranscodeFormat.entries.map { it to it.label }, s.format) { v -> update { it.copy(format = v) } }
                GroupChoice(stringResource(R.string.keep_played_music_on_the_phone), CACHE_SIZES, s.cacheMb) { v -> update { it.copy(cacheMb = v) } }
            }
            Text(
                stringResource(R.string.a_change_to_the_cache_size),
                style = MaterialTheme.typography.bodySmall,
                color = CtrlColors.Muted,
                modifier = Modifier.padding(horizontal = Gutter + 4.dp),
            )

            Group(stringResource(R.string.downloads)) {
                GroupChoice(stringResource(R.string.download_quality), STREAM_QUALITY, s.downloadBitrate) { v -> update { it.copy(downloadBitrate = v) } }
                GroupSwitch(stringResource(R.string.download_on_wi_fi_only), checked = s.downloadWifiOnly) { v -> update { it.copy(downloadWifiOnly = v) } }
            }

            Group(stringResource(R.string.listening_history), footer = stringResource(R.string.navidrome_passes_plays_on_to_last)) {
                GroupSwitch(stringResource(R.string.report_what_i_play), stringResource(R.string.updates_play_counts_recently_played_and), s.scrobble) { v -> update { it.copy(scrobble = v) } }
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
        topBar = { ScreenTopBar(stringResource(R.string.servers_and_accounts)) },
    ) { padding ->
        Column(Modifier.padding(padding).fillMaxSize().verticalScroll(rememberScrollState())) {
            Group(footer = stringResource(R.string.tap_an_account_to_switch_to)) {
                accounts.forEach { a ->
                    val isActive = a.key == active?.account?.key
                    GroupRow(
                        a.username,
                        if (a.isEncrypted) a.serverUrl else stringResource(R.string.x_not_encrypted_http, a.serverUrl),
                        onClick = if (isActive) null else {
                            {
                                c.music.reset()
                                scope.launch { c.sessions.switchTo(a.key) }
                                Unit
                            }
                        },
                        trailing = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                if (isActive) {
                                    Text(stringResource(R.string.active).uppercase(), style = CtrlType.Label, color = CtrlColors.Accent)
                                    Spacer(Modifier.width(8.dp))
                                    StatusDot(active = true)
                                }
                                IconButton(onClick = { removing = a.key }) { Icon(Icons.AutoMirrored.Filled.Logout, stringResource(R.string.sign_out_of_this_account), Modifier.size(20.dp), tint = CtrlColors.Muted) }
                            }
                        },
                    )
                }
            }
            Group {
                GroupRow(stringResource(R.string.add_a_server_or_account), icon = Icons.Filled.Add, showChevron = true, onClick = { nav.navigate(AddAccountRoute) })
            }
        }
    }

    removing?.let { key ->
        val acc = accounts.firstOrNull { it.key == key }
        ConfirmDialog(
            acc?.serverName?.let { stringResource(R.string.sign_out_of_x, it) } ?: stringResource(R.string.sign_out_of_this_server),
            stringResource(R.string.you_can_sign_in_again_any),
            stringResource(R.string.sign_out),
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
            ScreenTopBar(stringResource(R.string.downloads)) {
                if (mine.isNotEmpty()) TextButton(onClick = { clearing = true }, colors = ButtonDefaults.textButtonColors(contentColor = CtrlColors.Accent)) { Text(stringResource(R.string.remove_all)) }
            }
        },
    ) { padding ->
        if (mine.isEmpty()) {
            Box(Modifier.padding(padding).fillMaxSize()) {
                MessageView(
                    Icons.Outlined.DownloadForOffline,
                    stringResource(R.string.no_downloads),
                    stringResource(R.string.download_songs_albums_or_playlists_from),
                )
            }
            return@Scaffold
        }
        LazyColumn(Modifier.padding(padding).fillMaxSize(), contentPadding = PaddingValues(bottom = 24.dp + LocalBottomClearance.current)) {
            item(key = "summary") {
                CtrlLabel(
                    pluralStringResource(R.plurals.downloads_summary, done.size, done.size, formatBytes(c.downloads.usedBytes(all))),
                    Modifier.padding(horizontal = Gutter),
                    maxLines = 2,
                )
            }
            if (pending.isNotEmpty()) {
                item(key = "pending-h") {
                    CtrlLabel(stringResource(R.string.in_progress), Modifier.padding(start = Gutter, top = 20.dp, bottom = 8.dp))
                }
                items(pending, key = { "p-${it.song.id}" }) { e ->
                    Row(Modifier.fillMaxWidth().topDivider().padding(start = Gutter, end = 4.dp, top = 6.dp, bottom = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            Modifier.size(40.dp).clip(CtrlShapes.Badge).background(CtrlColors.Raised),
                            contentAlignment = Alignment.Center,
                        ) {
                            when (e.status) {
                                DownloadStatus.FAILED -> Icon(Icons.Filled.ErrorOutline, stringResource(R.string.failed), tint = MaterialTheme.colorScheme.error)
                                DownloadStatus.RUNNING -> CircularProgressIndicator(progress = { e.progress }, modifier = Modifier.size(24.dp), strokeWidth = 3.dp, color = CtrlColors.Accent, trackColor = CtrlColors.ProgressTrack)
                                else -> CircularProgressIndicator(Modifier.size(24.dp), strokeWidth = 2.dp, color = CtrlColors.Muted)
                            }
                        }
                        Spacer(Modifier.width(14.dp))
                        Column(Modifier.weight(1f)) {
                            Text(e.song.title, style = MaterialTheme.typography.bodyLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text(
                                when (e.status) {
                                    DownloadStatus.FAILED -> e.error ?: stringResource(R.string.failed)
                                    DownloadStatus.RUNNING -> "${(e.progress * 100).toInt()}%"
                                    else -> if (c.settings.settings.value.downloadWifiOnly) stringResource(R.string.waiting_for_wi_fi) else stringResource(R.string.waiting)
                                },
                                style = CtrlType.Mono,
                                color = if (e.status == DownloadStatus.FAILED) CtrlColors.Error else CtrlColors.Muted,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            if (e.status == DownloadStatus.RUNNING) CtrlProgressBar(e.progress, Modifier.padding(top = 6.dp), accent = true)
                        }
                        if (e.status == DownloadStatus.FAILED) IconButton(onClick = { c.downloads.retry(e) }) { Icon(Icons.Filled.Refresh, stringResource(R.string.retry)) }
                        IconButton(onClick = { c.downloads.remove(listOf(e.song.id)) }) { Icon(Icons.Outlined.Delete, stringResource(R.string.cancel)) }
                    }
                }
            }
            if (done.isNotEmpty()) {
                val songs = done.map { it.song }
                item(key = "done-h") {
                    CtrlLabel(stringResource(R.string.on_this_phone), Modifier.padding(start = Gutter, top = 20.dp, bottom = 4.dp))
                    PlayShuffleRow(onPlay = { c.music.play(songs) }, onShuffle = { c.music.shuffle(songs) })
                }
                items(songs.size, key = { "d-${songs[it].id}" }) { i ->
                    SongRow(songs[i], onClick = { c.music.play(songs, i) })
                }
            }
        }
    }

    if (clearing) {
        ConfirmDialog(stringResource(R.string.remove_all_downloads), stringResource(R.string.frees_x_on_this_phone, formatBytes(c.downloads.usedBytes(all))), stringResource(R.string.remove), onDismiss = { clearing = false }) {
            clearing = false
            c.downloads.removeAll()
        }
    }
}
