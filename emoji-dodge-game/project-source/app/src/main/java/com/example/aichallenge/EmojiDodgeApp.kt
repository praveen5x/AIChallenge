package com.example.aichallenge

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier

private enum class AppScreen {
    Home,
    Playing,
    GameOver
}

@Composable
fun EmojiDodgeApp(modifier: Modifier = Modifier) {
    var screen by remember { mutableStateOf(AppScreen.Home) }
    var playSession by remember { mutableIntStateOf(0) }
    var finalScore by remember { mutableIntStateOf(0) }

    when (screen) {
        AppScreen.Home -> HomeScreen(
            modifier = modifier,
            onStartGame = {
                playSession++
                screen = AppScreen.Playing
            }
        )

        AppScreen.Playing -> GameScreen(
            modifier = modifier,
            playSession = playSession,
            onGameOver = { score ->
                finalScore = score
                screen = AppScreen.GameOver
            }
        )

        AppScreen.GameOver -> GameOverScreen(
            modifier = modifier,
            score = finalScore,
            onPlayAgain = {
                playSession++
                screen = AppScreen.Playing
            },
            onHome = { screen = AppScreen.Home }
        )
    }
}
