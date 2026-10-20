package com.example.tp_minijeu

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.os.Process
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.exp
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.tanh
import java.util.concurrent.ConcurrentLinkedQueue
import kotlin.random.Random

/**
 * Musique du jeu : "Jingle Bells" en drum & bass, synthétisée en direct (aucun fichier audio).
 * Le BPM suit le multiplicateur de vitesse du jeu, sans changer la hauteur des notes.
 * La mélodie tourne en continu (menus compris) ; la batterie ne joue que pendant la partie.
 *
 * Grille : 16 pas (doubles croches) par mesure, la batterie et la mélodie partagent la même grille.
 * Les durées des notes sont comptées en pas : elles restent justes même quand le tempo change.
 */
internal class ChristmasMusic {

    @Volatile var drums = false           // true = batterie et grelots (partie en cours)
    @Volatile var speed = 1f              // multiplicateur de vitesse du jeu (x1 .. x3)
    @Volatile var volume = musicVolume    // volume choisi dans les paramètres (0..1)
    @Volatile private var running = false
    private var thread: Thread? = null

    // Bruitages demandés par le jeu, joués par le thread audio dès le prochain bloc (~5 ms)
    private val pendingSfx = ConcurrentLinkedQueue<Kind>()

    /** Bruit d'impact contre un obstacle (sapin ou rocher). */
    fun playCrash(isTree: Boolean) {
        if (running) pendingSfx.add(if (isTree) Kind.CRASH_TREE else Kind.CRASH_ROCK)
    }

    fun start() {
        if (running) return
        running = true
        thread = Thread { loop() }.apply { start() }
    }

    fun stop() {
        running = false
        thread?.join(500)
        thread = null
    }

    // ======================= PARTITION =======================
    // Mélodie relevée dans Jingle-Bells-1.mid (canal 3 pour le couplet, canal 5 pour le refrain),
    // mesures 5 à 36 du fichier, sans les notes d'harmonie, avec son rythme exact
    // (dont les notes en avance d'une double croche, ex. le "Jin-" du 2e refrain).
    private companion object {
        const val RATE = 48000              // fréquence native de la plupart des téléphones (chemin audio rapide)
        const val BLOCK = 240               // échantillons calculés à la fois : 5 ms
        const val STEPS_PER_BAR = 16                        // une noire = 4 pas, comme dans le MIDI
        const val MELODY_BARS = 32                          // couplet (16) + refrain (16)
        const val BREAK_BARS = 2                            // pause sans mélodie avant de reboucler
        const val SONG_STEPS = (MELODY_BARS + BREAK_BARS) * STEPS_PER_BAR

        // Notes MIDI utilisées par la mélodie
        const val G4 = 67; const val A4 = 69; const val B4 = 71
        const val C5 = 72; const val D5 = 74; const val E5 = 76; const val F5 = 77; const val G5 = 79; const val A5 = 81

        /** Une note : mesure, pas dans la mesure (0..15), durée en pas, note MIDI. */
        fun n(bar: Int, step: Int, len: Int, note: Int) = intArrayOf(bar, step, len, note)

        val MELODY = arrayOf(
            // ----- Couplet -----
            n(0, 0, 4, G4), n(0, 4, 4, E5), n(0, 8, 4, D5), n(0, 12, 4, C5),        // Dash-ing through the
            n(1, 0, 15, G4),                                                        // snow,
            n(2, 0, 4, G4), n(2, 4, 4, E5), n(2, 8, 4, D5), n(2, 12, 4, C5),        // in a one-horse o-
            n(3, 0, 12, A4),                                                        // pen sleigh,
            n(4, 0, 4, A4), n(4, 4, 4, F5), n(4, 8, 4, E5), n(4, 12, 3, D5),        // o'er the fields we
            n(5, 0, 14, B4),                                                        // go,
            n(6, 0, 3, G5), n(6, 4, 3, G5), n(6, 7, 7, F5), n(6, 14, 2, D5),        // laugh-ing all the
            n(7, 0, 8, E5), n(7, 12, 3, G4),                                        // way.
            n(8, 0, 4, G4), n(8, 4, 4, E5), n(8, 8, 3, D5), n(8, 12, 4, C5),        // Bells on bob-tail
            n(9, 0, 11, G4), n(9, 12, 3, G4),                                       // ring,
            n(10, 0, 4, G4), n(10, 4, 4, E5), n(10, 8, 4, D5), n(10, 12, 3, C5),    // mak-ing spir-its
            n(10, 15, 12, A4),                                                      // bright,
            n(11, 12, 3, A4),
            n(12, 0, 4, A4), n(12, 4, 4, F5), n(12, 8, 4, E5), n(12, 12, 3, D5),    // what fun it is to
            n(12, 15, 4, B4),
            n(13, 0, 3, G5), n(13, 4, 3, G5), n(13, 8, 3, G5), n(13, 12, 4, G5),    // ride and sing a
            n(14, 0, 4, A5), n(14, 4, 4, G5), n(14, 8, 4, F5), n(14, 12, 4, D5),    // sleigh-ing song to-
            n(15, 0, 10, C5),                                                       // night!
            // ----- Refrain -----
            n(16, 0, 3, E5), n(16, 4, 3, E5), n(16, 8, 7, E5),                      // Jin-gle bells,
            n(17, 0, 3, E5), n(17, 4, 3, E5), n(17, 8, 7, E5),                      // jin-gle bells,
            n(18, 0, 4, E5), n(18, 4, 3, G5), n(18, 8, 6, C5), n(18, 14, 2, D5),    // jin-gle all the
            n(19, 0, 14, E5), n(19, 14, 1, C5), n(19, 15, 1, D5),                   // way,
            n(20, 0, 4, F5), n(20, 4, 3, F5), n(20, 8, 5, F5), n(20, 14, 1, F5),    // oh what fun it
            n(21, 0, 4, F5), n(21, 4, 3, E5), n(21, 8, 3, E5), n(21, 12, 3, E5),    // is to ride in a
            n(22, 0, 4, E5), n(22, 4, 3, D5), n(22, 8, 3, D5), n(22, 12, 3, E5),    // one-horse o-pen
            n(23, 0, 14, D5), n(23, 14, 1, B4), n(23, 15, 4, E5),                   // sleigh, (Jin-)
            n(24, 4, 3, E5), n(24, 8, 7, E5),                                       // -gle bells,
            n(25, 0, 3, E5), n(25, 4, 3, E5), n(25, 8, 7, E5),                      // jin-gle bells,
            n(26, 0, 5, E5), n(26, 4, 4, G5), n(26, 8, 7, C5), n(26, 14, 2, D5),    // jin-gle all the
            n(27, 0, 14, E5), n(27, 14, 1, D5), n(27, 15, 4, F5),                   // way, (oh)
            n(28, 4, 3, F5), n(28, 8, 5, F5), n(28, 14, 1, F5), n(28, 15, 5, F5),   // what fun it is
            n(29, 3, 3, E5), n(29, 7, 4, E5), n(29, 12, 3, E5),                     // to ride in a
            n(30, 0, 3, G5), n(30, 4, 3, G5), n(30, 8, 4, F5), n(30, 11, 5, D5),    // one-horse o-pen
            n(31, 0, 8, C5)                                                         // sleigh!
        )

        // Basse : fondamentale de chaque mesure, relevée sur la basse du MIDI (canal 1), pause comprise
        private const val BC = 36; private const val BD = 38; private const val BF = 41; private const val BG = 43
        val BASS_ROOTS = intArrayOf(
            BC, BC, BC, BF, BF, BG, BG, BC, BC, BC, BC, BF, BF, BG, BG, BC,     // couplet
            BC, BC, BC, BC, BF, BC, BD, BG, BC, BC, BC, BC, BF, BC, BG, BC,     // refrain
            BC, BG                                                              // pause
        )

        // Batterie sur une mesure de 16 pas (rythme "two-step" typique de la DnB)
        val KICK_STEPS = setOf(0, 10)
        val SNARE_STEPS = setOf(4, 12)
        val GHOST_STEPS = setOf(7, 15)       // petites caisses claires discrètes
        // Basse sur une mesure de 16 pas : (pas, durée en pas, octave en plus)
        val BASS_HITS = listOf(Triple(0, 6, 0), Triple(6, 4, 0), Triple(10, 4, 0), Triple(14, 2, 12))

        fun freq(midi: Int) = 440.0 * 2.0.pow((midi - 69) / 12.0)
    }

    // Mélodie rangée par pas : note et durée (en pas), -1 = rien
    private val melodyNote = IntArray(SONG_STEPS) { -1 }
    private val melodyLen = IntArray(SONG_STEPS)

    init {
        for ((bar, step, len, note) in MELODY) {
            val s = bar * STEPS_PER_BAR + step
            melodyNote[s] = note
            melodyLen[s] = len
        }
    }

    // ======================= SYNTHÉ =======================
    private enum class Kind { KICK, SNARE, JINGLE, LEAD, BELL, BASS, CRASH_TREE, CRASH_ROCK }

    /**
     * Un son en cours.
     * Batterie : dure "length" échantillons.
     * Notes (lead, cloche, basse) : tenues pendant "gateSteps" pas, puis relâchées en "release" échantillons.
     */
    private class Voice(val kind: Kind, val freq: Double, val gain: Float, val length: Int = 0, var gateSteps: Int = 0) {
        var t = 0                 // échantillons depuis le début du son
        var releasedAt = -1       // échantillon où la note a été relâchée (-1 = encore tenue)
        var phase = 0.0           // phase de l'oscillateur du lead (0..1)
        var lowpass = 0.0         // état du filtre (basse et lead)
        var prevNoise = 0.0       // pour adoucir le bruit (filtre passe-haut simple)
        val release = when (kind) {
            Kind.LEAD -> RATE * 2 / 100    // 20 ms : la note s'arrête net
            Kind.BELL -> RATE * 6 / 100
            Kind.BASS -> RATE * 4 / 100
            else -> 0
        }
        val isNote get() = kind == Kind.LEAD || kind == Kind.BELL || kind == Kind.BASS

        fun releaseNow() { if (releasedAt < 0) releasedAt = t }

        /** Volume de l'enveloppe de fin (1 = tenue, 0 = fini). */
        fun releaseLevel() = if (releasedAt < 0) 1.0 else (1.0 - (t - releasedAt).toDouble() / release).coerceAtLeast(0.0)

        val finished get() = if (isNote) releasedAt >= 0 && t - releasedAt >= release else t >= length
    }

    private val voices = mutableListOf<Voice>()
    private val sfx = mutableListOf<Voice>()      // bruitages : mixés à part, hors volume de la musique
    private var step = 0
    private var sampleInStep = 0
    private var samplesPerStep = 1

    private fun bpm() = musicBaseBpm * (1f + (speed - 1f) * musicTempoFollow)

    /** Déclenche les sons du pas en cours. */
    private fun triggerStep() {
        samplesPerStep = (RATE * 60f / (bpm() * 4f)).toInt().coerceAtLeast(1)
        val s = step % SONG_STEPS
        val inBar = s % 16

        // Fin des notes dont la durée (en pas) est écoulée
        for (v in voices) {
            if (v.isNote && v.releasedAt < 0) {
                v.gateSteps--
                if (v.gateSteps <= 0) v.releaseNow()
            }
        }

        // Batterie et grelots : seulement pendant la partie
        if (drums) {
            if (inBar in KICK_STEPS) voices.add(Voice(Kind.KICK, 0.0, 0.75f, length = RATE * 3 / 10))
            if (inBar in SNARE_STEPS) voices.add(Voice(Kind.SNARE, 0.0, 0.5f, length = RATE / 5))
            if (inBar in GHOST_STEPS) voices.add(Voice(Kind.SNARE, 0.0, 0.12f, length = RATE / 10))
            // Grelots (sleigh bells) sur chaque pas, plus forts un pas sur deux
            voices.add(Voice(Kind.JINGLE, 0.0, if (inBar % 2 == 0) 0.1f else 0.04f, length = RATE / 12))
        }

        val root = BASS_ROOTS[s / STEPS_PER_BAR]
        for ((at, len, octave) in BASS_HITS) {
            if (inBar == at) voices.add(Voice(Kind.BASS, freq(root + octave), 0.26f, gateSteps = len))
        }

        if (melodyNote[s] >= 0) {
            // Une seule note de mélodie à la fois : la nouvelle coupe la précédente
            for (v in voices) if (v.kind == Kind.LEAD || v.kind == Kind.BELL) v.releaseNow()

            val f = freq(melodyNote[s])
            val len = melodyLen[s]
            // Lead : tenu exactement la durée du MIDI (qui contient déjà le détaché entre les notes)
            voices.add(Voice(Kind.LEAD, f, 0.42f, gateSteps = len))
            // Cloche par-dessus pour le côté Noël
            voices.add(Voice(Kind.BELL, f, 0.16f, gateSteps = len))
        }
    }

    private fun voiceSample(v: Voice): Double {
        val t = v.t.toDouble() / RATE
        return when (v.kind) {
            Kind.KICK -> {
                // Sinus dont la hauteur descend vite (120 Hz -> 45 Hz)
                val phase = 2 * PI * (45 * t + 120 * 0.03 * (1 - exp(-t / 0.03)))
                sin(phase) * exp(-t / 0.12)
            }
            Kind.SNARE -> {
                val n = Random.nextDouble(-1.0, 1.0)
                val hp = n - 0.5 * v.prevNoise
                v.prevNoise = n
                hp * 0.7 * exp(-t / 0.06) + sin(2 * PI * 190 * t) * 0.5 * exp(-t / 0.04)
            }
            Kind.JINGLE -> {
                val n = Random.nextDouble(-1.0, 1.0)
                val hp = n - v.prevNoise
                v.prevNoise = n
                hp * exp(-t / 0.025)
            }
            Kind.LEAD -> {
                // Lead clair : triangle + carré adouci, léger vibrato. Hauteur bien nette.
                val vibrato = 1 + 0.003 * sin(2 * PI * 5.5 * t)
                v.phase = (v.phase + v.freq * vibrato / RATE) % 1.0
                val tri = 4 * abs(v.phase - 0.5) - 1
                val square = if (v.phase < 0.5) 1.0 else -1.0
                v.lowpass += 0.2 * (square - v.lowpass)
                val attack = (t / 0.005).coerceAtMost(1.0)
                (tri * 0.7 + v.lowpass * 0.4) * attack * v.releaseLevel()
            }
            Kind.BELL -> {
                // Cloche FM avec un rapport harmonique (x2) : elle sonne juste avec le lead
                val index = 1.2 * exp(-t / 0.15)
                val tone = sin(2 * PI * v.freq * t + index * sin(2 * PI * v.freq * 2 * t))
                val attack = (t / 0.003).coerceAtMost(1.0)
                tone * attack * exp(-t / 0.4) * v.releaseLevel()
            }
            Kind.BASS -> {
                // "Reese" : deux dents de scie légèrement désaccordées + sous-basse, filtrées
                val saw1 = 2 * ((t * v.freq * 0.995) % 1.0) - 1
                val saw2 = 2 * ((t * v.freq * 1.005) % 1.0) - 1
                val cutoff = 250 + 900 * exp(-t / 0.08)
                val a = 1 - exp(-2 * PI * cutoff / RATE)
                v.lowpass += a * ((saw1 + saw2) * 0.5 - v.lowpass)
                val sub = sin(2 * PI * v.freq * t)
                val attack = (t / 0.01).coerceAtMost(1.0)
                (v.lowpass * 0.8 + sub * 0.7) * attack * v.releaseLevel()
            }
            Kind.CRASH_TREE -> {
                // Choc sourd dans le tronc + branches qui craquent + paquet de neige qui tombe
                val thudPhase = 2 * PI * (55 * t + 90 * 0.04 * (1 - exp(-t / 0.04)))
                val thud = sin(thudPhase) * exp(-t / 0.18)
                val n = Random.nextDouble(-1.0, 1.0)
                val crackle = (n - v.prevNoise) * exp(-t / 0.05) * (if (Random.nextDouble() < 0.15) 1.0 else 0.3)
                v.prevNoise = n
                v.lowpass += 0.06 * (n - v.lowpass)            // bruit étouffé = neige
                val snow = v.lowpass * 4 * exp(-t / 0.35) * (t / 0.03).coerceAtMost(1.0)
                thud * 0.9 + crackle * 0.5 + snow * 0.6
            }
            Kind.CRASH_ROCK -> {
                // Impact dur : claquement aigu + coup grave + éclats de pierre
                val hitPhase = 2 * PI * (70 * t + 160 * 0.025 * (1 - exp(-t / 0.025)))
                val hit = sin(hitPhase) * exp(-t / 0.12)
                val n = Random.nextDouble(-1.0, 1.0)
                val crack = (n - v.prevNoise) * exp(-t / 0.03)
                v.prevNoise = n
                v.lowpass += 0.15 * (n - v.lowpass)
                val debris = v.lowpass * 2.5 * exp(-t / 0.2) * (if (Random.nextDouble() < 0.08) 1.0 else 0.2)
                hit * 0.9 + crack * 0.7 + debris * 0.5
            }
        }
    }

    private fun render(out: ShortArray) {
        while (true) {
            val kind = pendingSfx.poll() ?: break
            sfx.add(Voice(kind, 0.0, 1f, length = RATE * 7 / 10))
        }

        for (i in out.indices) {
            if (sampleInStep == 0) triggerStep()
            sampleInStep++
            if (sampleInStep >= samplesPerStep) { sampleInStep = 0; step++ }

            var mix = 0.0
            val it = voices.iterator()
            while (it.hasNext()) {
                val v = it.next()
                mix += voiceSample(v) * v.gain
                v.t++
                if (v.finished) it.remove()
            }
            var effects = 0.0
            val fx = sfx.iterator()
            while (fx.hasNext()) {
                val v = fx.next()
                effects += voiceSample(v) * v.gain
                v.t++
                if (v.finished) fx.remove()
            }

            // Saturation douce pour éviter de grésiller quand tout se superpose
            val sample = (tanh(mix * 0.9) * volume + tanh(effects) * sfxVolume).coerceIn(-1.0, 1.0)
            out[i] = (sample * 32767 * 0.8).toInt().toShort()
        }
    }

    // ======================= THREAD AUDIO =======================
    private fun loop() {
        // Priorité audio : le thread n'est pas interrompu par le reste de l'appli (évite les coupures)
        Process.setThreadPriority(Process.THREAD_PRIORITY_URGENT_AUDIO)

        val minBuf = AudioTrack.getMinBufferSize(
            RATE, AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT
        )
        if (minBuf <= 0) { running = false; return }

        val track = AudioTrack.Builder()
            .setPerformanceMode(AudioTrack.PERFORMANCE_MODE_LOW_LATENCY)
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_GAME)
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .build()
            )
            .setAudioFormat(
                AudioFormat.Builder()
                    .setSampleRate(RATE)
                    .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                    .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                    .build()
            )
            .setBufferSizeInBytes(minBuf)
            .setTransferMode(AudioTrack.MODE_STREAM)
            .build()

        // Le son attend dans le tampon avant d'être joué : plus il est petit, moins le son est en retard.
        // On part du plus petit possible et on l'agrandit seulement si le son se coupe (sous-alimentation).
        val buffer = ShortArray(BLOCK)
        track.setBufferSizeInFrames(2 * BLOCK)
        var underruns = 0
        try {
            track.play()
            while (running) {
                render(buffer)
                track.write(buffer, 0, buffer.size)
                if (track.underrunCount > underruns) {
                    underruns = track.underrunCount
                    track.setBufferSizeInFrames(minOf(track.bufferSizeInFrames + BLOCK, track.bufferCapacityInFrames))
                }
            }
        } catch (_: Exception) {
            // Sortie audio indisponible : le jeu continue sans musique
        } finally {
            try { track.stop() } catch (_: Exception) {}
            track.release()
        }
    }
}
