# NaviCtrl

## Dependency verification

Gradle checks every library and plugin download against the sha256 checksums in `gradle/verification-metadata.xml` (checksums only, no PGP). When a dependency, plugin or the Gradle version changes, start the "Dependency verification metadata" workflow (`gh api -X POST repos/VoidedKN0X/NaviCtrl/actions/workflows/verification-metadata.yml/dispatches -f ref=main`), take the file from the job log (between the BEGIN/END markers: `base64 -d | gunzip`), check the diff (only Maven Central and Google Maven artifacts the build uses) and commit it in the same push. Never turn verification off or make it lenient to get a build through.

## Encryption at rest

- Sealed with AES-256-GCM (`data/crypto/SealedFile`, marker `NAVICTRL-REST1`) under the data key (`DataKeyStore`: 32 random bytes in `filesDir/data.key`, wrapped by the Keystore key `navictrl_data_at_rest` through `KeystoreWrapper`/`KeystoreAes`; no user authentication, so downloads and music work in the background): `filesDir/downloads.json` (through `SealedStore`), `filesDir/local_library.json` (`LocalServer`, still written with `AtomicFile`), `filesDir/queue.json` (`MusicController`) and `cacheDir/lyrics/**.json` (`LyricsRepository`). `KeystoreCrypto` (the password key) and the app-lock vault are separate and untouched.
- Not sealed, on purpose: downloaded songs and `.part` files in `filesDir/music/`, Media3's `SimpleCache` in `cacheDir/media` and its database (the player reads them directly), Coil's cache, DataStore settings and accounts, WorkManager's database.
- Readers must accept both sealed and ordinary files (`SealedFile.isSealed`). Without a data key the app reads and writes ordinary files as before.
- Never delete or overwrite a file of the user's own data that can't be opened (`downloads.json`, `local_library.json`), never replace an existing `data.key`, and never create a Keystore key on the decrypt path (`KeystoreAes.existing`). An ordinary file only becomes a sealed one through `SealedFile.sealInPlace` (temp file, sync, read back, compare, atomic rename). When one of those two can't be opened nothing is written to it and its `unreadable` flow goes on (`DownloadRepository.unreadable`, `LocalServer.unreadable`; `AppContainer.dataUnreadable` is both). The app is NOT blocked by that (browsing, streaming and playback don't need these files): the "Can't open your saved data" message (`UnreadableData` in `ui/nav/AppRoot.kt`; "Try again", and "Start empty", which renames the files to `*.unreadable`) only shows where the data is used: the Downloads page and "Downloaded only" on the Songs tab for `downloads.json`, and the Favourites and Playlists tabs of the "Music on this phone" account for `local_library.json` (`LocalLibraryData` in `AppRoot.kt`, which also drops what those pages loaded meanwhile). The queue and lyrics are caches: one that can't be opened counts as missing and is replaced.
- While the download list is unreadable its entries are unknown (the list in memory is empty), and that must never be read as "nothing downloaded, so clean up": `enqueue` (shows a notice) and `remove` do nothing, `perform` (the worker) returns `Outcome.RETRY` before it looks anything up or touches a file, and playback simply streams (`completedFile` is null). Never add code that removes files because they aren't in the list. `LocalServer.change` does nothing while its file is unreadable, so favourites, ratings, play counts and playlist edits made then are not kept.
- All of this reads the disk and may ask the Keystore: never on the main thread (`MusicController` runs on the main thread, so its queue file work stays inside `withContext(Dispatchers.IO)`; `LocalServer` runs on the request threads).
- `SealedFileTest` covers the format, the migration step, the key rules and `SealedStore` (`./gradlew :app:testDebugUnitTest`; CI doesn't run unit tests).

## Releasing

- A release is published only when `versionName` in `app/build.gradle.kts` is bumped. Bump `versionCode` by 1 at the same time.
- Tags are `vX.Y.Z` and releases are named "NaviCtrl X.Y.Z". Never add a build number (`-build.N`, "(build N)").
- Use the same version for every change that ships together; README, licence and CI changes don't need a bump.

## Changelog

Every new version needs a detailed entry at the top of `CHANGELOG.md`, under a heading for that version (match the existing headings). CI refuses to release without one, and the entry becomes the release notes on GitHub, in Obtainium and in the CtrlF-Droid repo.

- Write for people using the app, not for developers: what's new, what changed, what was fixed.
- Cover every user-visible change in the version, one bullet each, with enough detail to understand it without opening the app.
- Group with `### New`, `### Changed`, `### Fixed` and `### Security` when there's more than a couple of items.

## Build and signing (CI)

`.github/workflows/build.yml` has four jobs: `version` (is this a release?), `build` (Gradle, **no secrets**, `UNSIGNED_RELEASE=true` gives `app-release-unsigned.apk`; runs every check), `sign` (no checkout, no Gradle: `zipalign` + `apksigner` with the release key, then checks the signer against `EXPECTED_CERT_SHA256` and that the APK's contents didn't change) and `release` (attestation, release notes, publish).

- Never give the `build` job a secret, and never run Gradle or check out the repo in `sign`. New build checks (tests, lint, APK checks) go in `build`.
- `EXPECTED_CERT_SHA256` in `build.yml` must match the fingerprint in `SECURITY.md` and `README.md`; the build fails if it doesn't.
- No Gradle cache in any workflow (`cache-disabled: true`): a cache saved by a failed run used to break every later build.
- `settings.gradle.kts` lists only `google()` and `mavenCentral()` for plugins, not `gradlePluginPortal()`: the portal serves different files for the Kotlin plugin markers, which made verification fail whenever Maven Central had a hiccup. Don't add it back unfiltered (see the comment in that file).

## Build toolchain

- AGP 9.5.1, Gradle 9.9.2, Kotlin 2.4.21, Compose BOM 2026.09.00, `compileSdk` 37 (Compose 1.12 needs it), `minSdk` 35, `targetSdk` 36. All versions are in `gradle/libs.versions.toml`.
- AGP 9 has built-in Kotlin: there is no `org.jetbrains.kotlin.android` plugin any more. The Kotlin version is pinned by the `kotlin-gradle-plugin` classpath entry in the root `build.gradle.kts`; the Compose and serialization compiler plugins use the same `kotlin` version. Don't re-add the Kotlin Android plugin.
- AGP 9's R8 is stricter (`-keep class X` without a member block no longer keeps the constructor; resources are shrunk together with code). Keep rules in `proguard-rules.pro` must name the members they need, and resources must be referenced through `R`, never by name.
- Any change here needs the dependency verification procedure (see "Dependency verification").
