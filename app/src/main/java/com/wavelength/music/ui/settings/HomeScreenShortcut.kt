package com.wavelength.music.ui.settings

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
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

    fun pinPhotoAsShortcut(context: Context, photoUri: Uri, label: String) {
        val bitmap = loadSquareBitmap(context, photoUri, size = 192)
        val icon = IconCompat.createWithAdaptiveBitmap(bitmap)
        val shortcut = ShortcutInfoCompat.Builder(context, "photo_icon_${System.currentTimeMillis()}")
            .setShortLabel(label)
            .setIcon(icon)
            .setIntent(Intent(context, MainActivity::class.java).setAction(Intent.ACTION_MAIN))
            .build()
        ShortcutManagerCompat.requestPinShortcut(context, shortcut, null)
    }

    private fun loadSquareBitmap(context: Context, uri: Uri, size: Int): Bitmap {
        val original = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            val source = ImageDecoder.createSource(context.contentResolver, uri)
            ImageDecoder.decodeBitmap(source) { decoder, _, _ ->
                decoder.isMutableRequired = true
            }
        } else {
            @Suppress("DEPRECATION")
            MediaStore.Images.Media.getBitmap(context.contentResolver, uri)
        }
        val side = minOf(original.width, original.height)
        val x = (original.width - side) / 2
        val y = (original.height - side) / 2
        val cropped = Bitmap.createBitmap(original, x, y, side, side)
        return Bitmap.createScaledBitmap(cropped, size, size, true)
    }
}
