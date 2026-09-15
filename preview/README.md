# AzMusic UI Preview

This folder contains a static, UI-only preview of the Android app.

## What it is for

Use it to check layouts, navigation, colors, dialogs, player controls, library tabs, search UI, settings, statistics and activation screens before building an APK.

## What it does NOT do

- No song playback
- No JioSaavn/API requests
- No activation backend calls
- No database writes
- No downloads
- No Android permissions

All displayed songs, stats and account states are dummy preview data.

## Live preview

The GitHub Pages workflow publishes this folder at:

`https://mhd-azeem.github.io/music-app-azeem/preview/`

The repository root Pages URL redirects to the preview.

## Editing

For visual experiments, edit `preview/index.html`. It is deliberately self-contained so layout preview changes do not affect the Android project or Gradle build.
