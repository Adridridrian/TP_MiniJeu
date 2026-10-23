package com.example.tp_minijeu

import android.graphics.Canvas
import kotlin.math.abs
import kotlin.math.sign
import kotlin.random.Random

/**
 * Animation de la dernière vie perdue : le bonhomme se démonte, chaque morceau saute,
 * rebondit, puis roule (ou glisse) vers l'extérieur de l'écran. Ensuite : écran "Perdu !".
 */

/** Les morceaux : centre dans le dessin du bonhomme (dx, dy en dp) et rayon au sol (dp). */
internal enum class PartKind(val dx: Float, val dy: Float, val radius: Float, val rolls: Boolean) {
    SKI_LEFT(-14f, 20f, 6f, false),
    SKI_RIGHT(14f, 20f, 6f, false),
    BOTTOM(0f, 0f, 30f, true),
    MIDDLE(0f, -42f, 22f, true),
    HEAD(0f, -82f, 17f, true),
    HAT(0f, -108f, 12f, false)
}

/** Un morceau en vol : position et vitesse en pixels, angle en degrés, spin en degrés/s. */
internal class SnowmanPart(val kind: PartKind, var x: Float, var y: Float, var vx: Float, var vy: Float, var spin: Float) {
    var angle = 0f
}

private const val CRASH_MAX_TIME = 3.2f       // au plus tard, "Perdu !" s'affiche après 3,2 s

/** Démonte le bonhomme : chaque morceau part avec sa propre vitesse (en dp/s, un peu au hasard). */
internal fun GameView.startCrash() {
    gameState = GameState.CRASHING
    crashTime = 0f
    crashParts.clear()
    val dir = if (Random.nextBoolean()) 1f else -1f      // la boule du bas part à gauche ou à droite
    fun jitter() = 0.85f + Random.nextFloat() * 0.3f   // +/- 15 % pour que ce soit différent à chaque fois
    for (kind in PartKind.entries) {
        val (vx, vy, spin) = when (kind) {
            PartKind.SKI_LEFT -> Triple(-260f, -220f, -320f)
            PartKind.SKI_RIGHT -> Triple(260f, -220f, 320f)
            PartKind.BOTTOM -> Triple(dir * 170f, -280f, 0f)
            PartKind.MIDDLE -> Triple(-dir * 210f, -440f, 0f)
            PartKind.HEAD -> Triple(dir * 240f, -580f, 0f)
            PartKind.HAT -> Triple(-dir * 140f, -780f, -dir * 720f)
        }
        crashParts.add(SnowmanPart(
            kind,
            posX + kind.dx * s, baseY + kind.dy * s,
            vx * jitter() * d, vy * jitter() * d, spin
        ))
    }
}

internal fun GameView.updateCrash(dt: Float) {
    crashTime += dt
    val floor = baseY + 55f * s                 // le sol = sous les skis
    val gravity = 1600f * d
    for (p in crashParts) {
        val r = p.kind.radius * s
        p.vy += gravity * dt
        if (p.kind.rolls) p.vx += sign(p.vx) * 60f * d * dt   // les boules prennent de la vitesse en roulant
        p.x += p.vx * dt
        p.y += p.vy * dt

        // Rebond sur le sol, de moins en moins haut
        if (p.y + r > floor) {
            p.y = floor - r
            p.vy = if (p.vy > 120f * d) -p.vy * 0.45f else 0f
            if (!p.kind.rolls) p.spin *= 0.6f       // chapeau et skis tournent moins après chaque choc
        }

        // Les boules tournent comme si elles roulaient ; le reste tourne sur lui-même
        p.angle += if (p.kind.rolls) p.vx / r * 57.3f * dt else p.spin * dt
    }

    val allGone = crashParts.all { abs(it.x - width / 2f) > width / 2f + 60f * d }
    if (allGone || crashTime > CRASH_MAX_TIME) {
        gameState = GameState.GAME_OVER
        gameOverAt = System.currentTimeMillis()
    }
}

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
        }
        c.restore()
    }
}
