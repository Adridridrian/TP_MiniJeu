package com.example.tp_minijeu.model

// Réglages qui ne dépendent pas de la densité de l'écran
// (ceux qui en dépendent, comme speedFactor et baseScroll, sont dans GameView)

// ---------- Réglages du jeu ----------
internal val deadZone = 0.6f           // inclinaison ignorée (m/s²) : le téléphone n'est jamais parfaitement à plat
internal val maxDifficulty = 1f        // 1f = vitesse constante
internal val secondsToMax = 45f

// ---------- Vies ----------
internal val maxLives = 3              // nombre de vies au début de la partie
internal val invincibleTime = 2f       // secondes d'invincibilité (bonhomme qui clignote) après un choc

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

// ---------- Musique ----------
internal val musicVolume = 0.7f        // volume par défaut de la musique (0..1), réglable dans les paramètres
internal val sfxVolume = 0.9f          // volume des bruitages (impacts), indépendant de la musique
internal val musicBaseBpm = 174f       // tempo à x1 (tempo classique de la drum & bass)
internal val musicTempoFollow = 0.5f   // part du multiplicateur reprise par le BPM : x2 -> 261 BPM, x3 -> 348 BPM
                                       // (1f = BPM proportionnel à la vitesse)

// ---------- Bornes des paramètres modifiables par le joueur ----------
internal val moveMin = 0.25f; internal val moveMax = 2f     // sensibilité mouvement : 25 % .. 200 %
internal val micMin = 0.5f;   internal val micMax = 2f      // sensibilité micro : 50 % (facile) .. 200 % (difficile)
