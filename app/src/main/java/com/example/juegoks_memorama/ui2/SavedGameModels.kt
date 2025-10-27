package com.example.juegoks_memorama.ui2

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

// Usamos @Serializable para decirle a la librería que esto se puede convertir a JSON

@Serializable
data class SavedCardState(
    val iconIndex: Int, // En lugar del ImageVector, guardamos su índice de la lista original
    val isFaceUp: Boolean,
    val isMatched: Boolean,
    val id: Int // Mantenemos el ID original para la reconstrucción
)

@Serializable
data class SavedGameState(
    val cards: List<SavedCardState>,
    val score: Int
    // No guardamos 'selectedCards' ni 'isCheckingMatch' para simplificar.
    // El juego se cargará con todas las cartas boca abajo (excepto las emparejadas).
)