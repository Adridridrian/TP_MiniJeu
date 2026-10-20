package com.example.tp_minijeu

import kotlin.random.Random

internal class Obstacle(var x: Float, var y: Float, val isTree: Boolean, val size: Float) {
    val shape = FloatArray(8) { 0.8f + Random.nextFloat() * 0.35f }   // forme du rocher
}
