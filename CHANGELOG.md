# Changelog

What changed in each NaviCtrl release. Every release on the [Releases page](https://github.com/VoidedKN0X/NaviCtrl/releases) also lists the exact changes in that build.

To publish a new version: bump `versionCode` and `versionName` in `app/build.gradle.kts`, add a section here, and push to `main`. Pushes without a version bump only build; they don't make a release.

## v0.1.11 (2026-10-18)

A security follow-up to 0.1.9.

### Security

- **Your sign-in details no longer follow a redirect to another server in the address.** Navidrome requests carry your user name and a signed token with its salt in the address itself. When the server, or something in front of it, redirected a request to another site and copied the address along (a common way to move a server to a new domain), that site received them and could use them to sign in as you or try to work out your password. They are now removed from the address before the request goes anywhere else, along with the session headers 0.1.9 already removed. Redirects that stay on your server, and internet radio streams, work as before
- Error messages also hide an OpenSubsonic API key ("apiKey") if one ever appears in an address

## v0.1.10 (2026-10-18)

### Changed

- The **You** tab is gone from the bar at the bottom. Tap your profile picture at the top right of Songs, Favourites or Search to open it instead. Everything that was there (downloads, server info, library scan, settings, accounts and sign out) is still on that page
- The bottom bar now has three tabs: Songs, Favourites and Search. On tablets and unfolded foldables the side rail matches

## v0.1.9 (2026-10-18)

Security hardening and a fix for a black screen.

### Fixed

- Fixed a black screen that could appear when tapping Back twice quickly, or going back while a screen was still sliding in, until the app was restarted. Extra taps during a screen change are now ignored and the first screen is never closed
- Deleting a playlist now only closes that playlist's page, even if you've already moved to another screen

### Security

- Your Navidrome session and your password are never passed on if your server redirects the app to a different server
- Album art now follows the same connection rules as the rest of the app: no plain HTTP except to a server you chose, and no quiet switch from HTTPS to HTTP
- Signing out of an account now also clears that account's songs from the playback cache, the cached album art and its saved queue (downloads still stay until you remove them)
- Each account keeps its own cached songs, so one server's music is never played back for another
- Signing in to a different server now stops the previous server's queue, so its songs aren't reported as played on the new server
- Only NaviCtrl itself can add songs to the queue. The notification, lock screen, Bluetooth and Android Auto can still play, pause and skip, but can't make the player open other addresses or files
- Error messages never show the sign-in details that are part of server addresses
- The sign-in screen, where the password can be shown, is left out of the recent-apps preview

## v0.1.8 (2026-10-18)

- Version numbers are now plain, with no build number on the end, and a new version only comes out when something in the app actually changes. Nothing else in the app has changed.

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
