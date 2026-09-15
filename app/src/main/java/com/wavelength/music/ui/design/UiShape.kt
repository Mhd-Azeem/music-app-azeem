package com.wavelength.music.ui.design

import androidx.compose.foundation.shape.GenericShape

/**
 * Builds a smooth closed GenericShape from normalized x/y anchor pairs.
 * The path uses a Catmull-Rom-to-cubic conversion so every anchor is editable
 * while the outline remains smooth. Invalid input falls back to a rectangle.
 */
fun normalizedSmoothClosedShape(points: FloatArray): GenericShape = GenericShape { size, _ ->
    val count = points.size / 2
    if (count < 3 || points.size % 2 != 0) {
        moveTo(0f, 0f)
        lineTo(size.width, 0f)
        lineTo(size.width, size.height)
        lineTo(0f, size.height)
        close()
        return@GenericShape
    }

    fun x(i: Int): Float = points[((i % count + count) % count) * 2] * size.width
    fun y(i: Int): Float = points[((i % count + count) % count) * 2 + 1] * size.height

    moveTo(x(0), y(0))
    for (i in 0 until count) {
        val p0x = x(i - 1)
        val p0y = y(i - 1)
        val p1x = x(i)
        val p1y = y(i)
        val p2x = x(i + 1)
        val p2y = y(i + 1)
        val p3x = x(i + 2)
        val p3y = y(i + 2)

        val c1x = p1x + (p2x - p0x) / 6f
        val c1y = p1y + (p2y - p0y) / 6f
        val c2x = p2x - (p3x - p1x) / 6f
        val c2y = p2y - (p3y - p1y) / 6f
        cubicTo(c1x, c1y, c2x, c2y, p2x, p2y)
    }
    close()
}
