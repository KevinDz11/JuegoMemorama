package com.example.juegoks_memorama.model

import kotlinx.serialization.Serializable

@Serializable
sealed class BluetoothMessage {
    // CAMBIO CRUCIAL: Enviamos la lista completa de cartas (IDs o valores)
    // en lugar de solo la semilla, para evitar diferencias en el algoritmo de Random.
    @Serializable
    data class StartGame(
        val difficulty: Difficulty,
        val seed: Long, // Mantenemos seed por si acaso, pero la lista manda
        val cardValues: List<Int> // <-- ESTA ES LA CLAVE
    ) : BluetoothMessage()

    // Cuando un jugador voltea una carta
    @Serializable
    data class FlipCard(val cardId: Int) : BluetoothMessage()

    // Si hubo match (para sincronizar puntajes)
    @Serializable
    data class MatchFound(val card1Id: Int, val card2Id: Int, val scorerIsHost: Boolean, val points: Int) : BluetoothMessage()

    // Para cambiar el turno si no hubo match
    @Serializable
    data class TurnChange(val nextTurnIsHost: Boolean) : BluetoothMessage()

    // Reiniciar (Opcional, por si implementas "Jugar de Nuevo")
    @Serializable
    object RestartGame : BluetoothMessage()
}

// Estados de la conexión (Opcional, útil para UI)
enum class ConnectionState {
    DISCONNECTED,
    CONNECTING,
    CONNECTED
}