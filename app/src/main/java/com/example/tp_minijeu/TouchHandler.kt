package com.example.tp_minijeu

import android.view.MotionEvent

// ======================= INTERACTIONS =======================
internal fun GameView.handleTouch(event: MotionEvent): Boolean {
    val x = event.x
    val y = event.y

    when (event.actionMasked) {
        MotionEvent.ACTION_DOWN -> onTouchDown(x, y)
        MotionEvent.ACTION_MOVE -> if (dragging != 0) setSliderFromX(dragging, x)
        MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
            if (dragging != 0) {
                dragging = 0
                saveSettings()
            }
        }
    }
    invalidate()
    return true
}

internal fun GameView.onTouchDown(x: Float, y: Float) {
    when (gameState) {
        GameState.START -> {
            if (playButton.contains(x, y)) {
                performClick()
                resetGame()
                gameState = GameState.PLAYING
            } else if (settingsButton.contains(x, y)) {
                performClick()
                gameState = GameState.SETTINGS
            }
        }
        GameState.PLAYING -> {
            // Toucher l'écran n'importe où = pause / paramètres
            performClick()
            gameState = GameState.PAUSED
        }
        GameState.PAUSED, GameState.SETTINGS -> {
            val inGame = gameState == GameState.PAUSED
            when {
                hitSlider(moveY, x, y) -> { dragging = 1; setSliderFromX(1, x) }
                hitSlider(micY, x, y) -> { dragging = 2; setSliderFromX(2, x) }
                defaultsButton.contains(x, y) -> {
                    performClick()
                    moveSens = 1f
                    micSens = 1f
                    saveSettings()
                }
                primaryButton.contains(x, y) -> {
                    performClick()
                    gameState = if (inGame) GameState.PLAYING else GameState.START
                }
                inGame && secondaryButton.contains(x, y) -> {
                    performClick()
                    resetGame()
                    gameState = GameState.START
                }
            }
        }
        GameState.GAME_OVER -> {
            // Délai de 700 ms : évite de relancer par erreur en touchant l'écran au moment de la chute
            if (System.currentTimeMillis() - gameOverAt > 700) {
                if (replayButton.contains(x, y)) {
                    performClick()
                    resetGame()
                    gameState = GameState.PLAYING
                } else if (overMenuButton.contains(x, y)) {
                    performClick()
                    resetGame()
                    gameState = GameState.START
                }
            }
        }
    }
}
