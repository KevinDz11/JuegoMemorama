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
import com.example.juegoks_memorama.model.GameMode
import androidx.compose.foundation.layout.Row
import com.example.juegoks_memorama.model.AppThemeOption

@Composable
fun ModeSelectionScreen(
    onModeSelected: (GameMode) -> Unit,
    onThemeChange: (AppThemeOption) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "🧠 Juego de Memorama",
            style = MaterialTheme.typography.headlineLarge,
            fontSize = 32.sp
        )
        Spacer(modifier = Modifier.height(64.dp))
        Text(
            text = "¿Cómo quieres jugar?",
            style = MaterialTheme.typography.headlineMedium
        )
        Spacer(modifier = Modifier.height(32.dp))

        Button(
            onClick = { onModeSelected(GameMode.SINGLE_PLAYER) },
            modifier = Modifier.height(50.dp)
        ) {
            Text("👤 Un Jugador (Normal)", fontSize = 18.sp)
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = { onModeSelected(GameMode.BLUETOOTH) },
            modifier = Modifier.height(50.dp)
        ) {
            Text("📶 Multijugador (Bluetooth)", fontSize = 18.sp)
        }

        Spacer(modifier = Modifier.height(32.dp))
        Text("🎨 Seleccionar Tema:", style = MaterialTheme.typography.titleMedium)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = { onThemeChange(AppThemeOption.IPN) }) {
                Text("🦅 Tema IPN")
            }
            Button(onClick = { onThemeChange(AppThemeOption.ESCOM) }) {
                Text("💻 Tema ESCOM")
            }
        }
    }
}