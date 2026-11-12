package com.vknxworks.navictrl.data.local

import android.content.ContentUris
import android.content.Context
import android.content.pm.PackageManager
import android.database.ContentObserver
import android.graphics.Bitmap
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import android.util.AtomicFile
import android.util.Size
import androidx.core.content.ContextCompat
import com.vknxworks.navictrl.data.api.Album
import com.vknxworks.navictrl.data.api.ApiJson
import com.vknxworks.navictrl.data.api.Artist
import com.vknxworks.navictrl.data.api.ArtistIndex
import com.vknxworks.navictrl.data.api.ArtistInfo
import com.vknxworks.navictrl.data.api.ArtistsResult
import com.vknxworks.navictrl.data.api.NativeLogin
import com.vknxworks.navictrl.data.api.NativeSong
import com.vknxworks.navictrl.data.api.Playlist
import com.vknxworks.navictrl.data.api.ScanStatus
import com.vknxworks.navictrl.data.api.SearchResult
import com.vknxworks.navictrl.data.api.Song
import com.vknxworks.navictrl.data.api.Starred
import com.vknxworks.navictrl.data.api.SubsonicUser
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import okhttp3.HttpUrl
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.Response
import okhttp3.ResponseBody
import okhttp3.ResponseBody.Companion.toResponseBody
import okio.BufferedSource
import okio.buffer
import okio.source
import java.io.ByteArrayOutputStream
import java.io.File
import java.time.Instant
import java.util.Locale

/**
 * "Music on this phone": a Navidrome look-alike that answers inside the app, so every screen of NaviCtrl works with
 * the songs stored on the phone. Requests to [HOST] never reach the network: this interceptor answers them from
 * Android's media library (MediaStore). Favourites, ratings, play counts and playlists are kept in a small file in
 * the app's private storage. Playback reads the files directly (see [contentUri]).
 */
class LocalServer(private val context: Context) : Interceptor {

    // ---------- Library ----------

    private data class Track(
        val id: Long,
        val title: String,
        val album: String,
        val albumId: Long,
        val artist: String,
        val artistId: Long,
        val albumArtist: String?,
        val track: Int?,
        val disc: Int?,
        val year: Int?,
        val genre: String?,
        val durationMs: Long,
        val bitRate: Int?,
        val size: Long,
        val mime: String?,
        val suffix: String?,
        val addedSec: Long,
    )

    @Volatile private var cache: List<Track>? = null
    private val lock = Any()

    private val observer = object : ContentObserver(Handler(Looper.getMainLooper())) {
        override fun onChange(selfChange: Boolean) {
            cache = null
        }
    }
    private var observing = false

    fun hasPermission(): Boolean =
        ContextCompat.checkSelfPermission(context, android.Manifest.permission.READ_MEDIA_AUDIO) == PackageManager.PERMISSION_GRANTED

    private fun tracks(): List<Track> {
        cache?.let { return it }
        synchronized(lock) {
            cache?.let { return it }
            if (!hasPermission()) return emptyList()
            if (!observing) {
                runCatching { context.contentResolver.registerContentObserver(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, true, observer) }
                observing = true
            }
            val out = ArrayList<Track>()
            val proj = arrayOf(
                MediaStore.Audio.Media._ID, MediaStore.Audio.Media.TITLE, MediaStore.Audio.Media.ALBUM, MediaStore.Audio.Media.ALBUM_ID,
                MediaStore.Audio.Media.ARTIST, MediaStore.Audio.Media.ARTIST_ID, MediaStore.Audio.Media.ALBUM_ARTIST,
                MediaStore.Audio.Media.TRACK, MediaStore.Audio.Media.YEAR, MediaStore.Audio.Media.GENRE,
                MediaStore.Audio.Media.DURATION, MediaStore.Audio.Media.BITRATE, MediaStore.Audio.Media.SIZE,
                MediaStore.Audio.Media.MIME_TYPE, MediaStore.Audio.Media.DISPLAY_NAME, MediaStore.Audio.Media.DATE_ADDED,
                MediaStore.Audio.Media.DISC_NUMBER,
            )
            // Music only: no ringtones, notification sounds, voice recordings or podcasts.
            val sel = "${MediaStore.Audio.Media.IS_MUSIC}!=0 AND ${MediaStore.Audio.Media.IS_RECORDING}=0"
            runCatching {
                context.contentResolver.query(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, proj, sel, null, null)?.use { c ->
                    while (c.moveToNext()) {
                        val rawTrack = if (c.isNull(7)) null else c.getInt(7)
                        val disc = c.getString(16)?.substringBefore('/')?.trim()?.toIntOrNull() ?: rawTrack?.let { if (it >= 1000) it / 1000 else null }
                        val name = c.getString(14).orEmpty()
                        out += Track(
                            id = c.getLong(0),
                            title = c.getString(1)?.takeIf { it.isNotBlank() } ?: name.substringBeforeLast('.'),
                            album = c.getString(2)?.takeIf { it.isNotBlank() && it != "<unknown>" } ?: UNKNOWN_ALBUM,
                            albumId = c.getLong(3),
                            artist = c.getString(4)?.takeIf { it.isNotBlank() && it != "<unknown>" } ?: UNKNOWN_ARTIST,
                            artistId = c.getLong(5),
                            albumArtist = c.getString(6)?.takeIf { it.isNotBlank() && it != "<unknown>" },
                            track = rawTrack?.let { if (it >= 1000) it % 1000 else it }?.takeIf { it > 0 },
                            disc = disc?.takeIf { it > 0 },
                            year = if (c.isNull(8)) null else c.getInt(8).takeIf { it > 0 },
                            genre = c.getString(9)?.takeIf { it.isNotBlank() },
                            durationMs = c.getLong(10),
                            bitRate = if (c.isNull(11)) null else (c.getInt(11) / 1000).takeIf { it > 0 },
                            size = c.getLong(12),
                            mime = c.getString(13),
                            suffix = name.substringAfterLast('.', "").lowercase().takeIf { it.isNotEmpty() && it.length <= 5 },
                            addedSec = c.getLong(15),
                        )
                    }
                }
            }
            cache = out
            return out
        }
    }

    // ---------- What NaviCtrl keeps for this library ----------

    @Serializable
    private data class LocalPlaylist(val id: String, val name: String, val comment: String? = null, val songs: List<String> = emptyList(), val created: Long = 0, val changed: Long = 0)

    @Serializable
    private data class State(
        val starred: Map<String, Long> = emptyMap(),
        val ratings: Map<String, Int> = emptyMap(),
        val plays: Map<String, Long> = emptyMap(),
        val lastPlayed: Map<String, Long> = emptyMap(),
        val playlists: List<LocalPlaylist> = emptyList(),
        val nextPlaylist: Int = 1,
    )

    private val stateFile get() = AtomicFile(File(context.filesDir, "local_library.json"))
    @Volatile private var stateCache: State? = null

    private fun state(): State = stateCache ?: synchronized(lock) {
        stateCache ?: (runCatching { ApiJson.decodeFromString(State.serializer(), String(stateFile.readFully(), Charsets.UTF_8)) }.getOrNull() ?: State()).also { stateCache = it }
    }

    private fun change(f: (State) -> State) = synchronized(lock) {
        val next = f(state())
        stateCache = next
        val file = stateFile
        val out = runCatching { file.startWrite() }.getOrNull() ?: return@synchronized
        try {
            out.write(ApiJson.encodeToString(State.serializer(), next).toByteArray(Charsets.UTF_8))
            file.finishWrite(out)
        } catch (e: Exception) {
            file.failWrite(out)
        }
    }

    /** Removes favourites, ratings, play counts and playlists of this library (signing out of it). */
    fun forget() = change { State() }

    // ---------- Model ----------

    private fun iso(ms: Long) = Instant.ofEpochMilli(ms).toString()

    private fun Track.song(s: State): Song = Song(
        id = id.toString(),
        title = title,
        album = album,
        albumId = "al$albumId",
        artist = artist,
        artistId = "ar$artistId",
        track = track,
        discNumber = disc,
        year = year,
        genre = genre,
        coverArt = "mf-$id",
        duration = (durationMs / 1000).toInt(),
        bitRate = bitRate,
        suffix = suffix,
        contentType = mime,
        size = size,
        created = iso(addedSec * 1000),
        starred = s.starred[id.toString()]?.let(::iso),
        userRating = s.ratings[id.toString()],
        playCount = s.plays[id.toString()] ?: 0,
    )

    private fun albumOf(id: String, list: List<Track>, s: State, withSongs: Boolean): Album? {
        val songs = list.filter { "al${it.albumId}" == id }
        if (songs.isEmpty()) return null
        val sorted = songs.sortedWith(compareBy<Track>({ it.disc ?: 0 }, { it.track ?: 0 }, { it.title.lowercase() }))
        val first = sorted.first()
        val artistName = first.albumArtist ?: songs.map { it.artist }.distinct().singleOrNull() ?: first.artist
        val artistTrack = songs.firstOrNull { it.artist == artistName } ?: first
        return Album(
            id = id,
            name = first.album,
            artist = artistName,
            artistId = "ar${artistTrack.artistId}",
            coverArt = "mf-${first.id}",
            songCount = songs.size,
            duration = (songs.sumOf { it.durationMs } / 1000).toInt(),
            year = songs.mapNotNull { it.year }.maxOrNull(),
            genre = songs.mapNotNull { it.genre }.groupingBy { it }.eachCount().maxByOrNull { it.value }?.key,
            created = iso(songs.minOf { it.addedSec } * 1000),
            starred = s.starred[id]?.let(::iso),
            playCount = songs.sumOf { s.plays[it.id.toString()] ?: 0 },
            userRating = s.ratings[id],
            song = if (withSongs) sorted.map { it.song(s) } else emptyList(),
        )
    }

    private fun albums(list: List<Track>, s: State): List<Album> =
        list.map { "al${it.albumId}" }.distinct().mapNotNull { albumOf(it, list, s, withSongs = false) }

    private fun artistOf(id: String, list: List<Track>, s: State, withAlbums: Boolean): Artist? {
        val songs = list.filter { "ar${it.artistId}" == id }
        if (songs.isEmpty()) return null
        val albumIds = songs.map { "al${it.albumId}" }.distinct()
        val albums = albumIds.mapNotNull { albumOf(it, list, s, withSongs = false) }.sortedWith(compareBy({ it.year ?: 0 }, { it.name.lowercase() }))
        return Artist(
            id = id,
            name = songs.first().artist,
            coverArt = albums.firstOrNull()?.coverArt ?: "mf-${songs.first().id}",
            albumCount = albums.size,
            starred = s.starred[id]?.let(::iso),
            album = if (withAlbums) albums else emptyList(),
        )
    }

    private fun artists(list: List<Track>, s: State): List<Artist> =
        list.map { "ar${it.artistId}" }.distinct().mapNotNull { artistOf(it, list, s, withAlbums = false) }.sortedBy { sortKey(it.name) }

    private fun sortKey(s: String) = s.lowercase(Locale.ROOT).removePrefix("the ").trim()

    private fun playlistOf(p: LocalPlaylist, list: List<Track>, s: State, withSongs: Boolean): Playlist {
        val byId = list.associateBy { it.id.toString() }
        val songs = p.songs.mapNotNull { byId[it] }
        return Playlist(
            id = p.id, name = p.name, comment = p.comment, owner = USER, songCount = songs.size,
            duration = (songs.sumOf { it.durationMs } / 1000).toInt(), created = iso(p.created), changed = iso(p.changed),
            coverArt = songs.firstOrNull()?.let { "mf-${it.id}" },
            entry = if (withSongs) songs.map { it.song(s) } else emptyList(),
        )
    }

    // ---------- HTTP ----------

    override fun intercept(chain: Interceptor.Chain): Response {
        val req = chain.request()
        if (!req.url.host.equals(HOST, ignoreCase = true)) return chain.proceed(req)
        return try {
            handle(req)
        } catch (e: Exception) {
            json(req, error(0, e.message ?: "error"))
        }
    }

    private fun handle(req: Request): Response {
        val url = req.url
        val path = url.encodedPath.trimStart('/')
        return when {
            path == "auth/login" -> plain(req, ApiJson.encodeToString(NativeLogin.serializer(), NativeLogin(token = "local", isAdmin = false, name = USER)), "application/json")
            path == "api/song" -> nativeSongs(req, url)
            path.startsWith("rest/") -> rest(req, path.removePrefix("rest/").removeSuffix(".view"), url)
            else -> notFound(req)
        }
    }

    private fun HttpUrl.q(name: String): String? = queryParameter(name)
    private fun HttpUrl.qs(name: String): List<String> = queryParameterValues(name).filterNotNull()
    private fun HttpUrl.int(name: String, def: Int) = q(name)?.toIntOrNull() ?: def

    private fun <T> el(serializer: KSerializer<T>, v: T): JsonElement = ApiJson.encodeToJsonElement(serializer, v)
    private fun songList(key: String, songs: List<Song>) = buildJsonObject { put(key, JsonArray(songs.map { el(Song.serializer(), it) })) }

    private fun rest(req: Request, endpoint: String, url: HttpUrl): Response {
        val list = tracks()
        val s = state()
        fun ok(key: String? = null, value: JsonElement? = null) = json(req, okBody(key, value))
        fun songs(key: String, inner: String, v: List<Song>) = ok(key, songList(inner, v))
        return when (endpoint) {
            "ping" -> ok()
            "getUser" -> ok("user", el(SubsonicUser.serializer(), SubsonicUser(USER, adminRole = false)))
            "getOpenSubsonicExtensions" -> ok("openSubsonicExtensions", JsonArray(emptyList()))
            "getScanStatus", "startScan" -> {
                if (endpoint == "startScan") cache = null
                ok("scanStatus", el(ScanStatus.serializer(), ScanStatus(scanning = false, count = tracks().size.toLong())))
            }
            "getArtists" -> {
                val index = artists(list, s).groupBy { a ->
                    val ch = sortKey(a.name).firstOrNull()?.uppercaseChar() ?: '#'
                    if (ch.isLetter()) ch.toString() else "#"
                }.toSortedMap().map { (k, v) -> ArtistIndex(k, v) }
                ok("artists", el(ArtistsResult.serializer(), ArtistsResult(index)))
            }
            "getArtist" -> artistOf(url.q("id").orEmpty(), list, s, withAlbums = true)?.let { ok("artist", el(Artist.serializer(), it)) } ?: json(req, error(70, "not found"))
            "getArtistInfo2" -> ok("artistInfo2", el(ArtistInfo.serializer(), ArtistInfo()))
            "getAlbum" -> albumOf(url.q("id").orEmpty(), list, s, withSongs = true)?.let { ok("album", el(Album.serializer(), it)) } ?: json(req, error(70, "not found"))
            "getSong" -> list.firstOrNull { it.id.toString() == url.q("id") }?.let { ok("song", el(Song.serializer(), it.song(s))) } ?: json(req, error(70, "not found"))
            "getTopSongs" -> {
                val name = url.q("artist").orEmpty()
                val v = list.filter { it.artist.equals(name, true) || it.albumArtist.equals(name, true) }
                    .sortedByDescending { s.plays[it.id.toString()] ?: 0 }.take(url.int("count", 20)).map { it.song(s) }
                songs("topSongs", "song", v)
            }
            "getSimilarSongs2" -> {
                val seed = list.firstOrNull { "ar${it.artistId}" == url.q("id") || it.id.toString() == url.q("id") }
                val v = list.filter { seed == null || it.artistId == seed.artistId || (seed.genre != null && it.genre == seed.genre) }
                    .shuffled().take(url.int("count", 50)).map { it.song(s) }
                songs("similarSongs2", "song", v)
            }
            "getRandomSongs" -> {
                val g = url.q("genre")
                songs("randomSongs", "song", list.filter { g == null || it.genre.equals(g, true) }.shuffled().take(url.int("size", 100).coerceIn(1, 500)).map { it.song(s) })
            }
            "getAlbumList2" -> {
                val all = albums(list, s)
                val sorted = when (url.q("type")) {
                    "alphabeticalByArtist" -> all.sortedWith(compareBy({ sortKey(it.artist.orEmpty()) }, { sortKey(it.name) }))
                    "newest" -> all.sortedByDescending { it.created }
                    "recent" -> all.filter { a -> list.any { "al${it.albumId}" == a.id && s.lastPlayed.containsKey(it.id.toString()) } }
                        .sortedByDescending { a -> list.filter { "al${it.albumId}" == a.id }.maxOf { s.lastPlayed[it.id.toString()] ?: 0 } }
                    "frequent" -> all.filter { (it.playCount ?: 0) > 0 }.sortedByDescending { it.playCount }
                    "random" -> all.shuffled()
                    "starred" -> all.filter { it.starred != null }
                    else -> all.sortedBy { sortKey(it.name) }
                }
                val page = sorted.drop(url.int("offset", 0)).take(url.int("size", 50).coerceIn(1, 500))
                ok("albumList2", buildJsonObject { put("album", JsonArray(page.map { el(Album.serializer(), it) })) })
            }
            "search3" -> {
                val q = url.q("query").orEmpty().trim().trim('"').lowercase()
                val hit: (String?) -> Boolean = { it != null && (q.isEmpty() || it.lowercase().contains(q)) }
                val r = SearchResult(
                    artist = artists(list, s).filter { hit(it.name) }.take(url.int("artistCount", 20)),
                    album = albums(list, s).filter { hit(it.name) || hit(it.artist) }.take(url.int("albumCount", 20)),
                    song = list.filter { hit(it.title) || hit(it.artist) || hit(it.album) }
                        .drop(url.int("songOffset", 0)).take(url.int("songCount", 40)).map { it.song(s) },
                )
                ok("searchResult3", el(SearchResult.serializer(), r))
            }
            "getStarred2", "getStarred" -> {
                val r = Starred(
                    artist = artists(list, s).filter { it.starred != null },
                    album = albums(list, s).filter { it.starred != null },
                    song = list.filter { s.starred.containsKey(it.id.toString()) }.sortedByDescending { s.starred[it.id.toString()] }.map { it.song(s) },
                )
                ok(if (endpoint == "getStarred2") "starred2" else "starred", el(Starred.serializer(), r))
            }
            "star", "unstar" -> {
                val ids = url.qs("id") + url.qs("albumId") + url.qs("artistId")
                val now = System.currentTimeMillis()
                change { st -> st.copy(starred = if (endpoint == "star") st.starred + ids.associateWith { now } else st.starred - ids.toSet()) }
                ok()
            }
            "setRating" -> {
                val id = url.q("id").orEmpty()
                val r = url.int("rating", 0).coerceIn(0, 5)
                change { st -> st.copy(ratings = if (r == 0) st.ratings - id else st.ratings + (id to r)) }
                ok()
            }
            "scrobble" -> {
                if (url.q("submission") != "false") {
                    val ids = url.qs("id")
                    val now = System.currentTimeMillis()
                    change { st -> st.copy(plays = st.plays + ids.associateWith { (st.plays[it] ?: 0) + 1 }, lastPlayed = st.lastPlayed + ids.associateWith { now }) }
                }
                ok()
            }
            "getPlaylists" -> ok("playlists", buildJsonObject { put("playlist", JsonArray(s.playlists.map { el(Playlist.serializer(), playlistOf(it, list, s, false)) })) })
            "getPlaylist" -> s.playlists.firstOrNull { it.id == url.q("id") }?.let { ok("playlist", el(Playlist.serializer(), playlistOf(it, list, s, true))) } ?: json(req, error(70, "not found"))
            "createPlaylist" -> {
                val now = System.currentTimeMillis()
                val existing = url.q("playlistId")
                var made: LocalPlaylist? = null
                change { st ->
                    if (existing != null) {
                        st.copy(playlists = st.playlists.map { p -> if (p.id == existing) p.copy(songs = url.qs("songId"), changed = now).also { made = it } else p })
                    } else {
                        val p = LocalPlaylist("pl${st.nextPlaylist}", url.q("name").orEmpty().take(200).ifBlank { "Playlist" }, null, url.qs("songId"), now, now)
                        made = p
                        st.copy(playlists = st.playlists + p, nextPlaylist = st.nextPlaylist + 1)
                    }
                }
                made?.let { ok("playlist", el(Playlist.serializer(), playlistOf(it, list, state(), true))) } ?: json(req, error(70, "not found"))
            }
            "updatePlaylist" -> {
                val id = url.q("playlistId")
                val remove = url.qs("songIndexToRemove").mapNotNull { it.toIntOrNull() }.toSet()
                val add = url.qs("songIdToAdd")
                val now = System.currentTimeMillis()
                change { st ->
                    st.copy(playlists = st.playlists.map { p ->
                        if (p.id != id) p else p.copy(
                            name = url.q("name")?.take(200)?.ifBlank { null } ?: p.name,
                            comment = url.q("comment") ?: p.comment,
                            songs = p.songs.filterIndexed { i, _ -> i !in remove } + add,
                            changed = now,
                        )
                    })
                }
                ok()
            }
            "deletePlaylist" -> {
                val id = url.q("id")
                change { st -> st.copy(playlists = st.playlists.filter { it.id != id }) }
                ok()
            }
            "getLyricsBySongId" -> ok("lyricsList", buildJsonObject { put("structuredLyrics", JsonArray(emptyList())) })
            "getLyrics" -> ok("lyrics", JsonObject(emptyMap()))
            "getPlayQueue" -> json(req, error(70, "no play queue"))
            "savePlayQueue" -> ok()
            "getCoverArt" -> cover(req, url.q("id").orEmpty(), url.int("size", 600))
            "stream", "download" -> file(req, url.q("id").orEmpty())
            else -> ok()
        }
    }

    /** All songs, sorted and paged like Navidrome's own API. */
    private fun nativeSongs(req: Request, url: HttpUrl): Response {
        val s = state()
        val list = tracks()
        val asc = url.q("_order") != "DESC"
        val sorted = when (url.q("_sort")) {
            "createdAt" -> list.sortedBy { it.addedSec }
            "artist" -> list.sortedWith(compareBy({ sortKey(it.artist) }, { sortKey(it.album) }, { it.disc ?: 0 }, { it.track ?: 0 }))
            "album" -> list.sortedWith(compareBy({ sortKey(it.album) }, { it.disc ?: 0 }, { it.track ?: 0 }))
            else -> list.sortedBy { sortKey(it.title) }
        }.let { if (asc) it else it.reversed() }
        val start = url.int("_start", 0).coerceAtLeast(0)
        val end = url.int("_end", start + 100).coerceAtLeast(start)
        val page = sorted.drop(start).take(end - start).map { t ->
            val id = t.id.toString()
            NativeSong(
                id = id, title = t.title, album = t.album, albumId = "al${t.albumId}", artist = t.artist, artistId = "ar${t.artistId}",
                trackNumber = t.track, discNumber = t.disc, year = t.year, genre = t.genre, duration = t.durationMs / 1000.0,
                bitRate = t.bitRate, suffix = t.suffix, size = t.size, createdAt = iso(t.addedSec * 1000),
                starred = s.starred.containsKey(id), starredAt = s.starred[id]?.let(::iso), rating = s.ratings[id], playCount = s.plays[id] ?: 0,
            )
        }
        val body = ApiJson.encodeToString(ListSerializer(NativeSong.serializer()), page)
        return base(req, 200).header("X-Total-Count", list.size.toString())
            .body(body.toResponseBody("application/json".toMediaType())).build()
    }

    /** The song's own artwork (embedded or the album's), as a JPEG. */
    private fun cover(req: Request, id: String, size: Int): Response {
        val songId = when {
            id.startsWith("mf-") -> id.removePrefix("mf-").toLongOrNull()
            id.startsWith("al") -> tracks().firstOrNull { "al${it.albumId}" == id }?.id
            else -> null
        } ?: return notFound(req)
        val px = size.coerceIn(64, 1200)
        val bmp = runCatching { context.contentResolver.loadThumbnail(contentUri(songId.toString()), Size(px, px), null) }.getOrNull() ?: return notFound(req)
        val bytes = ByteArrayOutputStream().use { out -> bmp.compress(Bitmap.CompressFormat.JPEG, 90, out); out.toByteArray() }
        return base(req, 200).header("Cache-Control", "max-age=86400").body(bytes.toResponseBody("image/jpeg".toMediaType())).build()
    }

    /** The audio file itself (used when a song is saved for offline or opened by URL), with byte ranges for seeking. */
    private fun file(req: Request, id: String): Response {
        val t = tracks().firstOrNull { it.id.toString() == id } ?: return notFound(req)
        val uri = contentUri(id)
        val total = t.size.takeIf { it > 0 } ?: return notFound(req)
        val range = req.header("Range")?.let { Regex("bytes=(\\d+)-(\\d*)").find(it) }
        val from = range?.groupValues?.get(1)?.toLongOrNull()?.coerceIn(0, total - 1) ?: 0L
        val to = range?.groupValues?.get(2)?.toLongOrNull()?.coerceIn(from, total - 1) ?: (total - 1)
        val input = context.contentResolver.openInputStream(uri) ?: return notFound(req)
        if (from > 0) input.skipFully(from)
        val length = to - from + 1
        val body = object : ResponseBody() {
            override fun contentType() = (t.mime ?: "audio/mpeg").toMediaType()
            override fun contentLength() = length
            override fun source(): BufferedSource = input.source().buffer().let { src -> LimitedSource(src, length).buffer() }
        }
        val b = base(req, if (range != null) 206 else 200).header("Accept-Ranges", "bytes").header("Content-Length", length.toString())
        if (range != null) b.header("Content-Range", "bytes $from-$to/$total")
        return b.body(body).build()
    }

    private fun java.io.InputStream.skipFully(n: Long) {
        var left = n
        while (left > 0) {
            val k = skip(left)
            if (k <= 0) {
                if (read() < 0) return
                left--
            } else left -= k
        }
    }

    private class LimitedSource(private val src: okio.Source, private var left: Long) : okio.Source by src {
        override fun read(sink: okio.Buffer, byteCount: Long): Long {
            if (left <= 0) return -1
            val n = src.read(sink, minOf(byteCount, left))
            if (n > 0) left -= n
            return n
        }
    }

    private fun okBody(key: String?, value: JsonElement?) = buildJsonObject {
        put("subsonic-response", buildJsonObject {
            put("status", "ok")
            put("version", "1.16.1")
            put("type", "navictrl-local")
            put("openSubsonic", true)
            if (key != null && value != null) put(key, value)
        })
    }

    private fun error(code: Int, message: String) = buildJsonObject {
        put("subsonic-response", buildJsonObject {
            put("status", "failed")
            put("version", "1.16.1")
            put("error", buildJsonObject { put("code", code); put("message", message) })
        })
    }

    private fun base(req: Request, code: Int) = Response.Builder().request(req).protocol(Protocol.HTTP_1_1).code(code).message(if (code < 300) "OK" else "Not Found")

    private fun json(req: Request, body: JsonObject) = plain(req, body.toString(), "application/json")

    private fun plain(req: Request, body: String, type: String) = base(req, 200).body(body.toResponseBody(type.toMediaType())).build()

    private fun notFound(req: Request) = base(req, 404).body("".toResponseBody("text/plain".toMediaType())).build()

    companion object {
        /** Never resolved: [LocalServer] answers before any connection is made. `.invalid` can't exist on the internet. */
        const val HOST = "this-phone.navictrl.invalid"
        const val BASE_URL = "http://$HOST"
        const val USER = "this-phone"
        const val PASSWORD = "local"
        private const val UNKNOWN_ALBUM = "Unknown album"
        private const val UNKNOWN_ARTIST = "Unknown artist"

        fun isLocal(baseUrl: String?): Boolean = baseUrl?.let { Uri.parse(it).host.equals(HOST, ignoreCase = true) } == true

        /** The file of a song in the phone's media library; played directly, without the network or the cache. */
        fun contentUri(songId: String): Uri =
            ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, songId.toLongOrNull() ?: -1L)
    }
}
