# Minimal System Mode implementation

## Repository audit

- Native Kotlin / Jetpack Compose Android application; Android API 24–34, Java target 17.
- No Windows application, desktop target, or EXE workflow exists. The HTML preview is not a desktop music implementation.
- PlaybackService owns Media3 playback; PlayerController and PlayerViewModel expose the shared transport and queue.
- FloatingIslandService owns the native Live Island overlay, including foreground/background visibility and gesture behavior. It is unchanged.
- SettingsRepository persists preferences in wavelength_settings SharedPreferences. Room repositories own library data, playlists, favorites, downloads and history.
- WavelengthTheme, MainActivity, AppBackground, NowPlayingScreen and MiniPlayerBar provide the appearance pipeline.
- Existing expensive paths include Haze blur, album palette extraction, gyro motion, vinyl/frame loops, depth motion, waveform capture, and Aurora infinite transitions.
- Existing DragDropListState implements queue reordering and edge scrolling. No queue/playback operations were replaced.
- build-apk.yml assembles signed release APKs and publishes the mutable latest release. deploy-ui-preview.yml deploys the HTML preview. No workflow was dispatched and no existing release was overwritten.

## Design

AppExperienceMode is stored independently of theme preferences. AppSettingsState.forExperience produces a presentation-only copy for all AppSettingsViewModel consumers. Normal settings remain in SettingsRepository, including for services, and are restored immediately on exit. No playback object, library table, or downloaded file is changed by the mode setter.

Settings offers horizontal thumb dragging (95% threshold), reset on early release/cancellation, haptic completion, then a separate cancellable confirmation dialog. Accessibility activation and Enter/Space open the same dialog.

Minimal rendering uses fixed red/dark Material colors and a static diagonal gradient. Wallpaper composables are omitted. Existing Now Playing controls use static artwork with no album-motion style, adaptive palette, glass, or visualizer. Its queue expansion blur is disabled. Mini player keeps gestures/transport and gains a thin red progress line. Advanced appearance controls and visualizer settings are omitted. Animated startup video is skipped in Minimal Mode. The Aurora infinite transition is composed only for the Aurora theme.

Live Island uses its existing separate service and unchanged preferences. No mode-based suppression was added. Background playback, media notification, lock-screen controls, downloads, library, backup/restore, and queue behavior retain their existing implementations.

## Validation status

Policy JUnit tests executed successfully: 2 tests, 0 failures, 0 errors, 0 skipped. They cover identity in Normal Mode, decorative overrides, preserved functional settings and Live Island, and restoration without mutating preferences. The final-source debug APK build passed. Live Island controls were moved outside the Appearance category so they remain accessible in both modes; functional Up Next and system-volume preferences also remain accessible. The app background now observes distinct artwork URLs instead of playback-position ticks, and the static Minimal gradient brush is reused.

Device acceptance remains required: swipe/cancel/restart, continuous playback while switching, queue drag with auto-scroll, local/online/download playback, playlists, search, backup/restore, collapsed/expanded Island, gestures, rotation, outside-tap collapse and background behavior. These have not been verified on a device.

No comparative RAM, CPU, GPU, battery or frame-drop measurements have been collected. Source-level reductions are implemented; measured savings are not claimed. Use the same device, playlist, screen duration, brightness and network state for both modes; collect Android Studio profiler, dumpsys meminfo and gfxinfo results after warmup.

No Windows binary can be produced using this repository's existing infrastructure. No paid dependency or service was added.

## Signing and publication

The local environment has no permanent signing credentials configured. The repository's existing release configuration falls back to the local Android debug key. An APK built here is therefore not verified as update-compatible with an installed production build. Application ID, versioning logic and signing configuration were not changed. No GitHub release was published or replaced. Changes are on the local feature/minimal-system-mode branch and have not been pushed.

## Final build evidence

Executed on Windows, 2 November 2026, with the repository's Gradle 8.9 wrapper, JDK 21 (Java/Kotlin target 17), Android SDK 34 and Build Tools 34.0.0.

- Final command: gradlew.bat :app:assembleDebug :app:testDebugUnitTest :app:assembleRelease
- BUILD SUCCESSFUL, 22m 37s; 106 actionable tasks (66 executed, 40 up-to-date).
- Debug APK and optimized/shrunk release APK produced successfully.
- Release lint vital checks passed. Existing compiler deprecation warnings remain.
- Final policy test run: 2 tests, 0 failures, 0 errors, 0 skipped.
- apksigner verification of the output release APK passed. Its certificate is Android Debug, matching the existing local fallback behavior, not a verified production signing identity.
- git diff --check passed.
- ADB device inventory was empty. No runtime/UI, persistence restart, queue gesture, playback, download or Live Island device checks were executed. Full acceptance and production readiness are therefore not certified.
- No application performance measurements were possible; build times above are build evidence, not app performance data.

## Modified files

- .github/workflows/build-apk.yml
- .gitignore
- app/build.gradle.kts
- app/src/main/java/com/wavelength/music/MainActivity.kt
- app/src/main/java/com/wavelength/music/data/repository/SettingsRepository.kt
- app/src/main/java/com/wavelength/music/ui/components/MiniPlayerBar.kt
- app/src/main/java/com/wavelength/music/ui/navigation/NavGraph.kt
- app/src/main/java/com/wavelength/music/ui/nowplaying/NowPlayingScreen.kt
- app/src/main/java/com/wavelength/music/ui/settings/AppSettingsViewModel.kt
- app/src/main/java/com/wavelength/music/ui/settings/SettingsScreen.kt
- app/src/main/java/com/wavelength/music/ui/settings/SystemModeSection.kt
- app/src/main/java/com/wavelength/music/ui/theme/Theme.kt
- app/src/test/java/com/wavelength/music/data/repository/ExperiencePolicyTest.kt
- docs/MINIMAL_SYSTEM_MODE.md
