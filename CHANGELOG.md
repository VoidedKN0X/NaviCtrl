# Changelog

What changed in each NaviCtrl release. Every release on the [Releases page](https://github.com/VoidedKN0X/NaviCtrl/releases) also lists the exact changes in that build.

To publish a new version: bump `versionCode` and `versionName` in `app/build.gradle.kts`, add a section here, and push to `main`. Pushes without a version bump only build; they don't make a release.

## Unreleased

### Changed

- **README brought up to date with 0.1.13.** Playlists, genre pages and internet radio can't currently be opened in the app (they were on the old Library tab, which became Favourites), so the README now lists them under a new **Planned** section instead of as features. The feature list now also mentions starting a mix of similar songs from any song, **Add to playlist**, and **Scan for new music** for admin accounts. **Getting started** explains that downloads, settings and accounts are behind your profile picture now that the You tab is gone, and the tips now name the **Start a mix from this song** menu item and the **Report what I play** setting for scrobbling. Nothing in the app has changed.

## v0.1.13 (2026-10-19)

Fourth security and privacy review of the new usage stats.

### Security

- **Switching usage stats off takes effect straight away.** Turning off **Settings > Usage stats** now also throws away any stats still waiting to be sent and stops one being sent at that moment, and switching them on again starts a new random session. Before, a stat that was already on its way could still go out

### Changed

- The README's **Usage stats** section now lists everything the app's release notes and Settings already said is shared: when you start music it also says whether it was shuffled. It also says that, apart from those opt-in stats, NaviCtrl only talks to your own Navidrome server, plus a radio station's stream address when you play it
- - **All Ctrl apps in the README.** The README's Ctrl series table now lists all 14 apps. New in the table: [ArrCtrl](https://github.com/VoidedKN0X/ArrCtrl) (Radarr, Sonarr, Prowlarr, Bazarr, qBittorrent, autobrr and Cleanuparr in one app), [PaperCtrl](https://github.com/VoidedKN0X/PaperCtrl) (scan and browse documents on Paperless-ngx), [ProxCtrl](https://github.com/VoidedKN0X/ProxCtrl) (manage your Proxmox VE server) and [TrueCtrl](https://github.com/VoidedKN0X/TrueCtrl) (monitor and manage your TrueNAS server). Every app in the table is also in the [CtrlF-Droid repo](https://voidedkn0x.github.io/CtrlF-Droid/) for automatic updates.

## v0.1.12 (2026-10-19)

### New

- **Optional anonymous usage stats.** After you sign in, NaviCtrl asks once whether you'd like to share anonymous usage stats. Nothing is sent unless you tap **Share stats**, and you can switch it on or off any time in **Settings > Usage stats**
- **What's shared:** which screens you open (for example "Album", "Playlist" or "Settings", never which album, artist, playlist or genre), how you start music (from an album, playlist, artist, genre, favourites, search, downloads, the songs list, shuffle all, a song mix or internet radio, with or without shuffle, and whether the first song was downloaded), whether you download a song, an album or a playlist, the app version, Android version, phone model, language, phone or tablet, your theme, and whether you stream original files on Wi-Fi
- **What's never shared:** your server address, username, passwords, what you listen to or search for (no song, album, artist, playlist or radio station names or IDs), or any ID that follows you between sessions. Each session gets a new random ID, and stats from before and after you switch them off and on again aren't linked
- **Where it goes:** to VKNXWorks' own self-hosted server (Aptabase), not to Google or any analytics company. Builds you make from source can't send stats at all, and the setting doesn't appear in them

### Changed

- **PrintCtrl in the Ctrl series list.** The README's table of Ctrl apps now includes [PrintCtrl](https://github.com/VoidedKN0X/PrintCtrl), the app for Bambu Lab printers through a self-hosted Bambuddy server, and Spoolman spools.
- The README has a new **Usage stats** section listing exactly what's collected

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
