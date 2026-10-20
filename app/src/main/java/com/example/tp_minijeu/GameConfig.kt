package com.example.tp_minijeu

// Réglages qui ne dépendent pas de la densité de l'écran
// (ceux qui en dépendent, comme speedFactor et baseScroll, sont dans GameView)

// ---------- Réglages du jeu ----------
internal val deadZone = 0.6f           // inclinaison ignorée (m/s²) : le téléphone n'est jamais parfaitement à plat
internal val maxDifficulty = 1f        // 1f = vitesse constante
internal val secondsToMax = 45f

// ---------- Collisions ----------
internal val hitboxGrace = 1f          // tolérance (dp) : il faut toucher l'obstacle d'au moins 1dp pour perdre
internal val showHitboxes = false      // true = dessine les zones de collision en rouge (pour régler)

// ---------- Réglages du mode tactile ----------
internal val touchFollow = 8f          // vitesse à laquelle le bonhomme rejoint le doigt (plus haut = plus nerveux)
internal val touchMaxTilt = 9f         // vitesse max, équivalente à une inclinaison de 9 m/s² en mode immersif

// ---------- Réglages du micro ----------
internal val micThreshold = 0.7f       // position de la barre (0..1) : au-dessus, la vitesse augmente
internal val speedStep = 0.1f          // +10 % de vitesse à chaque dépassement
internal val speedCooldownTime = 1f    // délai mini (s) entre deux augmentations
internal val maxSpeed = 3f             // vitesse maximum (x3)

internal val micAttack = 8f            // vitesse de montée de la jauge (plus haut = plus réactif)
internal val micRelease = 2.5f         // vitesse de descente de la jauge (plus bas = plus doux)

// ---------- Bornes des paramètres modifiables par le joueur ----------
internal val moveMin = 0.25f; internal val moveMax = 2f     // sensibilité mouvement : 25 % .. 200 %
internal val micMin = 0.5f;   internal val micMax = 2f      // sensibilité micro : 50 % (facile) .. 200 % (difficile)
