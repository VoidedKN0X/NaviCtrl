# Changelog

What changed in each NaviCtrl release. Every release on the [Releases page](https://github.com/VoidedKN0X/NaviCtrl/releases) also lists the exact changes in that build.

To publish a new version: bump `versionCode` and `versionName` in `app/build.gradle.kts`, add a section here, and push to `main`. Pushes without a version bump only build; they don't make a release.

## v0.1.5 (2026-10-17)

- The Library tab is now a **Favourites** tab: your starred songs, albums and artists in one place

## v0.1.4 (2026-10-17)

- Removed the Home tab; the app now opens straight to **Songs**

## v0.1.3 (2026-10-16)

- Fixed the app icon: the note heads now join their stems

## v0.1.2 (2026-10-16)

Security hardening.

- Only this app, trusted media controllers, Bluetooth and Android Auto can control playback
- Your login token never leaves the app, not even in album art shown on the lock screen or in other apps
- Sign-in no longer quietly falls back from HTTPS to HTTP. If a server only works over HTTP you're asked first, and those accounts show "Not encrypted (HTTP)"
- Safer file names for downloads (existing downloads keep working)
- Nothing is included in cloud backups or device-to-device transfers

## v0.1.1 (2026-10-16)

First release: a full music player for Navidrome.

- All your songs in one list, A to Z or by date added, with Shuffle all
- Albums, artists, playlists and internet radio
- Background playback, synced lyrics and sleep timer
- Downloads for offline listening
- Scrobbling through Navidrome
