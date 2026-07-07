package com.wavelength.music.ui.settings

/**
 * Android can't set a launcher icon to an arbitrary runtime image — icons must be compiled into
 * the app. Switching "icons" means toggling which of these pre-built activity-aliases (see
 * AndroidManifest.xml) is enabled; [aliasSuffix] must match an `<activity-alias>` name there.
 */
enum class IconPreset(val aliasSuffix: String, val label: String) {
    CLASSIC(".IconClassic", "Classic"),
    PHOTO_1(".IconPhoto1", "Photo 1")
}
