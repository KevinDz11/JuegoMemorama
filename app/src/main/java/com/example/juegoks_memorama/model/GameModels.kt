@file:OptIn(kotlinx.serialization.InternalSerializationApi::class)
package com.example.juegoks_memorama.model
import kotlinx.serialization.Serializable

@Serializable
data class Card(
    val id: Int,
    val value: Int,
    var isFaceUp: Boolean = false,
    var isMatched: Boolean = false
)
@Serializable
data class GameState(
    val cards: List<Card> = emptyList(),
    val moves: Int = 0,
    val matchedPairs: Int = 0,
    val gameCompleted: Boolean = false,
    val elapsedTimeInSeconds: Long = 0,
    val isTimerRunning: Boolean = false
)