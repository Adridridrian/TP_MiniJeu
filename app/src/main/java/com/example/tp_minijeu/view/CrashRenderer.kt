package com.example.tp_minijeu.view

import com.example.tp_minijeu.controller.*
import com.example.tp_minijeu.model.*
import android.graphics.Canvas

// Dessin de l'animation de fin (la physique est dans SnowmanCrash.kt)

/** Dessine chaque morceau à sa place, tourné, à la taille du bonhomme. */
internal fun GameView.drawCrashParts(c: Canvas) {
    for (p in crashParts) {
        c.save()
        c.translate(p.x, p.y)
        c.rotate(p.angle)
        c.scale(taille, taille)
        // Les fonctions de dessin prennent l'origine du bonhomme : on la décale pour que le centre
        // du morceau tombe en (0, 0), le point autour duquel il tourne
        val ox = -p.kind.dx * d
        val oy = -p.kind.dy * d
        when (p.kind) {
            PartKind.SKI_LEFT -> drawSki(c, ox, oy, -1)
            PartKind.SKI_RIGHT -> drawSki(c, ox, oy, 1)
            PartKind.BOTTOM -> drawBottomBall(c, ox, oy)
            PartKind.MIDDLE -> { drawMiddleBall(c, ox, oy); drawScarf(c, ox, oy) }
            PartKind.HEAD -> drawHead(c, ox, oy)
            PartKind.HAT -> drawHat(c, ox, oy)
            PartKind.RUNNER_LEFT -> drawSleighRunner(c, ox, oy, -1)
            PartKind.RUNNER_RIGHT -> drawSleighRunner(c, ox, oy, 1)
            PartKind.SLEIGH -> { drawSleighBack(c, ox, oy); drawSleighFront(c, ox, oy) }
            PartKind.GIFT_GREEN -> drawGift(c, ox, oy, 0)
            PartKind.GIFT_BLUE -> drawGift(c, ox, oy, 1)
            PartKind.SANTA -> drawSanta(c, ox, oy)
            PartKind.SANTA_HAT -> drawSantaHat(c, ox, oy)
        }
        c.restore()
    }
}
