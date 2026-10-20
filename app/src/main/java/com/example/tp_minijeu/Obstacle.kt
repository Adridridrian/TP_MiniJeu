package com.example.tp_minijeu

import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

internal class Obstacle(var x: Float, var y: Float, val isTree: Boolean, val size: Float) {
    val shape = FloatArray(8) { 0.8f + Random.nextFloat() * 0.35f }   // forme du rocher
}

// Géométrie partagée entre le dessin (ObstacleRenderer.kt) et les collisions (Hitbox.kt)

/** Les 3 étages du sapin posé en (x, y) de taille s : (bas, haut, demi-largeur). */
internal fun treeTiers(y: Float, s: Float) = listOf(
    Triple(y - 0.35f * s, y - 1.5f * s, s),
    Triple(y - 0.95f * s, y - 1.95f * s, 0.8f * s),
    Triple(y - 1.45f * s, y - 2.4f * s, 0.6f * s)
)

/** Les 8 sommets du rocher dessiné en x (forme irrégulière). */
internal fun rockPoints(o: Obstacle, x: Float): HitPolygon {
    val y = o.y; val r = o.size
    val xs = FloatArray(8)
    val ys = FloatArray(8)
    for (i in 0 until 8) {
        val a = i * PI / 4
        xs[i] = x + (cos(a) * r * o.shape[i]).toFloat()
        ys[i] = y - 0.2f * r + (sin(a) * r * 0.65f * o.shape[i]).toFloat()
    }
    return HitPolygon(xs, ys)
}
