package com.vknxworks.navictrl.data.api

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull

// Typed wrappers around the Subsonic endpoints NaviCtrl uses. Lists come wrapped in an object
// ({"albumList2": {"album": [...]}}), so each has a tiny holder type.

@Serializable private class AlbumList(val album: List<Album> = emptyList())
@Serializable private class SongList(val song: List<Song> = emptyList())
@Serializable private class PlaylistList(val playlist: List<Playlist> = emptyList())
@Serializable private class GenreList(val genre: List<Genre> = emptyList())
@Serializable private class LyricsList(val structuredLyrics: List<StructuredLyrics> = emptyList())
@Serializable private class LegacyLyrics(val value: String? = null)

suspend fun SubsonicClient.ping(): JsonObject = call("ping")

suspend fun SubsonicClient.user(): SubsonicUser =
    get("getUser", "user", SubsonicUser.serializer(), listOf("username" to username))

suspend fun SubsonicClient.albumList(sort: AlbumSort, size: Int = 40, offset: Int = 0, genre: String? = null): List<Album> =
    get(
        "getAlbumList2", "albumList2", AlbumList.serializer(),
        listOf("type" to (if (genre != null) "byGenre" else sort.type), "size" to size, "offset" to offset, "genre" to genre),
    ).album

suspend fun SubsonicClient.album(id: String): Album = get("getAlbum", "album", Album.serializer(), listOf("id" to id))

suspend fun SubsonicClient.artists(): List<Artist> =
    get("getArtists", "artists", ArtistsResult.serializer()).index.flatMap { it.artist }

suspend fun SubsonicClient.artist(id: String): Artist = get("getArtist", "artist", Artist.serializer(), listOf("id" to id))

suspend fun SubsonicClient.artistInfo(id: String): ArtistInfo =
    get("getArtistInfo2", "artistInfo2", ArtistInfo.serializer(), listOf("id" to id, "count" to 12))

suspend fun SubsonicClient.topSongs(artistName: String, count: Int = 20): List<Song> =
    get("getTopSongs", "topSongs", SongList.serializer(), listOf("artist" to artistName, "count" to count)).song

suspend fun SubsonicClient.similarSongs(id: String, count: Int = 50): List<Song> =
    get("getSimilarSongs2", "similarSongs2", SongList.serializer(), listOf("id" to id, "count" to count)).song

suspend fun SubsonicClient.song(id: String): Song = get("getSong", "song", Song.serializer(), listOf("id" to id))

suspend fun SubsonicClient.randomSongs(size: Int = 100, genre: String? = null): List<Song> =
    get("getRandomSongs", "randomSongs", SongList.serializer(), listOf("size" to size, "genre" to genre)).song

suspend fun SubsonicClient.songsByGenre(genre: String, count: Int = 100, offset: Int = 0): List<Song> =
    get("getSongsByGenre", "songsByGenre", SongList.serializer(), listOf("genre" to genre, "count" to count, "offset" to offset)).song

suspend fun SubsonicClient.genres(): List<Genre> = get("getGenres", "genres", GenreList.serializer()).genre

suspend fun SubsonicClient.playlists(): List<Playlist> = get("getPlaylists", "playlists", PlaylistList.serializer()).playlist

suspend fun SubsonicClient.playlist(id: String): Playlist = get("getPlaylist", "playlist", Playlist.serializer(), listOf("id" to id))

suspend fun SubsonicClient.createPlaylist(name: String, songIds: List<String> = emptyList()): Playlist =
    get("createPlaylist", "playlist", Playlist.serializer(), listOf("name" to name) + songIds.map { "songId" to it })

suspend fun SubsonicClient.updatePlaylist(
    id: String,
    name: String? = null,
    comment: String? = null,
    add: List<String> = emptyList(),
    removeIndexes: List<Int> = emptyList(),
) {
    val params = mutableListOf<Pair<String, Any?>>("playlistId" to id, "name" to name, "comment" to comment)
    add.forEach { params += "songIdToAdd" to it }
    removeIndexes.forEach { params += "songIndexToRemove" to it }
    call("updatePlaylist", params)
}

/** Subsonic has no "move" for playlists, so a reorder rewrites the playlist with createPlaylist(playlistId=…). */
suspend fun SubsonicClient.replacePlaylistSongs(id: String, songIds: List<String>) {
    call("createPlaylist", listOf("playlistId" to id) + songIds.map { "songId" to it })
}

suspend fun SubsonicClient.deletePlaylist(id: String) {
    call("deletePlaylist", listOf("id" to id))
}

suspend fun SubsonicClient.starred(): Starred = get("getStarred2", "starred2", Starred.serializer())

enum class StarKind(val param: String) { SONG("id"), ALBUM("albumId"), ARTIST("artistId") }

suspend fun SubsonicClient.setStar(kind: StarKind, id: String, starred: Boolean) {
    call(if (starred) "star" else "unstar", listOf(kind.param to id))
}

suspend fun SubsonicClient.setRating(id: String, rating: Int) {
    call("setRating", listOf("id" to id, "rating" to rating.coerceIn(0, 5)))
}

suspend fun SubsonicClient.search(query: String, artists: Int = 10, albums: Int = 20, songs: Int = 40): SearchResult =
    get(
        "search3", "searchResult3", SearchResult.serializer(),
        listOf("query" to query, "artistCount" to artists, "albumCount" to albums, "songCount" to songs),
    )

suspend fun SubsonicClient.scrobble(id: String, submission: Boolean, timeMs: Long = System.currentTimeMillis()) {
    call("scrobble", listOf("id" to id, "submission" to submission, "time" to timeMs))
}

/** Synced lyrics when the server has them (OpenSubsonic), otherwise plain lyrics by artist and title. */
suspend fun SubsonicClient.lyrics(song: Song): Lyrics? {
    val structured = runCatching {
        get("getLyricsBySongId", "lyricsList", LyricsList.serializer(), listOf("id" to song.id)).structuredLyrics
    }.getOrDefault(emptyList())
    val best = structured.firstOrNull { it.synced && it.line.isNotEmpty() } ?: structured.firstOrNull { it.line.isNotEmpty() }
    if (best != null) {
        return Lyrics(
            synced = best.synced,
            lines = best.line.map { l -> l.copy(start = l.start?.let { (it - best.offset).coerceAtLeast(0) }) },
        )
    }
    val plain = runCatching {
        get("getLyrics", "lyrics", LegacyLyrics.serializer(), listOf("artist" to song.artist, "title" to song.title)).value
    }.getOrNull()
    if (plain.isNullOrBlank()) return null
    return Lyrics(false, plain.lines().map { LyricLine(null, it) })
}

suspend fun SubsonicClient.scanStatus(): ScanStatus = get("getScanStatus", "scanStatus", ScanStatus.serializer())

suspend fun SubsonicClient.startScan(full: Boolean = false): ScanStatus =
    get("startScan", "scanStatus", ScanStatus.serializer(), if (full) listOf("fullScan" to true) else emptyList())

/** Artwork URL. Sizes are rounded so the same picture isn't fetched at many sizes. */
fun SubsonicClient.coverArtUrl(coverArt: String?, size: Int = 500): String? {
    if (coverArt.isNullOrBlank()) return null
    val bucket = when {
        size <= 160 -> 160
        size <= 300 -> 300
        size <= 600 -> 600
        else -> 1200
    }
    return restUrl("getCoverArt", listOf("id" to coverArt, "size" to bucket))
}

/**
 * Stream URL. [maxKbps] 0 means the original file; otherwise the server transcodes to [format].
 * The URL is stable for a given song and quality, which keeps the playback cache effective.
 */
fun SubsonicClient.streamUrl(id: String, maxKbps: Int, format: String): String =
    if (maxKbps <= 0) restUrl("stream", listOf("id" to id, "format" to "raw"))
    else restUrl("stream", listOf("id" to id, "maxBitRate" to maxKbps, "format" to format))

fun SubsonicClient.downloadUrl(id: String): String = restUrl("download", listOf("id" to id))

/** Rough plain text from the HTML biographies Last.fm provides. */
fun String.stripHtml(): String =
    replace(Regex("<a [^>]*>.*?</a>", RegexOption.DOT_MATCHES_ALL), "")
        .replace(Regex("<[^>]+>"), "")
        .replace("&amp;", "&").replace("&quot;", "\"").replace("&#39;", "'").replace("&lt;", "<").replace("&gt;", ">")
        .trim()

fun JsonObject.str(key: String): String? = (this[key] as? JsonPrimitive)?.contentOrNull
