package com.wavelength.music.ui.splash

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.wavelength.music.R
import kotlinx.coroutines.delay

@Composable
fun AzMusicSplashScreen(
    onFinished: () -> Unit
) {
    var entered by remember { mutableStateOf(false) }
    val logoScale by animateFloatAsState(
        targetValue = if (entered) 1f else 0.72f,
        animationSpec = tween(620),
        label = "splashLogoScale"
    )
    val contentAlpha by animateFloatAsState(
        targetValue = if (entered) 1f else 0f,
        animationSpec = tween(720),
        label = "splashContentAlpha"
    )

    val pulse = rememberInfiniteTransition(label = "splashPulse")
    val glowScale by pulse.animateFloat(
        initialValue = 0.94f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(1100),
            repeatMode = RepeatMode.Reverse
        ),
        label = "splashGlowScale"
    )
    val bar1 by pulse.animateFloat(
        initialValue = 0.45f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(520), RepeatMode.Reverse),
        label = "splashBar1"
    )
    val bar2 by pulse.animateFloat(
        initialValue = 0.8f,
        targetValue = 0.35f,
        animationSpec = infiniteRepeatable(tween(640), RepeatMode.Reverse),
        label = "splashBar2"
    )
    val bar3 by pulse.animateFloat(
        initialValue = 0.35f,
        targetValue = 0.9f,
        animationSpec = infiniteRepeatable(tween(760), RepeatMode.Reverse),
        label = "splashBar3"
    )
    val bar4 by pulse.animateFloat(
        initialValue = 0.65f,
        targetValue = 0.3f,
        animationSpec = infiniteRepeatable(tween(580), RepeatMode.Reverse),
        label = "splashBar4"
    )

    LaunchedEffect(Unit) {
        entered = true
        delay(1850)
        onFinished()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(
                        Color(0xFF090414),
                        Color(0xFF16072E),
                        Color(0xFF25104B),
                        Color(0xFF090414)
                    )
                )
            )
            .clickable { onFinished() },
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(210.dp)
                .graphicsLayer {
                    scaleX = glowScale
                    scaleY = glowScale
                }
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        listOf(
                            Color(0xFFB66CFF).copy(alpha = 0.28f),
                            Color(0xFF6A3DFF).copy(alpha = 0.12f),
                            Color.Transparent
                        )
                    )
                )
        )

        Column(
            modifier = Modifier.alpha(contentAlpha),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(122.dp)
                    .graphicsLayer {
                        scaleX = logoScale
                        scaleY = logoScale
                    }
                    .shadow(
                        elevation = 24.dp,
                        shape = RoundedCornerShape(34.dp),
                        ambientColor = Color(0xFFB66CFF),
                        spotColor = Color(0xFFB66CFF)
                    )
                    .clip(RoundedCornerShape(34.dp))
                    .background(
                        Brush.linearGradient(
                            listOf(
                                Color(0xFF7046B8).copy(alpha = 0.92f),
                                Color(0xFF3E246B).copy(alpha = 0.92f),
                                Color(0xFF9A57B7).copy(alpha = 0.78f)
                            )
                        )
                    )
                    .border(
                        width = 1.5.dp,
                        brush = Brush.linearGradient(
                            listOf(
                                Color.White.copy(alpha = 0.70f),
                                Color.White.copy(alpha = 0.18f),
                                Color.White.copy(alpha = 0.48f)
                            )
                        ),
                        shape = RoundedCornerShape(34.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(R.drawable.ic_launcher_classic),
                    contentDescription = "AZ Music",
                    modifier = Modifier.size(84.dp)
                )
            }

            Spacer(Modifier.height(24.dp))

            Text(
                text = "AZ Music",
                color = Color.White,
                fontSize = 34.sp,
                fontWeight = FontWeight.SemiBold
            )

            Spacer(Modifier.height(6.dp))

            Text(
                text = "FEEL THE MUSIC",
                color = Color.White.copy(alpha = 0.66f),
                fontSize = 12.sp,
                letterSpacing = 3.sp,
                fontWeight = FontWeight.Medium
            )

            Spacer(Modifier.height(26.dp))

            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                SplashBar(bar1)
                SplashBar(bar2)
                SplashBar(bar3)
                SplashBar(bar4)
            }
        }

        Text(
            text = "MUSIC • YOUR WAY",
            style = MaterialTheme.typography.labelSmall,
            color = Color.White.copy(alpha = 0.36f),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .alpha(contentAlpha)
        )
        Spacer(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .height(30.dp)
        )
    }
}

@Composable
private fun SplashBar(scale: Float) {
    Box(
        modifier = Modifier
            .width(5.dp)
            .height(28.dp)
            .graphicsLayer { scaleY = scale }
            .clip(RoundedCornerShape(50))
            .background(
                Brush.verticalGradient(
                    listOf(
                        Color(0xFFB873FF),
                        Color(0xFF65E7FF)
                    )
                )
            )
    )
}
