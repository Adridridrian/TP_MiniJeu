package com.example.tp_minijeu.controller

import com.example.tp_minijeu.model.*
import android.view.MotionEvent
import kotlin.math.abs
import kotlin.math.roundToInt

// ======================= INTERACTIONS =======================
internal fun GameView.handleTouch(event: MotionEvent): Boolean {
    val x = event.x
    val y = event.y

    when (event.actionMasked) {
        MotionEvent.ACTION_DOWN -> onTouchDown(x, y)
        MotionEvent.ACTION_MOVE -> {
            if (dragging != 0) setSliderFromX(dragging, x)
            if (steering) touchX = x
        }
        MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
            steering = false
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
            if (tutorialButton.contains(x, y)) {
                // Bouton "?" : ouvre le tutoriel
                performClick()
                startTutorial()
            } else if (titleArea.contains(x, y)) {
                // Easter egg : toucher le titre transforme le bonhomme en Père Noël (et inversement)
                performClick()
                startTransform()
            } else if (playButton.contains(x, y)) {
                performClick()
                resetGame()
                gameState = GameState.PLAYING
            } else if (settingsButton.contains(x, y)) {
                performClick()
                gameState = GameState.SETTINGS
            } else if (startLeaderboardButton.contains(x, y)) {
                performClick()
                previousState = gameState      // on retient d'où l'on vient
                gameState = GameState.LEADERBOARD
            } else {
                val i = difficultyButtons.indexOfFirst { it.contains(x, y) }
                if (i >= 0) {
                    performClick()
                    difficulty = i
                    saveSettings()
                }
            }
        }
        GameState.PLAYING -> {
            if (touchMode && hitControlBar(y)) {
                // Mode tactile : le doigt sur la barre guide le bonhomme
                steering = true
                touchX = x
            } else {
                // Toucher l'écran n'importe où ailleurs = pause / paramètres
                performClick()
                gameState = GameState.PAUSED
            }
        }
        GameState.PAUSED, GameState.SETTINGS -> {
            val inGame = gameState == GameState.PAUSED
            when {
                modeTiltButton.contains(x, y) -> {
                    performClick()
                    touchMode = false
                    saveSettings()
                }
                modeTouchButton.contains(x, y) -> {
                    performClick()
                    touchMode = true
                    saveSettings()
                }
                hitSlider(moveY, x, y) -> { dragging = 1; setSliderFromX(1, x) }
                hitSlider(volumeY, x, y) -> { dragging = 2; setSliderFromX(2, x) }
                defaultsButton.contains(x, y) -> {
                    performClick()
                    moveSens = 1f
                    musicVol = musicVolume
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
                } else if (overLeaderboardButton.contains(x, y)) {
                    performClick()
                    previousState = gameState      // on retient d'où l'on vient
                    gameState = GameState.LEADERBOARD
                }
            }
        }
        GameState.LEADERBOARD -> {
            if (backButton.contains(x, y)) {
                performClick()
                gameState = previousState
            }
        }
        GameState.TUTORIAL -> {
            if (tutorialStep == 0) {
                // Choix du mode de contrôle, ou retour au menu
                when {
                    tutoChoiceTilt.contains(x, y) -> { performClick(); chooseControl(touch = false) }
                    tutoChoiceTouch.contains(x, y) -> { performClick(); chooseControl(touch = true) }
                    tutoCenterButton.contains(x, y) -> { performClick(); gameState = GameState.START }
                }
            } else {
                when {
                    tutoPrevButton.contains(x, y) -> { performClick(); goToTutorialStep(tutorialStep - 1) }
                    tutoNextButton.contains(x, y) -> {
                        performClick()
                        if (tutorialStep == TUTORIAL_LAST_STEP) finishTutorial() else goToTutorialStep(tutorialStep + 1)
                    }
                }
            }
        }
        GameState.CRASHING -> {}   // pendant l'animation de fin, les touches sont ignorées
    }
}

// ======================= ZONES TOUCHÉES =======================
/** Mode tactile : le doigt touche-t-il la barre de contrôle ? */
internal fun GameView.hitControlBar(y: Float) = abs(y - controlY) < 36f * d

internal fun GameView.hitSlider(sliderY: Float, x: Float, y: Float) =
    abs(y - sliderY) < 24f * d && x > trackLeft - 24f * d && x < trackRight + 24f * d

internal fun GameView.setSliderFromX(which: Int, x: Float) {
    val t = ((x - trackLeft) / (trackRight - trackLeft)).coerceIn(0f, 1f)
    if (which == 1) {
        var v = moveMin + t * (moveMax - moveMin)
        if (abs(v - 1f) < 0.05f) v = 1f        // se cale sur 100 % (réglage de base)
        moveSens = v
    } else {
        // Volume : de 0 (coupé) à 100 %, par pas de 5 %
        musicVol = (t * 20f).roundToInt() / 20f
    }
}
