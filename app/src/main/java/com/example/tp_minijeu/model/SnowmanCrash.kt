package com.example.tp_minijeu.model

import com.example.tp_minijeu.controller.*
import com.example.tp_minijeu.view.audio.*
import kotlin.math.abs
import kotlin.math.sign
import kotlin.random.Random

/**
 * Animation de la dernière vie perdue : le bonhomme (ou le Père Noël et son traîneau) se démonte,
 * chaque morceau saute, rebondit, puis roule (ou glisse) vers l'extérieur de l'écran.
 * Sons : "plop" au départ (+ grelots pour le traîneau), "pouf" à chaque rebond, trombone triste à la fin.
 */

/**
 * Les morceaux : centre dans le dessin (dx, dy en dp), rayon au sol (dp), s'il roule comme une boule,
 * et à quel skin il appartient (bonhomme de neige ou Père Noël dans son traîneau).
 */
internal enum class PartKind(val dx: Float, val dy: Float, val radius: Float, val rolls: Boolean, val santa: Boolean) {
    // Bonhomme de neige
    SKI_LEFT(-14f, 20f, 6f, false, false),
    SKI_RIGHT(14f, 20f, 6f, false, false),
    BOTTOM(0f, 0f, 30f, true, false),
    MIDDLE(0f, -42f, 22f, true, false),
    HEAD(0f, -82f, 17f, true, false),
    HAT(0f, -108f, 12f, false, false),
    // Père Noël : le traîneau se renverse, les cadeaux volent, le Père Noël roule sur son gros ventre
    RUNNER_LEFT(-15f, 20f, 6f, false, true),
    RUNNER_RIGHT(15f, 20f, 6f, false, true),
    SLEIGH(0f, 14f, 20f, false, true),
    GIFT_GREEN(-19f, -50f, 7f, false, true),
    GIFT_BLUE(19f, -54f, 7f, false, true),
    SANTA(0f, -35f, 26f, true, true),
    SANTA_HAT(0f, -85f, 10f, false, true)
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
    val santa = SantaSkin.enabled
    // Bruits : "plop" quand ça éclate (+ grelots du traîneau pour le Père Noël)
    music.play(Sfx.POP)
    if (santa) music.play(Sfx.SLEIGH_BELLS)
    for (kind in PartKind.entries.filter { it.santa == santa }) {
        val (vx, vy, spin) = when (kind) {
            PartKind.SKI_LEFT -> Triple(-260f, -220f, -320f)
            PartKind.SKI_RIGHT -> Triple(260f, -220f, 320f)
            PartKind.BOTTOM -> Triple(dir * 170f, -280f, 0f)
            PartKind.MIDDLE -> Triple(-dir * 210f, -440f, 0f)
            PartKind.HEAD -> Triple(dir * 240f, -580f, 0f)
            PartKind.HAT -> Triple(-dir * 140f, -780f, -dir * 720f)
            PartKind.RUNNER_LEFT -> Triple(-260f, -220f, -320f)
            PartKind.RUNNER_RIGHT -> Triple(260f, -220f, 320f)
            PartKind.SLEIGH -> Triple(dir * 150f, -380f, dir * 260f)     // fait des tonneaux
            PartKind.GIFT_GREEN -> Triple(-230f, -650f, -540f)
            PartKind.GIFT_BLUE -> Triple(230f, -720f, 540f)
            PartKind.SANTA -> Triple(-dir * 190f, -600f, 0f)             // rebondit sur son ventre
            PartKind.SANTA_HAT -> Triple(dir * 130f, -800f, dir * 720f)
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

        // Rebond sur le sol, de moins en moins haut, avec un "pouf" plus ou moins fort
        if (p.y + r > floor) {
            p.y = floor - r
            if (p.vy > 120f * d) {
                music.play(Sfx.THUD, volume = (p.vy / (900f * d)).coerceIn(0.3f, 1f))
                p.vy = -p.vy * 0.45f
            } else {
                p.vy = 0f
            }
            if (!p.kind.rolls) p.spin *= 0.6f       // chapeau et skis tournent moins après chaque choc
        }

        // Les boules tournent comme si elles roulaient ; le reste tourne sur lui-même
        p.angle += if (p.kind.rolls) p.vx / r * 57.3f * dt else p.spin * dt
    }

    val allGone = crashParts.all { abs(it.x - width / 2f) > width / 2f + 60f * d }
    if (allGone || crashTime > CRASH_MAX_TIME) {
        gameState = GameState.GAME_OVER
        gameOverAt = System.currentTimeMillis()
        music.play(Sfx.SAD_TROMBONE)          // "wah wah wah waaah" en même temps que "Perdu !"
    }
}
