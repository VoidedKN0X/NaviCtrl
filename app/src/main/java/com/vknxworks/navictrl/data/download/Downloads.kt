package com.vknxworks.navictrl.data.download

import android.content.Context
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.vknxworks.navictrl.container
import com.vknxworks.navictrl.data.api.ApiJson
import com.vknxworks.navictrl.data.api.Song
import com.vknxworks.navictrl.data.api.SubsonicClient
import com.vknxworks.navictrl.data.api.downloadUrl
import com.vknxworks.navictrl.data.api.streamUrl
import com.vknxworks.navictrl.data.auth.SessionManager
import com.vknxworks.navictrl.data.settings.SettingsRepository
import com.vknxworks.navictrl.ui.components.friendlyMessage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import kotlin.coroutines.cancellation.CancellationException
import com.vknxworks.navictrl.R
import com.vknxworks.navictrl.str

enum class DownloadStatus { QUEUED, RUNNING, DONE, FAILED }

@Serializable
data class DownloadEntry(
    val song: Song,
    val accountKey: String,
    val status: DownloadStatus = DownloadStatus.QUEUED,
    val progress: Float = 0f,
    val file: String? = null,
    val bytes: Long = 0,
    val error: String? = null,
)

/** Songs saved for offline listening. Each runs as its own WorkManager job so downloads survive the app closing. */
class DownloadRepository(
    private val context: Context,
    private val scope: CoroutineScope,
    private val sessions: SessionManager,
    private val settings: SettingsRepository,
    private val http: OkHttpClient,
) {
    private val index = File(context.filesDir, "downloads.json")
    private val root = File(context.filesDir, "music").apply { mkdirs() }
    private val _entries = MutableStateFlow<List<DownloadEntry>>(emptyList())
    val entries: StateFlow<List<DownloadEntry>> = _entries.asStateFlow()
    private val loaded = MutableStateFlow(false)

    /** Ids of finished downloads for the signed-in account, for the small "offline" marks on rows. */
    val completedIds: StateFlow<Set<String>> = combine(_entries, sessions.active) { list, session ->
        val key = session?.account?.key
        list.filter { it.accountKey == key && it.status == DownloadStatus.DONE }.map { it.song.id }.toSet()
    }.stateIn(scope, SharingStarted.Eagerly, emptySet())

    init {
        scope.launch(Dispatchers.IO) {
            val saved = runCatching { ApiJson.decodeFromString(ListSerializer(DownloadEntry.serializer()), index.readText()) }.getOrNull().orEmpty()
            // Anything interrupted mid-download is queued again by WorkManager; show it as waiting.
            _entries.value = saved.map { if (it.status == DownloadStatus.RUNNING) it.copy(status = DownloadStatus.QUEUED, progress = 0f) else it }
            loaded.value = true
        }
    }

    private fun activeKey(): String? = sessions.active.value?.account?.key

    /** Downloads for the signed-in account. */
    fun forActive(list: List<DownloadEntry>): List<DownloadEntry> {
        val key = activeKey()
        return list.filter { it.accountKey == key }
    }

    /** The local file for a song, if it's fully downloaded for this account. */
    fun completedFile(id: String): File? {
        val key = activeKey() ?: return null
        val e = _entries.value.firstOrNull { it.song.id == id && it.accountKey == key && it.status == DownloadStatus.DONE } ?: return null
        // The stored path is used as-is (older downloads keep their original names), but only if it
        // is inside the app's music folder.
        return e.file?.let(::File)?.takeIf { it.exists() && isInside(root, it) }
    }

    fun statusOf(list: List<DownloadEntry>, id: String): DownloadEntry? {
        val key = activeKey()
        return list.firstOrNull { it.song.id == id && it.accountKey == key }
    }

    fun enqueue(songs: List<Song>) {
        val key = activeKey() ?: return
        val wifiOnly = settings.settings.value.downloadWifiOnly
        val fresh = songs.distinctBy { it.id }.filter { s -> _entries.value.none { it.song.id == s.id && it.accountKey == key && it.status != DownloadStatus.FAILED } }
        if (fresh.isEmpty()) return
        update { list -> list.filterNot { e -> e.accountKey == key && fresh.any { it.id == e.song.id } } + fresh.map { DownloadEntry(it, key) } }
        val wm = WorkManager.getInstance(context)
        fresh.forEach { song ->
            val req = OneTimeWorkRequestBuilder<DownloadWorker>()
                .setInputData(workDataOf(KEY_ID to song.id, KEY_ACCOUNT to key))
                .setConstraints(Constraints.Builder().setRequiredNetworkType(if (wifiOnly) NetworkType.UNMETERED else NetworkType.CONNECTED).build())
                .addTag(TAG)
                .build()
            wm.enqueueUniqueWork(workName(key, song.id), ExistingWorkPolicy.REPLACE, req)
        }
    }

    fun retry(entry: DownloadEntry) = enqueue(listOf(entry.song))

    fun remove(ids: Collection<String>) {
        val key = activeKey() ?: return
        val wm = WorkManager.getInstance(context)
        ids.forEach { wm.cancelUniqueWork(workName(key, it)) }
        val gone = _entries.value.filter { it.accountKey == key && it.song.id in ids }
        gone.forEach { e -> e.file?.let(::File)?.takeIf { isInside(root, it) }?.delete() }
        // Half-finished parts of these songs too.
        val dir = File(root, SubsonicClient.md5(key).take(12))
        val names = ids.map { SubsonicClient.sha256(it).take(32) + "." }
        dir.listFiles { f -> f.name.endsWith(".part") && names.any { f.name.startsWith(it) } }?.forEach { it.delete() }
        update { list -> list.filterNot { it.accountKey == key && it.song.id in ids } }
    }

    fun removeAll() = remove(forActive(_entries.value).map { it.song.id })

    /** Something is queued or downloading, for any account. */
    fun busy(): Boolean = _entries.value.any { it.status == DownloadStatus.QUEUED || it.status == DownloadStatus.RUNNING }

    /** "Forgot PIN?" in the app lock: every download of every account, its files and anything still queued. */
    suspend fun deleteEverything() = withContext(Dispatchers.IO) {
        WorkManager.getInstance(context).cancelAllWorkByTag(TAG)
        update { emptyList() }
        root.listFiles()?.forEach { it.deleteRecursively() }
    }

    fun usedBytes(list: List<DownloadEntry>): Long = forActive(list).filter { it.status == DownloadStatus.DONE }.sumOf { it.bytes }

    /** What a download attempt ended with. */
    enum class Outcome { DONE, FAILED, RETRY }

    /**
     * Run by the worker. [attempt] counts WorkManager's earlier tries. A dropped connection keeps the
     * part already saved and asks WorkManager to try again later (it resumes from there); the app
     * lock waiting for an unlock also retries.
     */
    suspend fun perform(accountKey: String, id: String, attempt: Int, onProgress: suspend (Float) -> Unit): Outcome {
        // The worker may start the process cold; wait until saved state is read.
        loaded.first { it }
        sessions.loaded.first { it }
        // NaviCtrl restarted while the app lock is on: the password stays sealed until you unlock, so wait.
        if (sessions.isLocked && sessions.active.value == null) return Outcome.RETRY
        sessions.ready.first { it }
        val entry = _entries.value.firstOrNull { it.song.id == id && it.accountKey == accountKey } ?: return Outcome.DONE
        val session = sessions.active.value
        if (session == null || session.account.key != accountKey) {
            mark(accountKey, id) { it.copy(status = DownloadStatus.FAILED, error = str(R.string.sign_in_to_this_server_to)) }
            return Outcome.FAILED
        }
        mark(accountKey, id) { it.copy(status = DownloadStatus.RUNNING, error = null) }
        return try {
            val file = fetch(session.client, accountKey, entry.song, onProgress)
            mark(accountKey, id) { it.copy(status = DownloadStatus.DONE, progress = 1f, file = file.absolutePath, bytes = file.length()) }
            Outcome.DONE
        } catch (e: CancellationException) {
            throw e
        } catch (e: LimitReached) {
            mark(accountKey, id) { it.copy(status = DownloadStatus.FAILED, error = e.message) }
            Outcome.FAILED
        } catch (e: java.io.IOException) {
            if (e !is HttpFailure && attempt < MAX_ATTEMPTS) {
                // Connection lost: keep the part and resume when WorkManager runs this again.
                mark(accountKey, id) { it.copy(status = DownloadStatus.QUEUED, error = null) }
                Outcome.RETRY
            } else {
                mark(accountKey, id) { it.copy(status = DownloadStatus.FAILED, error = e.friendlyMessage()) }
                Outcome.FAILED
            }
        } catch (e: Throwable) {
            mark(accountKey, id) { it.copy(status = DownloadStatus.FAILED, error = e.friendlyMessage()) }
            Outcome.FAILED
        }
    }

    /** The server answered with an error: retrying won't help. */
    private class HttpFailure(message: String) : java.io.IOException(message)

    /** The download limit in Settings is reached. */
    private class LimitReached(message: String) : Exception(message)

    private suspend fun fetch(client: SubsonicClient, accountKey: String, song: Song, onProgress: suspend (Float) -> Unit): File = withContext(Dispatchers.IO) {
        val s = settings.settings.value
        val kbps = s.downloadBitrate
        val format = s.downloadFormat.param
        val ext = safeExtension(if (kbps > 0) format else song.suffix)
        val url = if (kbps > 0) client.streamUrl(song.id, kbps, format) else client.downloadUrl(song.id)
        val dir = File(root, SubsonicClient.md5(accountKey).take(12)).apply { mkdirs() }
        // Song ids and suffixes come from the server: never use them as file names directly.
        val name = SubsonicClient.sha256(song.id).take(32)
        val target = File(dir, "$name.$ext")
        // The part file names the quality, so a part saved at another quality is never continued.
        val tmp = File(dir, "$name.${partTag(kbps, format)}.part")
        if (!isInside(dir, target) || !isInside(dir, tmp)) throw java.io.IOException(str(R.string.invalid_file_name))
        dir.listFiles { f -> f.name.startsWith("$name.") && f.name.endsWith(".part") && f.name != tmp.name }?.forEach { it.delete() }

        val limit = s.downloadLimitMb.toLong() * 1024 * 1024
        val othersBytes = usedBytesAll() // finished downloads of every account
        val expected = expectedBytes(song, kbps)
        if (limit > 0 && exceedsLimit(othersBytes, expected, limit)) throw LimitReached(str(R.string.download_limit_reached, formatLimit(s.downloadLimitMb)))

        var have = if (tmp.isFile) tmp.length() else 0L
        val req = Request.Builder().url(url).apply { if (have > 0) header("Range", "bytes=$have-") }.build()
        http.newCall(req).execute().use { resp ->
            if (resp.code == 416) {
                // The part is already complete (or the file changed): start again.
                tmp.delete()
                throw java.io.IOException(str(R.string.server_error_x, resp.code))
            }
            if (!resp.isSuccessful) throw HttpFailure(str(R.string.server_error_x, resp.code))
            val body = resp.body ?: throw HttpFailure(str(R.string.empty_response))
            val append = have > 0 && resumeAccepted(resp.code, resp.header("Content-Range"), have)
            if (!append) have = 0
            val total = (body.contentLength().takeIf { it > 0 }?.plus(have)) ?: song.size?.takeIf { kbps <= 0 } ?: -1L
            var read = have
            var lastReport = 0L
            body.byteStream().use { input ->
                java.io.FileOutputStream(tmp, append).use { out ->
                    val buf = ByteArray(64 * 1024)
                    while (true) {
                        ensureActive()
                        val n = input.read(buf)
                        if (n < 0) break
                        out.write(buf, 0, n)
                        read += n
                        if (limit > 0 && othersBytes + read > limit) {
                            out.close()
                            tmp.delete()
                            throw LimitReached(str(R.string.download_limit_reached, formatLimit(s.downloadLimitMb)))
                        }
                        if (read - lastReport > 256 * 1024) {
                            lastReport = read
                            val p = if (total > 0) (read.toFloat() / total).coerceIn(0f, 1f) else 0f
                            mark(accountKey, song.id) { it.copy(progress = p) }
                            onProgress(p)
                        }
                    }
                }
            }
        }
        if (!tmp.renameTo(target)) {
            tmp.copyTo(target, overwrite = true)
            tmp.delete()
        }
        target
    }

    private fun usedBytesAll(): Long = _entries.value.filter { it.status == DownloadStatus.DONE }.sumOf { it.bytes }

    private fun formatLimit(mb: Int): String = if (mb >= 1024 && mb % 1024 == 0) str(R.string.x_gb, mb / 1024) else str(R.string.x_mb, mb)
    private fun mark(accountKey: String, id: String, change: (DownloadEntry) -> DownloadEntry) =
        update { list -> list.map { if (it.song.id == id && it.accountKey == accountKey) change(it) else it } }

    private fun update(change: (List<DownloadEntry>) -> List<DownloadEntry>) {
        _entries.update(change)
        val snapshot = _entries.value
        scope.launch(Dispatchers.IO) {
            synchronized(index) {
                runCatching { index.writeText(ApiJson.encodeToString(ListSerializer(DownloadEntry.serializer()), snapshot)) }
            }
        }
    }

    private fun workName(accountKey: String, id: String) = "dl:$accountKey:$id"

    companion object {
        private val EXTENSION = Regex("^[a-z0-9]{1,5}$")

        /** A file extension safe to use locally; anything unexpected becomes "audio". */
        fun safeExtension(raw: String?): String =
            raw?.trim()?.lowercase()?.takeIf { EXTENSION.matches(it) } ?: "audio"

        /** True when [file] resolves to a path strictly inside [dir]. */
        fun isInside(dir: File, file: File): Boolean =
            runCatching { file.canonicalPath.startsWith(dir.canonicalPath + File.separator) }.getOrDefault(false)

        /** Tries per song before a dropped connection counts as failed. */
        const val MAX_ATTEMPTS = 8

        /** Part-file tag for a quality: "orig", or bitrate and format ("192mp3"). */
        fun partTag(kbps: Int, format: String): String = if (kbps <= 0) "orig" else "$kbps${safeExtension(format)}"

        /**
         * True when the server continued where the part ends: 206 with a Content-Range starting at
         * [have]. Anything else (200, a different range) means the whole file is coming again.
         */
        fun resumeAccepted(code: Int, contentRange: String?, have: Long): Boolean {
            if (code != 206 || contentRange == null) return false
            val m = Regex("""^bytes\s+(\d+)-(\d+)/(\d+|\*)$""").find(contentRange.trim()) ?: return false
            return m.groupValues[1].toLongOrNull() == have
        }

        /** Size to expect: the file's size for originals, bitrate × length for transcoded downloads, 0 when unknown. */
        fun expectedBytes(song: Song, kbps: Int): Long = when {
            kbps <= 0 -> song.size ?: 0L
            song.duration != null -> kbps.toLong() * 1000 / 8 * song.duration
            else -> 0L
        }

        /** True when a download of [expected] bytes doesn't fit next to [used] bytes under [limit] (0 = no limit). */
        fun exceedsLimit(used: Long, expected: Long, limit: Long): Boolean = limit > 0 && used + expected > limit

        const val KEY_ID = "id"
        const val KEY_ACCOUNT = "account"
        const val TAG = "downloads"
    }
}

class DownloadWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        val id = inputData.getString(DownloadRepository.KEY_ID) ?: return Result.failure()
        val account = inputData.getString(DownloadRepository.KEY_ACCOUNT) ?: return Result.failure()
        val outcome = applicationContext.container.downloads.perform(account, id, runAttemptCount) { p ->
            setProgress(workDataOf("progress" to p))
        }
        return when (outcome) {
            DownloadRepository.Outcome.DONE -> Result.success()
            DownloadRepository.Outcome.FAILED -> Result.failure()
            DownloadRepository.Outcome.RETRY -> Result.retry()
        }
    }
}
