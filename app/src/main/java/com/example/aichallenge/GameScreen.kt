package com.example.aichallenge

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sign
import kotlin.math.sin
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
private const val STARTING_LIVES = 3
private const val INVINCIBLE_AFTER_HIT_SEC = 1.35f

/** Target time (seconds) for a hazard to cross the playfield height — lower speed = longer fall. */
private const val TARGET_FALL_DURATION_SEC = 2f

/** Player avatar — robot fits the “AI pilot” theme. */
private const val PLAYER_AVATAR = "\uD83E\uDD16" // 🤖

/**
 * Light assist: nudges horizontally away from hazards in the band above the player.
 * Keeps the human in control; drag still dominates.
 */
private fun aiAssistDeltaX(
    emojis: List<FallingEmoji>,
    playerCenterX: Float,
    playerTop: Float,
    playerHalf: Float,
    widthPx: Float,
    dtSec: Float
): Float {
    val zoneTop = playerTop - 440f
    val zoneBottom = playerTop + playerHalf * 1.6f
    var nudge = 0f
    for (e in emojis) {
        if (e.yCenter < zoneTop || e.yCenter > zoneBottom) continue
        val dx = e.xCenter - playerCenterX
        val verticalUrgency = 1f - ((playerTop - e.yCenter) / 440f).coerceIn(0f, 1f)
        val lateral = 520f / (abs(dx) + 72f)
        nudge -= dx.sign * verticalUrgency * lateral * 95f * dtSec
    }
    val maxStep = 200f * dtSec
    return nudge.coerceIn(-maxStep, maxStep)
}

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
    val primary = MaterialTheme.colorScheme.primary
    val tertiary = MaterialTheme.colorScheme.tertiary

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
            text = AnnotatedString(PLAYER_AVATAR),
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
            var gameElapsedSec by remember(playSession) { mutableFloatStateOf(0f) }
            var lives by remember(playSession) { mutableIntStateOf(STARTING_LIVES) }
            /** Elapsed game time (seconds) until which player is invincible; -1 = not invincible. */
            var invincibleUntilElapsedSec by remember(playSession) { mutableFloatStateOf(-1f) }

            var spawnAccumulatorNs by remember(playSession) { mutableLongStateOf(0L) }

            var lastFrameNs by remember(playSession) { mutableLongStateOf(0L) }
            var gameStartNs by remember(playSession) { mutableLongStateOf(0L) }
            val physicsAccumulator = remember(playSession) { doubleArrayOf(0.0) }

            val stars = remember(widthPx, heightPx, playSession) {
                if (widthPx <= 0f || heightPx <= 0f) {
                    emptyList()
                } else {
                    List(54) {
                        Offset(
                            Random.nextFloat() * widthPx,
                            Random.nextFloat() * heightPx * 0.72f
                        ) to (Random.nextFloat() * 1.1f + 0.35f)
                    }
                }
            }

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
                            lives = STARTING_LIVES
                            invincibleUntilElapsedSec = -1f
                            val padding = emojiHalf + 8f
                            val x = Random.nextFloat() * (widthPx - 2 * padding) + padding
                            val baseFall = (heightPx / TARGET_FALL_DURATION_SEC).coerceIn(200f, 520f)
                            emojis.add(
                                FallingEmoji(
                                    id = frameNs,
                                    xCenter = x,
                                    yCenter = spawnYAboveTop(emojiHalf),
                                    emoji = EMOJI_POOL.random(),
                                    fallSpeedPxPerSec = baseFall
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
                        gameElapsedSec = elapsedSec
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
                            val baseFall = (heightPx / TARGET_FALL_DURATION_SEC).coerceIn(200f, 520f)
                            val ramp = 1f + min(0.45f, elapsedSec * 0.022f)
                            val speed = (baseFall * ramp).coerceIn(200f, 720f)
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
                        val assist = aiAssistDeltaX(
                            emojis = emojis,
                            playerCenterX = playerCenterX,
                            playerTop = playerTop,
                            playerHalf = playerHalf,
                            widthPx = widthPx,
                            dtSec = dtSec
                        )
                        playerCenterX = (playerCenterX + assist * 0.28f)
                            .coerceIn(playerHalf, widthPx - playerHalf)

                        val playerRect = Rect(
                            offset = Offset(playerCenterX - playerHalf, playerTop),
                            size = Size(playerSizePx, playerSizePx)
                        )

                        if (elapsedSec >= invincibleUntilElapsedSec) {
                            val hitIterator = emojis.listIterator()
                            var tookHit = false
                            while (hitIterator.hasNext()) {
                                val e = hitIterator.next()
                                val emojiRect = Rect(
                                    offset = Offset(e.xCenter - emojiHalf, e.yCenter - emojiHalf),
                                    size = Size(emojiSizePx, emojiSizePx)
                                )
                                if (playerRect.collidesWith(emojiRect)) {
                                    if (!tookHit) {
                                        tookHit = true
                                        lives--
                                        invincibleUntilElapsedSec = elapsedSec + INVINCIBLE_AFTER_HIT_SEC
                                    }
                                    hitIterator.remove()
                                }
                            }
                            if (tookHit && lives <= 0) {
                                stopGame = true
                                onGameOver(score.toInt())
                                return@withFrameNanos
                            }
                        }
                    }
                    if (stopGame) break
                }
            }

            val scheme = MaterialTheme.colorScheme
            val playfieldBrush = Brush.verticalGradient(
                colors = listOf(
                    scheme.surfaceContainerHigh.copy(alpha = 0.95f),
                    scheme.background,
                    scheme.surfaceContainerLow
                )
            )

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(playfieldBrush)
                    .pointerInput(playSession) {
                        detectDragGestures { change, dragAmount ->
                            change.consume()
                            playerCenterX = (playerCenterX + dragAmount.x)
                                .coerceIn(playerHalf, widthPx - playerHalf)
                        }
                    }
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val pw = playerLayout.size.width.toFloat()
                    val ph = playerLayout.size.height.toFloat()
                    val playerTop = heightPx - bottomMarginPx - playerSizePx
                    val cx = playerCenterX
                    val cy = playerTop + playerSizePx / 2f
                    val baseR = max(pw, ph) / 2f + 6f
                    val pulse = (sin((gameElapsedSec * 5.0).toDouble()).toFloat() * 0.5f + 0.5f)
                    val invincible = gameElapsedSec < invincibleUntilElapsedSec
                    val gAlpha = if (invincible) (0.32f + 0.68f * pulse) else 1f

                    stars.forEach { (pos, r) ->
                        drawCircle(
                            color = Color.White.copy(alpha = 0.06f + 0.04f * pulse),
                            radius = r,
                            center = pos
                        )
                    }

                    emojis.forEach { e ->
                        val layout = emojiLayouts[e.emoji] ?: return@forEach
                        val w = layout.size.width.toFloat()
                        val h = layout.size.height.toFloat()
                        val phase = (e.id.toDouble() * 1e-7 + e.xCenter * 0.002).toFloat()
                        val sway = sin((gameElapsedSec * 2.75 + phase).toDouble()).toFloat() * 12f
                        val bob = 1f + 0.045f * sin((gameElapsedSec * 3.1 + e.yCenter * 0.015f).toDouble()).toFloat()
                        val core = max(w, h) * 0.5f
                        val cxLocal = w / 2f
                        val cyLocal = h / 2f

                        translate(e.xCenter, e.yCenter) {
                            rotate(sway) {
                                scale(bob, bob) {
                                    translate(-w / 2f, -h / 2f) {
                                        drawOval(
                                            color = Color.Black.copy(alpha = 0.14f),
                                            topLeft = Offset(cxLocal - core * 0.65f, cyLocal + core * 0.38f),
                                            size = Size(core * 1.3f, core * 0.32f)
                                        )
                                        drawCircle(
                                            brush = Brush.radialGradient(
                                                colors = listOf(
                                                    tertiary.copy(alpha = 0.42f),
                                                    primary.copy(alpha = 0.12f),
                                                    Color.Transparent
                                                ),
                                                center = Offset(cxLocal, cyLocal),
                                                radius = core * 1.15f
                                            ),
                                            radius = core * 1.1f,
                                            center = Offset(cxLocal, cyLocal)
                                        )
                                        drawCircle(
                                            color = Color.White.copy(alpha = 0.12f),
                                            radius = core * 0.52f,
                                            center = Offset(cxLocal, cyLocal),
                                            style = Stroke(width = 1.2f)
                                        )
                                        drawText(
                                            textLayoutResult = layout,
                                            color = onSurface.copy(alpha = 0.96f)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    drawCircle(
                        color = tertiary.copy(alpha = gAlpha * (0.12f + 0.1f * pulse)),
                        radius = baseR + 10f + 4f * pulse,
                        center = Offset(cx, cy)
                    )
                    drawCircle(
                        color = primary.copy(alpha = gAlpha * (0.35f + 0.2f * pulse)),
                        radius = baseR + 4f,
                        center = Offset(cx, cy),
                        style = Stroke(width = 3f)
                    )
                    drawCircle(
                        color = primary.copy(alpha = gAlpha * 0.15f),
                        radius = baseR + 14f,
                        center = Offset(cx, cy),
                        style = Stroke(width = 1.5f)
                    )

                    translate(
                        playerCenterX - pw / 2f,
                        playerTop + (playerSizePx - ph) / 2f
                    ) {
                        drawText(
                            textLayoutResult = playerLayout,
                            color = onSurface.copy(alpha = gAlpha)
                        )
                    }
                }

                Surface(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    shape = RoundedCornerShape(28.dp),
                    color = scheme.surfaceContainerHighest.copy(alpha = 0.92f),
                    shadowElevation = 6.dp
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Column {
                            Text(
                                text = "SCORE",
                                style = MaterialTheme.typography.labelLarge,
                                color = scheme.onSurfaceVariant
                            )
                            Text(
                                text = score.toInt().toString(),
                                style = MaterialTheme.typography.headlineMedium,
                                color = scheme.primary
                            )
                        }
                        VerticalDivider(
                            modifier = Modifier.height(44.dp),
                            color = scheme.outlineVariant.copy(alpha = 0.6f)
                        )
                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = "DODGED",
                                style = MaterialTheme.typography.labelLarge,
                                color = scheme.onSurfaceVariant
                            )
                            Text(
                                text = dodged.toString(),
                                style = MaterialTheme.typography.headlineMedium,
                                color = scheme.tertiary
                            )
                        }
                        VerticalDivider(
                            modifier = Modifier.height(44.dp),
                            color = scheme.outlineVariant.copy(alpha = 0.6f)
                        )
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "LIVES",
                                style = MaterialTheme.typography.labelLarge,
                                color = scheme.onSurfaceVariant
                            )
                            Text(
                                text = buildString { repeat(lives) { append("❤️") } },
                                style = MaterialTheme.typography.headlineSmall
                            )
                        }
                    }
                }

                Text(
                    text = "Drag to move",
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 8.dp),
                    style = MaterialTheme.typography.bodyMedium,
                    color = scheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
            }
        }
    }
}
