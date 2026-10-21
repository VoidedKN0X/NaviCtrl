package com.vknxworks.navictrl.ui.settings

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Gavel
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.vknxworks.navictrl.BuildConfig
import com.vknxworks.navictrl.R
import com.vknxworks.navictrl.container
import com.vknxworks.navictrl.data.api.scanStatus
import com.vknxworks.navictrl.data.api.startScan
import com.vknxworks.navictrl.data.api.user
import com.vknxworks.navictrl.data.download.DownloadStatus
import com.vknxworks.navictrl.ui.components.ConfirmDialog
import com.vknxworks.navictrl.ui.components.Group
import com.vknxworks.navictrl.ui.components.GroupRow
import com.vknxworks.navictrl.ui.components.LocalBottomClearance
import com.vknxworks.navictrl.ui.components.ScreenTopBar
import com.vknxworks.navictrl.ui.components.formatBytes
import com.vknxworks.navictrl.ui.components.friendlyMessage
import com.vknxworks.navictrl.ui.components.rememberLoader
import com.vknxworks.navictrl.ui.nav.AccountsRoute
import com.vknxworks.navictrl.ui.nav.DownloadsRoute
import com.vknxworks.navictrl.ui.nav.LocalNavigator
import com.vknxworks.navictrl.ui.nav.SettingsRoute
import com.vknxworks.navictrl.ui.theme.Gutter
import kotlinx.coroutines.launch
import java.text.NumberFormat
import com.vknxworks.navictrl.str
import androidx.compose.ui.res.pluralStringResource

private const val SPONSOR_URL = "https://github.com/sponsors/VoidedKN0X"
private const val REPO_URL = "https://github.com/VoidedKN0X/NaviCtrl"
private const val ISSUES_URL = "$REPO_URL/issues"
private const val LICENCE_URL = "https://www.gnu.org/licenses/gpl-3.0.html"

/** The You page, opened from your profile picture: who you're signed in as, your stuff, and where the settings live. */
@Composable
fun YouScreen() {
    val context = LocalContext.current
    val c = context.container
    val nav = LocalNavigator.current
    val scope = rememberCoroutineScope()
    val session by c.sessions.active.collectAsStateWithLifecycle()
    val downloads by c.downloads.entries.collectAsStateWithLifecycle()
    val acc = session?.account ?: return
    // Refresh the admin flag in case the user's role changed on the server.
    rememberLoader("you-user") { runCatching { c.api.user() }.getOrNull()?.let { c.sessions.updateAdmin(it.adminRole) } }
    val scan = rememberLoader("you-scan") { c.api.scanStatus() }
    var scanning by remember { mutableStateOf(false) }
    var confirmSignOut by remember { mutableStateOf(false) }
    val mine = c.downloads.forActive(downloads)
    val ready = mine.count { it.status == DownloadStatus.DONE }
    fun open(url: String) {
        runCatching { com.vknxworks.navictrl.ui.lock.AppLock.expectReturn(); context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)).addCategory(Intent.CATEGORY_BROWSABLE)) }
    }

    Column(Modifier.fillMaxSize()) {
        ScreenTopBar(stringResource(R.string.you))
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(bottom = 24.dp + LocalBottomClearance.current)) {
            Column(Modifier.fillMaxWidth().padding(start = Gutter, end = Gutter, top = 8.dp, bottom = 24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Box(Modifier.size(96.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary.copy(alpha = 0.18f)), contentAlignment = Alignment.Center) {
                    Text(acc.username.take(1).uppercase(), style = MaterialTheme.typography.displaySmall, color = MaterialTheme.colorScheme.primary)
                }
                Spacer(Modifier.height(14.dp))
                Text(acc.username, style = MaterialTheme.typography.headlineMedium)
                Text(
                    listOfNotNull(acc.serverName, acc.serverVersion?.let { stringResource(R.string.navidrome_x, it.substringBefore(' ')) }).joinToString("  ·  "),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            Group {
                GroupRow(
                    stringResource(R.string.downloads),
                    if (ready == 0) stringResource(R.string.keep_music_on_this_phone_for) else pluralStringResource(R.plurals.downloads_ready, ready, ready, formatBytes(c.downloads.usedBytes(downloads))),
                    icon = Icons.Filled.Download,
                    showChevron = true,
                    onClick = { nav.navigate(DownloadsRoute) },
                )
            }

            val status = scan.value
            Group(stringResource(R.string.server), footer = status?.lastScan?.let { stringResource(R.string.last_scanned_x, com.vknxworks.navictrl.ui.components.formatDate(it).orEmpty()) }) {
                GroupRow(
                    acc.serverUrl,
                    listOfNotNull(
                        if (acc.isEncrypted) null else stringResource(R.string.not_encrypted_http),
                        status?.let { pluralStringResource(R.plurals.library_files, it.count.toInt(), NumberFormat.getIntegerInstance().format(it.count)) },
                    ).joinToString("  ·  ").ifEmpty { null },
                    icon = Icons.Filled.Dns,
                )
                if (acc.isAdmin) {
                    GroupRow(
                        stringResource(R.string.scan_for_new_music),
                        if (status?.scanning == true || scanning) stringResource(R.string.scanning) else stringResource(R.string.picks_up_files_added_since_the),
                        icon = Icons.Filled.Refresh,
                        onClick = {
                            scanning = true
                            scope.launch {
                                runCatching { c.api.startScan() }
                                    .onSuccess { Toast.makeText(context, str(R.string.scan_started), Toast.LENGTH_SHORT).show() }
                                    .onFailure { Toast.makeText(context, it.friendlyMessage(), Toast.LENGTH_SHORT).show() }
                                scanning = false
                                scan.reload(silent = true)
                            }
                        },
                        trailing = {
                            if (status?.scanning == true || scanning) CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
                        },
                    )
                }
            }

            Group {
                GroupRow(stringResource(R.string.settings), icon = Icons.Filled.Settings, showChevron = true, onClick = { nav.navigate(SettingsRoute) })
                GroupRow(stringResource(R.string.servers_and_accounts), icon = Icons.Filled.SwapHoriz, showChevron = true, onClick = { nav.navigate(AccountsRoute) })
                GroupRow(
                    stringResource(R.string.sign_out),
                    icon = Icons.AutoMirrored.Filled.Logout,
                    iconTint = MaterialTheme.colorScheme.error,
                    titleColor = MaterialTheme.colorScheme.error,
                    onClick = { confirmSignOut = true },
                )
            }

            Group(stringResource(R.string.group_about)) {
                GroupRow(
                    stringResource(R.string.support_title),
                    stringResource(R.string.support_subtitle),
                    icon = Icons.Filled.Favorite,
                    showChevron = true,
                    onClick = { open(SPONSOR_URL) },
                )
                GroupRow(
                    stringResource(R.string.bugs_title),
                    stringResource(R.string.bugs_subtitle),
                    icon = Icons.Filled.BugReport,
                    showChevron = true,
                    onClick = { open(ISSUES_URL) },
                )
                GroupRow(
                    stringResource(R.string.source_title),
                    stringResource(R.string.source_subtitle),
                    icon = Icons.Filled.Code,
                    showChevron = true,
                    onClick = { open(REPO_URL) },
                )
                GroupRow(
                    stringResource(R.string.licence_title),
                    stringResource(R.string.licence_subtitle),
                    icon = Icons.Filled.Gavel,
                    showChevron = true,
                    onClick = { open(LICENCE_URL) },
                )
            }
            Text(
                stringResource(R.string.about_footer, BuildConfig.VERSION_NAME),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            )
        }
    }

    if (confirmSignOut) {
        ConfirmDialog(
            stringResource(R.string.sign_out_of_x, acc.serverName),
            stringResource(R.string.downloads_for_this_account_stay_on),
            stringResource(R.string.sign_out),
            onDismiss = { confirmSignOut = false },
        ) {
            confirmSignOut = false
            c.music.reset()
            c.appScope.launch { c.signOut(acc.key) }
        }
    }
}
