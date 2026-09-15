# AzMusic UI Designer

This folder contains the browser-based visual designer for AzMusic.

## Live designer

Once GitHub Pages is enabled with **GitHub Actions** as the source, open:

`https://mhd-azeem.github.io/music-app-azeem/preview/`

## Current Android-linked controls

The first stable designer version can edit:

- Quick Pick tile height
- Quick Pick tile corner radius
- Playlist card corner radius
- Mini-player height
- Mini-player corner radius
- Mini-player album-art size
- Mini-player album-art corner radius
- Mini-player horizontal padding
- Mini-player item spacing

The phone preview changes instantly while you move the sliders.

## Apply & Build APK

The designer generates:

`app/src/main/java/com/wavelength/music/ui/design/UiDesignConfig.kt`

The Android Compose UI reads these same values. Pressing **Apply Design & Build APK** commits the generated config to the `feature/email-activation-access` branch. Because that is an Android-source change, the existing `Build APK` GitHub Actions workflow runs automatically and publishes `AzMusic.apk` to Releases.

The designer asks for a fine-grained GitHub token with **Contents: Read and write** access to this repository. The token is used only by JavaScript in the open browser page; it is not written into the repository or localStorage by the designer.

## Safety boundary

This is intentionally a controlled component designer rather than arbitrary Kotlin generation. That keeps visual edits predictable and reduces the chance of generating an app that does not compile.

The designer does not play songs, call JioSaavn, use the activation backend, write the music database, or request Android permissions.
