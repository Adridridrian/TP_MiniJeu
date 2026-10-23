package com.example.tp_minijeu.model

/**
 * Skin secret (easter egg : toucher le titre sur l'accueil).
 * Objet global : garde sa valeur tant que l'appli tourne, même si l'écran est recréé.
 * Pas sauvegardé : au prochain lancement, on repart avec le bonhomme de neige.
 */
internal object SantaSkin {
    var enabled = false
}
