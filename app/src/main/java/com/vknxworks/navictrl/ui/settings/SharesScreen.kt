package com.vknxworks.navictrl.ui.settings

import android.content.ClipData
import android.content.ClipboardManager
import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.vknxworks.navictrl.R
import com.vknxworks.navictrl.container
import com.vknxworks.navictrl.data.api.Share
import com.vknxworks.navictrl.data.api.deleteShare
import com.vknxworks.navictrl.data.api.shares
import com.vknxworks.navictrl.str
import com.vknxworks.navictrl.ui.components.ConfirmDialog
import com.vknxworks.navictrl.ui.components.LoadContent
import com.vknxworks.navictrl.ui.components.MessageView
import com.vknxworks.navictrl.ui.components.ScreenTopBar
import com.vknxworks.navictrl.ui.components.formatDate
import com.vknxworks.navictrl.ui.components.isoToEpochMs
import com.vknxworks.navictrl.ui.components.rememberLoader
import com.vknxworks.navictrl.ui.components.runTask
import com.vknxworks.navictrl.ui.components.sendShareLink
import com.vknxworks.navictrl.ui.components.topDivider
import com.vknxworks.navictrl.ui.theme.CtrlColors
import com.vknxworks.navictrl.ui.theme.CtrlType
import com.vknxworks.navictrl.ui.theme.Gutter

/** The share links on the server: send one again, copy it, or delete it (which stops it working). */
@Composable
fun SharesScreen() {
    val context = LocalContext.current
    val c = context.container
    val loader = rememberLoader("shares") { c.api.shares() }
    var picked by remember { mutableStateOf<Share?>(null) }
    var deleting by remember { mutableStateOf<Share?>(null) }

    Scaffold(
        contentWindowInsets = WindowInsets(0),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = { ScreenTopBar(stringResource(R.string.share_links)) },
    ) { padding ->
        LoadContent(loader, Modifier.padding(padding)) { list ->
            if (list.isEmpty()) {
                Box(Modifier.padding(padding).fillMaxSize()) {
                    MessageView(Icons.Outlined.Share, stringResource(R.string.share_none), stringResource(R.string.share_none_body))
                }
            } else {
                LazyColumn(Modifier.padding(padding).fillMaxSize(), contentPadding = PaddingValues(bottom = 24.dp)) {
                    items(list, key = { it.id }) { share -> ShareRow(share) { picked = share } }
                }
            }
        }
    }

    picked?.let { share ->
        ModalBottomSheet(onDismissRequest = { picked = null }, containerColor = CtrlColors.Surface) {
            Column(Modifier.navigationBarsPadding().padding(bottom = 8.dp)) {
                Text(
                    share.title(),
                    style = MaterialTheme.typography.titleLarge,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp),
                )
                SheetRow(Icons.Outlined.Share, stringResource(R.string.share_send)) {
                    picked = null
                    sendShareLink(context, share.url, share.title())
                }
                SheetRow(Icons.Outlined.ContentCopy, stringResource(R.string.share_copy)) {
                    picked = null
                    // Anyone with the link can stream the shared songs without signing in: keep it out of
                    // clipboard previews and keyboards' clipboard history.
                    val clip = ClipData.newPlainText(str(R.string.share_link), share.url).apply {
                        description.extras = android.os.PersistableBundle().apply {
                            putBoolean(android.content.ClipDescription.EXTRA_IS_SENSITIVE, true)
                        }
                    }
                    context.getSystemService(ClipboardManager::class.java)?.setPrimaryClip(clip)
                }
                SheetRow(Icons.Outlined.Delete, stringResource(R.string.share_delete)) {
                    picked = null
                    deleting = share
                }
            }
        }
    }

    deleting?.let { share ->
        ConfirmDialog(
            stringResource(R.string.share_delete_title),
            stringResource(R.string.share_delete_body, share.title()),
            stringResource(R.string.delete),
            onDismiss = { deleting = null },
        ) {
            deleting = null
            c.runTask {
                c.api.deleteShare(share.id)
                Toast.makeText(context, str(R.string.share_deleted), Toast.LENGTH_SHORT).show()
                loader.reload(silent = true)
            }
        }
    }
}

private fun Share.title(): String = description?.takeIf { it.isNotBlank() } ?: url

@Composable
private fun ShareRow(share: Share, onClick: () -> Unit) {
    val expiresAt = isoToEpochMs(share.expires)
    val expired = expiresAt != null && expiresAt < System.currentTimeMillis()
    val expiry = when {
        expiresAt == null -> stringResource(R.string.share_no_expiry)
        expired -> stringResource(R.string.share_expired_x, formatDate(share.expires).orEmpty())
        else -> stringResource(R.string.share_expires_x, formatDate(share.expires).orEmpty())
    }
    Column(
        Modifier.fillMaxWidth().topDivider().clickable(onClick = onClick).heightIn(min = 62.dp).padding(horizontal = Gutter, vertical = 10.dp),
    ) {
        Text(share.title(), style = MaterialTheme.typography.bodyLarge, maxLines = 2, overflow = TextOverflow.Ellipsis)
        Text(share.url, style = CtrlType.Mono, color = CtrlColors.Muted, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Text(
            expiry + "  ·  " + pluralStringResource(R.plurals.share_visits, share.visitCount, share.visitCount),
            style = CtrlType.Mono,
            color = if (expired) CtrlColors.Error else CtrlColors.Muted,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun SheetRow(icon: ImageVector, label: String, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().topDivider(start = 24.dp, end = 24.dp).heightIn(min = 52.dp).clickable(onClick = onClick).padding(horizontal = 24.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, null, Modifier.size(20.dp), tint = CtrlColors.Muted)
        Spacer(Modifier.width(20.dp))
        Text(label, style = MaterialTheme.typography.bodyLarge)
    }
}
