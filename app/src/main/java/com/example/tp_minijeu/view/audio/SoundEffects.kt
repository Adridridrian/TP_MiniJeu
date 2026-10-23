package com.example.tp_minijeu.view.audio

import java.util.concurrent.ConcurrentLinkedQueue
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.pow
import kotlin.math.sin
import kotlin.random.Random

/** Les bruitages du jeu et leur durée (secondes). Tous synthétisés en direct. */
internal enum class Sfx(val seconds: Float, val gain: Float) {
    CRASH_TREE(0.7f, 1f),       // choc contre un sapin
    CRASH_ROCK(0.7f, 1f),       // choc contre un rocher
    POP(0.2f, 0.7f),            // le bonhomme éclate en morceaux
    THUD(0.3f, 0.6f),           // un morceau retombe dans la neige
    SAD_TROMBONE(2.3f, 0.55f),  // "wah wah wah waaah" : écran Perdu !
    MAGIC_UP(1f, 0.6f),         // transformation en Père Noël (arpège qui monte)
    MAGIC_DOWN(1f, 0.6f),       // retour au bonhomme de neige (arpège qui descend)
    HO_HO_HO(1.1f, 0.9f),       // le Père Noël arrive
    SLEIGH_BELLS(0.8f, 0.5f)    // grelots du traîneau qui se renverse
}

/**
 * Synthétiseur des bruitages, appelé par le thread audio de ChristmasMusic (un échantillon à la fois).
 * Le jeu demande un son avec play() depuis son propre thread : la file est thread-safe.
 */
internal class SoundEffects(private val rate: Int) {

    private class Request(val sfx: Sfx, val delay: Float, val volume: Float)

    private inner class Voice(val sfx: Sfx, val gain: Float, delay: Float) {
        var t = -(delay * rate).toInt()        // < 0 : le son attend son délai
        val length = (sfx.seconds * rate).toInt()
        var phase = 0.0                        // oscillateur principal (0..1)
        var lowpass = 0.0
        var lowpass2 = 0.0
        var prevNoise = 0.0
        val filters = DoubleArray(12)          // états des filtres passe-bande (3 filtres x 4 valeurs)
    }

    private val pending = ConcurrentLinkedQueue<Request>()
    private val voices = mutableListOf<Voice>()

    /** Demande un bruitage. delay en secondes, volume de 0 à 1. */
    fun play(sfx: Sfx, delay: Float = 0f, volume: Float = 1f) {
        pending.add(Request(sfx, delay, volume))
    }

    /** Thread audio : récupère les bruitages demandés (une fois par bloc). */
    fun collect() {
        while (true) {
            val r = pending.poll() ?: break
            voices.add(Voice(r.sfx, r.sfx.gain * r.volume, r.delay))
        }
    }

    /** Thread audio : somme des bruitages en cours pour l'échantillon suivant. */
    fun next(): Double {
        var out = 0.0
        val it = voices.iterator()
        while (it.hasNext()) {
            val v = it.next()
            if (v.t >= 0) out += sample(v, v.t.toDouble() / rate) * v.gain
            v.t++
            if (v.t >= v.length) it.remove()
        }
        return out
    }

    // ======================= SONS =======================
    private fun sample(v: Voice, t: Double): Double = when (v.sfx) {
        Sfx.CRASH_TREE -> {
            // Choc sourd dans le tronc + branches qui craquent + paquet de neige qui tombe
            val thud = sin(2 * PI * (55 * t + 90 * 0.04 * (1 - exp(-t / 0.04)))) * exp(-t / 0.18)
            val n = noise()
            val crackle = (n - v.prevNoise) * exp(-t / 0.05) * (if (Random.nextDouble() < 0.15) 1.0 else 0.3)
            v.prevNoise = n
            v.lowpass += 0.06 * (n - v.lowpass)              // bruit étouffé = neige
            val snow = v.lowpass * 4 * exp(-t / 0.35) * (t / 0.03).coerceAtMost(1.0)
            thud * 0.9 + crackle * 0.5 + snow * 0.6
        }
        Sfx.CRASH_ROCK -> {
            // Impact dur : coup grave + claquement aigu + éclats de pierre
            val hit = sin(2 * PI * (70 * t + 160 * 0.025 * (1 - exp(-t / 0.025)))) * exp(-t / 0.12)
            val n = noise()
            val crack = (n - v.prevNoise) * exp(-t / 0.03)
            v.prevNoise = n
            v.lowpass += 0.15 * (n - v.lowpass)
            val debris = v.lowpass * 2.5 * exp(-t / 0.2) * (if (Random.nextDouble() < 0.08) 1.0 else 0.2)
            hit * 0.9 + crack * 0.7 + debris * 0.5
        }
        Sfx.POP -> {
            // "Plop" de bouchon : sinus qui chute très vite + petit clic
            v.phase += (200 + 900 * exp(-t / 0.02)) / rate
            sin(2 * PI * v.phase) * exp(-t / 0.05) + noise() * 0.5 * exp(-t / 0.004)
        }
        Sfx.THUD -> {
            // Boule de neige qui retombe : "pouf" grave et étouffé
            val thud = sin(2 * PI * (60 * t + 80 * 0.03 * (1 - exp(-t / 0.03)))) * exp(-t / 0.09)
            v.lowpass += 0.08 * (noise() - v.lowpass)
            thud * 0.8 + v.lowpass * 3 * exp(-t / 0.12) * 0.5
        }
        Sfx.SAD_TROMBONE -> sadTrombone(v, t)
        Sfx.MAGIC_UP -> magic(t, intArrayOf(84, 88, 91, 96, 100))
        Sfx.MAGIC_DOWN -> magic(t, intArrayOf(100, 96, 91, 88, 84))
        Sfx.HO_HO_HO -> hoHoHo(v, t)
        Sfx.SLEIGH_BELLS -> {
            // Grelots secoués : un coup toutes les 65 ms, chacun un peu plus ou moins fort
            val strike = (t / 0.065).toInt()
            val ts = t - strike * 0.065
            val amp = 0.5 + 0.5 * ((strike * 7919) % 13) / 13.0
            val ring = (sin(2 * PI * 2900 * t) + 0.7 * sin(2 * PI * 4100 * t) + 0.5 * sin(2 * PI * 5300 * t)) * exp(-ts / 0.05)
            val n = noise()
            val click = (n - v.prevNoise) * exp(-ts / 0.01)
            v.prevNoise = n
            (ring * 0.4 + click * 0.6) * amp * exp(-t / 0.4)
        }
    }

    /** Trombone triste : si bémol, la, la bémol, puis un sol long qui vibre. Effet "wah" par filtre. */
    private fun sadTrombone(v: Voice, t: Double): Double {
        val notes = intArrayOf(58, 57, 56, 55)
        val starts = doubleArrayOf(0.0, 0.42, 0.84, 1.26)
        val durs = doubleArrayOf(0.38, 0.38, 0.38, 0.95)
        var i = starts.indexOfLast { t >= it }
        if (i < 0) i = 0
        val tn = t - starts[i]
        val dur = durs[i]
        val last = i == notes.size - 1

        var f = 440.0 * 2.0.pow((notes[i] - 69) / 12.0)
        if (last) f *= 1 + 0.025 * sin(2 * PI * 5.5 * tn) * (tn / 0.2).coerceAtMost(1.0)
        v.phase += f / rate
        val saw = 2 * (v.phase % 1.0) - 1

        // "Wah" : le filtre s'ouvre puis se referme sur chaque note (la main dans le pavillon)
        val open = sin(PI * (tn / dur).coerceIn(0.0, 1.0)).pow(0.7)
        val cutoff = 250 + 1300 * (if (last) exp(-tn / 0.6) else open)
        val a = 1 - exp(-2 * PI * cutoff / rate)
        v.lowpass += a * (saw - v.lowpass)
        v.lowpass2 += a * (v.lowpass - v.lowpass2)

        val attack = (tn / 0.03).coerceAtMost(1.0)
        val release = if (tn > dur) (1 - (tn - dur) / 0.05).coerceAtLeast(0.0) else 1.0
        return v.lowpass2 * attack * release * 1.4
    }

    /** Arpège de clochettes magiques (notes MIDI, une toutes les 70 ms). */
    private fun magic(t: Double, notes: IntArray): Double {
        var out = 0.0
        for ((i, note) in notes.withIndex()) {
            val tn = t - i * 0.07
            if (tn < 0) continue
            val f = 440.0 * 2.0.pow((note - 69) / 12.0)
            val tone = sin(2 * PI * f * tn + exp(-tn / 0.1) * sin(2 * PI * 2 * f * tn))
            out += tone * exp(-tn / 0.35) * (tn / 0.003).coerceAtMost(1.0)
        }
        return out * 0.35
    }

    /** "Ho ho ho !" : voix grave (dent de scie) passée dans les formants de la voyelle "o". */
    private fun hoHoHo(v: Voice, t: Double): Double {
        val ho = (t / 0.33).toInt().coerceAtMost(2)
        val tn = t - ho * 0.33
        val dur = 0.24
        if (tn > dur + 0.06) return 0.0

        // Voix : grave et qui descend un peu sur chaque "ho"
        val f0 = 105 * (1 - 0.12 * (tn / dur).coerceAtMost(1.0))
        v.phase += f0 / rate
        val voice = 2 * (v.phase % 1.0) - 1
        val voiced = ((tn - 0.03) / 0.03).coerceIn(0.0, 1.0) *
            (if (tn > dur) (1 - (tn - dur) / 0.06).coerceAtLeast(0.0) else 1.0)
        // Le "h" : un souffle au début de chaque "ho"
        val breath = noise() * exp(-tn / 0.04)
        val src = voice * voiced + breath * 0.6

        // Formants du "o" (450, 800 et 2600 Hz)
        val o = bandpass(v, 0, src, 450.0, 5.0) + 0.6 * bandpass(v, 4, src, 800.0, 6.0) +
            0.25 * bandpass(v, 8, src, 2600.0, 8.0)
        return o * 3.0
    }

    // ======================= OUTILS =======================
    private fun noise() = Random.nextDouble(-1.0, 1.0)

    /** Filtre passe-bande (biquad, gain 1 au centre). base = position de ses 4 états dans v.filters. */
    private fun bandpass(v: Voice, base: Int, x: Double, fc: Double, q: Double): Double {
        val w0 = 2 * PI * fc / rate
        val alpha = sin(w0) / (2 * q)
        val a0 = 1 + alpha
        val b0 = alpha / a0
        val a1 = -2 * cos(w0) / a0
        val a2 = (1 - alpha) / a0
        val s = v.filters
        val y = b0 * x - b0 * s[base + 1] - a1 * s[base + 2] - a2 * s[base + 3]
        s[base + 1] = s[base]; s[base] = x          // x1, x2 (s[base] = x1, s[base+1] = x2)
        s[base + 3] = s[base + 2]; s[base + 2] = y  // y1, y2
        return y
    }
}
