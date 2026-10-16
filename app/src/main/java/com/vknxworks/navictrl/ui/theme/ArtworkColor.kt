package com.vknxworks.navictrl.ui.theme

import android.util.LruCache
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.palette.graphics.Palette
import coil3.BitmapImage
import coil3.SingletonImageLoader
import com.vknxworks.navictrl.ui.components.artRequest
import coil3.request.SuccessResult
import coil3.request.allowHardware
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private val cache = LruCache<String, Int>(200)

/** A representative colour from an image, or null until it's known (or if the image has none). */
@Composable
fun rememberArtworkColor(url: String?): Color? {
    val context = LocalContext.current
    var color by remember(url) { mutableStateOf(url?.let { cache.get(it) }?.let { Color(it) }) }
    LaunchedEffect(url) {
        if (url == null || color != null) return@LaunchedEffect
        val result = runCatching {
            val request = artRequest(context, url)
                .size(160)
                .allowHardware(false)
                .build()
            val image = (SingletonImageLoader.get(context).execute(request) as? SuccessResult)?.image
            val bitmap = (image as? BitmapImage)?.bitmap ?: return@runCatching null
            withContext(Dispatchers.Default) {
                val p = Palette.from(bitmap).maximumColorCount(16).generate()
                (p.vibrantSwatch ?: p.lightVibrantSwatch ?: p.mutedSwatch ?: p.dominantSwatch)?.rgb
            }
        }.getOrNull()
        if (result != null) {
            cache.put(url, result)
            color = Color(result)
        }
    }
    return color
}
