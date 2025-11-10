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
import com.example.juegoks_memorama.model.Difficulty
import com.example.juegoks_memorama.model.GameMode
import com.example.juegoks_memorama.ui.screens.DifficultySelectionScreen
import com.example.juegoks_memorama.ui.screens.MemoryGameScreen
import com.example.juegoks_memorama.ui.screens.ModeSelectionScreen
import com.example.juegoks_memorama.ui.theme.MemoryGameTheme
import dagger.hilt.android.AndroidEntryPoint
import androidx.compose.runtime.collectAsState // Importar
import androidx.hilt.navigation.compose.hiltViewModel // Importar
import com.example.juegoks_memorama.viewmodel.MemoryGameViewModel // Importar
import com.example.juegoks_memorama.model.AppThemeOption // Importar

// --- NUEVO ESTADO DE NAVEGACIÓN ---
sealed class Screen {
    object ModeSelection : Screen()
    data class DifficultySelection(val mode: GameMode) : Screen()
    data class Game(val mode: GameMode, val difficulty: Difficulty?) : Screen()
}

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            // OBTENER VIEWMODEL Y TEMA
            val viewModel: MemoryGameViewModel = hiltViewModel()
            val currentTheme by viewModel.currentTheme.collectAsState()

            var currentScreen by remember { mutableStateOf<Screen>(Screen.ModeSelection) }

            // PASAR EL TEMA AL WRAPPER
            MemoryGameTheme(themeOption = currentTheme) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    when (val screen = currentScreen) {
                        is Screen.ModeSelection -> {
                            ModeSelectionScreen(
                                onModeSelected = { mode ->
                                    if (mode == GameMode.SINGLE_PLAYER) {
                                        // Ir a selección de dificultad para un jugador
                                        currentScreen = Screen.DifficultySelection(mode)
                                    } else {
                                        // Para Bluetooth, asumir dificultad media por ahora
                                        currentScreen = Screen.Game(mode, Difficulty.MEDIUM)
                                    }
                                },
                                // PASAR LA FUNCIÓN PARA CAMBIAR TEMA
                                onThemeChange = { theme ->
                                    viewModel.setTheme(theme)
                                }
                            )
                        }
                        is Screen.DifficultySelection -> {
                            DifficultySelectionScreen(
                                onDifficultySelected = { difficulty ->
                                    currentScreen = Screen.Game(screen.mode, difficulty)
                                },
                                onBack = {
                                    currentScreen = Screen.ModeSelection
                                }
                            )
                        }
                        is Screen.Game -> {
                            val difficulty = screen.difficulty ?: Difficulty.MEDIUM
                            MemoryGameScreen(
                                gameMode = screen.mode,
                                initialDifficulty = difficulty,
                                onExitGame = { currentScreen = Screen.ModeSelection } // Al salir vuelve a la selección
                            )
                        }
                    }
                }
            }
        }
    }
}