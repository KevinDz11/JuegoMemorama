package com.example.juegoks_memorama

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Star
import androidx.compose.ui.graphics.vector.ImageVector

// 1. Define cómo es una sola carta
data class MemoryCard(
    val id: Int,
    val imageVector: ImageVector,
    val isFaceUp: Boolean = false,
    val isMatched: Boolean = false
)

// 2. Define cómo se ve todo el estado de la UI
data class GameUiState(
    val cards: List<MemoryCard> = emptyList(),
    val selectedCards: List<Int> = emptyList(), // Los índices de las cartas seleccionadas
    val isCheckingMatch: Boolean = false, // Para bloquear clics mientras se revisa
    val score: Int = 0
)

// 3. Lista de iconos que usaremos para los pares
object CardValues {
    val icons = listOf(
        Icons.Default.Favorite,
        Icons.Default.Face,
        Icons.Default.Build,
        Icons.Default.Star,
        Icons.Default.Lock,
        Icons.Default.Send
        // Puedes agregar más pares si quieres (ej. 8 pares para una cuadrícula de 4x4)
        // Icons.Default.Add,
        // Icons.Default.Call
    )
}