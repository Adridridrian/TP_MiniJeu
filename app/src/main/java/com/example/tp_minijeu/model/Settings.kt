package com.example.tp_minijeu.model

import com.example.tp_minijeu.controller.*

/** Sauvegarde les paramètres du joueur (relus au lancement dans GameView). */
internal fun GameView.saveSettings() {
    prefs.edit()
        .putFloat("moveSens", moveSens)
        .putInt("difficulty", difficulty)
        .putBoolean("touchMode", touchMode)
        .putFloat("musicVolume", musicVol)
        .apply()
}
