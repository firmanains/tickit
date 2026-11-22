package io.github.firmanains.tickit.presentation.common

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import io.github.firmanains.tickit.presentation.Chalk
import io.github.firmanains.tickit.presentation.HandStyle
import io.github.firmanains.tickit.presentation.Paper
import java.io.File

/** A habit's emoji on a soft square of its colour. */
@Composable
fun EmojiBadge(emoji: String, color: Color, modifier: Modifier = Modifier, size: Dp = 44.dp) {
    Box(
        modifier.size(size).clip(RoundedCornerShape(size * 0.32f)).background(color.copy(alpha = 0.22f)),
        contentAlignment = Alignment.Center,
    ) { Text(emoji, fontSize = (size.value * 0.5f).sp) }
}

/** Circular progress; animates when the value changes. */
@Composable
fun ProgressRing(
    progress: Float,
    color: Color,
    modifier: Modifier = Modifier,
    stroke: Dp = 4.dp,
    track: Color = Chalk.Ink.copy(alpha = 0.1f),
    content: @Composable BoxScope.() -> Unit = {},
) {
    val sweep by animateFloatAsState(progress.coerceIn(0f, 1f), tween(700), label = "ring")
    Box(modifier, contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val width = stroke.toPx()
            val style = Stroke(width, cap = StrokeCap.Round)
            val inset = width / 2
            val arcSize = Size(size.width - width, size.height - width)
            val topLeft = Offset(inset, inset)
            drawArc(track, 0f, 360f, false, topLeft, arcSize, style = style)
            if (sweep > 0f) drawArc(color, -90f, 360f * sweep, false, topLeft, arcSize, style = style)
        }
        content()
    }
}

/** Small, stable tilt so pinned things look hand-placed rather than random on every recomposition. */
fun tiltFor(key: Any): Float = ((key.hashCode() and 0x7fffffff) % 7 - 3) * 0.7f

/** A proof photo as a polaroid: white frame, wide bottom margin, caption in handwriting. */
@Composable
fun Polaroid(path: String, caption: String, contentDescription: String, modifier: Modifier = Modifier, tilt: Float = 0f) {
    Column(
        modifier
            .graphicsLayer { rotationZ = tilt }
            .shadow(8.dp, RoundedCornerShape(4.dp))
            .background(Paper.Polaroid, RoundedCornerShape(4.dp))
            .padding(start = 10.dp, end = 10.dp, top = 10.dp, bottom = 4.dp),
    ) {
        AsyncImage(
            model = ImageRequest.Builder(LocalContext.current).data(File(path)).crossfade(true).build(),
            contentDescription = contentDescription,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxWidth().aspectRatio(1f).background(Chalk.Slate),
        )
        Text(caption, style = HandStyle, color = Paper.Ink, modifier = Modifier.padding(vertical = 6.dp))
    }
}

/** Dashed outline that says "a photo could go here". */
fun Modifier.dashedOutline(color: Color, radius: Dp = 16.dp): Modifier = drawBehind {
    drawRoundRect(
        color = color,
        cornerRadius = CornerRadius(radius.toPx()),
        style = Stroke(width = 1.5.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(14f, 10f))),
    )
}
