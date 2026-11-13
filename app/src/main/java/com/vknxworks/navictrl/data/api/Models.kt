package com.vknxworks.navictrl.data.api

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import androidx.annotation.StringRes
import com.vknxworks.navictrl.R

/** Lenient JSON: servers add fields over time and leave others out. */
val ApiJson = Json {
    ignoreUnknownKeys = true
    coerceInputValues = true
    explicitNulls = false
    isLenient = true
}

class ApiException(message: String, val code: Int = 0) : Exception(message)

/** A track. Shared by the Subsonic API, the queue, downloads and the saved queue. */
@Serializable
data class Song(
    val id: String,
    val title: String = "",
    val album: String? = null,
    val albumId: String? = null,
    val artist: String? = null,
    val artistId: String? = null,
    val track: Int? = null,
    val discNumber: Int? = null,
    val year: Int? = null,
    val genre: String? = null,
    val coverArt: String? = null,
    /** Seconds. */
    val duration: Int? = null,
    val bitRate: Int? = null,
    val suffix: String? = null,
    val contentType: String? = null,
    val size: Long? = null,
    val created: String? = null,
    val starred: String? = null,
    val userRating: Int? = null,
    val playCount: Long? = null,
    val isDir: Boolean = false,
    /** OpenSubsonic: ReplayGain values in dB (gains) and linear (peaks). Missing on servers or files without them. */
    val replayGain: ReplayGain? = null,
)

/** OpenSubsonic ReplayGain data for a song. Every field is optional. */
@Serializable
data class ReplayGain(
    val trackGain: Double? = null,
    val albumGain: Double? = null,
    val trackPeak: Double? = null,
    val albumPeak: Double? = null,
    val baseGain: Double? = null,
    /** Gain the server suggests for files without ReplayGain tags. */
    val fallbackGain: Double? = null,
) {
    val isEmpty: Boolean get() = trackGain == null && albumGain == null && fallbackGain == null
}

/** The play queue saved on the server (getPlayQueue), shared between the user's devices and apps. */
@Serializable
data class PlayQueue(
    val current: String? = null,
    /** Milliseconds into the current song. */
    val position: Long = 0,
    val username: String? = null,
    val changed: String? = null,
    /** The app (Subsonic client name) that saved it. */
    val changedBy: String? = null,
    val entry: List<Song> = emptyList(),
)

@Serializable
data class Album(
    val id: String,
    val name: String = "",
    val artist: String? = null,
    val artistId: String? = null,
    val coverArt: String? = null,
    val songCount: Int = 0,
    val duration: Int = 0,
    val year: Int? = null,
    val genre: String? = null,
    val created: String? = null,
    val starred: String? = null,
    val playCount: Long? = null,
    val userRating: Int? = null,
    val song: List<Song> = emptyList(),
)

@Serializable
data class Artist(
    val id: String,
    val name: String = "",
    val coverArt: String? = null,
    val albumCount: Int = 0,
    val starred: String? = null,
    val artistImageUrl: String? = null,
    val album: List<Album> = emptyList(),
)

@Serializable
data class ArtistInfo(
    val biography: String? = null,
    val largeImageUrl: String? = null,
    val mediumImageUrl: String? = null,
    val similarArtist: List<Artist> = emptyList(),
)

@Serializable
data class ArtistIndex(val name: String = "", val artist: List<Artist> = emptyList())

@Serializable
data class ArtistsResult(val index: List<ArtistIndex> = emptyList())

@Serializable
data class Playlist(
    val id: String,
    val name: String = "",
    val comment: String? = null,
    val owner: String? = null,
    val public: Boolean = false,
    val songCount: Int = 0,
    val duration: Int = 0,
    val created: String? = null,
    val changed: String? = null,
    val coverArt: String? = null,
    /** OpenSubsonic: songs can't be added or removed (a smart or synced playlist, or someone else's). */
    val readonly: Boolean = false,
    val entry: List<Song> = emptyList(),
) {
    /** Yours to rename or delete. Servers that leave the owner out only return your own playlists to edit. */
    fun ownedBy(username: String): Boolean = owner == null || owner.equals(username, ignoreCase = true)

    /** Yours, and songs can be added and removed. */
    fun songsEditableBy(username: String): Boolean = ownedBy(username) && !readonly
}

@Serializable
data class SearchResult(
    val artist: List<Artist> = emptyList(),
    val album: List<Album> = emptyList(),
    val song: List<Song> = emptyList(),
)

@Serializable
data class Starred(
    val artist: List<Artist> = emptyList(),
    val album: List<Album> = emptyList(),
    val song: List<Song> = emptyList(),
)

@Serializable
data class LyricLine(val start: Long? = null, val value: String = "")

@Serializable
data class StructuredLyrics(
    val lang: String? = null,
    val synced: Boolean = false,
    val offset: Long = 0,
    val line: List<LyricLine> = emptyList(),
)

/** Lyrics ready to show: lines with an optional start time in ms. */
data class Lyrics(val synced: Boolean, val lines: List<LyricLine>)

@Serializable
data class ScanStatus(val scanning: Boolean = false, val count: Long = 0, val folderCount: Long = 0, val lastScan: String? = null)

@Serializable
data class SubsonicUser(val username: String = "", val adminRole: Boolean = false, val email: String? = null)

/** Server facts gathered while signing in. */
data class ServerInfo(val url: String, val type: String?, val serverVersion: String?, val apiVersion: String?, val openSubsonic: Boolean)

/** A song as Navidrome's own (non-Subsonic) API returns it; used for the sortable all-songs list. */
@Serializable
data class NativeSong(
    val id: String,
    val title: String = "",
    val album: String? = null,
    val albumId: String? = null,
    val artist: String? = null,
    val artistId: String? = null,
    val trackNumber: Int? = null,
    val discNumber: Int? = null,
    val year: Int? = null,
    val genre: String? = null,
    val duration: Double? = null,
    val bitRate: Int? = null,
    val suffix: String? = null,
    val size: Long? = null,
    val createdAt: String? = null,
    val starred: Boolean = false,
    val starredAt: String? = null,
    val rating: Int? = null,
    val playCount: Long? = null,
    val rgTrackGain: Double? = null,
    val rgAlbumGain: Double? = null,
    val rgTrackPeak: Double? = null,
    val rgAlbumPeak: Double? = null,
) {
    fun toSong() = Song(
        id = id,
        title = title,
        album = album,
        albumId = albumId,
        artist = artist,
        artistId = artistId,
        track = trackNumber,
        discNumber = discNumber,
        year = year?.takeIf { it > 0 },
        genre = genre,
        coverArt = "mf-$id",
        duration = duration?.toInt(),
        bitRate = bitRate,
        suffix = suffix,
        size = size,
        created = createdAt,
        starred = if (starred) (starredAt ?: "1") else null,
        userRating = rating,
        playCount = playCount,
        replayGain = ReplayGain(rgTrackGain, rgAlbumGain, rgTrackPeak, rgAlbumPeak).takeUnless { it.isEmpty },
    )
}

@Serializable
data class NativeLogin(val token: String? = null, val isAdmin: Boolean = false, val name: String? = null)

enum class SongSort(val field: String, @StringRes val label: Int) {
    TITLE("title", R.string.sort_title),
    DATE_ADDED("createdAt", R.string.sort_date_added),
    ARTIST("artist", R.string.sort_artist),
    ALBUM("album", R.string.sort_album),
}

/** getAlbumList2 types, in the order the sort menu shows them. */
enum class AlbumSort(val type: String, @StringRes val label: Int) {
    NAME("alphabeticalByName", R.string.sort_name),
    ARTIST("alphabeticalByArtist", R.string.sort_artist),
    NEWEST("newest", R.string.sort_recently_added),
    RECENT("recent", R.string.sort_recently_played),
    FREQUENT("frequent", R.string.sort_most_played),
    RANDOM("random", R.string.sort_random),
    STARRED("starred", R.string.favourites),
}
