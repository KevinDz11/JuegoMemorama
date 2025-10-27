package com.example.juegoks_memorama // Tu nombre de paquete

// --- CORRECCIÓN AQUÍ ---
import androidx.compose.material3.icons.Icons // Importación de Material 3
import androidx.compose.material3.icons.filled.* // Importación de íconos "Filled"
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
    // --- CORRECCIÓN AQUÍ ---
    val icons = listOf(
        Icons.Filled.Favorite, // Se usa "Filled"
        Icons.Filled.Face,
        Icons.Filled.Build,
        Icons.Filled.Star,
        Icons.Filled.Lock,
        Icons.Filled.Send
    )
}