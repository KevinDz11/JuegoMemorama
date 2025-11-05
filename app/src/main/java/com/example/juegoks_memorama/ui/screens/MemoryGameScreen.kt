package com.example.juegoks_memorama.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.QuestionMark
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.juegoks_memorama.viewmodel.MemoryGameViewModel
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import com.example.juegoks_memorama.model.Difficulty
import com.example.juegoks_memorama.model.GameMode
import com.example.juegoks_memorama.model.SaveFormat
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch

@Composable
private fun formatTime(seconds: Long): String {
    val minutes = seconds / 60
    val remainingSeconds = seconds % 60
    return "%02d:%02d".format(minutes, remainingSeconds)
}

@Composable
fun MemoryGameScreen(
    gameMode: GameMode,
    initialDifficulty: Difficulty, // Nuevo parámetro
    onExitGame: () -> Unit,
    viewModel: MemoryGameViewModel = hiltViewModel()
) {
    val gameState by viewModel.gameState.collectAsStateWithLifecycle()
    val uiState by viewModel.gameUiState.collectAsStateWithLifecycle() // Nuevo estado UI
    val scope = rememberCoroutineScope() // Necesario para efectos visuales/sonoros (si se implementan)

    // Nota: La implementación de retroalimentación sonora con MediaPlayer requiere
    // añadir el archivo de sonido a res/raw y usar el contexto. Aquí se mantiene como mockup.
    val playMatchSound = {}
    val playNoMatchSound = {}

    // Detección de match para feedback visual/sonoro
    LaunchedEffect(gameState.matchedPairs, gameState.moves) {
        if (gameState.moves > 0) {
            // Si la racha es > 0, es un match
            if (gameState.matchStreak > 0) {
                // playMatchSound()
            } else if (gameState.cards.none { it.isFaceUp && !it.isMatched }) {
                // Si no hay cartas volteadas y la racha es 0, implica un no-match reciente
                // playNoMatchSound()
            }
        }
    }


    LaunchedEffect(initialDifficulty) {
        // Asegura que el ViewModel cargue la dificultad seleccionada
        viewModel.setDifficulty(initialDifficulty)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = if (gameMode == GameMode.SINGLE_PLAYER) "Modo: Un Jugador (${gameState.difficulty.name})" else "Modo: Multijugador (Bluetooth)",
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        GameHeader(
            moves = gameState.moves,
            matchedPairs = gameState.matchedPairs,
            score = gameState.score, // Pasar la puntuación
            elapsedTime = gameState.elapsedTimeInSeconds,
            maxPairs = gameState.difficulty.pairs, // Pasar el máximo de pares
            onNewGame = { viewModel.startNewGame() },
            onExitGame = onExitGame,
            onShowSaveDialog = { viewModel.showSaveDialog(true) }, // Nuevo
            onShowLoadDialog = { viewModel.showLoadDialog(true) } // Nuevo
        )

        Spacer(modifier = Modifier.height(16.dp))

        if (gameMode == GameMode.BLUETOOTH) {
            Text(
                text = "Lógica de conexión Bluetooth en desarrollo...",
                modifier = Modifier.padding(16.dp),
                color = MaterialTheme.colorScheme.error
            )
        }
        Spacer(modifier = Modifier.height(16.dp))

        CardGrid(
            cards = gameState.cards,
            columns = gameState.difficulty.columns, // Usar columnas de la dificultad
            onCardClick = { card -> viewModel.onCardClick(card) }
        )

        if (gameState.gameCompleted) {
            GameCompletedDialog(
                moves = gameState.moves,
                score = gameState.score, // Mostrar puntuación final
                onPlayAgain = { viewModel.startNewGame() }
            )
        }

        // --- DIÁLOGOS DE GUARDADO/CARGA ---
        if (uiState.showSaveDialog) {
            SaveGameDialog(
                onSave = { format ->
                    scope.launch { viewModel.saveGame(format) }
                },
                onDismiss = { viewModel.showSaveDialog(false) }
            )
        }

        if (uiState.showLoadDialog) {
            LoadGameDialog(
                saveFiles = uiState.saveFiles,
                onLoad = { filename, format ->
                    scope.launch { viewModel.loadGame(filename, format) }
                },
                onDismiss = { viewModel.showLoadDialog(false) }
            )
        }
    }
}

@Composable
fun GameHeader(
    moves: Int,
    matchedPairs: Int,
    score: Int, // Nuevo parámetro
    elapsedTime: Long,
    maxPairs: Int, // Nuevo parámetro
    onNewGame: () -> Unit,
    onExitGame: () -> Unit,
    onShowSaveDialog: () -> Unit,
    onShowLoadDialog: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(
                text = "Puntuación: $score", // Mostrar Puntuación
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Movimientos: $moves",
                style = MaterialTheme.typography.bodyMedium
            )
            Text(
                text = "Parejas: $matchedPairs/$maxPairs",
                style = MaterialTheme.typography.bodyMedium
            )
            Text(
                text = "Tiempo: ${formatTime(elapsedTime)}",
                style = MaterialTheme.typography.bodyMedium
            )
        }

        Column(horizontalAlignment = Alignment.End) {
            Button(onClick = onNewGame) {
                Text("Nuevo Juego")
            }
            Spacer(modifier = Modifier.height(8.dp))
            Row {
                Button(onClick = onShowSaveDialog, modifier = Modifier.padding(end = 8.dp)) { Text("Guardar") }
                Button(onClick = onShowLoadDialog) { Text("Cargar") }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Button(onClick = onExitGame) {
                Text("Salir")
            }
        }
    }
}

@Composable
fun CardGrid(
    cards: List<com.example.juegoks_memorama.model.Card>,
    columns: Int, // Nuevo parámetro
    onCardClick: (com.example.juegoks_memorama.model.Card) -> Unit
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(columns), // Usar columnas dinámicamente
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(cards) { card ->
            MemoryCard(
                card = card,
                onClick = { onCardClick(card) }
            )
        }
    }
}

@Composable
fun MemoryCard(
    card: com.example.juegoks_memorama.model.Card,
    onClick: () -> Unit
) {
    val rotation = remember { Animatable(0f) }
    var isFaceUp by remember { mutableStateOf(card.isFaceUp) }

    LaunchedEffect(card.isFaceUp) {
        if (card.isFaceUp != isFaceUp) {
            // Animación de rotación para voltear
            if (card.isFaceUp) {
                rotation.animateTo(180f, animationSpec = tween(500))
            } else {
                rotation.animateTo(0f, animationSpec = tween(500))
            }
            isFaceUp = card.isFaceUp
        }
    }

    // --- EFECTO VISUAL: Escala a 0 para "desaparecer" al matchear ---
    val animateScale by animateFloatAsState(
        targetValue = if (card.isMatched) 0.0f else 1f, // Escala a 0 al matchear
        animationSpec = if (card.isMatched) {
            spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessLow)
        } else {
            spring(dampingRatio = Spring.DampingRatioMediumBouncy)
        },
        label = "scale"
    )

    Box(
        modifier = Modifier
            .aspectRatio(0.75f)
            .scale(animateScale) // Aplicar el efecto de escala
    ) {
        Card(
            modifier = Modifier
                .fillMaxSize()
                .clickable(
                    enabled = !card.isMatched && !card.isFaceUp,
                    onClick = onClick
                ),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
            colors = CardDefaults.cardColors(
                containerColor = if (rotation.value > 90f) {
                    MaterialTheme.colorScheme.surface
                } else {
                    MaterialTheme.colorScheme.primary
                }
            )
        ) {
            if (rotation.value > 90f) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = card.value.toString(),
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            } else {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.QuestionMark,
                        contentDescription = "Carta volteada",
                        modifier = Modifier.size(32.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun GameCompletedDialog(
    moves: Int,
    score: Int, // Nuevo parámetro
    onPlayAgain: () -> Unit
) {
    AlertDialog(
        onDismissRequest = { },
        title = { Text("¡Felicidades!") },
        text = {
            Column {
                Text("Completaste el juego en $moves movimientos.")
                Text("Puntuación Final: $score puntos.") // Mostrar puntuación
            }
        },
        confirmButton = {
            Button(onClick = onPlayAgain) {
                Text("Jugar de nuevo")
            }
        }
    )
}

// --- NUEVO: Diálogo para Guardar Partida ---
@Composable
fun SaveGameDialog(
    onSave: (SaveFormat) -> Unit,
    onDismiss: () -> Unit
) {
    var selectedFormat by remember { mutableStateOf(SaveFormat.JSON) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Guardar Partida") },
        text = {
            Column {
                Text("Selecciona el formato de guardado:")
                Spacer(modifier = Modifier.height(16.dp))
                SaveFormat.entries.forEach { format ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { selectedFormat = format }
                            .padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = "${if (selectedFormat == format) "●" else "○"} ${format.name} (.${format.name.lowercase()})")
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = { onSave(selectedFormat) }) { Text("Guardar") }
        },
        dismissButton = {
            Button(onClick = onDismiss) { Text("Cancelar") }
        }
    )
}

// --- NUEVO: Diálogo para Cargar Partida ---
@Composable
fun LoadGameDialog(
    saveFiles: List<Pair<String, SaveFormat>>,
    onLoad: (String, SaveFormat) -> Unit,
    onDismiss: () -> Unit
) {
    var selectedFile by remember { mutableStateOf<Pair<String, SaveFormat>?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Cargar Partida") },
        text = {
            Column {
                Text("Selecciona un archivo para cargar:")
                Spacer(modifier = Modifier.height(16.dp))
                if (saveFiles.isEmpty()) {
                    Text("No hay partidas guardadas.")
                } else {
                    saveFiles.forEach { file ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { selectedFile = file }
                                .padding(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            val displayText = "${file.first} (${file.second.name})"
                            Text(text = "${if (selectedFile == file) "●" else "○"} $displayText")
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { selectedFile?.let { onLoad(it.first, it.second) } },
                enabled = selectedFile != null
            ) { Text("Cargar") }
        },
        dismissButton = {
            Button(onClick = onDismiss) { Text("Cancelar") }
        }
    )
}