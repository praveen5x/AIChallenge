package com.example.aichallenge

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.max
import kotlin.math.min
import kotlin.random.Random

private data class FallingEmoji(
    val id: Long,
    val xCenter: Float,
    var yCenter: Float,
    val emoji: String,
    val fallSpeedPxPerSec: Float
)

private val EMOJI_POOL = listOf("😈", "👻", "💀", "🔥", "⚡", "🎃", "🦇", "🕷️")

private fun spawnYAboveTop(emojiHalf: Float) = -emojiHalf * 2f

private fun Rect.collidesWith(other: Rect): Boolean =
    left < other.right && right > other.left && top < other.bottom && bottom > other.top

private const val PHYSICS_HZ = 120.0
private const val MAX_PHYSICS_STEPS = 6

@Composable
fun GameScreen(
    modifier: Modifier = Modifier,
    playSession: Int,
    onGameOver: (score: Int) -> Unit
) {
    val density = LocalDensity.current
    val playerSizeDp = 52.dp
    val emojiSizeDp = 44.dp
    val bottomMarginDp = 16.dp

    val playerSizePx = with(density) { playerSizeDp.toPx() }
    val emojiSizePx = with(density) { emojiSizeDp.toPx() }
    val bottomMarginPx = with(density) { bottomMarginDp.toPx() }
    val emojiFontSp = emojiSizeDp.value.sp * 0.88f
    val playerFontSp = playerSizeDp.value.sp * 0.9f

    val textMeasurer = rememberTextMeasurer()
    val onSurface = MaterialTheme.colorScheme.onSurface

    val emojiLayouts = remember(textMeasurer, emojiFontSp, playSession) {
        EMOJI_POOL.associateWith { emoji ->
            textMeasurer.measure(
                text = AnnotatedString(emoji),
                style = TextStyle(fontSize = emojiFontSp, textAlign = TextAlign.Center),
                constraints = Constraints(maxWidth = Constraints.Infinity)
            )
        }
    }
    val playerLayout = remember(textMeasurer, playerFontSp, playSession) {
        textMeasurer.measure(
            text = AnnotatedString("🙂"),
            style = TextStyle(fontSize = playerFontSp, textAlign = TextAlign.Center),
            constraints = Constraints(maxWidth = Constraints.Infinity)
        )
    }

    key(playSession) {
        BoxWithConstraints(modifier = modifier.fillMaxSize()) {
            val widthPx = constraints.maxWidth.toFloat()
            val heightPx = constraints.maxHeight.toFloat()

            val playerHalf = playerSizePx / 2f
            val emojiHalf = emojiSizePx / 2f

            var playerCenterX by remember(playSession) {
                mutableFloatStateOf(widthPx / 2f)
            }
            val emojis = remember(playSession) { mutableStateListOf<FallingEmoji>() }

            var score by remember(playSession) { mutableFloatStateOf(0f) }
            var dodged by remember(playSession) { mutableLongStateOf(0L) }

            var spawnAccumulatorNs by remember(playSession) { mutableLongStateOf(0L) }

            var lastFrameNs by remember(playSession) { mutableLongStateOf(0L) }
            var gameStartNs by remember(playSession) { mutableLongStateOf(0L) }
            val physicsAccumulator = remember(playSession) { doubleArrayOf(0.0) }

            LaunchedEffect(playSession) {
                while (true) {
                    var stopGame = false
                    withFrameNanos { frameNs ->
                        if (widthPx <= 0f || heightPx <= 0f) {
                            return@withFrameNanos
                        }
                        if (gameStartNs == 0L) {
                            gameStartNs = frameNs
                            lastFrameNs = frameNs
                            physicsAccumulator[0] = 0.0
                            val padding = emojiHalf + 8f
                            val x = Random.nextFloat() * (widthPx - 2 * padding) + padding
                            emojis.add(
                                FallingEmoji(
                                    id = frameNs,
                                    xCenter = x,
                                    yCenter = spawnYAboveTop(emojiHalf),
                                    emoji = EMOJI_POOL.random(),
                                    fallSpeedPxPerSec = 320f
                                )
                            )
                            spawnAccumulatorNs = 0L
                            return@withFrameNanos
                        }
                        val prevFrameNs = lastFrameNs
                        val rawDt = (frameNs - prevFrameNs).coerceAtLeast(0L) / 1_000_000_000.0
                        lastFrameNs = frameNs
                        val dtSec = min(0.05, rawDt).toFloat()
                        if (dtSec <= 0f) return@withFrameNanos

                        val elapsedSec = (frameNs - gameStartNs) / 1_000_000_000f
                        score = elapsedSec * 10f + dodged * 5f

                        val minInterval = 320_000_000L
                        val spawnIntervalNs = max(
                            minInterval,
                            (750_000_000L - (elapsedSec * 28_000_000L).toLong())
                        )

                        spawnAccumulatorNs += (dtSec * 1_000_000_000f).toLong()
                        if (spawnAccumulatorNs >= spawnIntervalNs) {
                            spawnAccumulatorNs = 0L
                            val padding = emojiHalf + 8f
                            val x = Random.nextFloat() * (widthPx - 2 * padding) + padding
                            val speed = 260f + min(300f, elapsedSec * 12f)
                            emojis.add(
                                FallingEmoji(
                                    id = frameNs,
                                    xCenter = x,
                                    yCenter = spawnYAboveTop(emojiHalf),
                                    emoji = EMOJI_POOL.random(),
                                    fallSpeedPxPerSec = speed
                                )
                            )
                        }

                        physicsAccumulator[0] += rawDt
                        val step = 1.0 / PHYSICS_HZ
                        var steps = 0
                        while (physicsAccumulator[0] >= step && steps < MAX_PHYSICS_STEPS) {
                            physicsAccumulator[0] -= step
                            steps++
                            val fixedDt = step.toFloat()
                            val iterator = emojis.listIterator()
                            while (iterator.hasNext()) {
                                val e = iterator.next()
                                e.yCenter += e.fallSpeedPxPerSec * fixedDt
                                if (e.yCenter - emojiHalf > heightPx) {
                                    iterator.remove()
                                    dodged++
                                }
                            }
                        }

                        val playerTop = heightPx - bottomMarginPx - playerSizePx
                        val playerRect = Rect(
                            offset = Offset(playerCenterX - playerHalf, playerTop),
                            size = Size(playerSizePx, playerSizePx)
                        )

                        for (e in emojis) {
                            val emojiRect = Rect(
                                offset = Offset(e.xCenter - emojiHalf, e.yCenter - emojiHalf),
                                size = Size(emojiSizePx, emojiSizePx)
                            )
                            if (playerRect.collidesWith(emojiRect)) {
                                stopGame = true
                                onGameOver(score.toInt())
                                return@withFrameNanos
                            }
                        }
                    }
                    if (stopGame) break
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.surfaceContainerLow)
                    .pointerInput(playSession) {
                        detectDragGestures { change, dragAmount ->
                            change.consume()
                            playerCenterX = (playerCenterX + dragAmount.x)
                                .coerceIn(playerHalf, widthPx - playerHalf)
                        }
                    }
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    emojis.forEach { e ->
                        val layout = emojiLayouts[e.emoji] ?: return@forEach
                        val w = layout.size.width.toFloat()
                        val h = layout.size.height.toFloat()
                        translate(e.xCenter - w / 2f, e.yCenter - h / 2f) {
                            drawText(
                                textLayoutResult = layout,
                                color = onSurface
                            )
                        }
                    }
                    val pw = playerLayout.size.width.toFloat()
                    val ph = playerLayout.size.height.toFloat()
                    val playerTop = heightPx - bottomMarginPx - playerSizePx
                    translate(
                        playerCenterX - pw / 2f,
                        playerTop + (playerSizePx - ph) / 2f
                    ) {
                        drawText(
                            textLayoutResult = playerLayout,
                            color = onSurface
                        )
                    }
                }

                Text(
                    text = "Score: ${score.toInt()}",
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(16.dp),
                    fontSize = 22.sp,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}
