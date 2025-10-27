package com.example.juegoks_memorama // Tu nombre de paquete

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
import com.example.juegoks_memorama.ui.theme.JuegoKSMemoramaTheme // Importa tu Tema
import com.example.juegoks_memorama.ui2.GameScreen // Importa tu pantalla de ui2

class MainActivity : ComponentActivity() {

    // Inicializa el ViewModel
    private val viewModel: GameViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            JuegoKSMemoramaTheme { // Usa el nombre de tu tema
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
    override fun onStop() {
        super.onStop()
    }
}