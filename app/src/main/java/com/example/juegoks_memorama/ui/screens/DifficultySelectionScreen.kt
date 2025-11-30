package com.example.juegoks_memorama.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.juegoks_memorama.model.Difficulty

@Composable
fun DifficultySelectionScreen(
    onDifficultySelected: (Difficulty) -> Unit,
    onBack: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "📊 Selecciona Dificultad",
            style = MaterialTheme.typography.headlineLarge,
            fontSize = 32.sp
        )
        Spacer(modifier = Modifier.height(64.dp))

        DifficultyButton(difficulty = Difficulty.EASY, label = "🟢 Fácil (4x3 - 6 Pares)", onClick = onDifficultySelected)
        Spacer(modifier = Modifier.height(16.dp))
        DifficultyButton(difficulty = Difficulty.MEDIUM, label = "🟡 Media (6x5 - 15 Pares)", onClick = onDifficultySelected)
        Spacer(modifier = Modifier.height(16.dp))
        DifficultyButton(difficulty = Difficulty.HARD, label = "🔴 Difícil (8x7 - 28 Pares)", onClick = onDifficultySelected)

        Spacer(modifier = Modifier.height(48.dp))
        Button(onClick = onBack) {
            Text("🔙 Volver al Modo")
        }
    }
}

@Composable
private fun DifficultyButton(
    difficulty: Difficulty,
    label: String,
    onClick: (Difficulty) -> Unit
) {
    Button(
        onClick = { onClick(difficulty) },
        modifier = Modifier.height(50.dp)
    ) {
        Text(label, fontSize = 18.sp)
    }
}