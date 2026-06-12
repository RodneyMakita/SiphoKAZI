package com.example

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlin.math.sin

enum class AvatarState {
    IDLE,
    THINKING,
    SPEAKING,
    CELEBRATING,
    ENCOURAGING
}

@Composable
fun SiphokaziAvatar(
    modifier: Modifier = Modifier,
    avatarState: AvatarState = AvatarState.IDLE,
    titleText: String? = null
) {
    // Infinite animation parameters
    val infiniteTransition = rememberInfiniteTransition(label = "avatar_anim")

    // Breathing effect (slight vertical panning)
    val breathingY by infiniteTransition.animateFloat(
        initialValue = -3f,
        targetValue = 3f,
        animationSpec = infiniteRepeatable(
            animation = tween(1800, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "breathing"
    )

    // Mouth movement during speaking
    val mouthYOffset by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 8f,
        animationSpec = infiniteRepeatable(
            animation = tween(230, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "mouth_speaking"
    )

    // Rotation of background halo
    val haloRotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(8000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "halo_rot"
    )

    // Celebrate particle bounce
    val bounceOffset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = -15f,
        animationSpec = infiniteRepeatable(
            animation = tween(500, easing = EaseOutBack),
            repeatMode = RepeatMode.Reverse
        ),
        label = "bounce"
    )

    // Manual periodic blink
    var isBlinking by remember { mutableStateOf(false) }
    LaunchedEffect(key1 = Unit) {
        while (true) {
            delay((2000..5000).random().toLong())
            isBlinking = true
            delay(120)
            isBlinking = false
        }
    }

    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val canvasWidth = size.width
            val canvasHeight = size.height
            val centerX = canvasWidth / 2f
            val centerY = canvasHeight / 2f
            val faceRadius = canvasWidth * 0.22f

            // 1. Draw glowing background orbit / aura based on state
            val circleColor = when (avatarState) {
                AvatarState.CELEBRATING -> Color(0xFFFFD700)
                AvatarState.THINKING -> Color(0xFF81C784)
                AvatarState.SPEAKING -> Color(0xFF64B5F6)
                AvatarState.ENCOURAGING -> Color(0xFFFF8A80)
                else -> Color(0xFF80CBC4) // Soft teal
            }

            // Rotating halo rings
            rotate(degrees = haloRotation, pivot = Offset(centerX, centerY)) {
                drawCircle(
                    brush = Brush.sweepGradient(
                        colors = listOf(
                            circleColor.copy(alpha = 0.5f),
                            Color.Transparent,
                            circleColor.copy(alpha = 0.1f),
                            circleColor.copy(alpha = 0.7f)
                        )
                    ),
                    radius = faceRadius * 1.7f,
                    style = Stroke(width = 4f)
                )

                // Extra orbiting dot details
                drawCircle(
                    color = circleColor.copy(alpha = 0.8f),
                    radius = 8f,
                    center = Offset(centerX, centerY - faceRadius * 1.7f)
                )
            }

            // Soft static backdrop circle
            drawCircle(
                color = circleColor.copy(alpha = 0.15f),
                radius = faceRadius * 1.5f
            )

            // Calculate current head baseline offset
            val headYOffset = if (avatarState == AvatarState.CELEBRATING) bounceOffset else breathingY
            val headCenter = Offset(centerX, centerY + headYOffset)

            // 2. Draw Shoulders & Green Tee Shirt
            val shirtColor = Color(0xFF5E8B75) // Dark Pastel Teal matching her green shirt
            val shirtPath = Path().apply {
                moveTo(centerX - faceRadius * 1.8f, centerY + faceRadius * 2.2f)
                quadraticTo(
                    centerX, centerY + faceRadius * 0.8f,
                    centerX + faceRadius * 1.8f, centerY + faceRadius * 2.2f
                )
                lineTo(centerX + faceRadius * 1.8f, canvasHeight)
                lineTo(centerX - faceRadius * 1.8f, canvasHeight)
                close()
            }
            drawPath(path = shirtPath, color = shirtColor)

            // Draw shirt collar inner cut
            drawCircle(
                color = Color(0xFF614E43), // Neck shadow skin color
                radius = faceRadius * 0.75f,
                center = Offset(centerX, centerY + faceRadius * 1.2f)
            )

            // Neck
            drawRoundRect(
                color = Color(0xFF7A6456), // Rich caramel warm brown
                topLeft = Offset(centerX - faceRadius * 0.35f, headCenter.y + faceRadius * 0.5f),
                size = Size(faceRadius * 0.7f, faceRadius * 0.8f),
                cornerRadius = CornerRadius(15f, 15f)
            )

            // 3. Draw Long Neat Box Braids (Behind head and shoulders)
            // Siphokazi's unique braids: neatly braided black hair rows
            val braidColor = Color(0xFF1E1E1E)
            val braidShadow = Color(0xFF0F0F0F)

            // Back braids hanging low on left & right
            val braidWidth = faceRadius * 0.16f
            for (i in -4..4) {
                if (i == 0) continue // Middle skip for neck/face
                val offsetMultiplier = i * 0.38f
                val braidX = headCenter.x + faceRadius * offsetMultiplier
                val braidYStart = headCenter.y - faceRadius * 0.2f
                val lengthFactor = if (i < -2 || i > 2) 2.1f else 2.6f
                val braidHeight = faceRadius * lengthFactor

                // Render segmented braids using small intersecting rounded capsules
                var segmentY = braidYStart
                var segmentCount = 0
                while (segmentY < headCenter.y + braidHeight) {
                    val angleOffset = sin(segmentY * 0.05f + breathingY * 0.1f) * 4f
                    val currentX = braidX + angleOffset

                    // Alternate colors to indicate braid patterns
                    val segmentColor = if (segmentCount % 2 == 0) braidColor else braidShadow
                    drawRoundRect(
                        color = segmentColor,
                        topLeft = Offset(currentX - braidWidth / 2f, segmentY),
                        size = Size(braidWidth, faceRadius * 0.5f),
                        cornerRadius = CornerRadius(20f, 20f)
                    )
                    segmentY += faceRadius * 0.35f
                    segmentCount++
                }
            }

            // 4. Draw Face Shape (Warm Caramel Skin - Siphokazi Timba)
            val skinTone = Color(0xFF8E7364) // Authentic rich caramel skin
            drawCircle(
                color = skinTone,
                radius = faceRadius,
                center = headCenter
            )

            // Hairline part (Middle parted box braids)
            val hairlinePath = Path().apply {
                moveTo(headCenter.x - faceRadius, headCenter.y - faceRadius * 0.3f)
                quadraticTo(
                    headCenter.x, headCenter.y - faceRadius * 0.4f,
                    headCenter.x, headCenter.y - faceRadius * 0.95f
                )
                quadraticTo(
                    headCenter.x, headCenter.y - faceRadius * 0.4f,
                    headCenter.x + faceRadius, headCenter.y - faceRadius * 0.3f
                )
                lineTo(headCenter.x + faceRadius, headCenter.y - faceRadius)
                lineTo(headCenter.x - faceRadius, headCenter.y - faceRadius)
                close()
            }
            drawPath(path = hairlinePath, color = braidColor)

            // Detailed braids draping over forehead sides (Siphokazi's hair structure)
            val frontHairLeft = Path().apply {
                moveTo(headCenter.x - faceRadius, headCenter.y - faceRadius * 0.3f)
                quadraticTo(
                    headCenter.x - faceRadius * 0.3f, headCenter.y - faceRadius * 0.7f,
                    headCenter.x, headCenter.y - faceRadius * 0.85f
                )
                lineTo(headCenter.x, headCenter.y - faceRadius)
                lineTo(headCenter.x - faceRadius, headCenter.y - faceRadius)
                close()
            }
            drawPath(path = frontHairLeft, color = braidShadow)

            val frontHairRight = Path().apply {
                moveTo(headCenter.x + faceRadius, headCenter.y - faceRadius * 0.3f)
                quadraticTo(
                    headCenter.x + faceRadius * 0.3f, headCenter.y - faceRadius * 0.7f,
                    headCenter.x, headCenter.y - faceRadius * 0.85f
                )
                lineTo(headCenter.x, headCenter.y - faceRadius)
                lineTo(headCenter.x + faceRadius, headCenter.y - faceRadius)
                close()
            }
            drawPath(path = frontHairRight, color = braidColor)

            // Draw hair parting line (the slit in the middle)
            drawLine(
                color = skinTone.copy(alpha = 0.8f),
                start = Offset(headCenter.x, headCenter.y - faceRadius * 0.83f),
                end = Offset(headCenter.x, headCenter.y - faceRadius * 0.98f),
                strokeWidth = 3f
            )

            // 5. Draw Eyebrows (Neat and dark black)
            val eyebrowColor = Color(0xFF151515)
            val eyebrowY = headCenter.y - faceRadius * 0.28f
            // Left Brow
            drawArc(
                color = eyebrowColor,
                startAngle = 190f,
                sweepAngle = 110f,
                useCenter = false,
                topLeft = Offset(headCenter.x - faceRadius * 0.58f, eyebrowY - 14f),
                size = Size(faceRadius * 0.42f, 25f),
                style = Stroke(width = 5.5f)
            )
            // Right Brow
            drawArc(
                color = eyebrowColor,
                startAngle = 240f,
                sweepAngle = 110f,
                useCenter = false,
                topLeft = Offset(headCenter.x + faceRadius * 0.16f, eyebrowY - 14f),
                size = Size(faceRadius * 0.42f, 25f),
                style = Stroke(width = 5.5f)
            )

            // 6. Draw Eyes (Large, expressive dark brown)
            val eyeColor = Color(0xFF1E1000)
            val eyeWhite = Color(0xFFFFFDF5)
            val leftEyeCenter = Offset(headCenter.x - faceRadius * 0.36f, headCenter.y - faceRadius * 0.08f)
            val rightEyeCenter = Offset(headCenter.x + faceRadius * 0.36f, headCenter.y - faceRadius * 0.08f)

            if (isBlinking || avatarState == AvatarState.CELEBRATING) {
                // Happy arc eyelids instead of eyes
                val arcY = headCenter.y - faceRadius * 0.06f
                drawArc(
                    color = eyeColor,
                    startAngle = 10f,
                    sweepAngle = 160f,
                    useCenter = false,
                    topLeft = Offset(leftEyeCenter.x - faceRadius * 0.18f, arcY - 4f),
                    size = Size(faceRadius * 0.36f, 15f),
                    style = Stroke(width = 6f)
                )
                drawArc(
                    color = eyeColor,
                    startAngle = 10f,
                    sweepAngle = 160f,
                    useCenter = false,
                    topLeft = Offset(rightEyeCenter.x - faceRadius * 0.18f, arcY - 4f),
                    size = Size(faceRadius * 0.36f, 15f),
                    style = Stroke(width = 6f)
                )
            } else {
                // Left Normal Eye
                drawOval(
                    color = eyeWhite,
                    topLeft = Offset(leftEyeCenter.x - faceRadius * 0.18f, leftEyeCenter.y - 12f),
                    size = Size(faceRadius * 0.36f, 24f)
                )
                // Right Normal Eye
                drawOval(
                    color = eyeWhite,
                    topLeft = Offset(rightEyeCenter.x - faceRadius * 0.18f, rightEyeCenter.y - 12f),
                    size = Size(faceRadius * 0.36f, 24f)
                )

                // Irises
                val pupilXOffset = when (avatarState) {
                    AvatarState.THINKING -> faceRadius * 0.06f
                    else -> 0f
                }
                val pupilYOffset = when (avatarState) {
                    AvatarState.THINKING -> -faceRadius * 0.06f
                    else -> 0f
                }

                drawCircle(
                    color = eyeColor,
                    radius = 11f,
                    center = Offset(leftEyeCenter.x + pupilXOffset, leftEyeCenter.y + pupilYOffset)
                )
                drawCircle(
                    color = eyeColor,
                    radius = 11f,
                    center = Offset(rightEyeCenter.x + pupilXOffset, rightEyeCenter.y + pupilYOffset)
                )

                // Eye highlights
                drawCircle(
                    color = Color.White,
                    radius = 3.5f,
                    center = Offset(leftEyeCenter.x + pupilXOffset - 4f, leftEyeCenter.y + pupilYOffset - 4f)
                )
                drawCircle(
                    color = Color.White,
                    radius = 3.5f,
                    center = Offset(rightEyeCenter.x + pupilXOffset - 4f, rightEyeCenter.y + pupilYOffset - 4f)
                )
            }

            // Cheek blush Siphokazi
            drawCircle(
                color = Color(0xFFE57373).copy(alpha = 0.2f),
                radius = faceRadius * 0.22f,
                center = Offset(headCenter.x - faceRadius * 0.58f, headCenter.y + faceRadius * 0.18f)
            )
            drawCircle(
                color = Color(0xFFE57373).copy(alpha = 0.2f),
                radius = faceRadius * 0.22f,
                center = Offset(headCenter.x + faceRadius * 0.58f, headCenter.y + faceRadius * 0.18f)
            )

            // Nose
            drawArc(
                color = Color(0xFF6F5649),
                startAngle = 40f,
                sweepAngle = 100f,
                useCenter = false,
                topLeft = Offset(headCenter.x - 12f, headCenter.y + faceRadius * 0.1f),
                size = Size(24f, 15f),
                style = Stroke(width = 3.5f)
            )

            // 7. Draw Mouth based on Avatar State
            val mouthColor = Color(0xFFB71C1C) // Nice rich berry red lip shade
            val mouthY = headCenter.y + faceRadius * 0.42f

            when (avatarState) {
                AvatarState.SPEAKING -> {
                    // Bobbing animated mouth
                    drawArc(
                        color = mouthColor,
                        startAngle = 0f,
                        sweepAngle = 180f,
                        useCenter = true,
                        topLeft = Offset(headCenter.x - faceRadius * 0.22f, mouthY - mouthYOffset),
                        size = Size(faceRadius * 0.44f, mouthYOffset * 2f)
                    )
                }

                AvatarState.CELEBRATING -> {
                    // Big wide open joyful mouth
                    val celebrateMouthPath = Path().apply {
                        moveTo(headCenter.x - faceRadius * 0.32f, mouthY)
                        quadraticTo(
                            headCenter.x, mouthY + faceRadius * 0.45f,
                            headCenter.x + faceRadius * 0.32f, mouthY
                        )
                        close()
                    }
                    drawPath(path = celebrateMouthPath, color = mouthColor)

                    // Teeth line
                    drawRoundRect(
                        color = Color.White,
                        topLeft = Offset(headCenter.x - faceRadius * 0.22f, mouthY + 1f),
                        size = Size(faceRadius * 0.44f, 6f),
                        cornerRadius = CornerRadius(2f, 2f)
                    )
                }

                AvatarState.THINKING -> {
                    // Neutral curious mouth line
                    drawArc(
                        color = mouthColor,
                        startAngle = 190f,
                        sweepAngle = 160f,
                        useCenter = false,
                        topLeft = Offset(headCenter.x - faceRadius * 0.18f, mouthY),
                        size = Size(faceRadius * 0.36f, 12f),
                        style = Stroke(width = 5f)
                    )
                }

                AvatarState.ENCOURAGING -> {
                    // Cute winking smile
                    drawArc(
                        color = mouthColor,
                        startAngle = 0f,
                        sweepAngle = 180f,
                        useCenter = false,
                        topLeft = Offset(headCenter.x - faceRadius * 0.24f, mouthY - 3f),
                        size = Size(faceRadius * 0.48f, faceRadius * 0.18f),
                        style = Stroke(width = 6f)
                    )
                }

                else -> {
                    // IDLE: Happy standard gentle smile arc
                    drawArc(
                        color = mouthColor,
                        startAngle = 0f,
                        sweepAngle = 180f,
                        useCenter = false,
                        topLeft = Offset(headCenter.x - faceRadius * 0.24f, mouthY),
                        size = Size(faceRadius * 0.48f, 14f),
                        style = Stroke(width = 5.5f)
                    )
                }
            }

            // 8. Draw special effects based on active state
            if (avatarState == AvatarState.THINKING) {
                // Draw lightbulb near head
                drawRoundRect(
                    color = Color(0xFFFFD54F),
                    topLeft = Offset(headCenter.x + faceRadius * 0.8f, headCenter.y - faceRadius * 0.7f),
                    size = Size(25f, 32f),
                    cornerRadius = CornerRadius(8f, 8f)
                )
                drawRect(
                    color = Color.Gray,
                    topLeft = Offset(headCenter.x + faceRadius * 0.8f + 5f, headCenter.y - faceRadius * 0.7f + 32f),
                    size = Size(15f, 6f)
                )
            } else if (avatarState == AvatarState.CELEBRATING) {
                // Little yellow spark stars
                val offsets = listOf(
                    Offset(headCenter.x - faceRadius * 1.2f, headCenter.y - faceRadius * 0.8f + bounceOffset),
                    Offset(headCenter.x + faceRadius * 1.1f, headCenter.y - faceRadius * 0.9f + bounceOffset),
                    Offset(headCenter.x - faceRadius * 0.4f, headCenter.y - faceRadius * 1.5f + bounceOffset),
                    Offset(headCenter.x + faceRadius * 0.3f, headCenter.y - faceRadius * 1.4f + bounceOffset)
                )
                offsets.forEach { starOffset ->
                    drawCircle(
                        color = Color(0xFFFFD54F),
                        radius = 8f,
                        center = starOffset
                    )
                }
            } else if (avatarState == AvatarState.ENCOURAGING) {
                // Floating red sweet heart
                val heartX = headCenter.x + faceRadius * 1.1f
                val heartY = headCenter.y - faceRadius * 0.4f + breathingY
                drawCircle(color = Color(0xFFFF5252), radius = 10f, center = Offset(heartX - 6f, heartY))
                drawCircle(color = Color(0xFFFF5252), radius = 10f, center = Offset(heartX + 6f, heartY))
                val heartPath = Path().apply {
                    moveTo(heartX - 15f, heartY + 3f)
                    lineTo(heartX, heartY + 16f)
                    lineTo(heartX + 15f, heartY + 3f)
                    close()
                }
                drawPath(path = heartPath, color = Color(0xFFFF5252))
            }
        }

        // Speech subtitles balloon if there's any text
        if (!titleText.isNullOrEmpty()) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 12.dp)
            ) {
                // Box title or subtitle rendered nicely below
                Text(
                    text = titleText,
                    color = Color.White,
                    fontSize = 11.sp,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                )
            }
        }
    }
}
