package com.example.tp_minijeu.view

import com.example.tp_minijeu.controller.*
import android.graphics.Canvas

// Dessin de la transformation (la logique est dans TransformAnimation.kt)

/** Dessine la neige et les étoiles du "pouf" sur l'écran d'accueil. */
internal fun GameView.drawMagicPuff(c: Canvas) {
    for (p in particles) {
        paints.spray.alpha = (p.life / 0.5f * 220).toInt().coerceIn(0, 255)
        c.drawCircle(p.x, p.y, 2.6f * d, paints.spray)
    }
    for (p in magicSparkles) {
        paints.magicSparkle.alpha = (p.life.coerceAtMost(0.4f) / 0.4f * 255).toInt()
        drawSparkle(c, p.x, p.y, 5f * d, paints.magicSparkle)
    }
}
