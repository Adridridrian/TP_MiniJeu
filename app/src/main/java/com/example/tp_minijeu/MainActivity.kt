package com.example.tp_minijeu

import android.Manifest
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import kotlin.math.log10
import kotlin.math.sqrt

class MainActivity : AppCompatActivity(), SensorEventListener {

    private lateinit var sensorManager: SensorManager
    private var accelerometer: Sensor? = null
    private lateinit var gameView: GameView

    // ---------- Micro ----------
    @Volatile private var micRunning = false
    private var micThread: Thread? = null

    // Demande d'autorisation du micro (fenêtre système au premier lancement)
    private val micPermission = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        gameView.micAvailable = granted
        if (granted) startMic()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        gameView = GameView(this)
        setContentView(gameView)

        sensorManager = getSystemService(SENSOR_SERVICE) as SensorManager
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

        if (hasMicPermission()) {
            gameView.micAvailable = true
        } else {
            micPermission.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    override fun onResume() {
        super.onResume()
        gameView.startGame()
        accelerometer?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_GAME)
        }
        startMic()
    }

    override fun onPause() {
        super.onPause()
        gameView.stopGame()                      // fige le jeu et passe en PAUSE si une partie est en cours
        sensorManager.unregisterListener(this)   // économise la batterie
        stopMic()                                // libère le micro pour les autres applis
    }

    override fun onSensorChanged(event: SensorEvent) {
        // Filtre passe-bas pour éviter les tremblements
        val alpha = 0.2f
        gameView.tilt = gameView.tilt * (1 - alpha) + event.values[0] * alpha
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

    // ======================= MICRO =======================
    private fun hasMicPermission() =
        ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) ==
                PackageManager.PERMISSION_GRANTED

    private fun startMic() {
        if (micRunning || !hasMicPermission()) return

        val sampleRate = 44100
        val minBuf = AudioRecord.getMinBufferSize(
            sampleRate, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT
        )
        if (minBuf <= 0) { gameView.micAvailable = false; return }

        val recorder = try {
            // MIC sans annulation d'écho : le micro entend aussi la musique du jeu,
            // qui peut donc le faire accélérer (difficulté en plus, voulue)
            AudioRecord(
                MediaRecorder.AudioSource.MIC, sampleRate,
                AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT, minBuf * 2
            )
        } catch (e: SecurityException) {
            gameView.micAvailable = false
            return
        }
        if (recorder.state != AudioRecord.STATE_INITIALIZED) {
            recorder.release()
            gameView.micAvailable = false
            return
        }

        gameView.micAvailable = true
        micRunning = true
        micThread = Thread {
            val buffer = ShortArray(1024)          // ~23 ms de son par lecture
            try {
                recorder.startRecording()
                while (micRunning) {
                    val n = recorder.read(buffer, 0, buffer.size)
                    if (n > 0) {
                        // Volume moyen (RMS) du morceau de son
                        var sum = 0.0
                        for (i in 0 until n) {
                            val v = buffer[i].toDouble()
                            sum += v * v
                        }
                        val rms = sqrt(sum / n)
                        // Conversion en décibels : -60 dB (silence) -> 0, 0 dB (très fort) -> 1
                        val db = if (rms > 0) 20 * log10(rms / 32767.0) else -100.0
                        gameView.micLevel = ((db + 60.0) / 60.0).toFloat().coerceIn(0f, 1f)
                    }
                }
            } catch (e: Exception) {
                gameView.micAvailable = false
            } finally {
                try { recorder.stop() } catch (_: Exception) {}
                recorder.release()
            }
        }.apply { start() }
    }

    private fun stopMic() {
        micRunning = false
        micThread?.join(500)
        micThread = null
        gameView.micLevel = 0f
    }
}