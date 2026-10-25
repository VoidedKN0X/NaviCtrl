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
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.em
import androidx.compose.ui.text.style.TextOverflow
import com.vknxworks.navictrl.ui.components.CtrlBackButton
import com.vknxworks.navictrl.ui.components.CtrlButton
import com.vknxworks.navictrl.ui.components.CtrlLabel
import com.vknxworks.navictrl.ui.components.initials
import com.vknxworks.navictrl.ui.theme.CtrlColors
import com.vknxworks.navictrl.ui.theme.CtrlShapes
import com.vknxworks.navictrl.ui.theme.CtrlType
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Logout
import androidx.compose.material.icons.outlined.BugReport
import androidx.compose.material.icons.outlined.Code
import androidx.compose.material.icons.outlined.Dns
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Gavel
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material.icons.outlined.SwapHoriz
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
        // Header: close on the left, mono "PROFILE" in the middle (as in the Ctrl / Signal profile screen).
        Row(
            Modifier.fillMaxWidth().statusBarsPadding().padding(start = Gutter, end = Gutter, top = 12.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            CtrlBackButton(icon = Icons.Outlined.Close, description = stringResource(R.string.close))
            CtrlLabel(stringResource(R.string.you), Modifier.weight(1f), textAlign = TextAlign.Center)
            Spacer(Modifier.size(44.dp))
        }
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(bottom = 24.dp + LocalBottomClearance.current)) {
            Row(
                Modifier.fillMaxWidth().padding(start = Gutter, end = Gutter, top = 12.dp, bottom = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(Modifier.size(68.dp).clip(CtrlShapes.Card).background(CtrlColors.Accent), contentAlignment = Alignment.Center) {
                    Text(initials(acc.username), style = MaterialTheme.typography.headlineSmall, color = CtrlColors.OnAccent)
                }
                Spacer(Modifier.width(16.dp))
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(acc.username, style = MaterialTheme.typography.headlineSmall, color = CtrlColors.Text, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(acc.serverUrl, style = CtrlType.Mono, color = CtrlColors.Muted, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    val sub = listOfNotNull(acc.serverName, acc.serverVersion?.let { stringResource(R.string.navidrome_x, it.substringBefore(' ')) }).joinToString("  ·  ")
                    if (sub.isNotBlank()) Text(sub, style = CtrlType.Mono, color = CtrlColors.Muted, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }

            Group {
                GroupRow(
                    stringResource(R.string.downloads),
                    if (ready == 0) stringResource(R.string.keep_music_on_this_phone_for) else pluralStringResource(R.plurals.downloads_ready, ready, ready, formatBytes(c.downloads.usedBytes(downloads))),
                    icon = Icons.Outlined.Download,
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
                    icon = Icons.Outlined.Dns,
                )
                if (acc.isAdmin) {
                    GroupRow(
                        stringResource(R.string.scan_for_new_music),
                        if (status?.scanning == true || scanning) stringResource(R.string.scanning) else stringResource(R.string.picks_up_files_added_since_the),
                        icon = Icons.Outlined.Refresh,
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
                            if (status?.scanning == true || scanning) CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp, color = CtrlColors.AccentText)
                        },
                    )
                }
            }

            Group {
                GroupRow(stringResource(R.string.settings), icon = Icons.Outlined.Tune, showChevron = true, onClick = { nav.navigate(SettingsRoute) })
                GroupRow(stringResource(R.string.servers_and_accounts), icon = Icons.Outlined.SwapHoriz, showChevron = true, onClick = { nav.navigate(AccountsRoute) })
                GroupRow(
                    stringResource(R.string.sign_out),
                    icon = Icons.AutoMirrored.Outlined.Logout,
                    iconTint = MaterialTheme.colorScheme.error,
                    titleColor = MaterialTheme.colorScheme.error,
                    onClick = { confirmSignOut = true },
                )
            }

            Group(stringResource(R.string.group_about)) {
                GroupRow(
                    stringResource(R.string.bugs_title),
                    stringResource(R.string.bugs_subtitle),
                    icon = Icons.Outlined.BugReport,
                    showChevron = true,
                    onClick = { open(ISSUES_URL) },
                )
                GroupRow(
                    stringResource(R.string.source_title),
                    stringResource(R.string.source_subtitle),
                    icon = Icons.Outlined.Code,
                    showChevron = true,
                    onClick = { open(REPO_URL) },
                )
                GroupRow(
                    stringResource(R.string.licence_title),
                    stringResource(R.string.licence_subtitle),
                    icon = Icons.Outlined.Gavel,
                    showChevron = true,
                    onClick = { open(LICENCE_URL) },
                )
            }
            Spacer(Modifier.height(16.dp))
            CtrlButton(
                stringResource(R.string.support_title),
                onClick = { open(SPONSOR_URL) },
                icon = Icons.Outlined.FavoriteBorder,
                modifier = Modifier.fillMaxWidth().padding(horizontal = Gutter),
            )
            Text(
                stringResource(R.string.support_subtitle),
                style = MaterialTheme.typography.bodySmall,
                color = CtrlColors.Muted,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().padding(start = Gutter, end = Gutter, top = 8.dp),
            )
            Spacer(Modifier.height(20.dp))
            Text(
                "GOOGLE-FREE · TELEMETRY-FREE · ARM64",
                style = CtrlType.Label.copy(letterSpacing = 0.12.em),
                color = CtrlColors.Muted,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().padding(horizontal = Gutter),
            )
            Text(
                stringResource(R.string.about_footer, BuildConfig.VERSION_NAME).uppercase(),
                style = CtrlType.Mono,
                color = CtrlColors.Muted,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().padding(start = Gutter, end = Gutter, top = 6.dp, bottom = 16.dp),
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
