# Changelog

What changed in each NaviCtrl release. Every release on the [Releases page](https://github.com/VoidedKN0X/NaviCtrl/releases) also lists the exact changes in that build.

To publish a new version: bump `versionCode` and `versionName` in `app/build.gradle.kts`, add a section here, and push to `main`. Pushes without a version bump only build; they don't make a release.

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
