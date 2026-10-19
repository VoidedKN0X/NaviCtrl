# NaviCtrl

**A full music player for your Navidrome server.**

[![Latest release](https://img.shields.io/github/v/release/VoidedKN0X/NaviCtrl?label=download)](https://github.com/VoidedKN0X/NaviCtrl/releases/latest) ![Android 13+](https://img.shields.io/badge/Android-13%2B-3DDC84?logo=android&logoColor=white) [![License: GPL-3.0](https://img.shields.io/badge/license-GPL--3.0-blue)](LICENSE) [![Sponsor](https://img.shields.io/badge/sponsor-GitHub-ea4aaa?logo=githubsponsors&logoColor=white)](https://github.com/sponsors/VoidedKN0X)

NaviCtrl is an Android music player for self-hosted [Navidrome](https://www.navidrome.org/) servers.

Part of the [Ctrl series](#the-ctrl-series) by VKNXWorks.

<a href="https://apps.obtainium.imranr.dev/redirect?r=obtainium://add/https://github.com/VoidedKN0X/NaviCtrl"><img src=".github/assets/badge_obtainium.png" alt="Get it on Obtainium" height="80"></a> <a href="https://voidedkn0x.github.io/CtrlF-Droid/"><img src=".github/assets/badge_ctrlf-droid.png" alt="Get it from our F-Droid repo" height="80"></a>

## Features

- **Songs tab** with your whole library, A to Z or by date added, that opens instantly
- **Shuffle all**: an endless random queue
- **Favourites tab** for songs, albums and artists
- **Background playback** with lock screen and Bluetooth controls
- **Synced lyrics**, sleep timer, and a mix of similar songs started from any song
- **Up next queue** you can reorder; your last queue comes back after a restart
- **Downloads** for offline listening, and a cache of played songs
- **Scrobbling** to Last.fm and ListenBrainz through Navidrome
- **Album and artist pages**, search, and **Add to playlist** from any song
- **Scan for new music** on your server from the app (admin accounts)
- **Several servers and accounts**; streaming quality for Wi-Fi and mobile data

## Planned

Not in the app yet: browsing and playing your playlists, genre pages and internet radio.

## Requirements

- Android 13 or newer
- A Navidrome server

## Install

1. Download the latest `.apk` file from the [Releases page](https://github.com/VoidedKN0X/NaviCtrl/releases/latest).
2. Open it on your phone. Android may ask you to allow installing apps from your browser or file manager.
3. Updates install over the old version. Your settings and login are kept.

**Automatic updates:** on your phone, tap one of the buttons below.

- **Obtainium:** install [Obtainium](https://github.com/ImranR98/Obtainium) first. The button adds NaviCtrl and installs every new release for you.
- **F-Droid:** adds the [CtrlF-Droid repo](https://voidedkn0x.github.io/CtrlF-Droid/) to F-Droid, Droid-ify or Neo Store, with every Ctrl app and automatic updates.

<a href="https://apps.obtainium.imranr.dev/redirect?r=obtainium://add/https://github.com/VoidedKN0X/NaviCtrl"><img src=".github/assets/badge_obtainium.png" alt="Get it on Obtainium" height="80"></a> <a href="https://voidedkn0x.github.io/CtrlF-Droid/"><img src=".github/assets/badge_ctrlf-droid.png" alt="Get it from our F-Droid repo" height="80"></a>

If the Obtainium button doesn't open the app, add `https://github.com/VoidedKN0X/NaviCtrl` in Obtainium by hand.

## Getting started

1. Enter your Navidrome address, e.g. `https://music.example.com`.
2. Sign in with your Navidrome username and password.
3. Tap **Shuffle all** or pick a song to start listening.

Downloads, **Settings**, accounts, sign out and (for admins) **Scan for new music** are behind your profile picture at the top right.

## Tips and troubleshooting

- **Plain HTTP** works, but the app warns you first because your password would be sent unencrypted.
- **Start a mix from this song** (in a song's menu) needs Last.fm set up in Navidrome.
- **Scrobbling** and play counts use **Settings > Report what I play**; Last.fm and ListenBrainz are linked in Navidrome itself.

## Usage stats

NaviCtrl can share anonymous usage stats, **only if you say yes**. It asks once after you sign in, and you can change it any time in **Settings > Usage stats**.

- **Shared:** which screens you open (the kind of screen, never which album, artist, playlist or genre), how you start music (from an album, playlist, artist, genre, favourites, search, downloads, the songs list, shuffle all, a mix or internet radio, with or without shuffle, and whether the song was downloaded), whether you download a song, album or playlist, the app version, Android version, phone model, language, phone or tablet, your theme, and whether you stream original files on Wi-Fi.
- **Never shared:** your server address, username or passwords, what you listen to or search for (no song, album, artist, playlist or station names or IDs), or any ID that stays the same between sessions.
- **Where:** VKNXWorks' own self-hosted [Aptabase](https://aptabase.com) server. Nothing goes to Google or analytics companies. Like any website, the server sees your IP address; Aptabase uses it only to work out the country and doesn't store it.

Builds from source don't include the stats key, so they can't send anything and the setting is hidden. The code is in [`Telemetry.kt`](app/src/main/java/com/vknxworks/navictrl/data/telemetry/Telemetry.kt).

Apart from these opt-in stats, NaviCtrl only talks to your own Navidrome server, plus the stream address of an internet radio station when you play it.

## Build from source

You need [Android Studio](https://developer.android.com/studio), or JDK 17 and the Android SDK.

1. Clone the repo:
   ```bash
   git clone https://github.com/VoidedKN0X/NaviCtrl.git
   cd NaviCtrl
   ```
2. Build the app:
   ```bash
   ./gradlew assembleDebug
   ```
   On Windows, use `gradlew.bat assembleDebug`.
3. The APK is in `app/build/outputs/apk/debug/`. Copy it to your phone and install it.

Or open the folder in Android Studio and press **Run** with your phone connected.

## Support the project

NaviCtrl is free and open source. If it's useful to you, you can support development on [GitHub Sponsors](https://github.com/sponsors/VoidedKN0X). Starring the repo and [reporting bugs](https://github.com/VoidedKN0X/NaviCtrl/issues) helps too.

## The Ctrl series

Native Android apps for self-hosted services and everyday tools, all with the same Material You design.

| App | What it's for |
| --- | --- |
| [ArrCtrl](https://github.com/VoidedKN0X/ArrCtrl) | Radarr, Sonarr, Prowlarr, Bazarr, qBittorrent, autobrr and Cleanuparr in one app |
| [BeamCtrl](https://github.com/VoidedKN0X/BeamCtrl) | TV remote for phones with an IR blaster |
| [CraftyCtrl](https://github.com/VoidedKN0X/CraftyCtrl) | Manage Minecraft servers on Crafty Controller |
| [ElementCtrl](https://github.com/VoidedKN0X/ElementCtrl) | Matrix chat client for matrix.org and self-hosted Synapse |
| [FilesCtrl](https://github.com/VoidedKN0X/FilesCtrl) | File manager with NFS, SMB, SFTP, FTP and WebDAV |
| [FrigateCtrl](https://github.com/VoidedKN0X/FrigateCtrl) | Watch and review your Frigate NVR cameras |
| [ImmichCtrl](https://github.com/VoidedKN0X/ImmichCtrl) | Photos and backup for your Immich server |
| [JellyCtrl](https://github.com/VoidedKN0X/JellyCtrl) | Movies, shows, music and Live TV from Jellyfin |
| **NaviCtrl** (this app) | Music player for Navidrome servers |
| [PaperCtrl](https://github.com/VoidedKN0X/PaperCtrl) | Scan and browse documents on Paperless-ngx |
| [PrintCtrl](https://github.com/VoidedKN0X/PrintCtrl) | Bambu Lab printers through Bambuddy, and Spoolman spools |
| [ProxCtrl](https://github.com/VoidedKN0X/ProxCtrl) | Manage your Proxmox VE server |
| [SeerrCtrl](https://github.com/VoidedKN0X/SeerrCtrl) | Request movies and shows on Seerr / Jellyseerr |
| [TrueCtrl](https://github.com/VoidedKN0X/TrueCtrl) | Monitor and manage your TrueNAS server |

## License

NaviCtrl is licensed under the [GNU General Public License v3.0](LICENSE). © VKNXWorks

The Manrope font is licensed under the SIL Open Font License; see `FONT-LICENSE-Manrope.txt`.
