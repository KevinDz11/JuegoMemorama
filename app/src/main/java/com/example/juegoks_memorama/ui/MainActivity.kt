package com.example.juegoks_memorama.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.example.juegoks_memorama.model.Difficulty
import com.example.juegoks_memorama.model.GameMode
import com.example.juegoks_memorama.ui.screens.DifficultySelectionScreen
import com.example.juegoks_memorama.ui.screens.MemoryGameScreen
import com.example.juegoks_memorama.ui.screens.ModeSelectionScreen
// Importar la nueva pantalla de Bluetooth
import com.example.juegoks_memorama.ui.screens.BluetoothScreen
import com.example.juegoks_memorama.ui.theme.MemoryGameTheme
import dagger.hilt.android.AndroidEntryPoint
import androidx.compose.runtime.collectAsState
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.juegoks_memorama.viewmodel.MemoryGameViewModel

// 1. ACTUALIZAMOS LAS PANTALLAS DISPONIBLES
sealed class Screen {
    object ModeSelection : Screen()
    data class DifficultySelection(val mode: GameMode) : Screen()
    // Nueva pantalla para conectar dispositivos
    object Bluetooth : Screen()
    data class Game(val mode: GameMode, val difficulty: Difficulty?) : Screen()
}

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            // Compartimos el ViewModel para mantener la conexión Bluetooth activa entre pantallas
            val viewModel: MemoryGameViewModel = hiltViewModel()
            val currentTheme by viewModel.currentTheme.collectAsState()

            var currentScreen by remember { mutableStateOf<Screen>(Screen.ModeSelection) }

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
                                        currentScreen = Screen.DifficultySelection(mode)
                                    } else {
                                        // 2. CORRECCIÓN: Ir a la pantalla de conexión Bluetooth
                                        currentScreen = Screen.Bluetooth
                                    }
                                },
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
                        // 3. INTEGRACIÓN: Mostrar la pantalla de búsqueda de dispositivos
                        is Screen.Bluetooth -> {
                            BluetoothScreen(
                                viewModel = viewModel, // Pasamos el VM compartido
                                onGameStart = {
                                    // Cuando conectan, iniciamos el juego en modo Bluetooth
                                    // La dificultad ya se habrá negociado internamente en el VM o por defecto
                                    currentScreen = Screen.Game(GameMode.BLUETOOTH, Difficulty.MEDIUM)
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
                                viewModel = viewModel, // Usar el mismo VM
                                onExitGame = {
                                    // Desconectar al salir si es necesario
                                    if (screen.mode == GameMode.BLUETOOTH) {
                                        viewModel.bluetoothService.disconnect()
                                    }
                                    currentScreen = Screen.ModeSelection
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}