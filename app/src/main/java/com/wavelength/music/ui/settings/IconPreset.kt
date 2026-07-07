package com.wavelength.music.ui.settings

/**
 * Android can't set a launcher icon to an arbitrary runtime image — icons must be compiled into
 * the app. Switching "icons" means toggling which of these pre-built activity-aliases (see
 * AndroidManifest.xml) is enabled; [aliasSuffix] must match an `<activity-alias>` name there.
 */
enum class IconPreset(val aliasSuffix: String, val label: String) {
    CLASSIC(".IconClassic", "Classic"),
    HEADPHONES(".IconHeadphones", "Headphones"),
    VINYL(".IconVinyl", "Vinyl"),
    EQUALIZER(".IconEqualizer", "Equalizer"),
    LETTER_A_PURPLE(".IconLetterAPurple", "A Purple"),
    LETTER_A_DARK(".IconLetterADark", "A Dark")
}
