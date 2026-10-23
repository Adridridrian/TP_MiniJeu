package com.example.tp_minijeu.view

import com.example.tp_minijeu.controller.*
import com.example.tp_minijeu.model.*
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

// ======================= TUTORIEL (logique : model/Tutorial.kt) =======================
// Même présentation que les paramètres : décor + voile sombre, carte blanche, titre,
// zone d'animation, 2 lignes d'explication, points de progression et boutons.
// Les animations sont muettes : elles tournent en boucle avec tutorialTime.

internal fun GameView.drawTutorialScreen(c: Canvas) {
    c.drawRect(0f, 0f, width.toFloat(), height.toFloat(), paints.bg)
    drawFallingSnow(c)
    c.drawRect(0f, 0f, width.toFloat(), height.toFloat(), paints.overlay)
    drawCard(c, tutorialCard)

    val cx = width / 2f
    val top = tutorialCard.top
    c.drawText("Tutoriel", cx, top + 50f * d, paints.titleText)

    if (tutorialStep == 0) drawControlChoice(c) else drawTutorialStage(c)

    val (line1, line2) = tutorialCaption()
    c.drawText(line1, cx, top + 334f * d, paints.tutoCaption)
    c.drawText(line2, cx, top + 356f * d, paints.tutoCaption)

    if (tutorialStep == 0) {
        drawButton(c, tutoCenterButton, "Retour", paints.buttonAlt, paints.buttonAltText)
    } else {
        drawTutorialDots(c, top + 386f * d)
        drawButton(c, tutoPrevButton, "Retour", paints.buttonAlt, paints.buttonAltText)
        val next = if (tutorialStep == TUTORIAL_LAST_STEP) "Jouer !" else "Suivant"
        drawButton(c, tutoNextButton, next, paints.button, paints.buttonText)
    }
}

/** Les 2 lignes d'explication de chaque étape (elles dépendent du mode de contrôle choisi). */
private fun GameView.tutorialCaption(): Pair<String, String> = when (tutorialStep) {
    0 -> "Comment veux-tu diriger" to "ton personnage ?"
    1 -> if (touchMode) "Pose ton doigt sur la barre :" to "le perso suit ton doigt."
         else "Penche le téléphone à gauche" to "ou à droite pour te diriger."
    2 -> "Évite les sapins et les rochers !" to "Tu as 3 vies, un choc en coûte une."
    3 -> "Fais du bruit : si la jauge passe" to "la barre, le jeu accélère !"
    else -> if (touchMode) "Touche ailleurs que sur la barre" to "pour faire une pause."
            else "Touche l'écran pour faire une" to "pause et changer les réglages."
}

// ======================= ÉTAPE 0 : CHOIX DU CONTRÔLE =======================
private fun GameView.drawControlChoice(c: Canvas) {
    val t = tutorialTime
    for (touch in booleanArrayOf(false, true)) {
        val r = if (touch) tutoChoiceTouch else tutoChoiceTilt
        val selected = touch == touchMode                    // le mode actuel est entouré en bleu
        val rad = 18f * d
        c.drawRoundRect(r, rad, rad, if (selected) paints.buttonAlt else paints.rowFill)
        if (selected) c.drawRoundRect(r, rad, rad, paints.tutoSelected)

        val cx = r.centerX()
        val iconY = r.top + 72f * d
        if (touch) {
            // Une petite barre dont la poignée va et vient, poussée par un doigt
            val kx = cx + sin(t * 2.5f) * 30f * d
            drawMiniBar(c, r.left + 14f * d, r.right - 14f * d, iconY, kx)
            drawEmoji(c, "👆", kx + 4f * d, iconY + 30f * d, paints.tutoEmojiSmall)
        } else {
            // Un téléphone qui penche d'un côté puis de l'autre
            c.save()
            c.rotate(sin(t * 2.5f) * 22f, cx, iconY)
            drawEmoji(c, "📱", cx, iconY, paints.tutoEmoji)
            c.restore()
        }
        c.drawText(if (touch) "Tactile" else "Immersif", cx, r.top + 150f * d, paints.buttonAltText)
        c.drawText(if (touch) "Glisse ton doigt" else "Penche le", cx, r.top + 176f * d, paints.tutoSmall)
        c.drawText(if (touch) "sur la barre" else "téléphone", cx, r.top + 194f * d, paints.tutoSmall)
    }
}

// ======================= ÉTAPES 1 À 4 : ANIMATIONS =======================
private fun GameView.drawTutorialStage(c: Canvas) {
    val r = tutorialStage
    val rad = 18f * d
    c.save()
    c.clipPath(Path().apply { addRoundRect(r, rad, rad, Path.Direction.CW) })
    c.drawRect(r, paints.bg)
    when (tutorialStep) {
        1 -> drawSteerDemo(c, r)
        2 -> drawObstacleDemo(c, r)
        3 -> drawMicDemo(c, r)
        else -> drawPauseDemo(c, r)
    }
    c.restore()
    c.drawRoundRect(r, rad, rad, paints.tutoStageEdge)
}

/** Hauteur du perso dans la zone d'animation (centre de la boule du bas). */
private fun GameView.stagePlayerY(r: RectF) = r.top + 100f * d

/** Étape 1 : le perso va de gauche à droite, guidé par le téléphone qui penche ou par le doigt. */
private fun GameView.drawSteerDemo(c: Canvas, r: RectF) {
    val phase = tutorialTime / 4f * 2f * PI.toFloat()       // un aller-retour toutes les 4 s
    val offset = sin(phase)
    val x = r.centerX() + offset * 90f * d
    val moving = cos(phase)                                  // > 0 : va vers la droite
    drawPlayer(c, x, stagePlayerY(r), moving * 14f)

    if (touchMode) {
        val barY = r.bottom - 52f * d
        drawMiniBar(c, r.left + 14f * d, r.right - 14f * d, barY, x)
        drawEmoji(c, "👆", x + 4f * d, barY + 34f * d, paints.tutoEmojiSmall)
    } else {
        // Le téléphone penche du côté où va le perso ; la flèche de ce côté s'allume
        val py = r.bottom - 42f * d
        c.save()
        c.rotate(offset * 25f, r.centerX(), py)
        drawEmoji(c, "📱", r.centerX(), py, paints.tutoEmoji)
        c.restore()
        paints.tutoArrow.alpha = if (moving < 0f) 255 else 70
        c.drawText("◀", r.centerX() - 50f * d, py + 8f * d, paints.tutoArrow)
        paints.tutoArrow.alpha = if (moving > 0f) 255 else 70
        c.drawText("▶", r.centerX() + 50f * d, py + 8f * d, paints.tutoArrow)
        paints.tutoArrow.alpha = 255
    }
}

/** Étape 2 : un sapin arrive au milieu, le perso l'esquive ; puis un rocher passe sur le côté. */
private fun GameView.drawObstacleDemo(c: Canvas, r: RectF) {
    val p = tutorialTime % 4f
    val py = stagePlayerY(r)
    val dodge = smooth(0.3f, 0.9f, p) - smooth(1.7f, 2.3f, p)
    val x = r.centerX() + dodge * 70f * d
    val lean = when (p) { in 0.3f..0.9f -> 14f; in 1.7f..2.3f -> -14f; else -> 0f }

    // Le sapin monte pendant les 2 premières secondes, le rocher pendant les 2 suivantes
    val treeY = if (p < 2f) lerp(r.bottom + 75f * d, r.top - 10f * d, p / 2f) else Float.NaN
    val rockY = if (p >= 2f) lerp(r.bottom + 40f * d, r.top - 40f * d, (p - 2f) / 2f) else Float.NaN
    val rockX = r.centerX() + 70f * d
    tutorialRock.y = rockY

    // Comme en jeu : un obstacle plus haut que le bas du perso est dessiné derrière lui
    val limit = py + 55f * s
    if (!treeY.isNaN() && treeY <= limit) drawTree(c, r.centerX(), treeY, 30f * d)
    if (!rockY.isNaN() && rockY <= limit) drawRock(c, tutorialRock, rockX)
    drawPlayer(c, x, py, lean)
    if (!treeY.isNaN() && treeY > limit) drawTree(c, r.centerX(), treeY, 30f * d)
    if (!rockY.isNaN() && rockY > limit) drawRock(c, tutorialRock, rockX)

    // Les 3 vies en haut à droite
    for (i in 0 until maxLives) {
        drawHeart(c, r.right - 20f * d - i * 24f * d, r.top + 20f * d, 16f * d, paints.heartFull)
    }
}

/** Étape 3 : la jauge du micro monte, passe la barre, et la vitesse augmente (en rouge). */
private fun GameView.drawMicDemo(c: Canvas, r: RectF) {
    val p = tutorialTime % 3f
    val level = when {
        p < 1.6f -> p / 1.6f
        p < 2.2f -> 1f
        else -> 1f - (p - 2.2f) / 0.8f
    }
    val fast = p >= 1.6f * micThreshold                     // la jauge vient de dépasser la barre

    // Traces dans la neige qui défilent, plus vite quand ça accélère
    val py = stagePlayerY(r)
    val speed = if (fast) 2.6f else 1f
    val step = 44f * d
    val scroll = (tutorialTime * speed * 90f * d) % step
    for (side in intArrayOf(-1, 1)) {
        val tx = r.centerX() + side * 8f * s
        var y = r.top - scroll
        while (y < r.bottom) {
            c.drawLine(tx, y, tx, y + 22f * d, paints.track)
            y += step
        }
    }
    drawPlayer(c, r.centerX(), py, 0f)

    // Jauge comme en jeu : micro, niveau, barre du seuil et vitesse
    val left = r.left + 10f * d
    val right = r.right - 10f * d
    val gy = r.bottom - 34f * d
    val rad = 22f * d
    c.drawRoundRect(left, gy - rad + 3f * d, right, gy + rad + 3f * d, rad, rad, paints.pillShadow)
    c.drawRoundRect(left, gy - rad, right, gy + rad, rad, rad, paints.pill)
    val micX = left + 24f * d
    drawEmoji(c, "🎤", micX, gy, paints.tutoEmojiTiny)

    val barLeft = left + 46f * d
    val barRight = right - 64f * d
    val halfH = 6f * d
    c.drawRoundRect(barLeft, gy - halfH, barRight, gy + halfH, halfH, halfH, paints.gaugeTrack)
    if (level > 0.01f) {
        c.drawRoundRect(barLeft, gy - halfH, barLeft + (barRight - barLeft) * level, gy + halfH, halfH, halfH,
            if (level > micThreshold) paints.gaugeLoud else paints.gaugeCalm)
    }
    val sx = barLeft + (barRight - barLeft) * micThreshold
    c.drawLine(sx, gy - halfH - 5f * d, sx, gy + halfH + 5f * d, paints.gaugeMark)
    c.drawText(if (fast) "x1.1" else "x1.0", right - 14f * d, gy + 6f * d, if (fast) paints.speedUpText else paints.speedText)

    // Ondes sonores au-dessus du micro, d'autant plus visibles que c'est fort
    paints.tutoWave.alpha = (level * 255).toInt()
    for (i in 1..3) {
        val wr = i * 7f * d
        c.drawArc(micX - wr, gy - 36f * d - wr, micX + wr, gy - 36f * d + wr, 210f, 120f, false, paints.tutoWave)
    }
    paints.tutoWave.alpha = 255
}

/** Étape 4 : un doigt touche l'écran, un cercle s'agrandit et la pause apparaît. */
private fun GameView.drawPauseDemo(c: Canvas, r: RectF) {
    val p = tutorialTime % 2.6f
    drawPlayer(c, r.centerX(), stagePlayerY(r), 0f)
    if (touchMode) drawMiniBar(c, r.left + 14f * d, r.right - 14f * d, r.bottom - 52f * d, r.centerX())

    // Le doigt arrive, touche (0,6 s), puis le cercle s'agrandit
    val tx = r.centerX() + 70f * d
    val ty = r.top + 60f * d
    val approach = (1f - p / 0.6f).coerceIn(0f, 1f)
    drawEmoji(c, "👆", tx + 4f * d, ty + 26f * d + approach * 30f * d, paints.tutoEmojiSmall)
    if (p in 0.6f..1.2f) {
        val k = (p - 0.6f) / 0.6f
        paints.tutoRipple.alpha = ((1f - k) * 255).toInt()
        c.drawCircle(tx, ty, 6f * d + k * 30f * d, paints.tutoRipple)
        paints.tutoRipple.alpha = 255
    }

    // La pause s'affiche
    if (p in 0.9f..2.3f) {
        c.drawRect(r, paints.overlay)
        val card = RectF(r.centerX() - 64f * d, r.centerY() - 40f * d, r.centerX() + 64f * d, r.centerY() + 40f * d)
        c.drawRoundRect(card, 16f * d, 16f * d, paints.card)
        drawEmoji(c, "⏸️", r.centerX(), card.top + 26f * d, paints.tutoEmojiSmall)
        c.drawText("Pause", r.centerX(), card.bottom - 12f * d, paints.buttonAltText)
    }
}

// ======================= OUTILS =======================
/** Points de progression des étapes 1 à 4 (le point de l'étape en cours est bleu). */
private fun GameView.drawTutorialDots(c: Canvas, y: Float) {
    val gap = 16f * d
    val first = width / 2f - (TUTORIAL_LAST_STEP - 1) * gap / 2f
    for (i in 1..TUTORIAL_LAST_STEP) {
        c.drawCircle(first + (i - 1) * gap, y, 4f * d, if (i == tutorialStep) paints.tutoDotOn else paints.gaugeTrack)
    }
}

/** Petite barre de contrôle tactile (même style que celle du jeu), poignée en kx. */
private fun GameView.drawMiniBar(c: Canvas, left: Float, right: Float, y: Float, kx: Float) {
    val rad = 18f * d
    c.drawRoundRect(left, y - rad + 3f * d, right, y + rad + 3f * d, rad, rad, paints.pillShadow)
    c.drawRoundRect(left, y - rad, right, y + rad, rad, rad, paints.pill)
    val h = 3f * d
    c.drawRoundRect(left + rad, y - h, right - rad, y + h, h, h, paints.gaugeTrack)
    val x = kx.coerceIn(left + rad, right - rad)
    c.drawCircle(x, y + 2f * d, 14f * d, paints.pillShadow)
    c.drawCircle(x, y, 14f * d, paints.thumbFill)
    c.drawCircle(x, y, 14f * d, paints.thumbEdge)
}

/** Emoji centré en (x, y). */
private fun drawEmoji(c: Canvas, emoji: String, x: Float, y: Float, paint: Paint) {
    c.drawText(emoji, x, y - (paint.ascent() + paint.descent()) / 2f, paint)
}

private fun lerp(a: Float, b: Float, t: Float) = a + (b - a) * t.coerceIn(0f, 1f)

/** Passe en douceur de 0 à 1 entre a et b. */
private fun smooth(a: Float, b: Float, v: Float): Float {
    val k = ((v - a) / (b - a)).coerceIn(0f, 1f)
    return k * k * (3f - 2f * k)
}

// ======================= BOUTON "?" DE L'ACCUEIL =======================
internal fun GameView.drawTutorialButton(c: Canvas) {
    val r = tutorialButton
    val rad = r.width() / 2f
    c.drawCircle(r.centerX(), r.centerY() + 3f * d, rad, paints.pillShadow)
    c.drawCircle(r.centerX(), r.centerY(), rad, paints.pill)
    val ty = r.centerY() - (paints.tutoQuestion.ascent() + paints.tutoQuestion.descent()) / 2f
    c.drawText("?", r.centerX(), ty, paints.tutoQuestion)
    c.drawText("Tuto", r.centerX(), r.bottom + 16f * d, paints.tutoLabel)
}
