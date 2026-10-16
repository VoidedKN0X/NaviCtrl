# NaviCtrl

A full music player for self-hosted [Navidrome](https://www.navidrome.org/) servers, part of the VKNXWorks Ctrl series (CraftyCtrl, FrigateCtrl, SeerrCtrl, JellyCtrl, FilesCtrl). Android 13+.

## Features

**Songs tab**
- Every song in your library in one list, A to Z by default
- Sort by title, date added, artist or album, either direction (date added defaults to newest first)
- Shuffle all: an endless random queue that tops itself up as it plays
- Play from any song: the queue continues through the list in the chosen order, fetched a page at a time
- Fast-scroll thumb with a letter (or month, for date added) bubble; pages load as they come into view, so huge libraries open instantly

**Library**
- Albums (grid or list; sort by name, artist, recently added, recently played, most played, random, favourites)
- Artists (A to Z with sticky letter headers), Playlists, Genres, Internet radio

**Playback**
- Background playback with the media notification, lock screen and Bluetooth controls
- Now playing tinted from the artwork, seek bar, shuffle, repeat one/all, favourite
- Up next: drag to reorder, remove, clear
- Synced lyrics (OpenSubsonic) with tap-to-seek, falling back to plain lyrics
- Sleep timer (minutes or end of song)
- Song mixes from similar songs (needs Last.fm configured in Navidrome)
- Scrobbling: play counts, recently played, and Last.fm / ListenBrainz through Navidrome
- Your last queue comes back after the app restarts
- Streaming quality for Wi-Fi and mobile data (original or transcoded MP3/Opus/AAC)
- On-disk cache of played music (off to 8 GB)

**Everything else**
- Home: recently added, recently played, most played, rediscover, favourite albums, playlists
- Album, artist (popular songs, albums, similar artists, biography), playlist and genre pages
- Favourites for songs, albums and artists
- Playlists: create, rename, delete, add songs, remove songs
- Downloads for offline listening (songs, albums, playlists), with their own quality and Wi-Fi-only option
- Search across artists, albums and songs
- Several servers and accounts, switch between them
- Admins can start a library scan
- Phone layout with the floating glass tab island; tablet and unfolded foldable layout with a side rail

## Building

GitHub Actions builds a release APK on every push to `main` and publishes it on the Releases page.

Release signing uses the same setup as the other Ctrl apps:

| Name | Where |
| --- | --- |
| `KEYSTORE_BASE64` | Secret: the keystore, base64 encoded |
| `KEYSTORE_PASSWORD` | Secret |
| `KEY_PASSWORD` | Secret |
| `KEY_ALIAS` | Variable (or secret) |

Without them, the APK is signed with a debug key so it still installs.

Locally: `./gradlew assembleRelease` (JDK 17, Android SDK 36).

## Notes

- Sign-in uses Subsonic token auth with a per-account salt, plus Navidrome's own API for the sortable all-songs list. The password is encrypted with an Android Keystore key and only sent to your server.
- Plain HTTP servers work (the sign-in screen warns about it), and user-installed certificates are trusted for self-signed reverse proxies.

Manrope font: SIL Open Font License, see `FONT-LICENSE-Manrope.txt`.
