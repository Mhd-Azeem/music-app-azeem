# Wavelength

A native Android music streaming app built with Kotlin and Jetpack Compose. Wavelength streams
from three sources — the [Jamendo](https://www.jamendo.com/) catalog of free, legal,
royalty-free music, an optional self-hosted [JioSaavn](https://github.com/sumitkolhe/jiosaavn-api)
deployment, and songs already on your device — all through one player. It's a personal-use app:
no login, no backend server, single user.

## Features

- **Home** — featured tracks, genre/mood shortcuts, a recently-played row
- **Search** — live search across Jamendo tracks/artists/albums, merged with JioSaavn track
  results when a deployment is configured; each result is tagged with its source
- **My Device** (Library tab) — songs scanned from local storage via MediaStore, cached in Room,
  with a manual rescan action
- **Now Playing** — full-screen player with seek bar, play/pause/skip/shuffle/repeat, favorite toggle
- **Mini Player** — persistent bar above the bottom navigation, expands to Now Playing on tap
- **Library** — Favorites, Recently Played, and My Device tabs
- **Artist / Album detail** — track listing with play-all
- Background playback via Media3 `MediaSessionService`, with lock-screen/notification controls,
  proper audio focus handling, and queue/shuffle/repeat management — the same pipeline plays
  Jamendo streams, JioSaavn streams, and local file URIs alike
- No-internet and empty/error states with retry, throughout; a failing JioSaavn deployment never
  breaks Jamendo search — it's an unofficial API, treated as best-effort

## Tech stack

- Kotlin + Jetpack Compose (Material 3), dark theme
- Media3 ExoPlayer + MediaSessionService for playback
- Retrofit + Moshi for networking
- Room for favorites / recently played
- Coil for image loading
- Hilt for dependency injection
- MVVM: `data/`, `ui/`, `playback/`, `di/`

## Setup

1. Get a free client ID from the [Jamendo developer portal](https://devportal.jamendo.com/).
2. Copy `local.properties.example` to `local.properties` (git-ignored) and fill in your Android
   SDK path and client ID:

   ```properties
   sdk.dir=/path/to/your/Android/sdk
   JAMENDO_CLIENT_ID=your_jamendo_client_id

   # Optional — see below
   JIOSAAVN_BASE_URL=https://your-deployment.vercel.app/api/
   ```

3. Open the project in Android Studio (or run `./gradlew assembleDebug` from the command line).
   The client ID is exposed to the app via `BuildConfig.JAMENDO_CLIENT_ID` — it is never
   hardcoded in committed source.

### JioSaavn (optional second source)

JioSaavn search is powered by [sumitkolhe/jiosaavn-api](https://github.com/sumitkolhe/jiosaavn-api),
an unofficial API you self-host (e.g. a free Vercel deployment). Set `JIOSAAVN_BASE_URL` in
`local.properties` to your deployment's `/api/` URL to enable it. Leave it unset and JioSaavn
results are simply absent from search — Jamendo keeps working normally either way, since it's an
unofficial API that can go down or change shape without notice.

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
want a specific commit's build. By default it uses the free demo client ID; add a repository
secret named `JAMENDO_CLIENT_ID` to build with your own.

> This project's Gradle build depends on the Android SDK and Google's Maven repository
> (`dl.google.com`) to resolve the Android Gradle Plugin and AndroidX libraries. If you're building
> in a network-restricted environment, make sure that host is reachable, or build via the CI
> workflow above, which runs on a standard GitHub-hosted runner with full internet access.

## Architecture

```
app/src/main/java/com/wavelength/music/
├── data/
│   ├── model/         # Track (+ TrackSource), Artist, Album, JioSaavnSong domain models
│   ├── remote/         # Jamendo Retrofit service/DTOs; remote/jiosaavn/ for JioSaavn
│   ├── local/           # Room entities and DAOs (favorites, recently played, local songs)
│   └── repository/      # JamendoRepository, JioSaavnRepository, LocalSongRepository,
│                         # and MusicRepository — the facade the UI actually talks to
├── playback/             # PlaybackService (MediaSessionService), PlayerController (MediaController wrapper)
├── di/                   # Hilt modules (network — qualified Jamendo/JioSaavn Retrofit — and database)
└── ui/
    ├── theme/            # Compose Material 3 dark theme
    ├── navigation/       # NavHost, routes, bottom nav + mini player scaffold
    ├── components/       # Shared composables (TrackRow, MiniPlayerBar, loading/error/empty states, ...)
    ├── home/             # Home + Genre screens
    ├── search/           # Search screen (Jamendo + JioSaavn merged)
    ├── nowplaying/        # Now Playing screen + shared PlayerViewModel
    ├── library/           # Favorites / Recently Played / My Device tabs
    ├── artist/             # Artist detail
    └── album/              # Album detail
```

`MusicRepository` is a thin facade: it delegates browsing/search to `JamendoRepository` and
`JioSaavnRepository`, device files to `LocalSongRepository`, and owns the source-agnostic
favorites/recently-played tables itself (any track — Jamendo, JioSaavn, or local — can be
favorited or shows up in recently-played, since `Track.source` travels with it end to end).

Playback flows through a single `PlayerController` (a Hilt singleton) that wraps a Media3
`MediaController` connected to `PlaybackService`. It's fed plain `MediaItem`s built from
`Track.audioUrl` regardless of source — an HTTPS stream URL or a `content://` device URI both work
without any source-specific playback code. Every screen that can start playback injects
`PlayerController` directly; the Now Playing screen and mini player share one `PlayerViewModel`
instance so their state always stays in sync.

## Screenshots

_Add screenshots here once you've run the app — e.g. `docs/screenshots/home.png`,
`now_playing.png`, `library.png`._
