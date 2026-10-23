package com.example.tp_minijeu.model

import com.example.tp_minijeu.view.*
import com.example.tp_minijeu.view.audio.*
import com.example.tp_minijeu.controller.*
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

/**
 * Easter egg : toucher le titre sur l'accueil transforme le bonhomme en Père Noël (et inversement).
 * 0 -> 0,4 s : le perso tourne en rapetissant (clochettes magiques)
 * 0,4 s      : "pouf" de neige et d'étoiles, le skin change ("Ho ho ho !" pour le Père Noël)
 * 0,4 -> 1 s : le nouveau perso grandit avec un petit rebond
 */
private const val SHRINK_TIME = 0.4f
private const val TOTAL_TIME = 1f

internal fun GameView.startTransform() {
    if (transformTime >= 0f) return              // déjà en cours
    transformTime = 0f
    transformSwitched = false
    music.play(if (SantaSkin.enabled) Sfx.MAGIC_DOWN else Sfx.MAGIC_UP)
}

internal fun GameView.updateTransform(dt: Float) {
    // Étoiles dorées : elles retombent doucement et s'éteignent
    for (p in magicSparkles) { p.vy += 120f * d * dt; p.x += p.vx * dt; p.y += p.vy * dt; p.life -= dt }
    magicSparkles.removeAll { it.life <= 0f }

    if (transformTime < 0f) return
    transformTime += dt
    if (!transformSwitched && transformTime >= SHRINK_TIME) {
        transformSwitched = true
        SantaSkin.enabled = !SantaSkin.enabled
        music.play(Sfx.POP)
        if (SantaSkin.enabled) music.play(Sfx.HO_HO_HO, delay = 0.15f)
        magicPuff()
    }
    if (transformTime >= TOTAL_TIME) transformTime = -1f
}

/** Échelle et rotation (degrés) du perso pendant la transformation : (1, 0) au repos. */
internal fun GameView.transformPose(): Pair<Float, Float> {
    val t = transformTime
    if (t < 0f) return 1f to 0f
    return if (t < SHRINK_TIME) {
        val k = t / SHRINK_TIME
        (1f - k * k) to 540f * k * k                            // rétrécit en tournant de plus en plus vite
    } else {
        val k = ((t - SHRINK_TIME) / (TOTAL_TIME - SHRINK_TIME)).coerceIn(0f, 1f)
        // "Ease out back" : dépasse un peu la taille normale puis revient (effet ressort)
        val c1 = 1.70158f
        val m = k - 1f
        val scale = 1f + (c1 + 1f) * m * m * m + c1 * m * m
        scale to -180f * m * m                                  // finit de tourner en ralentissant
    }
}

/** Le "pouf" : gerbe de neige et étoiles dorées au milieu du perso. */
private fun GameView.magicPuff() {
    val cx = width / 2f
    val cy = startPlayerY() - 35f * s
    repeat(30) {
        val a = Random.nextFloat() * 6.28f
        val v = (80f + Random.nextFloat() * 160f) * d
        particles.add(Particle(cx, cy, cos(a) * v, sin(a) * v, 0.5f))
    }
    repeat(16) {
        val a = Random.nextFloat() * 6.28f
        val v = (60f + Random.nextFloat() * 140f) * d
        magicSparkles.add(Particle(cx, cy, cos(a) * v, sin(a) * v - 60f * d, 0.6f + Random.nextFloat() * 0.5f))
    }
}
