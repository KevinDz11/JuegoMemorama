package com.example.juegoks_memorama // Tu nombre de paquete

import androidx.compose.material.icons.Icons // Importación genérica de íconos
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
    // Estos íconos SÍ están en el paquete básico "Icons.Default"
    val icons = listOf(
        Icons.Default.Favorite,
        Icons.Default.Face,
        Icons.Default.Build,
        Icons.Default.Star,
        Icons.Default.Lock,
        Icons.Default.Send
    )
}