package com.example.juegoks_memorama.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue // Importar para el delegado 'by'
import androidx.compose.runtime.mutableStateOf // Importar para mutableStateOf
import androidx.compose.runtime.remember // Importar para remember
import androidx.compose.runtime.setValue // Importar para el delegado 'by'
import androidx.compose.ui.Modifier
import com.example.juegoks_memorama.model.GameMode // Importar nuevo enum
import com.example.juegoks_memorama.ui.screens.MemoryGameScreen
import com.example.juegoks_memorama.ui.screens.ModeSelectionScreen // Importar nueva pantalla
import com.example.juegoks_memorama.ui.theme.MemoryGameTheme//import com.example.memorygame.ui.theme.MemoryGameTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            // --- CAMBIO: Estado para manejar la navegación entre pantallas/modos ---
            var selectedMode by remember { mutableStateOf<GameMode?>(null) }

            MemoryGameTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    when (selectedMode) {
                        null -> {
                            ModeSelectionScreen(
                                onModeSelected = { mode ->
                                    selectedMode = mode
                                }
                            )
                        }
                        GameMode.SINGLE_PLAYER -> {
                            MemoryGameScreen(
                                gameMode = GameMode.SINGLE_PLAYER,
                                onExitGame = { selectedMode = null } // Al salir vuelve a la selección
                            )
                        }
                        GameMode.BLUETOOTH -> {
                            MemoryGameScreen(
                                gameMode = GameMode.BLUETOOTH,
                                onExitGame = { selectedMode = null } // Al salir vuelve a la selección
                            )
                        }
                    }
                }
            }
        }
    }
}