package com.wavelength.music.data.repository

import org.junit.Assert.*
import org.junit.Test

class ExperiencePolicyTest {
    @Test fun normalIsUnchanged() {
        val saved = AppSettingsState(albumArtStyle = AlbumArtStyle.VINYL)
        assertSame(saved, saved.forExperience())
    }
    @Test fun minimalDisablesDecorationWithoutMutatingSavedSettings() {
        val saved = AppSettingsState(experienceMode = AppExperienceMode.MINIMAL,
            albumArtStyle = AlbumArtStyle.VINYL, audioVisualizerEnabled = true,
            glassmorphismNowPlaying = true, floatingIslandEnabled = true,
            crossfadeDurationMs = 3000, visualThemeMode = VisualThemeMode.AURORA)
        val rendered = saved.forExperience()
        assertEquals(AlbumArtStyle.OFF, rendered.albumArtStyle)
        assertEquals(VisualThemeMode.SOLID, rendered.visualThemeMode)
        assertFalse(rendered.audioVisualizerEnabled)
        assertFalse(rendered.glassmorphismNowPlaying)
        assertFalse(rendered.animateThemeTransitions)
        assertTrue(rendered.floatingIslandEnabled)
        assertEquals(3000, rendered.crossfadeDurationMs)
        assertEquals(AlbumArtStyle.VINYL, saved.albumArtStyle)
        val restored = saved.copy(experienceMode = AppExperienceMode.NORMAL).forExperience()
        assertEquals(AlbumArtStyle.VINYL, restored.albumArtStyle)
        assertTrue(restored.audioVisualizerEnabled)
    }
}
