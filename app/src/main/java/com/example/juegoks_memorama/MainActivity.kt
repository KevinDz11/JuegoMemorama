package com.example.juegoks_memorama

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.example.juegoks_memorama.ui2.GameScreen // <-- Asegúrate que la ruta sea correcta
import com.example.juego.ui.theme.JuegoTheme // <-- El nombre de tu tema

class MainActivity : ComponentActivity() {

    // Inicializa el ViewModel
    private val viewModel: GameViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            JuegoTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    // Observa el estado (uiState) del ViewModel
                    val state by viewModel.uiState.collectAsState()

                    // Muestra la pantalla del juego
                    GameScreen(
                        state = state,
                        onCardClick = { index ->
                            viewModel.onCardSelected(index)
                        },
                        onResetClick = {
                            viewModel.resetGame()
                        }
                    )
                }
            }
        }
    }
}