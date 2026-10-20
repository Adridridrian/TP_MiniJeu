package com.example.tp_minijeu

import android.graphics.Canvas

// ======================= DESSIN DES ÉCRANS =======================
internal fun GameView.drawCurrentScreen(canvas: Canvas) {
    when (gameState) {
        GameState.START -> drawStartScreen(canvas)
        GameState.PLAYING -> drawGame(canvas)
        GameState.PAUSED -> drawPauseScreen(canvas)
        GameState.SETTINGS -> drawSettingsScreen(canvas)
        GameState.GAME_OVER -> drawGameOverScreen(canvas)
    }
}

internal fun GameView.drawStartScreen(c: Canvas) {
    c.drawRect(0f, 0f, width.toFloat(), height.toFloat(), paints.bg)
    val cx = width / 2f
    c.drawText("MINI-JEU", cx, height * 0.18f, paints.titleText)
    c.drawText("Record : $best m", cx, height * 0.18f + 36f * d, paints.infoText)
    drawPlayer(c, cx, height * 0.42f, 0f)
    drawButton(c, playButton, "Jouer", paints.button, paints.buttonText)
    drawButton(c, settingsButton, "Paramètres", paints.buttonAlt, paints.buttonAltText)
    drawFallingSnow(c)
}

internal fun GameView.drawGame(c: Canvas) {
    drawScene(c)
    drawHud(c)

    // Rappel en début de partie : toucher l'écran = pause / paramètres
    if (gameTime < 3f) {
        paints.hintText.alpha = ((3f - gameTime).coerceIn(0f, 1f) * 255).toInt()
        if (touchMode) {
            c.drawText("Glisse la barre pour bouger", width / 2f, controlY - 64f * d, paints.hintText)
            c.drawText("Touche ailleurs : pause / paramètres", width / 2f, controlY - 42f * d, paints.hintText)
        } else {
            c.drawText("Touche l'écran : pause / paramètres", width / 2f, height - 100f * d, paints.hintText)
        }
    }
}

internal fun GameView.drawPauseScreen(c: Canvas) {
    drawScene(c)
    drawHud(c)
    c.drawRect(0f, 0f, width.toFloat(), height.toFloat(), paints.overlay)
    drawSettingsPanel(c, "Pause", pauseCard, inGame = true)
}

internal fun GameView.drawSettingsScreen(c: Canvas) {
    c.drawRect(0f, 0f, width.toFloat(), height.toFloat(), paints.bg)
    drawFallingSnow(c)
    c.drawRect(0f, 0f, width.toFloat(), height.toFloat(), paints.overlay)
    drawSettingsPanel(c, "Paramètres", settingsCard, inGame = false)
}

internal fun GameView.drawGameOverScreen(c: Canvas) {
    drawScene(c)
    drawHud(c)
    c.drawRect(0f, 0f, width.toFloat(), height.toFloat(), paints.overlay)
    drawCard(c, overCard)
    val cx = width / 2f
    val cy = height * 0.45f
    c.drawText("Perdu !", cx, cy - 55f * d, paints.titleText)
    c.drawText("Distance : ${score()} m", cx, cy - 10f * d, paints.infoText)
    c.drawText("Record : $best m", cx, cy + 22f * d, paints.infoText)
    drawButton(c, replayButton, "Rejouer", paints.button, paints.buttonText)
    drawButton(c, overMenuButton, "Menu", paints.buttonAlt, paints.buttonAltText)
}
