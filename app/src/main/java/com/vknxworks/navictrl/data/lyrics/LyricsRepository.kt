package com.vknxworks.navictrl.data.lyrics

import android.content.Context
import com.vknxworks.navictrl.BuildConfig
import com.vknxworks.navictrl.data.api.ApiJson
import com.vknxworks.navictrl.data.api.Lyrics
import com.vknxworks.navictrl.data.api.Song
import com.vknxworks.navictrl.data.api.SubsonicClient
import com.vknxworks.navictrl.data.api.serverLyrics
import com.vknxworks.navictrl.data.auth.SessionManager
import com.vknxworks.navictrl.data.local.LocalServer
import com.vknxworks.navictrl.data.settings.SettingsRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import java.io.File

/**
 * Lyrics for the song that's playing: from the cache on the phone, else the server, else LRCLIB
 * (when "Find lyrics online" is on and the account is a server, never for music on this phone).
 * Every answer, including "none found", is cached per account and song so a song is looked up once.
 */
class LyricsRepository(
    private val context: Context,
    http: OkHttpClient,
    private val sessions: SessionManager,
    private val settings: SettingsRepository,
) {
    private val root = File(context.cacheDir, "lyrics")
    private val lrclib = Lrclib(http, Lrclib.userAgent(BuildConfig.VERSION_NAME))
    private val lock = Mutex()
    private var writes = 0

    /** Lyrics for [song]; [refresh] ("Search again") skips the cache. Throws when no source could be reached. */
    suspend fun lyrics(song: Song, refresh: Boolean = false): Lyrics? {
        val session = sessions.active.value ?: return null
        val client = session.client
        val accountKey = session.account.key
        val local = LocalServer.isLocal(client.baseUrl)
        val onlineOn = settings.settings.value.lyricsOnline && !local
        val file = fileFor(accountKey, song.id)
        val now = System.currentTimeMillis()
        if (!refresh) {
            val cached = read(file)
            if (LyricsResolver.cacheValid(cached, now, onlineOn)) return cached?.lyrics
        }
        val resolved = LyricsResolver.resolve(
            now = now,
            server = { client.serverLyrics(song) },
            online = if (onlineOn) ({ lrclib.find(song) }) else null,
        )
        resolved.cache?.let { write(file, it) }
        return resolved.lyrics
    }

    /** Signing out: forget that account's lyrics. */
    suspend fun forgetAccount(accountKey: String) = withContext(Dispatchers.IO) {
        lock.withLock { File(root, accountDir(accountKey)).deleteRecursively() }
    }

    /** "Forgot PIN?": forget every cached lyric. */
    suspend fun forgetEverything() = withContext(Dispatchers.IO) {
        lock.withLock { root.deleteRecursively() }
    }

    private fun fileFor(accountKey: String, songId: String): File =
        // Song ids come from the server: hashed, never used as a file name directly.
        File(File(root, accountDir(accountKey)), SubsonicClient.sha256(songId).take(32) + ".json")

    private fun accountDir(accountKey: String) = SubsonicClient.md5(accountKey).take(12)

    private suspend fun read(file: File): CachedLyrics? = withContext(Dispatchers.IO) {
        lock.withLock {
            if (!file.isFile || file.length() > MAX_FILE) return@withLock null
            runCatching { ApiJson.decodeFromString(CachedLyrics.serializer(), file.readText()) }.getOrNull()
        }
    }

    private suspend fun write(file: File, entry: CachedLyrics) = withContext(Dispatchers.IO) {
        lock.withLock {
            runCatching {
                file.parentFile?.mkdirs()
                val tmp = File(file.parentFile, file.name + ".tmp")
                tmp.writeText(ApiJson.encodeToString(CachedLyrics.serializer(), entry))
                if (!tmp.renameTo(file)) { tmp.copyTo(file, overwrite = true); tmp.delete() }
                if (writes++ % 50 == 0) trim()
            }
        }
    }

    /** Keeps the cache bounded: the oldest entries go first once there are too many. */
    private fun trim() {
        val files = root.walkTopDown().filter { it.isFile && it.name.endsWith(".json") }.toList()
        if (files.size <= MAX_ENTRIES) return
        files.sortedBy { it.lastModified() }.take(files.size - MAX_ENTRIES).forEach { it.delete() }
    }

    companion object {
        /** About 2,000 songs; lyrics files are a few KB each. */
        const val MAX_ENTRIES = 2_000
        private const val MAX_FILE = 1024L * 1024
    }
}
