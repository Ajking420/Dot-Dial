package com.example.ui.components

import android.graphics.Bitmap
import android.graphics.BitmapShader
import android.graphics.Paint
import android.graphics.Shader
import android.net.Uri
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.annotation.OptIn
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.R
import com.example.data.preferences.ColorPlateStyle
import com.example.data.preferences.DialerSettings
import com.example.data.preferences.UiBackgroundMode
import com.example.ui.theme.LocalDotDialStyle
import java.io.File
import kotlin.math.PI
import kotlin.math.roundToInt
import kotlin.math.sin

private val CardCornerShape = RoundedCornerShape(22.dp)

@Composable
fun DotDialBackgroundHost(
    settings: DialerSettings,
    modifier: Modifier = Modifier
) {
    val dotStyle = LocalDotDialStyle.current
    val themeBg = MaterialTheme.colorScheme.background
    val densityFactor = if (settings.ultraSmoothMode) minOf(settings.dotDensity, 0.35f) else settings.dotDensity

    when (settings.uiBackgroundMode) {
        UiBackgroundMode.DOT_MATRIX -> {
            val baseColor = remember(settings.amoledBlackEnabled, settings.dotMatrixBgHex, themeBg) {
                when {
                    settings.amoledBlackEnabled -> Color.Black
                    settings.dotMatrixBgHex != 0L -> Color(settings.dotMatrixBgHex)
                    else -> themeBg
                }
            }
            val effectiveDotColor = remember(baseColor, dotStyle.dotGridColor, settings.dotMatrixBgHex) {
                if (settings.dotMatrixBgHex == 0L) {
                    dotStyle.dotGridColor
                } else if (baseColor.luminance() < 0.45f) {
                    Color.White.copy(alpha = 0.16f)
                } else {
                    Color.Black.copy(alpha = 0.14f)
                }
            }

            if (settings.dotMatrixEnabled && !settings.ultraSmoothMode) {
                Box(
                    modifier = modifier
                        .fillMaxSize()
                        .background(baseColor)
                        .drawWithCache {
                            val spacingPx = (32f - densityFactor * 14f)
                                .coerceIn(14f, 34f)
                                .dp
                                .toPx()
                                .roundToInt()
                                .coerceAtLeast(8)
                            val radiusPx = (1.1f + densityFactor * 0.6f).dp.toPx()

                            val tile = Bitmap.createBitmap(spacingPx, spacingPx, Bitmap.Config.ARGB_8888)
                            val androidCanvas = android.graphics.Canvas(tile)
                            val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                                color = effectiveDotColor.copy(alpha = 0.50f).toArgb()
                                style = Paint.Style.FILL
                            }
                            androidCanvas.drawCircle(
                                spacingPx * 0.5f,
                                spacingPx * 0.5f,
                                radiusPx,
                                paint
                            )
                            val shader = BitmapShader(tile, Shader.TileMode.REPEAT, Shader.TileMode.REPEAT)
                            val brush = ShaderBrush(shader)
                            onDrawBehind {
                                drawRect(brush = brush)
                            }
                        }
                )
            } else {
                Box(
                    modifier = modifier
                        .fillMaxSize()
                        .background(baseColor)
                )
            }
        }

        UiBackgroundMode.COLOR_PLATE -> {
            if (settings.colorPlateStyle == ColorPlateStyle.SOLID) {
                val solidColor = if (settings.amoledBlackEnabled) Color.Black else Color(settings.solidBgHex)
                Box(
                    modifier = modifier
                        .fillMaxSize()
                        .background(solidColor)
                )
            } else {
                val startColor = Color(settings.gradientStartHex)
                val endColor = if (settings.amoledBlackEnabled) Color.Black else Color(settings.gradientEndHex)
                Box(
                    modifier = modifier
                        .fillMaxSize()
                        .drawWithCache {
                            val gradientBrush = Brush.verticalGradient(
                                colors = listOf(startColor, endColor)
                            )
                            onDrawBehind {
                                drawRect(brush = gradientBrush)
                            }
                        }
                )
            }
        }

        UiBackgroundMode.WALLPAPER -> {
            val context = LocalContext.current
            val wallpaperPath = settings.wallpaperPath
            val wallpaperFile = remember(wallpaperPath) {
                wallpaperPath?.let { File(it) }?.takeIf { it.exists() }
            }
            Box(
                modifier = modifier
                    .fillMaxSize()
                    .background(if (settings.amoledBlackEnabled) Color.Black else themeBg)
            ) {
                if (wallpaperFile != null && !settings.ultraSmoothMode) {
                    val request = remember(wallpaperPath) {
                        ImageRequest.Builder(context)
                            .data(wallpaperFile)
                            .size(1080, 1920)
                            .crossfade(false)
                            .build()
                    }
                    AsyncImage(
                        model = request,
                        contentDescription = "Custom UI wallpaper",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = settings.wallpaperDimStrength))
                    )
                } else {
                    val fallbackStart = Color(settings.gradientStartHex)
                    val fallbackEnd = if (settings.amoledBlackEnabled) Color.Black else Color(settings.gradientEndHex)
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Brush.verticalGradient(listOf(fallbackStart, fallbackEnd)))
                    )
                }
            }
        }
    }
}

@Composable
fun NothingSurfaceCard(
    modifier: Modifier = Modifier,
    highlighted: Boolean = false,
    onClick: (() -> Unit)? = null,
    content: @Composable () -> Unit
) {
    val style = LocalDotDialStyle.current
    val borderColor = if (highlighted) MaterialTheme.colorScheme.primary else style.hairlineBorder
    val containerColor = MaterialTheme.colorScheme.surfaceVariant

    Surface(
        modifier = modifier.then(
            if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier
        ),
        shape = CardCornerShape,
        color = containerColor,
        contentColor = MaterialTheme.colorScheme.onSurface,
        border = BorderStroke(1.dp, borderColor),
        tonalElevation = 0.dp
    ) {
        content()
    }
}

@Composable
fun ContactAvatarCircle(
    name: String,
    photoUri: String?,
    size: Dp = 44.dp,
    highlighted: Boolean = false,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val borderColor = if (highlighted) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.outline
    }

    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.16f))
            .border(1.2.dp, borderColor, CircleShape),
        contentAlignment = Alignment.Center
    ) {
        if (!photoUri.isNullOrBlank()) {
            val imageRequest = remember(photoUri) {
                ImageRequest.Builder(context)
                    .data(Uri.parse(photoUri))
                    .size(128)
                    .crossfade(false)
                    .build()
            }
            AsyncImage(
                model = imageRequest,
                contentDescription = "$name photo",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            Text(
                text = name.trim().take(1).uppercase().ifEmpty { "#" },
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Composable
fun PulsingDotIndicator(
    color: Color = MaterialTheme.colorScheme.primary,
    size: Dp = 10.dp,
    active: Boolean = true
) {
    if (!active) {
        Box(
            modifier = Modifier
                .size(size)
                .clip(CircleShape)
                .background(color.copy(alpha = 0.72f))
        )
        return
    }
    val transition = rememberInfiniteTransition(label = "dot_pulse")
    val pulse by transition.animateFloat(
        initialValue = 0.40f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(720, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_alpha"
    )
    Box(
        modifier = Modifier
            .size(size)
            .graphicsLayer { alpha = pulse }
            .clip(CircleShape)
            .background(color)
    )
}

@Composable
fun DotMatrixTagChip(
    label: String,
    active: Boolean = false,
    onClick: (() -> Unit)? = null,
    testTag: String? = null
) {
    val bg = if (active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
    val fg = if (active) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
    val border = if (active) MaterialTheme.colorScheme.primary else LocalDotDialStyle.current.hairlineBorder

    Surface(
        modifier = Modifier
            .minimumInteractiveComponentSize()
            .then(if (testTag != null) Modifier.testTag(testTag) else Modifier)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier),
        shape = RoundedCornerShape(50),
        color = bg,
        contentColor = fg,
        border = BorderStroke(1.dp, border)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 13.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            if (active) {
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(fg)
                )
            }
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = fg
            )
        }
    }
}

@kotlin.OptIn(ExperimentalFoundationApi::class)
@Composable
fun DotKeypadButton(
    digit: String,
    letters: String,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)? = null,
    size: Dp = 70.dp,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.90f else 1.0f,
        animationSpec = spring(dampingRatio = 0.65f, stiffness = 560f),
        label = "keypad_scale"
    )
    val style = LocalDotDialStyle.current
    val bgColor = if (pressed) {
        MaterialTheme.colorScheme.primary.copy(alpha = 0.24f)
    } else {
        MaterialTheme.colorScheme.surfaceVariant
    }

    Box(
        modifier = modifier
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .minimumInteractiveComponentSize()
            .size(size)
            .clip(CircleShape)
            .background(bgColor)
            .border(
                width = 1.dp,
                color = if (pressed) MaterialTheme.colorScheme.primary else style.hairlineBorder,
                shape = CircleShape
            )
            .testTag("keypad_btn_$digit")
            .combinedClickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick,
                onLongClick = onLongClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = digit,
                style = MaterialTheme.typography.displayMedium.copy(
                    fontSize = if (size < 60.dp) 21.sp else 26.sp,
                    lineHeight = if (size < 60.dp) 23.sp else 28.sp
                ),
                color = MaterialTheme.colorScheme.onSurface
            )
            if (letters.isNotEmpty()) {
                Text(
                    text = letters,
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontSize = if (size < 60.dp) 8.sp else 9.sp,
                        letterSpacing = 1.1.sp
                    ),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun AudioGlyphVisualizer(
    isActive: Boolean,
    barColor: Color = MaterialTheme.colorScheme.primary,
    modifier: Modifier = Modifier
) {
    val transition = rememberInfiniteTransition(label = "glyph_eq")
    val phase by transition.animateFloat(
        initialValue = 0f,
        targetValue = (2f * PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "eq_phase"
    )

    Canvas(modifier = modifier.height(26.dp).fillMaxWidth()) {
        val bars = 18
        val gap = size.width / bars
        val dotSize = 2.6.dp.toPx()
        for (i in 0 until bars) {
            val amplitude = if (isActive) {
                (0.25f + 0.75f * kotlin.math.abs(sin(phase + i * 0.55f)))
            } else {
                0.18f
            }
            val barH = size.height * amplitude
            val x = gap * i + gap / 2f
            val dotsCount = (barH / (dotSize * 2.4f)).toInt().coerceIn(1, 5)
            for (d in 0 until dotsCount) {
                val y = size.height / 2f + (d - dotsCount / 2f) * dotSize * 2.3f
                drawCircle(
                    color = barColor,
                    radius = dotSize,
                    center = Offset(x, y),
                    alpha = if (isActive) 0.92f else 0.35f
                )
            }
        }
    }
}

@Composable
fun GlyphCallVisualizer(
    isRingingOrActive: Boolean,
    isRecording: Boolean,
    accentColor: Color = MaterialTheme.colorScheme.primary,
    modifier: Modifier = Modifier
) {
    val transition = rememberInfiniteTransition(label = "phone_glyph_call")
    val sweep by transition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(2600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "glyph_sweep"
    )
    val pulse by transition.animateFloat(
        initialValue = 0.5f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(780, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glyph_pulse"
    )

    Canvas(modifier = modifier.size(116.dp)) {
        val stroke = 3.dp.toPx()
        val inset = 8.dp.toPx()
        val arcSize = Size(size.width - inset * 2, size.height - inset * 2)
        val topLeft = Offset(inset, inset)

        // Outer Nothing Phone (3a) Lite segmented Glyph C-arcs
        drawArc(
            color = Color.White.copy(alpha = if (isRingingOrActive) 0.72f * pulse else 0.25f),
            startAngle = sweep,
            sweepAngle = 110f,
            useCenter = false,
            topLeft = topLeft,
            size = arcSize,
            style = Stroke(width = stroke, cap = StrokeCap.Round)
        )
        drawArc(
            color = accentColor.copy(alpha = if (isRecording) 0.95f else 0.78f * pulse),
            startAngle = sweep + 145f,
            sweepAngle = 85f,
            useCenter = false,
            topLeft = topLeft,
            size = arcSize,
            style = Stroke(width = stroke, cap = StrokeCap.Round)
        )
        drawArc(
            color = Color.White.copy(alpha = 0.45f * pulse),
            startAngle = sweep + 260f,
            sweepAngle = 60f,
            useCenter = false,
            topLeft = topLeft,
            size = arcSize,
            style = Stroke(width = stroke, cap = StrokeCap.Round)
        )
    }
}

@OptIn(UnstableApi::class)
@Composable
fun CallerVideoBackdrop(
    videoUriString: String?,
    callingCardUriString: String? = null,
    redTintStrength: Float,
    isRingingOrActive: Boolean,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val primaryRed = MaterialTheme.colorScheme.primary
    val baseBlack = MaterialTheme.colorScheme.background

    Box(modifier = modifier.fillMaxSize().background(baseBlack)) {
        when {
            !videoUriString.isNullOrBlank() -> {
                val exoPlayer = remember(videoUriString) {
                    ExoPlayer.Builder(context).build().apply {
                        setMediaItem(MediaItem.fromUri(Uri.parse(videoUriString)))
                        repeatMode = Player.REPEAT_MODE_ALL
                        volume = 0f
                        prepare()
                        playWhenReady = true
                    }
                }

                DisposableEffect(exoPlayer) {
                    onDispose { exoPlayer.release() }
                }

                AndroidView(
                    factory = { ctx ->
                        PlayerView(ctx).apply {
                            player = exoPlayer
                            useController = false
                            resizeMode = AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                            layoutParams = FrameLayout.LayoutParams(
                                ViewGroup.LayoutParams.MATCH_PARENT,
                                ViewGroup.LayoutParams.MATCH_PARENT
                            )
                        }
                    },
                    update = { view -> view.player = exoPlayer },
                    modifier = Modifier.fillMaxSize()
                )
            }
            !callingCardUriString.isNullOrBlank() -> {
                AsyncImage(
                    model = Uri.parse(callingCardUriString),
                    contentDescription = "Caller calling card backdrop",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }
            else -> {
                Image(
                    painter = painterResource(id = R.drawable.img_nothing_glyph_backdrop_1794769643357),
                    contentDescription = "Nothing Glyph caller screen backdrop",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }

        // Red-black Nothing OS duotone atmospheric scrim
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            baseBlack.copy(alpha = 0.70f),
                            primaryRed.copy(alpha = redTintStrength * 0.45f),
                            baseBlack.copy(alpha = 0.88f)
                        )
                    )
                )
        )

        val transition = rememberInfiniteTransition(label = "glyph_rings")
        val ringSweep by transition.animateFloat(
            initialValue = 0.78f,
            targetValue = 1.06f,
            animationSpec = infiniteRepeatable(
                animation = tween(1400, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "ring_sweep"
        )

        Canvas(modifier = Modifier.fillMaxSize()) {
            val center = Offset(size.width / 2f, size.height * 0.34f)
            val baseRadius = size.minDimension * 0.28f * if (isRingingOrActive) ringSweep else 0.9f
            drawCircle(
                color = primaryRed.copy(alpha = 0.34f),
                radius = baseRadius,
                center = center,
                style = Stroke(width = 2.dp.toPx())
            )
            drawCircle(
                color = Color.White.copy(alpha = 0.16f),
                radius = baseRadius * 1.24f,
                center = center,
                style = Stroke(width = 1.dp.toPx())
            )
        }
    }
}

@Composable
fun SectionHeaderRow(
    title: String,
    subtitle: String? = null,
    trailing: (@Composable () -> Unit)? = null
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                PulsingDotIndicator(size = 8.dp, active = false)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }
            if (!subtitle.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        if (trailing != null) {
            trailing()
        }
    }
}
