# Changelog

What changed in each NaviCtrl release. Every release on the [Releases page](https://github.com/VoidedKN0X/NaviCtrl/releases) also lists the exact changes in that build.

To publish a new version: bump `versionCode` and `versionName` in `app/build.gradle.kts`, add a section here, and push to `main`. Pushes without a version bump only build; they don't make a release.

## v0.13.2 (2026-11-22)

Built and checked by CI only: nothing in this release had been run on a phone when it was published.

### Changed

- **Less battery and mobile data while music plays.** While a song played, NaviCtrl rewrote its saved queue (up to 2000 songs, encrypted) and sent the whole queue to your Navidrome server every 30 seconds; on mobile data that woke the radio twice a minute for your whole listening session. It now does this every 5 minutes, on top of the saves that already happened on every song change, pause, queue change and when you leave the app. The only difference you could notice: after a crash, or when you continue on another device without pausing here first, the position can be up to 5 minutes old instead of 30 seconds.
- **The music screens no longer rebuild the whole queue twice a second.** The state behind the player screens was rebuilt from the whole queue (thousands of songs with an endless queue) twice a second, also with the screen off and NaviCtrl in the background. Now only the play position is updated, only while NaviCtrl is on screen; the song, queue and play/pause state still change the moment they change. You shouldn't notice anything apart from the battery.
- **Restoring a settings file can no longer switch online lyrics or scrobbling on.** **Find lyrics online (LRCLIB)** and **Report what I play** were taken from the file whichever way they went. A file can now only switch them off; if you want them on, switch them on by hand.

### Fixed

- **Settings are read before NaviCtrl acts on them after a cold start.** When playback was resumed from Bluetooth or the car while NaviCtrl wasn't running, or a download started the app in the background, a few things could use the default settings for a moment instead of yours: the first song could be reported to your server (scrobbled) with **Report what I play** switched off, the restored queue streamed at the default quality, a download used the default quality and ignored your size limit, and the play queue sync setting was checked before it was read. All of these now wait for your saved settings.

### Security

- **Copied share links are marked as private.** A share link lets anyone who has it stream the shared songs without signing in. Copying one now marks it as sensitive, so Android shows dots instead of the link in its copy preview and keyboards leave it out of their clipboard history.
- **"Forgot PIN?" now asks for your phone's screen lock first.** Resetting removes your accounts, downloads and played music, so it's no longer open to anyone who picks up your unlocked phone: after you confirm, NaviCtrl asks for your phone's own PIN, pattern, password or fingerprint before it resets. If your phone has no screen lock, the reset works as before, with just the confirmation.

## v0.13.1 (2026-11-21)

Built and checked by CI only: nothing in this release had been run on a phone when it was published.

### Fixed

- **Dutch labels that were missing.** "Download", "Playlists", "Albums", "Downloads", "Server", "Crossfade", "Equalizer" and "Presets" had no Dutch entry. They are now in the Dutch file: "Downloaden" and "Afspeellijsten", the others as Dutch already says them.

## v0.13.0 (2026-11-21)

Built and checked by CI only: nothing in this release had been run on a phone when it was published.

### New

- **NaviCtrl in French and German, following the phone's language.** When your phone is set to French or German, every screen, message and notification of NaviCtrl now appears in that language (French with "vous", German with "du"), next to the English and Dutch it already had. NaviCtrl also now tells Android which languages it has, so Android's per-app language setting (Settings › Apps › NaviCtrl › Language) offers English, Dutch, French and German. Names of products and formats (Navidrome, Subsonic, ReplayGain, LRCLIB, Last.fm…) stay as they are. Translated by Claude, not yet checked by a native speaker or run on a phone.

## v0.12.0 (2026-11-21)

Built and checked by CI only: nothing in this release had been run on a phone when it was published.

### New

- **Share links.** The menu of a song, an album and a playlist has a new **Share a link**. NaviCtrl asks your Navidrome server to make a public link (anyone who has it can listen in a browser, without an account) and opens Android's share sheet with it, so you can send it in any app. The link's description is the title, and the server decides how long it works (its default is a year). Under your profile picture, **Share links** lists every link on the server with its address, end date and number of visits; tap one to share it again, copy it, or delete it (after a confirmation: the link stops working for everyone, permanently). Sharing must be turned on on the server (`EnableSharing` in Navidrome's settings); when it's off NaviCtrl says so. Not offered for **Music on this phone**, which has no server. Checked against Navidrome's source (`getShares`, `createShare`, `deleteShare`); not yet run on a phone.
- **Load the next song ahead on mobile data.** A new switch under **Settings > Streaming**, off by default. While a song plays on mobile data, NaviCtrl fetches the whole next song in the queue and keeps it in the played-music cache, so the music carries on through a tunnel or a stretch without reception instead of stopping after the 30 seconds gapless playback loads ahead. One song at a time, only for songs streamed from your server (downloads and music on this phone are files already), never on Wi-Fi, and it stops when you pause. It uses mobile data for songs you may end up skipping, and needs **Keep played music on the phone** to be on (the row says so when it's off). Not yet run on a phone.
- **Parametric equalizer.** On the **Equalizer** page (Settings > Playback) there is a second equalizer below the phone's own. You add up to 10 bands yourself; each band is a **Peak** (a bell around one frequency), a **Low shelf** or a **High shelf**, with its own frequency (20 Hz to 20 kHz), gain (-15 to +15 dB in half-dB steps) and width (Q 0.2 to 10). A curve shows what all bands do together, changes are heard while you drag, and a **Preamp** (0 to -15 dB) makes room for boosts: when the curve goes above 0 dB, the page says by how much, because loud passages could distort. Unlike the existing equalizer, which is the phone's own and differs per phone, these are NaviCtrl's own filters inside the player, so they work the same on every phone and output, also on a phone that has no equalizer (that page used to show only a message there). Both can be on together. Off by default; with it off the sound passes through unchanged. The filter maths has unit tests; nothing was run on a phone, so listen for clicks or distortion when you try it.
- **Export and import of settings.** At the bottom of **Settings**, **Export settings** saves NaviCtrl's settings as a small file in a place you pick (Downloads, a cloud folder), and **Import settings** loads such a file on this or another phone. The file holds streaming and download quality, the cache size, playback (gapless, crossfade, ReplayGain), both equalizers with their bands, lyrics lookup, listening history, queue sync, theme and sort order. It never holds accounts, server addresses or passwords, and nothing of the app lock, **Lock again**, **Hide in screenshots** or **Hide in recent apps**: an import cannot sign in anywhere or weaken the app's protection, and leaves those as they are. Before anything changes, NaviCtrl shows when and with which version the file was saved and what it replaces, and asks. Values the settings don't offer are skipped; files from a newer, incompatible NaviCtrl and files that aren't NaviCtrl's are refused. Not yet run on a phone.
- **NaviCtrl in Dutch.** The app now follows your phone's language: with the phone set to Dutch, every screen, message and notification text of NaviCtrl is in Dutch (Belgian Dutch, informal); with any other language it stays English. There is no language switch in the app; Android decides (on Android 13 and newer you can also pick a language per app in Android's settings). Names such as Navidrome, ReplayGain and LRCLIB, units and the app name are not translated. Two texts that were still fixed in the code ("Unknown album" and "Unknown artist" for music on this phone, and the line under the About section) now come from the translations as well. The translation was written with this release and has not been read on a phone yet: tell me where a word is off.

## v0.11.0 (2026-11-21)

Built and checked by CI only: nothing in this release had been run on a phone when it was published. It is the first release built with the new build tools (Android Gradle Plugin 9, stricter code shrinking), so update over your installed version and check that your data and settings are still there.

### Fixed

- **Fixed before release: the app would have crashed at start with the new build tools.** NaviCtrl uses WorkManager for its background work, and WorkManager stores its jobs in a small Room database that is created by name when the app starts. The shrinker (R8) in Android Gradle Plugin 9 is stricter than the old one and removed the constructor that needs, which makes the app stop right at launch (a known problem with Room 2.6.1 under Android Gradle Plugin 9). A keep rule for that constructor, the same one newer Room versions ship themselves, is now in `app/proguard-rules.pro`. No released version was affected: the new build tools have not been in any release. Built by CI only; not yet run on a phone.

### Changed

- **Saved data that can't be opened no longer blocks the whole app.** Since 0.10.0 the list of your downloads and, for **Music on this phone**, your favourites, ratings, play counts and playlists are encrypted on the phone. When one of them couldn't be opened (the key in the phone's secure storage was lost, or the file was damaged) NaviCtrl showed "Can't open your saved data" over everything, so you couldn't browse or play music either. Now that message, with the same **Try again** and **Start empty** (and the same question before starting empty), only appears where the data is used: on the **Downloads** page and under **Downloaded only** on the Songs tab for the download list, and on the **Favourites** and **Playlists** tabs of Music on this phone for its saved favourites and playlists. Everything else (songs, albums, artists, search, streaming, the queue, lyrics) works as usual, and a server account is not affected by a problem with the file of Music on this phone. The Downloads row on the You page says "Can't be opened: tap to see why" instead of a count.
- **What stays exactly as careful as before while a file can't be opened:** nothing is deleted, renamed or written over until you choose **Start empty** (which still only sets the unreadable files aside). A song you downloaded earlier streams from your server instead of playing from the phone, and can't be played offline until the list opens again. Tapping **Download** shows a short notice instead of starting a download, because it couldn't be saved in the list, and **Remove download** does nothing. A download that Android restarts in the background waits and tries again later, without touching any file; it carries on once the list opens, and ends if you start empty. For Music on this phone, hearts, ratings, play counts and playlist changes made while its file can't be opened are not kept.
- **Starting the app waits less on the settings file.** At start NaviCtrl reads its settings before showing anything, so the app lock can never show unlocked for a moment. That read now begins in the background before the network client is set up, instead of after, so the two overlap and the app usually no longer has to stand still for it. What is read, and that the app waits for it, is unchanged.
- **The Now Playing screen does less work while music plays.** The position is updated twice a second, and every update made the whole screen work out its layout again: artwork, titles, buttons and the queue. Now only the seek bar with its two times follows the position, and the lyrics only when the line being sung changes (as the mini player already did since 0.9.1). Nothing looks or behaves differently. The sleep button's "x min left" now counts down by itself once a second, also while paused.
- **A very long Songs list no longer keeps every page it passed in memory.** The list loads 200 songs at a time as you scroll or fast-scroll. It now keeps the 30 pages (6,000 songs) nearest to where you are and lets the rest go; rows you scroll back to after going further than that show as loading for a moment and are fetched again. Playing from a row, the fast-scroll letters and jumping work as before.
- **Newer build tools and libraries; nothing changes in how NaviCtrl looks or works.** NaviCtrl is now built with the current stable toolchain: Android Gradle Plugin 8.11.1 → 9.5.1, Gradle 8.14.3 → 9.9.2, Kotlin 2.2.0 → 2.4.21 (with its Compose and serialization compiler plugins) and Compose BOM 2025.07.00 → 2026.09.00 (Compose 1.12.1, Material 3 1.4.0). The libraries move to the versions the newest Ctrl apps use: AndroidX Activity 1.10.1 → 1.13.0, Core 1.16.0 → 1.18.0, Lifecycle 2.9.1 → 2.10.0, Navigation 2.9.1 → 2.9.8, DataStore 1.1.7 → 1.3.0, kotlinx.serialization 1.9.0 → 1.13.0 and kotlinx.coroutines 1.10.2 → 1.11.0. Everything else stays at the version it had (Media3 1.8.0, OkHttp 4.12.0, Coil 3.2.0, WorkManager 2.10.2, concurrent-futures 1.2.0). No library was added, and the app is still Google-free (CI checks the release dependencies for Play services, Firebase, ML Kit, Play Core and Tink).
- **Compiled against the Android 17 SDK.** Compose 1.12 needs `compileSdk` 37, so that goes from 36 to 37. The Android versions NaviCtrl runs on and targets are unchanged (`minSdk` 35, `targetSdk` 36), so Android treats the app exactly as before.
- **The app is shrunk by a newer R8.** Android Gradle Plugin 9 compiles Kotlin itself (the separate Kotlin Android plugin is gone from the build files) and brings the R8 version that understands Kotlin 2.4. NaviCtrl's own keep rules are unchanged. The newer R8 is stricter about what it keeps, so the release build needs a check on a phone (playback, the notification and lock-screen controls, downloads, on-device music) before the next release.
- The checksums Gradle verifies every download against (`gradle/verification-metadata.xml`) were regenerated on CI for the new versions. Built and checked by CI only; not yet run on a phone.

### Security

- **The signing key is no longer available to the build (nothing in the app changed).** Until now the release key and its passwords were present in the same GitHub Actions job that runs Gradle, so any build plugin or library could in principle have read them. The build job now has no secrets at all and produces an unsigned APK. A separate job, which checks out no code and runs no Gradle, signs it with `apksigner`, and then refuses to hand it on unless it is signed by exactly the certificate published in SECURITY.md and README.md and its contents are identical to what the build job made. Releases are signed with the same key and the same signature scheme (v2) as before, so updates install as usual. The first NaviCtrl release made this way is the next one; the same pipeline already published a release of another Ctrl app (DateCtrl 0.3.1).
- **The build checks that the published fingerprint and the enforced one are the same.** If `build.yml`, SECURITY.md and README.md ever disagree about the signing certificate, the build fails.

### Project

- **Builds no longer use a saved Gradle cache, and the Gradle plugin portal is no longer a plugin source.** Builds of Ctrl apps sometimes failed with "Dependency verification failed ... from repository Gradle Central Plugin Repository", and kept failing until the caches were deleted by hand. The cause: with a restored cache Gradle still fetches the plugin files from Maven Central on every build; when Maven Central can't be reached for a moment, Gradle takes the Kotlin plugin "marker" file from the Gradle plugin portal instead, the portal serves a different file under the same name, verification rightly refuses it, and the failed run then saved that state as the cache for every later run. Now every build and the weekly code scan start from a clean Gradle home (a build takes a few minutes longer), and `settings.gradle.kts` lists only Maven Central and Google's Maven for plugins: everything the build needs is there. Dependency verification itself is unchanged and as strict as before.

## v0.10.0 (2026-11-20)

**This release encrypts some of what the app stores on the phone. An older version cannot read it afterwards, so export or back up anything you need before you update (details below).** Built and checked by CI only: nothing in this release had been run on a phone when it was published.

### Security

- **What NaviCtrl keeps about your listening is now encrypted on the phone.** Four kinds of files in the app's private storage held readable text: the list of your downloads with every song's title, artist and album (`downloads.json`), the saved play queue with the same details (`queue.json`), cached lyrics (`cache/lyrics`), and, for **Music on this phone**, your favourites, ratings, play counts and playlists (`local_library.json`). They are now encrypted with AES-256-GCM. The key is 32 random bytes kept in the app's private folder (`data.key`), itself encrypted by a key in the phone's secure hardware (Android Keystore; StrongBox when the phone has it) that can't be copied off the phone. It needs no PIN or fingerprint, so downloads and music keep working in the background. This protects those files when they are copied off the phone; it does not protect against someone who can use the unlocked phone or a compromised app while it runs, and it works the same with the app lock on or off.
- **What is not encrypted, and why.** Downloaded songs and half-finished downloads (the player opens the files directly), the playback cache of streamed songs and its small index database (Media3 reads and writes them itself, in pieces, while playing), and cached cover art (it is downloaded again when needed) stay as they were. Their file names are hashes or numbers, not titles. Also unchanged: your settings and the list of accounts (the passwords in it were already encrypted), and the small database Android's WorkManager keeps for queued downloads. Songs in the phone's own music library are not NaviCtrl's files.
- **Moving to the encrypted files is automatic and can't lose them.** The first time a file from before this version is read, it is copied in encrypted form to a temporary file, written to disk, read back, decrypted and compared with the original, and only then put in the original's place in one step. If anything goes wrong, or the app is stopped halfway, the original file stays in use and it is tried again the next time. The download list is done at the first start, the saved queue the first time it is restored, a lyric the next time its song is shown, and the file for music on this phone the first time that library is used. On a phone where the secure hardware doesn't work, the files simply stay as they were.
- **If the key is ever lost** (for example the phone's secure storage was reset), NaviCtrl does not start empty and does not overwrite your download list or the favourites and playlists of music on this phone: it shows "Can't open your saved data" with **Try again**. **Start empty** is there for when that doesn't help: what can't be opened is renamed (`downloads.json.unreadable`, `local_library.json.unreadable`), not deleted, and downloaded songs stay on the phone without being listed. The saved queue and cached lyrics are only conveniences: ones that can't be opened are dropped and made again (the queue at the next save, lyrics by looking them up again).
- **Don't go back to an older version.** NaviCtrl 0.9.1 and older can't read the encrypted files: they show no downloads and, for music on this phone, no favourites, ratings, play counts or playlists, and they write new, empty files over the encrypted ones the next time something changes (for music on this phone that is the next song you play). NaviCtrl has no export for these, so going back loses them; the songs themselves, and everything on your Navidrome server, are not affected.

### Changed

- **Less writing to storage while downloading.** The download list used to be saved again for every step of a download's progress bar (about every 256 KB). That progress was never read back (after a restart a download shows as waiting again), so it is now only kept in memory; the list is still saved when a download starts, finishes or fails.
- **The download list is saved to a temporary file, written to disk and then moved into place**, so an interrupted save can't leave a half-written list.
- **The saved queue is written to a temporary file and then moved into place**, so a save that is interrupted can no longer leave a half-written queue behind.

## v0.9.1 (2026-11-20)

Built and checked by CI only: nothing in this release had been run on a phone when it was published.

### Security

- **Lyric lookups can't be intercepted with an installed certificate.** NaviCtrl trusts certificate authorities you install on the phone, so a server behind your own private CA works. That also applied to LRCLIB, the public lyrics database. Connections to `lrclib.net` now only accept the phone's built-in certificate authorities and can never be plain HTTP. Your own server is not affected. If your network inspects HTTPS with a certificate installed on the phone, online lyrics won't load there any more; lyrics from your server still do.

### Changed

- **Starting music no longer waits for the playback cache on the main thread.** The cache of streamed songs has to read its index from disk before the first song can start, and that happened on the main thread at the moment you tapped a song. It is now opened in the background right after the app starts. If you tap a song before that has finished, playback waits for it as before, so the player never runs without its cache. One small difference: the size chosen for **Keep played music on the phone** in Settings is now read when the app starts instead of when the first song plays, so a change to it always applies after NaviCtrl restarts.
- **The mini player redraws less.** The bar above the tabs was redrawn completely twice a second while music played, because it followed the playback position. Now only its thin progress line follows the position; the title, artwork and buttons are redrawn when the song or the play state changes.
- **Artwork is drawn in a lighter way.** Covers in song rows, album and artist grids, playlists, the queue and the player were each drawn through a slower kind of image view (Coil's `SubcomposeAsyncImage`) that the image library itself advises against in lists. They now use the plain image view. It should look the same: the same plain colour while a cover loads, and the same icon or name in its place when there is no cover or it can't be loaded. Not yet checked on a phone.
- **Songs list:** rows that are still loading and rows that show a song are now reused separately while you scroll, which saves work during fast scrolling.

## v0.9.0 (2026-11-13)

Lyrics from the internet when your server has none, and more control over downloads.

### New

- **Lyrics found online.** When your Navidrome server has no lyrics for a song, NaviCtrl now looks them up on [LRCLIB](https://lrclib.net), a free, open lyrics database, and prefers synced lyrics that follow the music. Your server always comes first: synced lyrics from the song's tags or .lrc file (on servers that offer them), then its plain lyrics. Only the song's title, artist, album and length are sent to LRCLIB, nothing about you or your server. It's on by default and can be turned off under **Settings > Lyrics > Find lyrics online (LRCLIB)**. Music on this phone never goes online.
- **Lyrics are kept on the phone**, so each song is looked up once and shows straight away next time, even offline. When nothing was found, NaviCtrl looks again after a week. **Search again** at the top of the lyrics looks right away (for example after adding lyrics on your server).
- **The lyrics screen says where lyrics came from** (your server or LRCLIB), and songs LRCLIB knows have no words show as **Instrumental**.
- **Downloaded only** in the Songs tab: the new download button next to Sort shows just the songs saved on the phone, sorted the same way, with Play and Shuffle. Tap it again for your whole library.
- **Space for downloads** in **Settings > Downloads**: pick how much room downloads may use (1 to 64 GB, or no limit). The Downloads page shows how much of it is used, and a download that doesn't fit says so instead of filling the phone.
- **Download format**: when downloads are converted to a lower quality, you can now pick MP3, Opus or AAC for them separately from streaming. It starts out as your streaming format.

### Changed

- **Synced lyrics follow the song more smoothly:** the line being sung is highlighted and kept a third of the way down the screen, lines already sung fade out, and if you scroll through the lyrics yourself they stay where you put them for a few seconds. Tap any line to play from there.
- **Downloads carry on where they stopped.** When the connection drops (leaving Wi-Fi, a tunnel, the server restarting), the part already saved is kept and the download continues from there once the phone is back online, instead of failing and starting again. Original-quality downloads resume mid-file; converted ones start that song again, because the server converts them on the fly.
- On servers that don't support OpenSubsonic, NaviCtrl no longer asks for synced lyrics it can't get, and plain lyrics that contain LRC timestamps now play synced.
## v0.8.7 (2026-11-13)

### Security

- **Saved logins are protected by the phone's secure chip where it has one.** On a fresh install (or after clearing NaviCtrl's data), the key that encrypts your passwords and tokens is now created in the phone's StrongBox secure element when the phone has one, and in the normal Android Keystore otherwise. If you already use NaviCtrl nothing changes for you: your existing key keeps working and you stay signed in.

### Project

- The Gradle wrapper download is now checked against its published SHA-256 checksum before a build runs.

## v0.8.6 (2026-11-12)

### Changed

- **Slightly smaller download.** The bundled JetBrains Mono font (numbers, readouts and labels) no longer carries the Cyrillic, Vietnamese and maths-alphabet letters NaviCtrl never shows itself, which takes about 0.1 MB off the download and 0.2 MB off the installed app. Everything looks the same; if a name or a log line shown in that font uses one of those scripts, those letters are now drawn with your phone's own font.
- **One font file for Space Grotesk instead of four.** Regular, Medium, SemiBold and Bold now all come from one variable font file, which takes about another 0.15 MB off the download. Text should look the same as before.

## v0.8.5 (2026-11-05)

NaviCtrl now needs Android 15 or newer.

### Changed

- **Needs Android 15 or newer.** Android 13 no longer gets security updates and Android 14's end early 2027, so NaviCtrl now supports Android 15, 16 and 17 only. On older phones the version you have keeps working, but it won't get updates.

## v0.8.4 (2026-11-05)

Password managers work on the sign-in screen again.

### Fixed

- Password managers can fill the sign-in screen (server address, username and password) again. 0.8.3 hid it from every app that reads the screen through accessibility, which also blocked password managers that fill that way; now only the lock screen, the PIN screens and "Forgot PIN?" are hidden from them. Other apps' floating windows are still kept off the sign-in screen, and screen readers such as TalkBack keep working everywhere.

## v0.8.3 (2026-11-05)

More protection for the PIN screen. NaviCtrl works exactly as before.

### Security

- **No floating windows over the PIN screen.** While the lock screen, choosing or changing the app PIN, the sign-in screen or a "can't be undone" confirmation (Forgot PIN, delete a playlist, remove downloads, sign out) is showing, other apps' floating windows such as chat heads and "draw over other apps" overlays are hidden, so no app can cover or imitate these screens to trick a tap or watch your PIN. They come back as soon as the screen closes, and other screens aren't affected (Android 12+, no prompt).
- **The PIN and sign-in screens are hidden from apps that read the screen.** On Android 14+, apps that use Android's accessibility features to read other apps' screens can't see the lock, PIN or sign-in screens. Real accessibility tools such as screen readers still work there.
- **Stricter requests from other apps.** On Android 16+, other apps can only open NaviCtrl's screen and connect to its music player with the requests NaviCtrl declares (opening the app, and media controls such as your watch, car or lock screen), not with made-up requests aimed directly at its parts.

### Project

- **Checked downloads:** every library and plugin the build downloads is now checked against its recorded SHA-256 checksum, and the build stops if one doesn't match.
- **README:** a "Check a download" note (the APK's SHA-256 in the release notes and `gh attestation verify`), the new permission in the permissions table, and how to update the dependency checksums when building from source.

## Project updates (2026-11-05)

### Project

- **Releases can be checked against this repo.** From the next release on, the release notes list the APK's SHA-256 checksum, and each APK gets a signed build provenance attestation from GitHub: `gh attestation verify <apk> -R VoidedKN0X/NaviCtrl` shows it was built by this repo's build workflow from the tagged commit. The release is now published with GitHub's own `gh` tool instead of a third-party action.
- **Least privilege in CI:** every workflow starts with no permissions, and each job gets only what it needs. Only the job that publishes a release can write to the repo, and only it can create attestations.
- **Workflow security check:** a new workflow runs zizmor, a security linter for GitHub Actions, on every change to the workflows. zizmor is installed at one pinned version, checked against its published file hashes.
- **Code scanning:** GitHub's CodeQL analyses the Kotlin code weekly and on every app change; results go to the repo's Security tab and never block a release.
- **Dependency verification:** a new, manually started workflow records the SHA-256 checksum of every library and plugin the build downloads, so the build can refuse anything that doesn't match. No app changes, so no new version.

## v0.8.2 (2026-11-05)

A small security fix. NaviCtrl works exactly as before, including plain HTTP to your own Navidrome servers on your home network or over Tailscale.

### Security

- **A redirect can no longer move an unencrypted connection to another server.** Plain HTTP is only used for a Navidrome server address you chose after the "unencrypted" warning. Until now that check looked only at the address a request started at, so if your server (or something between you and it) answered with a redirect to a different address, NaviCtrl followed it over plain HTTP. Every step of a redirect now has to be an address you allowed for HTTP; otherwise the request stops with the "blocked an unencrypted connection" error. Redirects within your own servers, and redirects up to HTTPS, keep working.

## v0.8.1 (2026-11-05)

A security and privacy check of the whole app and its repo. NaviCtrl works exactly as before.

### Security

- **Release builds no longer write debug or info messages to the phone's log**, from NaviCtrl or from the libraries it uses (such as the music player). NaviCtrl itself never logged your password, server address or what you play, and now nothing from its libraries ends up there either.

### Project

- **Build checks:** every build now verifies that the Gradle wrapper is an official Gradle release, and checks that no Google Play services, Firebase, ML Kit, Play Core or Tink library has made it into the app.
- **Repo:** signing keys, keystore settings and Google service files are ignored by git, so they can't be committed by accident.
- **README:** the permissions table now also lists **Run at startup** (picking up waiting downloads after a restart), the Privacy section lists every place NaviCtrl connects to and says it's Google-free with no update check, and the Ctrl series list has every app in the series, including CallCtrl, ClockCtrl, ContactCtrl, DateCtrl, LijnCtrl, MailCtrl, RecorderCtrl and TextCtrl.

## v0.8.0 (2026-11-05)

NaviCtrl can now play the music stored on your phone, without a Navidrome server.

### New

- **Music on this phone.** The sign-in screen has a new **Use music on this phone** button. It asks for the Music and audio permission, then shows the songs on your phone in every tab: all songs (sorted by title, date added, artist or album), albums and artists with their artwork, favourites, playlists and search. Songs play straight from the phone's storage, so there is no streaming, transcoding or playback cache for them.
- **Favourites, ratings, play counts and playlists for your own music** are kept in NaviCtrl's private storage on the phone. Recently played and most played albums work from those play counts.
- **Both at once:** you can add "Music on this phone" next to your Navidrome servers and switch between them in Settings, like between servers. The sign-in screen in Settings also offers it.
- Recordings, ringtones and notification sounds are left out of the library; only music files are shown.

### Changed

- The account list and the You page show "On this phone" instead of an address for the music on this phone, and never mark it as unencrypted (nothing leaves the phone).
- **New permission: Music and audio** (`READ_MEDIA_AUDIO`). It's only asked for when you tap **Use music on this phone**; NaviCtrl works with your servers without it.

## Project updates (2026-11-03)

### Project

- **README:** the Ctrl series list now links to ChatCtrl, the Matrix chat app in the series. No app changes, so no new version.
- **README:** the Ctrl series list now includes SumCtrl, the offline calculator, unit converter and work-tools app. No app changes, so no new version.

## v0.7.3 (2026-11-02)

A small privacy fix, plus a security policy and clearer install help for the project.

### Security

- **Artist pictures only come from your own server.** On an artist page, NaviCtrl used the picture link your server passed on from Last.fm or similar sites as is, so the phone could fetch that picture straight from an outside website, which then saw your IP address. NaviCtrl now only uses such a link when it points at your own Navidrome server; otherwise it asks your server for the artist picture, as it already did for every other picture. No password or sign-in was ever sent to those sites.

### Project

- **SECURITY.md** explains how to report a security problem privately, which versions get fixes, and how releases are signed (with the signing certificate fingerprint).
- **Issue forms** for bug reports and feature requests, which ask for the details needed to help (NaviCtrl, Android and Navidrome versions) and remind you to leave out server addresses and passwords. Security problems are pointed to the private report page instead.
- **README:** clearer install steps for people new to GitHub (which file to tap on the Releases page), a new **Is it safe to install?** section with the signing fingerprint and what every permission is for, a **How NaviCtrl is made** section, the full Ctrl series list, and the empty Screenshots section removed.

## v0.7.2 (2026-11-02)

### Fixed

- **Bottom sheets no longer show a stray grey box/outline.** Every sheet that slides up from the bottom (the song, album and playlist menus, Add to playlist, the Songs sort sheet, the favourites playlist menu, choice lists in Settings, and Up next, lyrics and the sleep timer on the now-playing screen) had a thin border that was drawn in the wrong place: a grey rectangle cut across the sheet partway down (often right through the first row) and a loose rounded outline showed near the top of the screen. The border is gone; sheets are now a plain card-coloured panel with rounded top corners, set apart from the screen by the dimmed background behind them, in both light and dark mode. Dialogs keep their thin border, which was always drawn correctly.

## v0.7.1 (2026-10-25)

### Fixed

- **Closing NaviCtrl while music is paused or stopped now really closes it.** Swiping NaviCtrl away from recent apps stopped the player but left the music service connected and running in the background, so the paused notification could stay up for up to 10 minutes and Android could end the service with a crash. Now the service stops, its notification goes away and nothing keeps running. Music that's playing when you swipe the app away still carries on, as before.
- **Playback controls work again after the music service was closed.** If NaviCtrl stayed in memory after its music service stopped (closed from Recents, or ended by the phone to save memory), play, the mini player and the queue could stop responding until the app was restarted. NaviCtrl now notices and reconnects on the next tap, bringing back your last queue paused.
- **No crash when the app lock locks as you leave.** With **Lock again** set to **as soon as you leave**, pressing Back on the first screen or swiping the app away locked the app while its screen was closing, and the lock screen (or its fingerprint prompt) tried to open on that closing screen, which could crash. The app still locks right away and opens locked next time; nothing is shown on the closing screen. The fingerprint prompt is also never asked for on a screen that's closing.
- **Less battery use while nothing plays.** The playback timers (crossfade volume, play counting for scrobbles and the position shown in the app) kept waking up several times a second in the background even when music was paused. They now sleep until music plays again.
- **Your last queue is kept when the service stops right after a pause.** A queue change still waiting to be saved on the phone is now written before the connection closes, so it comes back after a restart.

### Security

- **Fewer ways in for other apps:** two components that libraries add for app-store and developer tools (the profile installer and WorkManager's diagnostics receiver) are removed. NaviCtrl's launcher screen and its music service are now the only parts other apps can reach.

### Changed

- **Telemetry-free, also towards Android:** the player no longer hands playback statistics to Android's built-in media metrics service.

## v0.7.0 (2026-10-25)

### New

- **Play queue sync between devices.** NaviCtrl now saves your queue, the song that's playing and how far into it you are on your Navidrome server (Subsonic's saved play queue, the same one other Subsonic apps use). It saves a moment after the song changes, when you pause, when you leave the app, and every 30 seconds while music plays. Long queues are saved in full on servers that accept it (OpenSubsonic form posts, as Navidrome does); others get the songs around the one that's playing.
- **Continue from another device.** When NaviCtrl starts and the queue on your server is different from the one on the phone, it asks whether to **Continue here** or **Keep this queue**, showing the song, the position, how many songs the queue has, and which device or app saved it and when. Queues saved by NaviCtrl on another phone show that phone's model. Continuing replaces the queue here and starts at the same song and position; keeping it saves your own queue to the server again the next time it changes. NaviCtrl doesn't overwrite the server's queue before you've answered.
- **Resume from another device** in **Settings > Other devices** and in the now-playing menu looks for the queue on your server straight away (also when sync is off), and says so when there's no other queue.
- **Sync the play queue** switch in **Settings > Other devices**, on by default. With it off, nothing is saved on the server and you're not asked at start.
- **Gapless playback** (**Settings > Playback**, on by default): the next song is loaded 30 seconds ahead, so it follows without a pause, also when streaming. Downloaded songs work the same.
- **Crossfade** (**Settings > Playback**): off by default, or 1 to 12 seconds. The end of a song fades out and the next one fades in when it follows on its own. It's skipped when you pick or skip a song, on repeat one, and, while gapless playback is on, between songs that run on from the same album (the next track or the first track of the next disc), so live and concept albums play through untouched.
- **ReplayGain** (**Settings > Playback**): Off (the default), **Track** (every song equally loud) or **Album** (keeps the loudness differences within an album). It uses the ReplayGain values your server reads from the files (OpenSubsonic), falling back to the track value when there's no album value and the other way round. Songs without any values play at their own loudness, unless the server suggests a gain for them. **Preamp** adds −6 to +12 dB, and **Prevent clipping** (on by default) never raises a song so far that its loudest moment would distort. Songs are turned down with the player's volume and, where the phone supports it, raised with Android's loudness enhancer.
- **Equalizer** (**Settings > Playback > Equalizer**): the phone's own equalizer on NaviCtrl's playback, with its built-in presets or **Custom** band levels you set with a slider per band (changes are heard while you drag), and **Bass boost** with a strength slider when the phone has it. It's off until you turn on **Use the equalizer**, and works on the speaker, headphones, Bluetooth and in the car. Phones without an equalizer show a message instead.

### Changed

- **Android permission:** NaviCtrl now asks Android for "change your audio settings" (granted at install, no question), which the equalizer and bass boost need.
- **README**: queue sync, gapless playback, crossfade, ReplayGain and the equalizer are listed under Features.

### Fixed

- **Build fix before release:** the first 0.7.0 build failed because some new texts had apostrophes Android's resource compiler doesn't accept unescaped. They are now escaped (commit f946843); the released 0.7.0 APK already contains this fix, so nothing changes for you.

## v0.6.0 (2026-10-25)

### New

- **Light mode that follows the device theme.** NaviCtrl is now light when your phone is in light mode and dark when it's in dark mode, and switches straight away when you change it (or when your phone's dark theme schedule switches it), without restarting the app. There is no theme setting in the app: it always follows the phone. Wallpaper colours (Material You) are still not used.
- **Signal Light colours:** a warm off-white background (#F5F5F1), white cards and sheets with thin light grey borders, near-black text (#0B0B0D) and grey secondary text (#5C5C64).
- **The violet accent stays the same in both modes** for everything filled with it: the big card on the Songs tab, main buttons, the selected tab in the bottom bar, switches that are on, PIN cells, the play/pause buttons, your profile square and the lock badge, all with dark content on top.
- **Violet text and icons are darker in light mode** (#6A3BDB) so they stay readable on white: the slash in the NAVI/CTRL wordmark, the song that's playing, hearts and download marks in lists, links such as the artist name on album pages, "Active" on Servers and accounts, the selected sort option, the shuffle and repeat buttons when on, text fields while you type, loading spinners and the fingerprint key on the lock screen. In dark mode they keep the lighter violet.

### Changed

- **Every screen works in both modes:** the main tabs, album, artist and playlist pages, search, sign-in, settings, the You page, downloads, servers and accounts, menus, sheets, dialogs, the mini player, the now-playing screen (including Up next, lyrics and the sleep timer) and the app lock screen. The now-playing screen follows the phone's theme too; it doesn't stay dark.
- **Status bar and navigation bar** take the app background in both modes, with light icons in dark mode and dark icons in light mode. The short splash before the app appears matches the mode as well.
- **Light mode details:** switches that are off have a light grey track with a grey knob; progress bars have a light grey track with a near-black fill (violet for downloads in progress); the floating bottom bar and mini player are white at 92 % with a light grey border; empty PIN cells have a grey outline and the keypad keys are white with a thin border; errors use a deeper red (#D93636).

## v0.5.0 (2026-10-25)

NaviCtrl gets the new Ctrl / Signal look shared by the whole Ctrl series: near-black and high contrast, one violet accent, square-cornered controls and two new typefaces. Only the look changes; every screen, button and setting still does what it did before.

### Changed

- **New colours.** NaviCtrl is now always dark: a near-black background (#08080A), dark grey cards (#141417) with a thin border, off-white text and one violet accent (#B48CFF) for whatever matters most on a screen. The status bar and navigation bar take the same background with light icons, edge to edge.
- **Album, artist, playlist and now-playing pages no longer take on the colours of the artwork.** The coloured wash behind the cover and the recoloured buttons are gone; every page uses the same background and accent, so the artwork itself stands out.
- **New fonts.** All text uses Space Grotesk; numbers and small labels (track numbers, times, counts, section headings, the server address) use JetBrains Mono. Both are bundled with the app, so nothing is downloaded. Manrope, the previous font, is removed.
- **New top bar on the main tabs.** Songs, Favourites, Playlists and Search show the **NAVI/CTRL** wordmark on the left and your profile picture on the right: a square with your initials and a small violet square in its corner while you're signed in. Tapping it opens your profile and settings, as before. Below it each tab has a large bold title.
- **Sub screens** (Settings, Downloads, Servers and accounts, album, artist and playlist pages) have a square back button next to the title instead of a plain arrow.
- **Songs tab:** the number of songs in your library is now a large violet card at the top, with the current sort order, and the **Shuffle all** and **Play** buttons inside it. The sort button is a square button in the top bar.
- **Lists** (songs, albums, artists, playlists, the queue, downloads and menus) are plain rows separated by thin lines. Track numbers and times are in the mono font, and the song that's playing is shown in violet.
- **New bottom bar.** The frosted, rounded tab bar is replaced by a floating bar with square corners. The selected tab is a violet block with its icon and name; the other tabs show only their icon (TalkBack still reads their names). Tablets and unfolded foldables keep the side rail, in the new colours.
- **Mini player** is a dark card with square cover art, a violet play/pause button, a next button and a thin violet progress line along the bottom.
- **Now playing** follows the new design: a square button to close it and one for the menu, the album and file format in small capitals at the top, the cover in a large rounded square, the title in bold with the artist and album below it and the heart next to it, a bar-style seek bar with the played part in violet, times in the mono font, and the controls as shuffle, previous, a large violet play/pause button, next and repeat. Lyrics, Up next and the sleep timer are square-cornered buttons at the bottom.
- **Album, artist and playlist pages** show the cover in a rounded square (artist pictures too, instead of a circle) with the title in large bold type below it, the artist in violet, and year, genre, song count and length in small mono capitals. Play, Shuffle and the heart, download and menu buttons are square-cornered.
- **Switches** are a new square-cornered switch: violet with a dark knob when on, grey when off. A whole settings row still turns its switch on and off, and TalkBack still reads it as a switch.
- **Buttons:** the main button on a screen is violet with dark text; secondary buttons are dark grey with a thin border. Pill-shaped buttons and chips are gone.
- **Text fields** (sign in, search, playlist names) are dark grey with a thin border that turns violet while you type.
- **Settings and profile:** grouped settings sit in dark cards with thin lines between rows, under small mono headings. The You page (opened from your profile picture) shows a large violet square with your initials, your name and your server address, and ends with a violet **Support NaviCtrl** button and the line "GOOGLE-FREE · TELEMETRY-FREE · ARM64". The settings row there now has a sliders icon instead of a gear.
- **Menus, sheets and dialogs** use the dark card colour with rounder corners and a thin border.
- **App lock screen** is redesigned: a violet square with a lock, "VKNXWORKS · CTRL" in small capitals, a large title, square PIN cells that fill in violet, and a keypad of square-cornered keys at the bottom of the screen in the mono font. When fingerprint unlock is on, the bottom-left key is the fingerprint key (outlined in violet); once you've typed enough digits for a PIN that doesn't unlock on its own, that key becomes **OK** and the fingerprint button moves below the keypad. The delete key is a plain icon. How the lock works is unchanged.
- **Empty and error screens**, the sign-in screen and the fast-scroll bubble on the Songs tab use the new colours and shapes.
- **README**: the font licence note now names Space Grotesk and JetBrains Mono.

### Removed

- **The Appearance section in Settings is gone**: the **Theme** choice (Follow system, Dark, Light, Pure black) and **Use wallpaper colours**. NaviCtrl is dark only now, and the colours no longer follow your wallpaper. A theme you picked before is simply ignored.

## v0.4.5 (2026-10-23)

### Changed

- **The Genres tab is gone.** The bar at the bottom (and the side rail on tablets and unfolded foldables) now has four tabs: **Songs**, **Favourites**, **Playlists** and **Search**. The genre pages that opened from it, with their Songs and Albums lists and their **Play** and **Shuffle** buttons, are removed too, and so is the shuffle button that started an endless random mix of one genre. Everything else about your library is unchanged: **Shuffle all** on the Songs tab still plays your whole library in random order, and a song's or album's genre still shows where it did before (for example on album pages and in the now-playing details on your lock screen and Bluetooth devices).
- **README**: the Genres tab is no longer listed under Features.

## v0.4.4 (2026-10-22)

### Fixed

- **NaviCtrl asks for notifications as soon as you're in.** On Android 13 and newer an app has to ask before it can show notifications. NaviCtrl only asked once music had already started, so the question popped up in the middle of playback, and if it was dismissed or didn't show, the music controls in the notification and on the lock screen were silently missing. It now asks once, the first time your library is shown after you've signed in (and unlocked the app, if the app lock is on). If you've already answered, it doesn't ask again.
- **No more repeated questions when music starts.** NaviCtrl used to ask again every time music started while notifications weren't allowed. Android's question now only comes up automatically once; after that you can turn notifications on from **Settings**.

### New

- **Notifications in Settings.** A new **Notifications** section shows whether NaviCtrl may show notifications. When they're blocked it says **Notifications are blocked** with an **Allow** button: it shows Android's question again if Android still allows that, or otherwise opens NaviCtrl's notification settings so you can turn them on there. It updates as soon as you come back from Android's settings. When they're allowed, tapping the row opens those settings too.

## v0.4.3 (2026-10-21)

### Fixed

- **Unlock with fingerprint now really works.** NaviCtrl was missing the Android permission that lets an app use the fingerprint sensor, so the phone always answered "no fingerprint available": **Unlock with fingerprint** under **Settings > Privacy and security** stayed greyed out with "Set up a fingerprint on this phone first", even with a fingerprint set up, and the lock screen only ever asked for the PIN. The permission is now included, so you can turn on fingerprint unlock after updating. It still only accepts strong biometrics such as the fingerprint sensor, and NaviCtrl never sees your fingerprint itself; Android only tells it whether the match succeeded.

## v0.4.2 (2026-10-21)

### Fixed

- **Fingerprint unlock works again after you leave the app.** Once you'd left NaviCtrl with the back gesture or by swiping home, the lock screen no longer asked for your fingerprint when you came back, and tapping **Use fingerprint** did nothing either; only the PIN worked until the app was force-closed. The fingerprint prompt was asked for while the app was still coming back on screen, and the phone dropped it without answering, which left NaviCtrl waiting for an answer that never came. Now the prompt only opens once NaviCtrl is fully back on screen, a prompt that never answered can't block the next one, and the lock screen offers your fingerprint again every time you come back to it, not just the first time after it locked. The same applies to the one-time check with your phone's screen lock for people updating from the old app lock.

## v0.4.1 (2026-10-21)

### Security

- **Nothing stays usable on top of the lock screen any more.** When NaviCtrl locked while a menu, dialog or sheet was open (for example the actions sheet of a song or album, the add-to-playlist sheet or the sort sheet on Songs), that window stayed on top of the lock screen and could still be read and tapped, and a hardware keyboard could still move to buttons hidden behind the lock. While NaviCtrl is locked, its screens are now closed completely instead of only covered. After you unlock you're back on the same screen and tab as before (an open menu or sheet is closed). Music that's playing carries on as before, with its controls in the notification, on the lock screen and on Bluetooth devices.
- **Server passwords are always sealed with your app PIN when the app lock is on.** If NaviCtrl was closed at exactly the wrong moment while you turned the app lock on, your saved passwords could stay protected by the phone's keystore only, without the extra encryption from your PIN. NaviCtrl now checks this every time you unlock (and at start-up when it's unlocked) and seals any password that isn't sealed yet.

### Privacy

- **No hidden dependency list in the app file any more**: the build tools used to tuck an encrypted list of the libraries NaviCtrl is built with into the APK's signature block. Only Google can read it, so nobody checking the app could see what it holds. It's now left out, the same as in the other Ctrl apps. Nothing changes in how the app works.

## v0.4.0 (2026-10-21)

Genre pages are here, the last item that was still planned.

### New

- **Genres tab** in the bar at the bottom (and in the side rail on tablets and unfolded foldables), between Playlists and Search. It lists every genre on your Navidrome server from A to Z, with how many songs and albums each has. Pull down to refresh the list
- **Genre pages**: tap a genre to open it, with **Songs** and **Albums** at the top. Songs has **Play** and **Shuffle** buttons and loads more as you scroll; Albums shows the genre's albums in your usual grid or list
- **Shuffle a genre straight from the list** with the shuffle button next to it. It starts an endless random mix of that genre, the same as **Shuffle** on the genre page
- Songs without a genre (which Navidrome groups as "<Empty>") don't show up as a genre you can open

### Changed

- **All text is now in one place, ready for translations.** Counts such as "1 song" and "5 songs", "1 minute" and "5 minutes", or "Added to queue" and "Added 3 songs to queue" are now worded properly for one and for more than one everywhere
- **README**: the Genres tab is listed under Features, and the **Planned** section is gone because nothing is left in it. The App lock and privacy setting paths now start at your profile picture (tap your profile picture › Settings › Privacy and security)

## v0.3.3 (2026-10-21)

### Security

- **With the app lock on, the recent-apps preview always stays blank**, whatever **Hide in recent apps** is set to, so it never shows what's behind the lock. Android takes that preview the moment you leave the app, before it locks.
- **README**: the App lock and privacy text now reads the same as in the other Ctrl apps.

## v0.3.2 (2026-10-21)

### Changed

- **Hide in screenshots** and **Hide in recent apps** are now two separate switches in Settings › Privacy and security, both off by default. **Hide in screenshots** makes screenshots and screen recordings show a blank screen; **Hide in recent apps** makes the recent apps view show a blank card instead of the app, while screenshots still work. If you had turned on the single "Hide in screenshots and recent apps" switch, both are on after the update.
- **The recent apps view no longer goes blank just because the app lock is on.** It follows **Hide in recent apps** now, so turn that on if you want a blank card there.
- **Always hidden, whatever the switches say**: the lock screen, choosing or changing the app PIN, the sign-in screen, because a password or PIN is typed or shown there.
- **README**: describes the two switches.

## v0.3.1 (2026-10-21)

### Changed

- **Hide in screenshots and recent apps** now reads "Screenshots, screen recordings and the recent apps view show a blank screen", the same as in the other Ctrl apps.
- **README**: the App lock section now uses the same wording as the other Ctrl apps.

### Security

- **The lock and PIN screens can't be captured.** While NaviCtrl shows the lock screen, or a screen for choosing, changing or turning off the app PIN, screenshots and screen recordings come out black, whatever "Hide in screenshots and recent apps" is set to.
- **No snapshot at start-up**: the recent-apps view shows a blank card from the moment NaviCtrl opens, before its settings are read.

## v0.3.0 (2026-10-21)

NaviCtrl gets the app lock every Ctrl app shares.

### New

- **App lock** (**Settings > Privacy and security > App lock**, off until you turn it on). NaviCtrl asks for its own app PIN when you open it: at least 6 digits, separate from your phone's screen lock, and very obvious ones (like 111111 or 123456) are refused. Turning the lock off asks for the PIN too.
- **Unlock with fingerprint** (optional). Only strong biometrics such as the fingerprint sensor count, not face unlock that only uses the camera. The PIN is still needed after the phone restarts and at least every 72 hours.
- **Lock again**: as soon as you leave the app (the default), after 1, 5 or 15 minutes or 1 hour away, or only when the app restarts. Opening a link from the About section or answering the notification permission question doesn't count as leaving, unless you stay away for more than 5 minutes.
- **Change app PIN** in the same place; it asks for your current PIN first.
- **Music and downloads keep going while NaviCtrl is locked.** Music that's playing (or paused with a queue) and downloads in progress carry on, with their controls in the notification, on the lock screen and on Bluetooth devices, and plays are still reported to your server. Starting something new needs an unlock. Downloads that were still waiting when the phone restarted start once you unlock. Your queue is kept as it was.
- **Hide in screenshots and recent apps** (**Settings > Privacy and security**): screenshots and screen recordings of NaviCtrl come out black, and the recent-apps screen shows a blank card. Off by default. With the app lock on, the recent-apps screen always shows a blank card.

### Changed

- **Settings are in the same order as in the other Ctrl apps**: Appearance and Privacy and security first, then Streaming, Downloads and Listening history.

### Security

- **Your server password is encrypted with your PIN while NaviCtrl is locked.** The key that decrypts it only exists in memory while the app is unlocked and is wiped the moment it locks. A locked app, or a copy of its files, can't use your password. Only music or downloads that are already running keep the sign-in they need in memory, until they stop.
- **The PIN can't be guessed off the phone.** It's never stored. It's stretched with 600,000 rounds of PBKDF2 and mixed with a secret held in the phone's secure hardware (StrongBox where available), so copying NaviCtrl's files to a computer isn't enough to try PINs there.
- **Wrong PINs lock the screen**: after 5 tries you wait 30 seconds, doubling with every further 5 wrong tries up to 15 minutes. Restarting the app or the phone, or changing the clock, doesn't reset the wait.
- **Fingerprint unlock can't be faked**: the fingerprint has to unlock a key in the phone's secure hardware, so a tampered "success" on a rooted phone doesn't open the app. Adding a new fingerprint turns fingerprint unlock off until you turn it on again with your PIN.
- **Forgot your PIN?** The lock screen offers to remove every account, and delete the downloads, played music, saved queues and artwork kept on the phone. Because the PIN encrypts your passwords, there's no way around it. Nothing changes on your Navidrome server.
- **The sign-in screen stays out of screenshots** and screen recordings now, not only out of the recent-apps preview, because the password can be shown there.

### Fixed

- **README**: the Ctrl series table lists AdCtrl and MemoCtrl, and the README now describes the app lock and how your password is kept.

## v0.2.5 (2026-10-20)

### Changed

- **64-bit ARM phones only.** NaviCtrl now ships only the 64-bit ARM (arm64-v8a) build that nearly every phone from the last several years uses, like every Ctrl app, so the download is smaller. Old 32-bit phones and x86 devices (emulators, some Chromebooks) are no longer supported.
- **README**: Requirements say 64-bit ARM phones only

## v0.2.4 (2026-10-20)

NaviCtrl is now Google-free and de-Googled.

### Changed

- **Media controls trust the apps built into your phone.** NaviCtrl's music player used to accept controls only from a fixed list of named apps (system UI, Bluetooth and car mode), some of them Google's. It now accepts the apps that are part of your phone's system, plus apps Android itself marks as trusted for media control, so the notification, lock screen, Bluetooth and car controls keep working as before, also on de-Googled phones whose built-in apps have different names. NaviCtrl checks which app is really asking before it trusts it, and apps you installed yourself still can't control playback.
- **README**: the Google-free line now reads "Google-free and de-Googled": no Google Play services, Firebase or other Google services, the app never contacts Google, and it works the same on de-Googled phones (GrapheneOS, CalyxOS, /e/OS, LineageOS without Google apps). It's the only place Google is mentioned in the README.

### Privacy

- **No mention of Google left in the app's own code.** NaviCtrl has no Google Play services, Firebase or other Google services and never contacts Google: it only talks to your Navidrome server. The music player no longer names Google apps or uses Google's helper code directly (it uses an AndroidX helper instead), and a technical note in its setup file is gone. It works the same on de-Googled phones such as GrapheneOS, CalyxOS, /e/OS and LineageOS without Google apps.

## v0.2.3 (2026-10-20)

### Privacy

- **No more usage stats: NaviCtrl is now telemetry-free.** The optional anonymous usage stats are gone completely, along with the Usage stats setting and the one-time question. The app no longer contacts VKNXWorks' stats server at all, and the settings it used are deleted from your phone when you update. NaviCtrl now only talks to your own Navidrome server.

### Changed

- **README**: a Screenshots section (screenshots are coming soon), and a link preview image (`.github/assets/social-preview.png`) with NaviCtrl's icon, name and summary, for when the repo is shared on Reddit, Discord or elsewhere
- **README**: a Privacy section and a Telemetry-free badge replace the Usage stats section, and the link preview image has a Telemetry-free tag

## v0.2.2 (2026-10-19)

### Changed

- **One About section in every Ctrl app**: Support, Report a bug, Source code and Licence (GPL-3.0), with the version underneath, at the bottom of the You page (tap your profile picture). Every Ctrl app now has the same four rows, in the same order, with the same wording and icons
- **Report a bug** is a new row in **About** that opens NaviCtrl's GitHub issues page, where you can report a problem or suggest an idea
- **Source code and issues** is now called **Source code**, since reporting bugs has its own row. It still opens NaviCtrl's GitHub repository

## v0.2.1 (2026-10-19)

### New

- **Support NaviCtrl** under **About** at the bottom of the You page (tap your profile picture) opens VKNXWorks' GitHub Sponsors page in your browser. NaviCtrl is free and open source, and sponsoring helps keep it that way
- **Source code and issues** in the same **About** section opens NaviCtrl's GitHub repository, where you can read the code or report a bug
- **Licence** in the same section opens the GNU General Public License v3.0 that NaviCtrl is released under

## v0.2.0 (2026-10-19)

Playlists get their own tab, internet radio is gone, and the app is now completely Google-free.

### New

- **Playlists tab.** A new tab in the bar at the bottom (and in the side rail on tablets and unfolded foldables) lists your playlists and the ones other people share on your Navidrome server. Each shows its cover, how many songs it has, how long it is, and who made it if it isn't yours. Pull down to refresh the list
- **Make a playlist** with the **+** button at the top of the Playlists tab. It starts empty; add songs with **Add to playlist** in any song's or album's menu
- **Playlist menu.** Tap the three dots next to a playlist, or press and hold it, to play or shuffle it without opening it, play it next, or add it to the queue. On your own playlists the menu also has **Rename** and **Delete playlist**; deleting asks first, and the songs stay in your library
- **Playlist page.** Tap a playlist to see its songs with **Play** and **Shuffle** buttons. Tap any song to play the playlist from that song. The download button saves the whole playlist for offline listening, and the **...** menu has Play next, Add to queue, and for your own playlists Rename and Delete
- **Remove a song from a playlist** with **Remove from this playlist** in the song's menu on your own playlists

### Changed

- The bar at the bottom now has four tabs: Songs, Favourites, Playlists and Search
- **Add to playlist** now only lists playlists you can add songs to: your own, but not smart playlists or playlists synced from a file, which Navidrome doesn't let you change. Before, picking one of those failed with an error. The list is also fetched again each time you open it, so song counts are up to date
- Playlists you make, rename, delete or add songs to anywhere in the app update straight away on the Playlists tab and on an open playlist page
- Smart playlists and playlists synced from a file no longer offer **Remove from this playlist**, which Navidrome doesn't allow on them. You can still rename or delete your own
- Removing a song from a playlist first checks the playlist on your server, so the right song is removed even if the playlist was changed in another app after you opened it
- **README brought up to date.** Playlists are now listed as a feature, and the **Planned** section only lists genre pages. The README also mentions starting a mix of similar songs from any song, **Add to playlist**, and **Scan for new music** for admin accounts; **Getting started** explains that downloads, settings and accounts are behind your profile picture, and the tips name the **Start a mix from this song** menu item and the **Report what I play** setting for scrobbling
- Tidied the changelog: removed a doubled bullet marker in the 0.1.13 notes

### Removed

- **Internet radio is gone.** NaviCtrl is a player for the music on your Navidrome server, and radio stations could only be reached from the old Library screen, which hasn't been in the app since 0.1.10. Everything for radio has been removed: the station list, playing a station's stream, the "Internet radio" and "Live" display in the player, and the exception that let a radio stream use plain HTTP. NaviCtrl now only ever connects to your own Navidrome server (plus the opt-in usage stats)
- The opt-in usage stats no longer have "internet radio" as a way to start music

### Privacy

- **Google-free.** NaviCtrl no longer asks Google Play services for an emoji font when it starts (a lookup built into an Android library it uses). Emoji now use your phone's own font. The app has no Google services, Firebase or Google network connections, and the README says so under **Requirements**

## v0.1.13 (2026-10-19)

Fourth security and privacy review of the new usage stats.

### Security

- **Switching usage stats off takes effect straight away.** Turning off **Settings > Usage stats** now also throws away any stats still waiting to be sent and stops one being sent at that moment, and switching them on again starts a new random session. Before, a stat that was already on its way could still go out

### Changed

- The README's **Usage stats** section now lists everything the app's release notes and Settings already said is shared: when you start music it also says whether it was shuffled. It also says that, apart from those opt-in stats, NaviCtrl only talks to your own Navidrome server, plus a radio station's stream address when you play it
- **All Ctrl apps in the README.** The README's Ctrl series table now lists all 14 apps. New in the table: [ArrCtrl](https://github.com/VoidedKN0X/ArrCtrl) (Radarr, Sonarr, Prowlarr, Bazarr, qBittorrent, autobrr and Cleanuparr in one app), [PaperCtrl](https://github.com/VoidedKN0X/PaperCtrl) (scan and browse documents on Paperless-ngx), [ProxCtrl](https://github.com/VoidedKN0X/ProxCtrl) (manage your Proxmox VE server) and [TrueCtrl](https://github.com/VoidedKN0X/TrueCtrl) (monitor and manage your TrueNAS server). Every app in the table is also in the [CtrlF-Droid repo](https://voidedkn0x.github.io/CtrlF-Droid/) for automatic updates.

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
