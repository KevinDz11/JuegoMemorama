package com.example.juegoks_memorama.ui.screens

import android.view.SoundEffectConstants
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.QuestionMark
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.animateFloatAsState
import com.example.juegoks_memorama.model.Difficulty
import com.example.juegoks_memorama.model.GameHistoryItem
import com.example.juegoks_memorama.model.GameMode
import com.example.juegoks_memorama.model.SaveFormat
import com.example.juegoks_memorama.viewmodel.MemoryGameViewModel
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
private fun formatTime(seconds: Long): String {
    val minutes = seconds / 60
    val remainingSeconds = seconds % 60
    return "%02d:%02d".format(minutes, remainingSeconds)
}

@Composable
private fun formatTimestamp(timestamp: Long): String {
    val sdf = remember { SimpleDateFormat("dd/MM/yy HH:mm", Locale.getDefault()) }
    return sdf.format(Date(timestamp))
}

@Composable
fun MemoryGameScreen(
    gameMode: GameMode,
    initialDifficulty: Difficulty,
    onExitGame: () -> Unit,
    viewModel: MemoryGameViewModel = hiltViewModel()
) {
    val gameState by viewModel.gameState.collectAsStateWithLifecycle()
    val uiState by viewModel.gameUiState.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    val view = LocalView.current

    LaunchedEffect(initialDifficulty) {
        viewModel.setDifficulty(initialDifficulty)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // --- CABECERA ---
        if (gameMode == GameMode.BLUETOOTH) {
            Text(
                text = "📶 Multijugador (${gameState.difficulty.name})",
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            if (gameState.cards.isNotEmpty()) {
                // Indicador de turno
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = if (gameState.isMyTurn) Color(0xFF4CAF50) else Color(0xFFE57373)
                    ),
                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (gameState.isMyTurn) "¡TU TURNO! 🟢" else "RIVAL 🔴",
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                        // MOSTRAR TIEMPO EN MULTIJUGADOR
                        Text(
                            text = "⏱ ${formatTime(gameState.elapsedTimeInSeconds)}",
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    ScoreBox(label = "TÚ", score = gameState.score, pairs = gameState.myPairs, active = gameState.isMyTurn)
                    ScoreBox(label = "RIVAL", score = gameState.opponentScore, pairs = gameState.opponentPairs, active = !gameState.isMyTurn)
                }
            }
        } else {
            // UN JUGADOR
            Text(
                text = "👤 Un Jugador (${gameState.difficulty.name})",
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            GameHeader(
                moves = gameState.moves,
                matchedPairs = gameState.matchedPairs,
                score = gameState.score,
                elapsedTime = gameState.elapsedTimeInSeconds,
                maxPairs = gameState.difficulty.pairs,
                onNewGame = { viewModel.startNewGame() },
                onExitGame = {
                    viewModel.onExitGame()
                    onExitGame()
                },
                onSaveClick = { viewModel.onSaveClick() },
                onShowHistoryDialog = { viewModel.showHistoryDialog(true) }
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // --- TABLERO ---
        if (gameMode == GameMode.BLUETOOTH && gameState.cards.isEmpty()) {
            Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator()
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Conectado. Esperando partida...", style = MaterialTheme.typography.titleMedium)
                }
            }
        }
        else if (gameState.cards.isNotEmpty()) {
            CardGrid(
                cards = gameState.cards,
                columns = gameState.difficulty.columns,
                onCardClick = { card -> viewModel.onCardClick(card) },
                modifier = Modifier
                    .weight(1f)
                    .alpha(if (gameMode == GameMode.BLUETOOTH && !gameState.isMyTurn) 0.6f else 1f)
            )
        } else {
            Spacer(modifier = Modifier.weight(1f))
        }

        // --- DIÁLOGOS ---
        if (gameState.gameCompleted) {
            GameCompletedDialog(
                moves = gameState.moves,
                myScore = gameState.score,
                opponentScore = gameState.opponentScore,
                elapsedTime = gameState.elapsedTimeInSeconds,
                isMultiplayer = gameMode == GameMode.BLUETOOTH,
                onPlayAgain = {
                    if (gameMode == GameMode.SINGLE_PLAYER) viewModel.startNewGame()
                    else onExitGame()
                },
                onSaveResult = { viewModel.onSaveClick() },
                onExit = {
                    viewModel.onExitGame()
                    onExitGame()
                }
            )
        }

        if (uiState.showSaveDialog) {
            SaveGameDialog(
                existingSaveNames = uiState.existingSaveNames,
                onSave = { filename, format -> scope.launch { viewModel.saveGame(filename, format) } },
                onDismiss = { viewModel.showSaveDialog(false) }
            )
        }
        if (uiState.showHistoryDialog) {
            HistoryDialog(
                historyItems = uiState.historyItems,
                onLoad = { filename, format -> scope.launch { viewModel.loadGameFromHistory(filename, format) } },
                onDismiss = { viewModel.showHistoryDialog(false) }
            )
        }
        if (uiState.showPostSaveDialog) {
            PostSaveDialog(
                onContinue = { viewModel.dismissPostSaveDialog() },
                onNewGame = { viewModel.dismissPostSaveDialog(); viewModel.startNewGame() },
                onExit = {
                    viewModel.dismissPostSaveDialog()
                    viewModel.onExitGame()
                    onExitGame()
                }
            )
        }
        if (uiState.showPostWinSaveDialog) {
            PostWinSaveDialog(
                onNewGame = { viewModel.dismissPostWinSaveDialog(); viewModel.startNewGame() },
                onExit = {
                    viewModel.dismissPostWinSaveDialog()
                    viewModel.onExitGame()
                    onExitGame()
                }
            )
        }
    }
}

// --- COMPONENTES UI MODIFICADOS ---

@Composable
fun ScoreBox(label: String, score: Int, pairs: Int, active: Boolean) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = if(active) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
        ),
        elevation = CardDefaults.cardElevation(if(active) 4.dp else 0.dp),
        modifier = Modifier.padding(8.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(label, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelMedium)
            Text("${score}", style = MaterialTheme.typography.headlineLarge)
            Text("Pts", style = MaterialTheme.typography.bodySmall)
            Spacer(modifier = Modifier.height(4.dp))
            // AÑADIDO: Muestra los pares
            Text("🃏 $pairs Pares", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun GameHeader(moves: Int, matchedPairs: Int, score: Int, elapsedTime: Long, maxPairs: Int, onNewGame: () -> Unit, onExitGame: () -> Unit, onSaveClick: () -> Unit, onShowHistoryDialog: () -> Unit) {
    val view = LocalView.current
    Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        Row(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp), horizontalArrangement = Arrangement.SpaceAround) {
            StatItem(label = "🏆 Puntos", value = "$score")
            StatItem(label = "🔄 Movs", value = "$moves")
            StatItem(label = "🃏 Pares", value = "$matchedPairs/$maxPairs")
            StatItem(label = "⏱ Tiempo", value = formatTime(elapsedTime))
        }
        Spacer(Modifier.height(8.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
            Button(onClick = { view.playSoundEffect(SoundEffectConstants.CLICK); onNewGame() }) { Text("🔄 Nuevo") }
            Button(onClick = { view.playSoundEffect(SoundEffectConstants.CLICK); onSaveClick() }) { Text("💾 Guardar") }
        }
        Row(modifier = Modifier.fillMaxWidth().padding(top = 8.dp), horizontalArrangement = Arrangement.SpaceEvenly) {
            Button(onClick = { view.playSoundEffect(SoundEffectConstants.CLICK); onShowHistoryDialog() }) { Text("📜 Historial") }
            Button(onClick = { view.playSoundEffect(SoundEffectConstants.CLICK); onExitGame() }) { Text("🚪 Salir") }
        }
    }
}

@Composable
private fun StatItem(label: String, value: String, modifier: Modifier = Modifier) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = modifier.padding(horizontal = 4.dp)) {
        Text(text = label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(text = value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun CardGrid(cards: List<com.example.juegoks_memorama.model.Card>, columns: Int, onCardClick: (com.example.juegoks_memorama.model.Card) -> Unit, modifier: Modifier = Modifier) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(columns),
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(cards) { card -> MemoryCard(card = card, onClick = { onCardClick(card) }) }
    }
}

@Composable
fun MemoryCard(card: com.example.juegoks_memorama.model.Card, onClick: () -> Unit) {
    val rotation = remember { Animatable(0f) }
    var isFaceUp by remember { mutableStateOf(card.isFaceUp) }

    LaunchedEffect(card.isFaceUp) {
        if (card.isFaceUp != isFaceUp) {
            if (card.isFaceUp) rotation.animateTo(180f, animationSpec = tween(400))
            else rotation.animateTo(0f, animationSpec = tween(400))
            isFaceUp = card.isFaceUp
        }
    }
    val animateScale by animateFloatAsState(targetValue = if (card.isMatched) 0.0f else 1f, label = "scale")

    Box(modifier = Modifier.aspectRatio(0.75f).scale(animateScale)) {
        Card(
            modifier = Modifier.fillMaxSize().clickable(enabled = !card.isMatched && !card.isFaceUp, onClick = onClick),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
            colors = CardDefaults.cardColors(containerColor = if (rotation.value > 90f) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.primary)
        ) {
            if (rotation.value > 90f) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(text = card.value.toString(), style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                }
            } else {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Icon(imageVector = Icons.Default.QuestionMark, contentDescription = "Carta", modifier = Modifier.size(24.dp))
                }
            }
        }
    }
}

@Composable
fun GameCompletedDialog(moves: Int, myScore: Int, opponentScore: Int, elapsedTime: Long, isMultiplayer: Boolean, onPlayAgain: () -> Unit, onSaveResult: () -> Unit, onExit: () -> Unit) {
    val view = LocalView.current
    val won = myScore > opponentScore
    val tie = myScore == opponentScore

    AlertDialog(
        onDismissRequest = { if (!isMultiplayer) onPlayAgain() else onExit() },
        title = { Text(if (isMultiplayer) (if (tie) "¡🤝 Empate!" else if (won) "¡🏆 GANASTE!" else "😞 Perdiste...") else "¡🎉 Felicidades!") },
        text = {
            Column {
                if (isMultiplayer) {
                    Text("Tus Puntos: $myScore")
                    Text("Puntos Rival: $opponentScore")
                    Text("Tiempo: ${formatTime(elapsedTime)}")
                } else {
                    Text("Tiempo: ${formatTime(elapsedTime)}")
                    Text("Movimientos: $moves")
                    Text("Puntuación Final: $myScore pts")
                }
            }
        },
        confirmButton = {
            Column(horizontalAlignment = Alignment.End) {
                if (!isMultiplayer) {
                    Button(onClick = { view.playSoundEffect(SoundEffectConstants.CLICK); onSaveResult() }) { Text("💾 Guardar Resultado") }
                    Button(onClick = { view.playSoundEffect(SoundEffectConstants.CLICK); onPlayAgain() }) { Text("🔄 Jugar de nuevo") }
                }
                Button(onClick = { view.playSoundEffect(SoundEffectConstants.CLICK); onExit() }) { Text("🚪 Salir") }
            }
        },
        dismissButton = {}
    )
}

@Composable
fun SaveGameDialog(existingSaveNames: List<String>, onSave: (String, SaveFormat) -> Unit, onDismiss: () -> Unit) {
    var selectedFormat by remember { mutableStateOf(SaveFormat.JSON) }
    var filename by rememberSaveable { mutableStateOf("") }

    val isBlank = filename.isBlank()
    val isDuplicate = existingSaveNames.any { it.equals(filename, ignoreCase = true) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("💾 Guardar Partida") },
        text = {
            Column {
                Text("Nombre del archivo:")
                OutlinedTextField(
                    value = filename,
                    onValueChange = { filename = it },
                    isError = isBlank || isDuplicate,
                    supportingText = {
                        if (isDuplicate) Text("Este nombre ya existe", color = MaterialTheme.colorScheme.error)
                    }
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text("Formato:")
                Row { SaveFormat.entries.forEach { f -> Text("${if(selectedFormat==f)"●" else "○"} ${f.name}", Modifier.clickable{selectedFormat=f}.padding(4.dp)) } }
            }
        },
        confirmButton = { Button(onClick = { onSave(filename, selectedFormat) }, enabled = !isBlank && !isDuplicate) { Text("Guardar") } },
        dismissButton = { Button(onClick = onDismiss) { Text("Cancelar") } }
    )
}

@Composable
fun HistoryDialog(historyItems: List<GameHistoryItem>, onLoad: (String, SaveFormat) -> Unit, onDismiss: () -> Unit) {
    var selectedFile by remember { mutableStateOf<Pair<String, SaveFormat>?>(null) }
    val (completed, inProgress) = historyItems.partition { it.state.gameCompleted }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("📜 Historial de Partidas") },
        text = {
            LazyColumn(modifier = Modifier.fillMaxWidth()) {
                item {
                    Text("▶️ Partidas en Curso (Cargar)", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    HorizontalDivider()
                }
                if (inProgress.isEmpty()) item { Text("No hay partidas guardadas.", Modifier.padding(8.dp)) }
                items(inProgress) { item ->
                    val isSelected = selectedFile?.first == item.filename
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { selectedFile = item.filename to item.format }
                            .background(if (isSelected) MaterialTheme.colorScheme.secondaryContainer else Color.Transparent)
                            .padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(if (isSelected) "◉ " else "○ ", fontWeight = FontWeight.Bold)
                        Column {
                            Text(item.filename, fontWeight = FontWeight.Bold)
                            Text("Puntos: ${item.state.score} | ${formatTime(item.state.elapsedTimeInSeconds)} | ${formatTimestamp(item.timestamp)}", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }

                item { Spacer(modifier = Modifier.height(16.dp)) }

                item {
                    Text("✅ Partidas Finalizadas (Solo ver)", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.secondary)
                    HorizontalDivider()
                }
                if (completed.isEmpty()) item { Text("No hay partidas completadas.", Modifier.padding(8.dp)) }
                items(completed) { item ->
                    Column(modifier = Modifier.padding(8.dp).alpha(0.7f)) {
                        Text(item.filename, fontWeight = FontWeight.Bold)
                        Text("FINAL: ${item.state.score} pts | Tiempo: ${formatTime(item.state.elapsedTimeInSeconds)}", style = MaterialTheme.typography.bodySmall)
                        Text("Fecha: ${formatTimestamp(item.timestamp)}", style = MaterialTheme.typography.bodySmall)
                    }
                    HorizontalDivider(thickness = 0.5.dp)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { selectedFile?.let { onLoad(it.first, it.second) } },
                enabled = selectedFile != null
            ) { Text("Cargar") }
        },
        dismissButton = { Button(onClick = onDismiss) { Text("Cerrar") } }
    )
}

@Composable fun PostSaveDialog(onContinue: () -> Unit, onNewGame: () -> Unit, onExit: () -> Unit) {
    AlertDialog(onDismissRequest = onContinue, title = { Text("💾 Guardado") }, text = { Text("Partida guardada con éxito. ¿Qué sigue?") }, confirmButton = { Column(horizontalAlignment = Alignment.End) { Button(onClick = onContinue) { Text("▶️ Seguir jugando") }; Button(onClick = onNewGame) { Text("🔄 Nuevo juego") }; Button(onClick = onExit) { Text("🚪 Salir") } } })
}
@Composable fun PostWinSaveDialog(onNewGame: () -> Unit, onExit: () -> Unit) {
    AlertDialog(onDismissRequest = onNewGame, title = { Text("💾 Resultado Guardado") }, text = { Text("¡Partida registrada! ¿Qué deseas hacer?") }, confirmButton = { Column(horizontalAlignment = Alignment.End) { Button(onClick = onNewGame) { Text("🔄 Nuevo juego") }; Button(onClick = onExit) { Text("🚪 Salir") } } })
}