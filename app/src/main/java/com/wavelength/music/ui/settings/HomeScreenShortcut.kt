package com.wavelength.music.ui.settings

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import androidx.core.content.pm.ShortcutInfoCompat
import androidx.core.content.pm.ShortcutManagerCompat
import androidx.core.graphics.drawable.IconCompat
import com.wavelength.music.MainActivity

/**
 * Android has no API for an app to replace its own launcher icon with an arbitrary runtime
 * image — icons must be compiled in (see IconPreset). The closest real equivalent is pinning a
 * *shortcut* to the home screen with a custom bitmap icon, which is exactly what "add to home
 * screen" features in other apps use. It opens the app like a second icon; it doesn't replace
 * the app's actual launcher icon. Android requires its own one-time confirmation UI before the
 * icon is actually added — that step can't be skipped or automated away.
 */
object HomeScreenShortcut {

    fun isSupported(context: Context): Boolean =
        ShortcutManagerCompat.isRequestPinShortcutSupported(context)

    /** [croppedBitmap] is expected to already be the user's chosen square crop (see
     * [com.wavelength.music.ui.components.ImageCropDialog]); this only resizes it to a
     * consistent icon resolution. */
    fun pinPhotoAsShortcut(context: Context, croppedBitmap: Bitmap, label: String) {
        val scaled = Bitmap.createScaledBitmap(croppedBitmap, 192, 192, true)
        val icon = IconCompat.createWithAdaptiveBitmap(scaled)
        val shortcut = ShortcutInfoCompat.Builder(context, "photo_icon_${System.currentTimeMillis()}")
            .setShortLabel(label)
            .setIcon(icon)
            .setIntent(Intent(context, MainActivity::class.java).setAction(Intent.ACTION_MAIN))
            .build()
        ShortcutManagerCompat.requestPinShortcut(context, shortcut, null)
    }
}
