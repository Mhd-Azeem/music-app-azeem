package com.wavelength.music.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.wavelength.music.R
import java.io.File

/** App-wide background photo at a user-adjustable opacity: the user's custom pick if set,
 * otherwise the bundled default. Drawn once behind the whole nav host so it shows through every
 * screen. */
@Composable
fun AppBackground(hasCustomBackground: Boolean, customBackgroundFile: File, opacity: Float = 0.25f) {
    if (hasCustomBackground && customBackgroundFile.exists()) {
        val context = LocalContext.current
        AsyncImage(
            model = ImageRequest.Builder(context)
                .data(customBackgroundFile)
                // The file is overwritten in place each time the user re-crops a background, but
                // Coil otherwise keys its cache on the File's path alone, so it would keep
                // serving the old cached bitmap — busting the key with the mtime forces a reload.
                .memoryCacheKey("${customBackgroundFile.absolutePath}_${customBackgroundFile.lastModified()}")
                .diskCacheKey("${customBackgroundFile.absolutePath}_${customBackgroundFile.lastModified()}")
                .build(),
            contentDescription = null,
            modifier = Modifier.fillMaxSize().alpha(opacity),
            contentScale = ContentScale.Crop
        )
    } else {
        Image(
            painter = painterResource(R.drawable.bg_default),
            contentDescription = null,
            modifier = Modifier.fillMaxSize().alpha(opacity),
            contentScale = ContentScale.Crop
        )
    }
}
