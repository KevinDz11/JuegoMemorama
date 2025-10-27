package com.example.juegoks_memorama.ui2 // Tu paquete ui2

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.juegoks_memorama.GameUiState // Importa tu modelo

@Composable
fun GameScreen(
    state: GameUiState,
    onCardClick: (Int) -> Unit,
    onResetClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Texto del puntaje
        Text(
            text = "Pares encontrados: ${state.score}",
            style = MaterialTheme.typography.headlineMedium
        )

        // El tablero de juego
        LazyVerticalGrid(
            columns = GridCells.Fixed(4), // Cuadrícula de 4 columnas
            modifier = Modifier.weight(1f)
        ) {
            items(state.cards.size) { index ->
                val card = state.cards[index]
                MemoryCardItem( // Llama a la carta del mismo paquete
                    card = card,
                    onClick = { onCardClick(index) }
                )
            }
        }

        // Botón de reiniciar
        Button(onClick = onResetClick) {
            Text(text = "Reiniciar Juego")
        }
    }
}