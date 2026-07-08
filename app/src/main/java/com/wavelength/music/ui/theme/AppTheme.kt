package com.wavelength.music.ui.theme

/** [isGlass] splits themes into the "Default" family (flat color surfaces, unaffected by this
 * flag) and the "Liquid" family (real frosted-glass surfaces via Haze) — Settings groups the
 * theme picker into two rows using this flag rather than a fixed allowlist of names. */
enum class AppTheme(val label: String, val isGlass: Boolean = false) {
    CLASSIC("Classic"),
    MARVEL("Marvel"),
    PINK("Pink"),
    BLACK("Black"),
    GREY("Grey"),
    OCEAN("Ocean"),
    SUNSET("Sunset"),
    LIQUID("Liquid Cyan", isGlass = true),
    LIQUID_PURPLE("Liquid Purple", isGlass = true),
    LIQUID_ROSE("Liquid Rose", isGlass = true)
}
