package com.vknxworks.navictrl.music

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import androidx.media3.common.util.BitmapLoader
import androidx.media3.datasource.DataSourceBitmapLoader
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.okhttp.OkHttpDataSource
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import com.vknxworks.navictrl.data.api.SubsonicClient
import com.vknxworks.navictrl.data.api.coverArtUrl
import okhttp3.OkHttpClient

/**
 * Artwork URIs put in MediaMetadata. They carry only the cover id and size, never the auth token,
 * because media metadata is shared with other apps (system UI, Bluetooth, Android Auto).
 */
object ArtworkUris {
    const val SCHEME = "navictrl-art"
    private const val HOST = "cover"

    fun forCover(coverArt: String?, size: Int): Uri? {
        if (coverArt.isNullOrBlank()) return null
        return Uri.Builder()
            .scheme(SCHEME)
            .authority(HOST)
            .appendPath(coverArt)
            .appendQueryParameter("size", size.toString())
            .build()
    }

    /** The real, authenticated artwork URL for one of our URIs; null for anything else. */
    fun resolve(uri: Uri, client: SubsonicClient?): Uri? {
        if (uri.scheme != SCHEME || uri.authority != HOST || client == null) return null
        val id = uri.lastPathSegment?.takeIf { it.isNotBlank() } ?: return null
        val size = uri.getQueryParameter("size")?.toIntOrNull() ?: 600
        return client.coverArtUrl(id, size)?.let(Uri::parse)
    }
}

/**
 * Loads notification and lock-screen artwork inside the app process: [ArtworkUris] are turned into
 * authenticated requests here, so the token never leaves the app.
 */
class ArtworkBitmapLoader(
    context: Context,
    http: OkHttpClient,
    private val client: () -> SubsonicClient?,
) : BitmapLoader {
    private val delegate = DataSourceBitmapLoader(
        DataSourceBitmapLoader.DEFAULT_EXECUTOR_SERVICE.get(),
        DefaultDataSource.Factory(context, OkHttpDataSource.Factory(http)),
    )

    override fun supportsMimeType(mimeType: String): Boolean = delegate.supportsMimeType(mimeType)

    override fun decodeBitmap(data: ByteArray): ListenableFuture<Bitmap> = delegate.decodeBitmap(data)

    override fun loadBitmap(uri: Uri): ListenableFuture<Bitmap> {
        val real = ArtworkUris.resolve(uri, client())
            ?: return Futures.immediateFailedFuture<Bitmap>(IllegalArgumentException("No artwork for $uri"))
        return delegate.loadBitmap(real)
    }
}
