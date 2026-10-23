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
        GameState.LEADERBOARD -> drawLeaderboardScreen(canvas)
        GameState.CRASHING -> { drawScene(canvas); drawHud(canvas) }   // animation de fin (SnowmanCrash.kt)
    }
}

internal fun GameView.drawStartScreen(c: Canvas) {
    c.drawRect(0f, 0f, width.toFloat(), height.toFloat(), paints.bg)
    val cx = width / 2f
    // Titre sur deux lignes, en bloc de glace
    val line1 = height * 0.10f + 34f * d
    val line2 = line1 + 58f * d
    drawIceTitle(c, "JINGLE", cx, line1)
    drawIceTitle(c, "RUSH", cx, line2)
    c.drawText("Record : $best m", cx, line2 + 44f * d, paints.infoText)
    drawPlayer(c, cx, height * 0.45f, 0f)

    drawDifficultyPicker(c)

    drawButton(c, playButton, "Jouer", paints.button, paints.buttonText)
    drawButton(c, startLeaderboardButton, "Classement", paints.buttonAlt, paints.buttonAltText)
    drawButton(c, settingsButton, "Paramètres", paints.buttonAlt, paints.buttonAltText)
    drawFallingSnow(c)
}

private val DIFFICULTY_LABELS = arrayOf("Facile", "Moyen", "Difficile")
private val DIFFICULTY_EMOJIS = arrayOf("😊", "😬", "🔥")

/**
 * Choix de la difficulté (sensibilité du micro) : une pastille blanche coupée en 3 segments.
 * Le segment choisi est rempli de sa couleur (vert / orange / rouge), les autres sont estompés.
 */
internal fun GameView.drawDifficultyPicker(c: Canvas) {
    val first = difficultyButtons.first()
    val last = difficultyButtons.last()
    val rad = first.height() / 2f
    tmpRect.set(first.left, first.top + 3f * d, last.right, first.bottom + 3f * d)
    c.drawRoundRect(tmpRect, rad, rad, paints.pillShadow)
    tmpRect.set(first.left, first.top, last.right, first.bottom)
    c.drawRoundRect(tmpRect, rad, rad, paints.pill)

    val inset = 4f * d
    difficultyButtons.forEachIndexed { i, r ->
        val selected = i == difficulty
        if (selected) {
            tmpRect.set(r.left + inset, r.top + inset, r.right - inset, r.bottom - inset)
            c.drawRoundRect(tmpRect, rad - inset, rad - inset, paints.difficultyFills[i])
        }
        paints.difficultyEmoji.alpha = if (selected) 255 else 140
        c.drawText(DIFFICULTY_EMOJIS[i], r.centerX(), r.top + 26f * d, paints.difficultyEmoji)
        c.drawText(DIFFICULTY_LABELS[i], r.centerX(), r.bottom - 11f * d,
            if (selected) paints.difficultyOnText else paints.difficultyOffText)
    }
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

internal fun GameView.drawLeaderboardScreen(c: Canvas) {
    // Comme l'écran Paramètres : décor, voile sombre, puis la carte
    c.drawRect(0f, 0f, width.toFloat(), height.toFloat(), paints.bg)
    drawFallingSnow(c)
    c.drawRect(0f, 0f, width.toFloat(), height.toFloat(), paints.overlay)
    drawLeaderboardPanel(c)
}

internal fun GameView.drawGameOverScreen(c: Canvas) {
    drawScene(c)
    drawHud(c)
    c.drawRect(0f, 0f, width.toFloat(), height.toFloat(), paints.overlay)
    drawCard(c, overCard)
    val cx = width / 2f
    val cy = height * 0.45f
    c.drawText("Perdu !", cx, cy - 64f * d, paints.titleText)
    c.drawText("Distance : ${score()} m", cx, cy - 28f * d, paints.infoText)
    c.drawText("Record : $best m", cx, cy - 2f * d, paints.infoText)

    // Feedback sur le classement (lastRank : 0 = 1re place, null = hors du top 10)
    val rank = lastRank
    if (rank == 0) {
        c.drawText("Nouveau record !", cx, cy + 28f * d, paints.recordText)
    } else if (rank != null) {
        c.drawText("${rank + 1}e place au classement", cx, cy + 28f * d, paints.rankText)
    }
    drawButton(c, replayButton, "Rejouer", paints.button, paints.buttonText)
    drawButton(c, overLeaderboardButton, "Classement", paints.buttonAlt, paints.buttonAltText)
    drawButton(c, overMenuButton, "Menu", paints.buttonAlt, paints.buttonAltText)
}
