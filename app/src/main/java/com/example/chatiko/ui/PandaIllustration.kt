package com.example.chatiko.ui

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.unit.dp

@Composable
fun PandaIllustration(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {

        val w = size.width
        val h = size.height

        val outlineWidth = 6.dp.toPx()
        val outlineColor = Color(0xFF2C3545)
        val primaryPurple = Color(0xFFA596FA)

        val bodyTop = h * 0.55f
        val bodyWidth = w * 0.65f
        val bodyLeft = (w - bodyWidth) / 2f

        val bodyPath = Path().apply {
            moveTo(bodyLeft + bodyWidth * 0.2f, bodyTop)
            quadraticBezierTo(bodyLeft - bodyWidth * 0.1f, h, bodyLeft, h)
            lineTo(bodyLeft + bodyWidth, h)
            quadraticBezierTo(
                bodyLeft + bodyWidth + bodyWidth * 0.1f,
                h,
                bodyLeft + bodyWidth * 0.8f,
                bodyTop
            )
            close()
        }

        drawPath(
            path = bodyPath,
            brush = Brush.verticalGradient(
                colors = listOf(primaryPurple, Color(0xFFCEC8FA)),
                startY = bodyTop,
                endY = h
            )
        )

        drawPath(
            path = bodyPath,
            color = outlineColor,
            style = Stroke(width = outlineWidth, join = StrokeJoin.Round)
        )


        val bellyPath = Path().apply {
            moveTo(bodyLeft + bodyWidth * 0.15f, h)
            quadraticBezierTo(
                w / 2f,
                h - h * 0.2f,
                bodyLeft + bodyWidth * 0.85f,
                h
            )
            close()
        }

        drawPath(bellyPath, color = Color.White)
        drawPath(
            bellyPath,
            color = outlineColor,
            style = Stroke(width = outlineWidth)
        )


        val earRadius = w * 0.14f
        val leftEarCenter = Offset(w * 0.28f, h * 0.25f)
        val rightEarCenter = Offset(w * 0.72f, h * 0.25f)

        drawCircle(primaryPurple, earRadius, leftEarCenter)
        drawCircle(outlineColor, earRadius, leftEarCenter, style = Stroke(outlineWidth))

        drawCircle(primaryPurple, earRadius, rightEarCenter)
        drawCircle(outlineColor, earRadius, rightEarCenter, style = Stroke(outlineWidth))


        val headRadiusX = w * 0.42f
        val headRadiusY = h * 0.35f
        val headCenter = Offset(w / 2f, h * 0.45f)

        val headTopLeft = Offset(
            headCenter.x - headRadiusX,
            headCenter.y - headRadiusY
        )

        drawOval(
            brush = Brush.radialGradient(
                colors = listOf(Color.White, Color(0xFFF3F0FF)),
                center = headCenter,
                radius = headRadiusX
            ),
            topLeft = headTopLeft,
            size = Size(headRadiusX * 2, headRadiusY * 2)
        )

        drawOval(
            color = outlineColor,
            topLeft = headTopLeft,
            size = Size(headRadiusX * 2, headRadiusY * 2),
            style = Stroke(width = outlineWidth)
        )


        val patchWidth = w * 0.22f
        val patchHeight = h * 0.16f

        val leftPatchCenter = Offset(w * 0.34f, h * 0.42f)
        val rightPatchCenter = Offset(w * 0.66f, h * 0.42f)

        withTransform({
            rotate(18f, leftPatchCenter)
        }) {
            drawOval(
                color = primaryPurple,
                topLeft = Offset(
                    leftPatchCenter.x - patchWidth / 2,
                    leftPatchCenter.y - patchHeight / 2
                ),
                size = Size(patchWidth, patchHeight)
            )

            drawOval(
                color = outlineColor,
                topLeft = Offset(
                    leftPatchCenter.x - patchWidth / 2,
                    leftPatchCenter.y - patchHeight / 2
                ),
                size = Size(patchWidth, patchHeight),
                style = Stroke(width = outlineWidth * 0.8f)
            )
        }

        withTransform({
            rotate(-18f, rightPatchCenter)
        }) {
            drawOval(
                color = primaryPurple,
                topLeft = Offset(
                    rightPatchCenter.x - patchWidth / 2,
                    rightPatchCenter.y - patchHeight / 2
                ),
                size = Size(patchWidth, patchHeight)
            )

            drawOval(
                color = outlineColor,
                topLeft = Offset(
                    rightPatchCenter.x - patchWidth / 2,
                    rightPatchCenter.y - patchHeight / 2
                ),
                size = Size(patchWidth, patchHeight),
                style = Stroke(width = outlineWidth * 0.8f)
            )
        }


        val pupilRadius = w * 0.04f
        val pupilOffset = h * 0.015f

        drawCircle(
            outlineColor,
            pupilRadius,
            Offset(leftPatchCenter.x + pupilOffset, leftPatchCenter.y)
        )

        drawCircle(
            outlineColor,
            pupilRadius,
            Offset(rightPatchCenter.x - pupilOffset, rightPatchCenter.y)
        )


        val noseCenter = Offset(w / 2f, h * 0.51f)
        val noseWidth = w * 0.08f
        val noseHeight = h * 0.04f

        val nosePath = Path().apply {
            moveTo(noseCenter.x - noseWidth / 2, noseCenter.y - noseHeight / 2)
            lineTo(noseCenter.x + noseWidth / 2, noseCenter.y - noseHeight / 2)
            quadraticBezierTo(
                noseCenter.x,
                noseCenter.y + noseHeight,
                noseCenter.x - noseWidth / 2,
                noseCenter.y - noseHeight / 2
            )
        }

        drawPath(nosePath, color = outlineColor)


        val mouthPath = Path().apply {
            moveTo(w * 0.44f, h * 0.56f)
            quadraticBezierTo(w * 0.5f, h * 0.61f, w * 0.56f, h * 0.56f)
        }

        drawPath(
            mouthPath,
            color = outlineColor,
            style = Stroke(
                width = outlineWidth * 0.8f,
                cap = StrokeCap.Round
            )
        )
    }
}
