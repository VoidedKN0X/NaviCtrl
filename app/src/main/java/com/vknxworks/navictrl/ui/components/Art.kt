package com.vknxworks.navictrl.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.MusicNote
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.PlatformContext
import coil3.compose.LocalPlatformContext
import coil3.compose.SubcomposeAsyncImage
import coil3.request.ImageRequest
import com.vknxworks.navictrl.data.api.SubsonicClient
import com.vknxworks.navictrl.data.api.Song
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import com.vknxworks.navictrl.R
import com.vknxworks.navictrl.str
import androidx.annotation.PluralsRes
import com.vknxworks.navictrl.plural

/** Artwork with a quiet placeholder while loading and a note icon when there's none. */
@Composable
fun ArtImage(
    url: String?,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Crop,
    placeholderText: String? = null,
    placeholderIcon: ImageVector = Icons.Outlined.MusicNote,
) {
    val fallback: @Composable () -> Unit = {
        Box(
            Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surfaceContainerHigh),
            contentAlignment = Alignment.Center,
        ) {
            if (placeholderText != null) {
                Text(
                    placeholderText,
                    modifier = Modifier.fillMaxWidth().padding(10.dp),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                )
            } else {
                Icon(placeholderIcon, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
    if (url == null) {
        Box(modifier) { fallback() }
    } else {
        val context = LocalPlatformContext.current
        val request = remember(url) { artRequest(context, url).build() }
        SubcomposeAsyncImage(
            model = request,
            contentDescription = null,
            modifier = modifier,
            contentScale = contentScale,
            error = { fallback() },
            loading = { Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surfaceContainer)) },
        )
    }
}

/**
 * An image request for a server artwork URL. The URL carries auth parameters that change every
 * session, so the memory and disk caches are keyed on the URL without them.
 */
fun artRequest(context: PlatformContext, url: String): ImageRequest.Builder {
    val key = SubsonicClient.authFreeKey(url)
    return ImageRequest.Builder(context)
        .data(url)
        .memoryCacheKey(key)
        .diskCacheKey(key)
}

fun formatClock(ms: Long): String {
    val total = (ms / 1000).coerceAtLeast(0)
    val h = total / 3600
    val m = (total % 3600) / 60
    val s = total % 60
    return if (h > 0) "%d:%02d:%02d".format(h, m, s) else "%d:%02d".format(m, s)
}

/** "1 hr 12 min" or "42 min" for album and playlist lengths. */
fun formatLength(seconds: Int): String? {
    if (seconds <= 0) return null
    val min = (seconds + 30) / 60
    return if (min >= 60) str(R.string.x_hr_x_min, min / 60, min % 60) else str(R.string.x_min, min)
}

fun formatBytes(bytes: Long): String {
    if (bytes < 1024) return "$bytes B"
    val units = listOf("KB", "MB", "GB", "TB")
    var v = bytes / 1024.0
    var i = 0
    while (v >= 1024 && i < units.lastIndex) { v /= 1024; i++ }
    return "%.1f %s".format(v, units[i])
}

fun countLabel(n: Int, @PluralsRes id: Int): String = plural(id, n, n)

fun isoToEpochMs(iso: String?): Long? = iso?.let { s ->
    runCatching { Instant.parse(s).toEpochMilli() }.getOrNull()
        ?: runCatching { java.time.OffsetDateTime.parse(s).toInstant().toEpochMilli() }.getOrNull()
}

fun formatDate(iso: String?): String? = isoToEpochMs(iso)?.let {
    DateTimeFormatter.ofPattern("d MMM yyyy").withZone(ZoneId.systemDefault()).format(Instant.ofEpochMilli(it))
}

/** The line under a song title: artist, and album when it helps. */
fun Song.subtitle(withAlbum: Boolean = true): String =
    listOfNotNull(artist?.takeIf { it.isNotBlank() }, album?.takeIf { withAlbum && it.isNotBlank() }).joinToString("  ·  ")

/** Letter used for alphabetical headers and the fast-scroll bubble. */
fun sortLetter(text: String?): String {
    val c = text?.trim()?.removePrefix("The ")?.firstOrNull { it.isLetterOrDigit() } ?: return "#"
    return if (c.isLetter()) c.uppercaseChar().toString() else "#"
}
