package com.vknxworks.navictrl.music

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.vknxworks.navictrl.R
import com.vknxworks.navictrl.container
import com.vknxworks.navictrl.ui.components.ctrlDialogBorder
import com.vknxworks.navictrl.ui.components.formatClock
import com.vknxworks.navictrl.ui.components.isoToEpochMs
import com.vknxworks.navictrl.ui.theme.CtrlColors
import com.vknxworks.navictrl.ui.theme.CtrlShapes
import com.vknxworks.navictrl.ui.theme.CtrlType
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/** "Continue from another device?": the play queue saved on the server differs from the one here. */
@Composable
fun RemoteQueueDialog() {
    val music = LocalContext.current.container.music
    val offer by music.remoteOffer.collectAsStateWithLifecycle()
    val o = offer ?: return
    val q = o.queue
    val from = q.changedBy?.takeIf { it.isNotBlank() } ?: stringResource(R.string.queue_sync_unknown_app)
    val whenText = isoToEpochMs(q.changed)?.let {
        DateTimeFormatter.ofPattern("d MMM, HH:mm").withZone(ZoneId.systemDefault()).format(Instant.ofEpochMilli(it))
    }
    AlertDialog(
        onDismissRequest = { music.declineRemote() },
        title = { Text(stringResource(R.string.queue_sync_offer_title), style = MaterialTheme.typography.titleLarge) },
        text = {
            Column {
                Text(
                    listOfNotNull(o.current.title, o.current.artist?.takeIf { it.isNotBlank() }).joinToString(" — "),
                    style = MaterialTheme.typography.bodyLarge,
                    color = CtrlColors.Text,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    stringResource(R.string.queue_sync_offer_at, formatClock(q.position)) + "  ·  " +
                        pluralStringResource(R.plurals.count_songs, q.entry.size, q.entry.size),
                    style = CtrlType.Mono,
                    color = CtrlColors.Muted,
                )
                Spacer(Modifier.height(12.dp))
                Text(
                    if (whenText != null) stringResource(R.string.queue_sync_offer_from_when, from, whenText)
                    else stringResource(R.string.queue_sync_offer_from, from),
                    style = MaterialTheme.typography.bodyMedium,
                    color = CtrlColors.Muted,
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { music.acceptRemote() }, colors = ButtonDefaults.textButtonColors(contentColor = CtrlColors.AccentText)) {
                Text(stringResource(R.string.queue_sync_continue))
            }
        },
        dismissButton = {
            TextButton(onClick = { music.declineRemote() }, colors = ButtonDefaults.textButtonColors(contentColor = CtrlColors.Muted)) {
                Text(stringResource(R.string.queue_sync_keep))
            }
        },
        shape = CtrlShapes.Hero,
        containerColor = CtrlColors.Surface,
        modifier = Modifier.ctrlDialogBorder(),
    )
}
