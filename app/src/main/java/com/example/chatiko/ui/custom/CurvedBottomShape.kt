package com.example.chatiko.ui.custom

import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection

class CurvedBottomShape : Shape {
    override fun createOutline(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density
    ): Outline {
        val path = Path().apply {
            moveTo(0f, 0f)
            lineTo(size.width.toFloat(), 0f)
            lineTo(size.width.toFloat(), size.height * 0.55f)
            quadraticBezierTo(
                size.width / 2f, size.height * 1.03f,
                0f, size.height * 0.55f
            )
            close()
        }
        return Outline.Generic(path)
    }
}