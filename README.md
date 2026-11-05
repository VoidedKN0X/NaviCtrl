# NaviCtrl

**A full music player for your Navidrome server.**

[![Latest release](https://img.shields.io/github/v/release/VoidedKN0X/NaviCtrl?label=download)](https://github.com/VoidedKN0X/NaviCtrl/releases/latest) ![Android 15+](https://img.shields.io/badge/Android-15%2B-3DDC84?logo=android&logoColor=white) [![License: GPL-3.0](https://img.shields.io/badge/license-GPL--3.0-blue)](LICENSE) ![Telemetry-free](https://img.shields.io/badge/telemetry-none-brightgreen) [![Sponsor](https://img.shields.io/badge/sponsor-GitHub-ea4aaa?logo=githubsponsors&logoColor=white)](https://github.com/sponsors/VoidedKN0X)

NaviCtrl is an Android music player for self-hosted [Navidrome](https://www.navidrome.org/) servers.

Part of the [Ctrl series](#the-ctrl-series) by VKNXWorks.

<a href="https://apps.obtainium.imranr.dev/redirect?r=obtainium://add/https://github.com/VoidedKN0X/NaviCtrl"><img src=".github/assets/badge_obtainium.png" alt="Get it on Obtainium" height="80"></a> <a href="https://voidedkn0x.github.io/CtrlF-Droid/"><img src=".github/assets/badge_ctrlf-droid.png" alt="Get it from our F-Droid repo" height="80"></a>

## Features

- **Songs tab** with your whole library, A to Z or by date added, that opens instantly
- **Shuffle all**: an endless random queue
- **Favourites tab** for songs, albums and artists
- **Playlists tab** with your playlists and the ones shared on your server: play, shuffle or download them, make new ones, rename or delete your own, and remove songs
- **Background playback** with lock screen and Bluetooth controls
- **Synced lyrics**, sleep timer, and a mix of similar songs started from any song
- **Up next queue** you can reorder; your last queue comes back after a restart
- **Play queue sync between devices**: your queue, song and position are saved on your server, and NaviCtrl offers to continue a queue saved on another device or app (with where and when it was saved), or on request with **Resume from another device**
- **Gapless playback**, **crossfade** (up to 12 seconds, skipped between songs that run on from the same album) and **ReplayGain** (track or album, with preamp and clipping prevention)
- **Equalizer** with your phone's presets or your own band levels, and bass boost
- **Downloads** for offline listening, and a cache of played songs
- **Scrobbling** to Last.fm and ListenBrainz through Navidrome
- **Album and artist pages**, search, and **Add to playlist** from any song or album
- **Scan for new music** on your server from the app (admin accounts)
- **Several servers and accounts**; streaming quality for Wi-Fi and mobile data
- **App lock** with its own PIN and optional fingerprint unlock; your server password stays encrypted while the app is locked, and music and downloads that are already running carry on
- **Hide in screenshots** and **Hide in recent apps**, two separate switches, both off by default

## Requirements

- Android 15 or newer, on a 64-bit ARM phone (nearly every phone from the last several years)
- A Navidrome server, or music stored on the phone (**Use music on this phone** on the sign-in screen)
- **Google-free and de-Googled:** no Google Play services, Firebase or other Google services, and the app never contacts Google. It works the same on de-Googled phones (GrapheneOS, CalyxOS, /e/OS, LineageOS without Google apps).

## Install

1. On your phone, open the [latest release](https://github.com/VoidedKN0X/NaviCtrl/releases/latest), scroll down to **Assets** and tap the file that ends in `.apk` (for example `NaviCtrl-v0.7.3.apk`) to download it. The "Source code" zip and tar.gz files are not the app; you don't need them.
2. Open the downloaded file. Android may ask you to allow installing apps from your browser or file manager.
3. Updates install over the old version. Your settings and login are kept.

**Automatic updates:** on your phone, tap one of the buttons below.

- **Obtainium:** install [Obtainium](https://github.com/ImranR98/Obtainium) first. The button adds NaviCtrl and installs every new release for you.
- **F-Droid:** adds the [CtrlF-Droid repo](https://voidedkn0x.github.io/CtrlF-Droid/) to F-Droid, Droid-ify or Neo Store, with every Ctrl app and automatic updates.

<a href="https://apps.obtainium.imranr.dev/redirect?r=obtainium://add/https://github.com/VoidedKN0X/NaviCtrl"><img src=".github/assets/badge_obtainium.png" alt="Get it on Obtainium" height="80"></a> <a href="https://voidedkn0x.github.io/CtrlF-Droid/"><img src=".github/assets/badge_ctrlf-droid.png" alt="Get it from our F-Droid repo" height="80"></a>

If the Obtainium button doesn't open the app, add `https://github.com/VoidedKN0X/NaviCtrl` in Obtainium by hand.

## Is it safe to install?

- **Open source.** All of NaviCtrl's code is in this repo, under the GPL-3.0 license. Anyone can read it.
- **Built by GitHub, not on a personal computer.** Every release on the Releases page is built by [the build workflow](.github/workflows/build.yml) on GitHub Actions, straight from the code in this repo.
- **Always signed with the same key.** Android refuses to install an update that's signed with a different key, so nobody else can publish an update over your copy. The [CtrlF-Droid repo](https://voidedkn0x.github.io/CtrlF-Droid/) only accepts APKs signed with this key too. The SHA-256 fingerprint of the signing certificate is:

  ```
  20:78:6F:26:52:F1:D3:27:F8:FC:C1:8B:3D:D5:43:06:6F:AD:33:34:90:E2:3B:FE:00:34:93:09:A1:10:FE:FC
  ```

  Advanced users can compare it with [AppVerifier](https://github.com/soupslurpr/AppVerifier).
- **Check a download:** the release notes list the APK's SHA-256 checksum, so you can compare it with the file you downloaded. With the [GitHub CLI](https://cli.github.com/), `gh attestation verify <apk> -R VoidedKN0X/NaviCtrl` proves the APK was built by this repo's build workflow from the tagged commit. The signing-certificate fingerprint above stays the check that matters for updates.

**Permissions** NaviCtrl asks for, and why:

| Permission | What it's for |
| --- | --- |
| Internet | Talking to your Navidrome server (music, artwork, lyrics, your library) |
| Network state | Picking the Wi-Fi or mobile data streaming quality, and waiting for Wi-Fi for downloads when you ask |
| Notifications | The playback notification with its controls (Android asks you first) |
| Foreground service and media playback service | Keeping music playing while NaviCtrl is in the background |
| Wake lock | Keeping music and downloads going while the screen is off |
| Change audio settings | The equalizer and bass boost on NaviCtrl's own playback |
| Use biometrics | Optional fingerprint unlock for the app lock |
| Hide other apps' floating windows | Hiding other apps' floating windows (chat heads, overlays) over the PIN screen, sign-in and "can't be undone" confirmations, so nothing can cover them. No prompt, and nothing changes on other screens |
| Music and audio (optional) | **Music on this phone**: playing the songs stored on the phone, only asked for when you choose it |
| Run at startup | Picking up downloads that were still waiting when the phone restarted (they start after you unlock NaviCtrl) |

Android also lists a permission named after NaviCtrl itself (`DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION`): it only lets NaviCtrl's own parts talk to each other, and no other app can hold it.

NaviCtrl can't read your contacts, location or microphone, or any of your files other than music (and those only after you choose Music on this phone). See [Privacy](#privacy) for what it sends and where.

## Getting started

No server? Tap **Use music on this phone** on the sign-in screen to play the songs stored on the phone. Favourites, playlists and play counts for them are kept on the phone.

1. Enter your Navidrome address, e.g. `https://music.example.com`.
2. Sign in with your Navidrome username and password.
3. Tap **Shuffle all**, pick a song, or open the **Playlists** tab to start listening.

Downloads, **Settings**, accounts, sign out and (for admins) **Scan for new music** are behind your profile picture at the top right.

## App lock

Turn it on under your profile picture › **Settings > Privacy and security > App lock**. It's off until you turn it on.

- **Its own app PIN** of at least 6 digits, separate from your phone's screen lock. Very obvious PINs such as 111111 or 123456 are refused.
- **Unlock with fingerprint** (optional) only accepts strong biometrics, such as the fingerprint sensor, checked by the phone's secure hardware. Face unlock that only uses the camera isn't accepted. The PIN is still needed after the phone restarts and at least every 72 hours, and adding a new fingerprint to the phone turns fingerprint unlock off until you turn it on again.
- **Your server password is encrypted with the PIN while NaviCtrl is locked.** The key only exists in memory while the app is unlocked and is wiped the moment it locks. The PIN itself is never stored: it's stretched with 600,000 rounds of PBKDF2 and mixed with a key held in the phone's secure hardware, so a copy of the app's files isn't enough to guess it on a computer.
- **Wrong PINs:** after 5 tries you wait 30 seconds, doubling up to 15 minutes. Restarting the app or the phone, or changing the clock, doesn't skip the wait.
- **Lock again:** as soon as you leave the app (the default), after 1, 5 or 15 minutes or 1 hour away, or only when the app restarts. Opening a link from the About section and answering the notification permission question don't count as leaving, unless you stay away for more than 5 minutes.
- **Change app PIN** asks for your current PIN first, and turning the app lock off asks for the PIN too.
- **Forgot your PIN?** It can't be recovered, because it encrypts your server passwords. The lock screen offers to remove every account and delete the downloads, played music and artwork kept on the phone; nothing changes on your Navidrome server.
- **With the app lock on**, the recent-apps screen always shows a blank card, music that's already playing (or paused with a queue) and downloads in progress carry on while NaviCtrl is locked, with their controls in the notification, on the lock screen and on Bluetooth devices, and plays are still reported to your server: they keep the sign-in they need in memory until they stop, and anything new needs an unlock. Downloads that were waiting when the phone restarted start after you unlock.

Two more switches in the same section work with or without the app lock, and are both off until you turn them on:

- **Hide in screenshots**: screenshots and screen recordings show a blank screen (and so does the recent-apps view).
- **Hide in recent apps**: the recent-apps view shows a blank card instead of NaviCtrl. Screenshots still work.

The sign-in screen and the lock and PIN screens are always kept out of screenshots and recordings, whatever these switches say.

While the lock, PIN or sign-in screen or a "can't be undone" confirmation is showing, other apps' floating windows (chat heads, overlays) are hidden so nothing can cover it, and apps that read the screen through accessibility can't see the PIN or lock screen; real accessibility tools such as screen readers still work, and password managers can still fill the sign-in screen.

## Tips and troubleshooting

- **Plain HTTP** works, but the app warns you first because your password would be sent unencrypted.
- **Start a mix from this song** (in a song's menu) needs Last.fm set up in Navidrome.
- **Scrobbling** and play counts use **Settings > Report what I play**; Last.fm and ListenBrainz are linked in Navidrome itself.

## Privacy

**Telemetry-free.** NaviCtrl has no analytics, crash reporting, usage stats or tracking of any kind. It only talks to your own Navidrome server: cover art, lyrics and similar songs all come through it, and scrobbles to Last.fm and ListenBrainz are sent by Navidrome, not by the app. Your password is stored encrypted on the phone (and with your app PIN while the app lock is on), because Navidrome's music API needs it to sign every request; it's only ever sent to your server: as a salted hash for music requests, and as is (over your server's connection) when signing in to Navidrome's own API for the Songs tab and admin features.

- **Servers:** only the Navidrome servers you sign in to. A redirect to another server never gets your sign-in, and plain HTTP is only used for a server you chose after the "unencrypted" warning. **Music on this phone** never goes online.
- **Links:** the Support, Report a bug, Source code and Licence rows in About open in your browser only when you tap them; the app itself doesn't contact GitHub.
- **No update check:** NaviCtrl never checks for updates by itself; Obtainium or the F-Droid client does that if you use one.
- **Other apps** can't control playback or browse your library through NaviCtrl's music service; only NaviCtrl and the phone's own media controls (notification, lock screen, Bluetooth, car) can.
- **No backups:** Android doesn't back up NaviCtrl's data or move it to a new phone.

**Google-free.** No Google Play services, Firebase or other Google libraries, and nothing ever goes to Google. Every build checks its dependencies for them, and release builds write nothing about you to the phone's log.

## How NaviCtrl is made

NaviCtrl is a one-person project by VoidedKN0X (VKNXWorks). Much of the code is written with the help of AI (Claude, by Anthropic). I decide what the app does, review the changes and use the app myself. Every change is listed in the [changelog](CHANGELOG.md), and you can read every line of code here.

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

**Dependency verification:** every library and plugin the build downloads is checked against its SHA-256 checksum in `gradle/verification-metadata.xml`, and the build stops if one doesn't match. When you change a dependency, a plugin or the Gradle version, run the **Dependency verification metadata** workflow (Actions tab, **Run workflow**), decode the file it prints, check the diff and commit it together with the change.

## Support the project

NaviCtrl is free and open source. If it's useful to you, tap **Support NaviCtrl** under **About** on the You page (tap your profile picture) to sponsor development on [GitHub Sponsors](https://github.com/sponsors/VoidedKN0X). Found a problem or have an idea? Tap **Report a bug** in the same section, or [open an issue](https://github.com/VoidedKN0X/NaviCtrl/issues/new/choose) and pick the bug report or feature request form. Starring the repo helps too.

Found a security problem? Please don't open a public issue; see [SECURITY.md](SECURITY.md).

## The Ctrl series

Native Android apps for self-hosted services and everyday tools, all with the same Material You design.

| App | What it's for |
| --- | --- |
| [AdCtrl](https://github.com/VoidedKN0X/AdCtrl) | Control your AdGuard Home DNS server |
| [ArrCtrl](https://github.com/VoidedKN0X/ArrCtrl) | Radarr, Sonarr, Prowlarr, Bazarr, qBittorrent, autobrr and Cleanuparr in one app |
| [BeamCtrl](https://github.com/VoidedKN0X/BeamCtrl) | TV remote for phones with an IR blaster |
| [CallCtrl](https://github.com/VoidedKN0X/CallCtrl) | Phone app: dialer, call history and in-call screen |
| [ChatCtrl](https://github.com/VoidedKN0X/ChatCtrl) | Matrix chat client for matrix.org and self-hosted Synapse |
| [ClockCtrl](https://github.com/VoidedKN0X/ClockCtrl) | Alarms, timers, stopwatch and world clock |
| [ContactCtrl](https://github.com/VoidedKN0X/ContactCtrl) | Contacts: browse, edit, import and export |
| [CraftyCtrl](https://github.com/VoidedKN0X/CraftyCtrl) | Manage Minecraft servers on Crafty Controller |
| [DateCtrl](https://github.com/VoidedKN0X/DateCtrl) | Calendar for the calendars on your phone and your accounts |
| [FilesCtrl](https://github.com/VoidedKN0X/FilesCtrl) | File manager with NFS, SMB, SFTP, FTP and WebDAV |
| [FrigateCtrl](https://github.com/VoidedKN0X/FrigateCtrl) | Watch and review your Frigate NVR cameras |
| [ImmichCtrl](https://github.com/VoidedKN0X/ImmichCtrl) | Photos and backup for your Immich server |
| [JellyCtrl](https://github.com/VoidedKN0X/JellyCtrl) | Movies, shows, music and Live TV from Jellyfin |
| [LijnCtrl](https://github.com/VoidedKN0X/LijnCtrl) | Live De Lijn bus and tram departures |
| [MailCtrl](https://github.com/VoidedKN0X/MailCtrl) | Email for Gmail and Proton Mail |
| [MemoCtrl](https://github.com/VoidedKN0X/MemoCtrl) | Notes on your Memos server |
| **NaviCtrl** (this app) | Music player for Navidrome servers |
| [PaperCtrl](https://github.com/VoidedKN0X/PaperCtrl) | Scan and browse documents on Paperless-ngx |
| [PrintCtrl](https://github.com/VoidedKN0X/PrintCtrl) | Bambu Lab printers through Bambuddy, and Spoolman spools |
| [ProxCtrl](https://github.com/VoidedKN0X/ProxCtrl) | Manage your Proxmox VE server |
| [RecorderCtrl](https://github.com/VoidedKN0X/RecorderCtrl) | Private, offline voice recorder |
| [RideCtrl](https://github.com/VoidedKN0X/RideCtrl) | Offline maintenance, fuel and mod log for your vehicles |
| [SeerrCtrl](https://github.com/VoidedKN0X/SeerrCtrl) | Request movies and shows on Seerr / Jellyseerr |
| [SumCtrl](https://github.com/VoidedKN0X/SumCtrl) | Offline calculator, unit converter and work tools |
| [SynapseCtrl](https://github.com/VoidedKN0X/SynapseCtrl) | Administer your Matrix Synapse server |
| [TextCtrl](https://github.com/VoidedKN0X/TextCtrl) | Private SMS and MMS messaging |
| [TrueCtrl](https://github.com/VoidedKN0X/TrueCtrl) | Monitor and manage your TrueNAS server |
| [WrtCtrl](https://github.com/VoidedKN0X/WrtCtrl) | Manage your OpenWrt router |

## License

NaviCtrl is licensed under the [GNU General Public License v3.0](LICENSE). © VKNXWorks

NaviCtrl is an independent app and isn't made by or affiliated with the Navidrome project.

The [Space Grotesk](https://github.com/floriankarsten/space-grotesk) and [JetBrains Mono](https://github.com/JetBrains/JetBrainsMono) fonts are bundled with the app and licensed under the SIL Open Font License 1.1; see `FONT-LICENSE-SpaceGrotesk.txt` and `FONT-LICENSE-JetBrainsMono.txt`.
