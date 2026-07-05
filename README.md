# Wavelength

A native Android music streaming app built with Kotlin and Jetpack Compose. Wavelength streams
from the [Jamendo](https://www.jamendo.com/) catalog of free, legal, royalty-free music. It's a
personal-use app: no login, no backend server, single user.

## Features

- **Home** — featured tracks, genre/mood shortcuts, a recently-played row
- **Search** — live search across tracks, artists, and albums
- **Now Playing** — full-screen player with seek bar, play/pause/skip/shuffle/repeat, favorite toggle
- **Mini Player** — persistent bar above the bottom navigation, expands to Now Playing on tap
- **Library** — Favorites and Recently Played tabs
- **Artist / Album detail** — track listing with play-all
- Background playback via Media3 `MediaSessionService`, with lock-screen/notification controls,
  proper audio focus handling, and queue/shuffle/repeat management
- No-internet and empty/error states with retry, throughout

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
   ```

3. Open the project in Android Studio (or run `./gradlew assembleDebug` from the command line).
   The client ID is exposed to the app via `BuildConfig.JAMENDO_CLIENT_ID` — it is never
   hardcoded in committed source.

### Building an APK without Android Studio

A GitHub Actions workflow (`.github/workflows/build-apk.yml`) builds a debug APK on every push and
uploads it as a downloadable build artifact (Actions tab → latest run → Artifacts →
`wavelength-debug-apk`). By default it uses the free demo client ID; add a repository secret named
`JAMENDO_CLIENT_ID` to build with your own.

> This project's Gradle build depends on the Android SDK and Google's Maven repository
> (`dl.google.com`) to resolve the Android Gradle Plugin and AndroidX libraries. If you're building
> in a network-restricted environment, make sure that host is reachable, or build via the CI
> workflow above, which runs on a standard GitHub-hosted runner with full internet access.

## Architecture

```
app/src/main/java/com/wavelength/music/
├── data/
│   ├── model/        # Track, Artist, Album domain models
│   ├── remote/        # Retrofit service, DTOs, DTO -> domain mappers
│   ├── local/          # Room entities and DAOs (favorites, recently played)
│   └── repository/     # MusicRepository — single source of truth for the UI layer
├── playback/            # PlaybackService (MediaSessionService), PlayerController (MediaController wrapper)
├── di/                  # Hilt modules (network, database)
└── ui/
    ├── theme/           # Compose Material 3 dark theme
    ├── navigation/      # NavHost, routes, bottom nav + mini player scaffold
    ├── components/      # Shared composables (TrackRow, MiniPlayerBar, loading/error/empty states, ...)
    ├── home/            # Home + Genre screens
    ├── search/          # Search screen
    ├── nowplaying/       # Now Playing screen + shared PlayerViewModel
    ├── library/          # Favorites / Recently Played
    ├── artist/            # Artist detail
    └── album/             # Album detail
```

Playback flows through a single `PlayerController` (a Hilt singleton) that wraps a Media3
`MediaController` connected to `PlaybackService`. Every screen that can start playback injects
`PlayerController` directly; the Now Playing screen and mini player share one `PlayerViewModel`
instance so their state always stays in sync.

## Screenshots

_Add screenshots here once you've run the app — e.g. `docs/screenshots/home.png`,
`now_playing.png`, `library.png`._
