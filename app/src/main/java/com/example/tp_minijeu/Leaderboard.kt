package com.example.tp_minijeu

import android.content.SharedPreferences
import androidx.core.content.edit

/**
 * Top 10 local des scores, sauvegardé dans les SharedPreferences.
 * SharedPreferences ne stocke pas de listes : on sauvegarde une chaîne "score:date;score:date;..."
 */
internal class Leaderboard(private val prefs: SharedPreferences) {
    val entries = mutableListOf<Pair<Int, Long>>()   // (score en m, date en ms)

    fun load() {
        entries.clear()
        val saved = prefs.getString("leaderboard", "") ?: ""
        saved.split(";")
            .filter { it.isNotBlank() }          // "" (premier lancement) donnerait une entrée vide
            .forEach { item ->
                val parts = item.split(":")
                val score = parts.getOrNull(0)?.toIntOrNull()
                val date = parts.getOrNull(1)?.toLongOrNull()
                if (score != null && date != null) entries.add(Pair(score, date))   // ignore une ligne abîmée
            }
    }

    /** Ajoute un score et renvoie sa position (0 = premier), ou null s'il n'entre pas dans le top 10. */
    fun add(score: Int): Int? {
        val newEntry = Pair(score, System.currentTimeMillis())
        entries.add(newEntry)
        entries.sortByDescending { it.first }
        while (entries.size > 10) entries.removeAt(entries.size - 1)
        save()
        // indexOf compare le score ET la date : on retrouve bien cette partie, même en cas d'égalité
        return entries.indexOf(newEntry).takeIf { it >= 0 }
    }

    private fun save() {
        prefs.edit {
            putString("leaderboard", entries.joinToString(";") { "${it.first}:${it.second}" })
        }
    }
}
