# NaviCtrl

**A full music player for your Navidrome server.**

[![Latest release](https://img.shields.io/github/v/release/VoidedKN0X/NaviCtrl?label=download)](https://github.com/VoidedKN0X/NaviCtrl/releases/latest) ![Android 13+](https://img.shields.io/badge/Android-13%2B-3DDC84?logo=android&logoColor=white) [![License: GPL-3.0](https://img.shields.io/badge/license-GPL--3.0-blue)](LICENSE) [![Sponsor](https://img.shields.io/badge/sponsor-GitHub-ea4aaa?logo=githubsponsors&logoColor=white)](https://github.com/sponsors/VoidedKN0X)

NaviCtrl is an Android music player for self-hosted [Navidrome](https://www.navidrome.org/) servers.

Part of the [Ctrl series](#the-ctrl-series) by VKNXWorks.

<a href="https://apps.obtainium.imranr.dev/redirect?r=obtainium://add/https://github.com/VoidedKN0X/NaviCtrl"><img src=".github/assets/badge_obtainium.png" alt="Get it on Obtainium" height="80"></a> <a href="https://voidedkn0x.github.io/CtrlF-Droid/"><img src="https://img.shields.io/badge/Add%20to-F--Droid-1976D2?style=for-the-badge&logo=fdroid&logoColor=white" alt="Add the CtrlF-Droid repo to F-Droid" height="56"></a>

## Features

- **Songs tab** with your whole library, A to Z or by date added, that opens instantly
- **Shuffle all**: an endless random queue
- **Favourites tab** for songs, albums and artists
- **Background playback** with lock screen and Bluetooth controls
- **Synced lyrics**, sleep timer and song mixes
- **Up next queue** you can reorder; your last queue comes back after a restart
- **Downloads** for offline listening, and a cache of played songs
- **Scrobbling** to Last.fm and ListenBrainz through Navidrome
- **Playlists**, album, artist and genre pages, search
- **Several servers and accounts**; streaming quality for Wi-Fi and mobile data

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

<a href="https://apps.obtainium.imranr.dev/redirect?r=obtainium://add/https://github.com/VoidedKN0X/NaviCtrl"><img src=".github/assets/badge_obtainium.png" alt="Get it on Obtainium" height="80"></a> <a href="https://voidedkn0x.github.io/CtrlF-Droid/"><img src="https://img.shields.io/badge/Add%20to-F--Droid-1976D2?style=for-the-badge&logo=fdroid&logoColor=white" alt="Add the CtrlF-Droid repo to F-Droid" height="56"></a>

If the Obtainium button doesn't open the app, add `https://github.com/VoidedKN0X/NaviCtrl` in Obtainium by hand.

## Getting started

1. Enter your Navidrome address, e.g. `https://music.example.com`.
2. Sign in with your Navidrome username and password.
3. Tap **Shuffle all** or pick a song to start listening.

## Tips and troubleshooting

- **Plain HTTP** works, but the app warns you first because your password would be sent unencrypted.
- Song mixes need Last.fm set up in Navidrome.

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
| [BeamCtrl](https://github.com/VoidedKN0X/BeamCtrl) | TV remote for phones with an IR blaster |
| [CraftyCtrl](https://github.com/VoidedKN0X/CraftyCtrl) | Manage Minecraft servers on Crafty Controller |
| [ElementCtrl](https://github.com/VoidedKN0X/ElementCtrl) | Matrix chat client for matrix.org and self-hosted Synapse |
| [FilesCtrl](https://github.com/VoidedKN0X/FilesCtrl) | File manager with NFS, SMB, SFTP, FTP and WebDAV |
| [FrigateCtrl](https://github.com/VoidedKN0X/FrigateCtrl) | Watch and review your Frigate NVR cameras |
| [ImmichCtrl](https://github.com/VoidedKN0X/ImmichCtrl) | Photos and backup for your Immich server |
| [JellyCtrl](https://github.com/VoidedKN0X/JellyCtrl) | Movies, shows, music and Live TV from Jellyfin |
| **NaviCtrl** (this app) | Music player for Navidrome servers |
| [SeerrCtrl](https://github.com/VoidedKN0X/SeerrCtrl) | Request movies and shows on Seerr / Jellyseerr |

## License

NaviCtrl is licensed under the [GNU General Public License v3.0](LICENSE). © VKNXWorks

The Manrope font is licensed under the SIL Open Font License; see `FONT-LICENSE-Manrope.txt`.
