# Wavelength

A native Android music streaming app built with Kotlin and Jetpack Compose. Wavelength streams
from [JioSaavn](https://github.com/sumitkolhe/jiosaavn-api) (via your own self-hosted deployment
of that unofficial API) plus songs already on your device — all through one player. It's a
personal-use app: no login, no backend server, single user.

> Wavelength originally streamed from Jamendo, a royalty-free/Creative-Commons catalog. That
> catalog has essentially no mainstream regional film music (Tamil, Hindi, etc.), so Jamendo has
> been removed entirely in favor of JioSaavn, which actually carries that catalog.

## Features

- **Home** — featured tracks, language shortcuts (Tamil, Hindi, Telugu, English, ...), a
  recently-played row
- **Search** — live search across JioSaavn tracks
- **My Device** (Library tab) — songs scanned from local storage via MediaStore, cached in Room,
  with a manual rescan action
- **Now Playing** — full-screen player with seek bar, play/pause/skip/shuffle/repeat, favorite toggle
- **Mini Player** — persistent bar above the bottom navigation, expands to Now Playing on tap
- **Library** — Favorites, Recently Played, and My Device tabs
- Background playback via Media3 `MediaSessionService`, with lock-screen/notification controls,
  proper audio focus handling, and queue/shuffle/repeat management — the same pipeline plays
  JioSaavn streams and local file URIs alike
- No-internet and empty/error states with retry, throughout

## Tech stack

- Kotlin + Jetpack Compose (Material 3), dark theme
- Media3 ExoPlayer + MediaSessionService for playback
- Retrofit + Moshi for networking
- Room for favorites / recently played / local song cache
- Coil for image loading
- Hilt for dependency injection
- MVVM: `data/`, `ui/`, `playback/`, `di/`

## Setup

Wavelength ships pointed at a working JioSaavn API deployment by default (a Cloudflare Workers
instance of [sumitkolhe/jiosaavn-api](https://github.com/sumitkolhe/jiosaavn-api)), so it works out
of the box. To use your own instance instead:

1. Deploy [sumitkolhe/jiosaavn-api](https://github.com/sumitkolhe/jiosaavn-api) somewhere — Cloudflare
   Workers is this project's native deploy target (its own "Deploy with Cloudflare Workers" button);
   Vercel's one-click deploy for this repo is currently broken (stale `vercel.json`/entrypoint
   mismatch), so prefer Cloudflare or Docker.
2. Copy `local.properties.example` to `local.properties` (git-ignored) and set your Android SDK
   path and your deployment's base URL:

   ```properties
   sdk.dir=/path/to/your/Android/sdk
   JIOSAAVN_BASE_URL=https://your-worker-name.your-subdomain.workers.dev/api/
   ```

3. Open the project in Android Studio (or run `./gradlew assembleDebug` from the command line).
   The base URL is exposed to the app via `BuildConfig.JIOSAAVN_BASE_URL` and defaults (see
   `app/build.gradle.kts`) to the project's own deployment if you don't override it locally.

### Local device songs

On first opening the **My Device** tab (under Library), the app requests permission to read audio
on the device (`READ_MEDIA_AUDIO` on Android 13+, `READ_EXTERNAL_STORAGE` below that), then scans
`MediaStore` once and caches the result in Room. Use the refresh icon in the top bar to rescan
after adding new files.

### Building an APK without Android Studio

A GitHub Actions workflow (`.github/workflows/build-apk.yml`) builds a debug APK on every push.
The easiest way to get it: open the repo's **Releases** tab — every push to this branch republishes
the **"Latest build"** release with `app-debug.apk` attached directly (no zip, just download and
install). It's also uploaded as a build artifact under Actions → latest run → Artifacts, if you
want a specific commit's build. CI builds against the project's default JioSaavn deployment
unless you add a repository secret named `JIOSAAVN_BASE_URL` pointing at your own instance.

> This project's Gradle build depends on the Android SDK and Google's Maven repository
> (`dl.google.com`) to resolve the Android Gradle Plugin and AndroidX libraries. If you're building
> in a network-restricted environment, make sure that host is reachable, or build via the CI
> workflow above, which runs on a standard GitHub-hosted runner with full internet access.

## Architecture

```
app/src/main/java/com/wavelength/music/
├── data/
│   ├── model/         # Track (+ TrackSource), JioSaavnSong domain models
│   ├── remote/
│   │   └── jiosaavn/    # JioSaavnApiService, DTOs, DTO -> domain mappers
│   ├── local/           # Room entities and DAOs (favorites, recently played, local songs)
│   └── repository/      # JioSaavnRepository, LocalSongRepository, and MusicRepository —
│                         # the facade the UI actually talks to
├── playback/             # PlaybackService (MediaSessionService), PlayerController (MediaController wrapper)
├── di/                   # Hilt modules (network, database)
└── ui/
    ├── theme/            # Compose Material 3 dark theme
    ├── navigation/       # NavHost, routes, bottom nav + mini player scaffold
    ├── components/       # Shared composables (TrackRow, MiniPlayerBar, loading/error/empty states, ...)
    ├── home/             # Home + Genre (language) screens
    ├── search/           # Search screen
    ├── nowplaying/        # Now Playing screen + shared PlayerViewModel
    └── library/           # Favorites / Recently Played / My Device tabs
```

`MusicRepository` is a thin facade: it delegates browsing/search to `JioSaavnRepository`, device
files to `LocalSongRepository`, and owns the source-agnostic favorites/recently-played tables
itself (any track — JioSaavn or local — can be favorited or shows up in recently-played, since
`Track.source` travels with it end to end).

Playback flows through a single `PlayerController` (a Hilt singleton) that wraps a Media3
`MediaController` connected to `PlaybackService`. It's fed plain `MediaItem`s built from
`Track.audioUrl` regardless of source — an HTTPS stream URL or a `content://` device URI both work
without any source-specific playback code. Every screen that can start playback injects
`PlayerController` directly; the Now Playing screen and mini player share one `PlayerViewModel`
instance so their state always stays in sync.

## Screenshots

_Add screenshots here once you've run the app — e.g. `docs/screenshots/home.png`,
`now_playing.png`, `library.png`._
