package com.wavelength.music.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import coil.compose.AsyncImage
import com.wavelength.music.R
import java.io.File

/** App-wide background photo at 25% opacity: the user's custom pick if set, otherwise the
 * bundled default. Drawn once behind the whole nav host so it shows through every screen. */
@Composable
fun AppBackground(hasCustomBackground: Boolean, customBackgroundFile: File) {
    if (hasCustomBackground && customBackgroundFile.exists()) {
        AsyncImage(
            model = customBackgroundFile,
            contentDescription = null,
            modifier = Modifier.fillMaxSize().alpha(0.25f),
            contentScale = ContentScale.Crop
        )
    } else {
        Image(
            painter = painterResource(R.drawable.ic_launcher_photo),
            contentDescription = null,
            modifier = Modifier.fillMaxSize().alpha(0.25f),
            contentScale = ContentScale.Crop
        )
    }
}
