package com.example.juegoks_memorama.ui.screens

import android.view.SoundEffectConstants
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.QuestionMark
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import com.example.juegoks_memorama.model.Difficulty
import com.example.juegoks_memorama.model.GameHistoryItem
import com.example.juegoks_memorama.model.GameMode
import com.example.juegoks_memorama.model.SaveFormat
import com.example.juegoks_memorama.viewmodel.MemoryGameViewModel
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.HorizontalDivider
import androidx.compose.foundation.layout.statusBarsPadding

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
            // CABECERA MULTIJUGADOR
            Text(
                text = "Multijugador Bluetooth",
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            // Indicador de Turno
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = if (gameState.isMyTurn) Color(0xFF4CAF50) else Color(0xFFE57373)
                ),
                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
            ) {
                Text(
                    text = if (gameState.isMyTurn) "¡TU TURNO!" else "TURNO DEL OPONENTE (Espera)",
                    modifier = Modifier.padding(12.dp).align(Alignment.CenterHorizontally),
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
            }

            // Marcador detallado
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                ScoreBox(label = "TÚ", score = gameState.score, active = gameState.isMyTurn)
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Tiempo", style = MaterialTheme.typography.bodySmall)
                    Text(formatTime(gameState.elapsedTimeInSeconds), style = MaterialTheme.typography.titleMedium)
                }
                ScoreBox(label = "RIVAL", score = gameState.opponentScore, active = !gameState.isMyTurn)
            }

            // Mensaje de espera inicial
            if (gameState.cards.isEmpty()) {
                Spacer(modifier = Modifier.height(32.dp))
                Text("Sincronizando partida...", style = MaterialTheme.typography.bodyLarge)
            }

        } else {
            // CABECERA UN JUGADOR
            Text(
                text = "Un Jugador (${gameState.difficulty.name})",
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
                onExitGame = onExitGame,
                onSaveClick = { viewModel.onSaveClick() },
                onShowHistoryDialog = { viewModel.showHistoryDialog(true) }
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // --- TABLERO ---
        if (gameState.cards.isNotEmpty()) {
            CardGrid(
                cards = gameState.cards,
                columns = gameState.difficulty.columns,
                onCardClick = { card -> viewModel.onCardClick(card) },
                // En bluetooth, si no es mi turno, bajamos opacidad para indicar "bloqueado"
                modifier = Modifier
                    .weight(1f)
                    .alpha(if (gameMode == GameMode.BLUETOOTH && !gameState.isMyTurn) 0.7f else 1f)
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
                    else onExitGame() // En BT, salir al menú por ahora
                },
                onSaveResult = { viewModel.onSaveClick() },
                onExit = onExitGame
            )
        }

        if (uiState.showSaveDialog) {
            SaveGameDialog(
                existingSaveNames = uiState.existingSaveNames,
                onSave = { filename, format ->
                    scope.launch { viewModel.saveGame(filename, format) }
                },
                onDismiss = { viewModel.showSaveDialog(false) }
            )
        }

        if (uiState.showHistoryDialog) {
            HistoryDialog(
                historyItems = uiState.historyItems,
                onLoad = { filename, format ->
                    scope.launch { viewModel.loadGameFromHistory(filename, format) }
                },
                onDismiss = { viewModel.showHistoryDialog(false) }
            )
        }

        if (uiState.showPostSaveDialog) {
            PostSaveDialog(
                onContinue = { viewModel.dismissPostSaveDialog() },
                onNewGame = {
                    viewModel.dismissPostSaveDialog()
                    viewModel.startNewGame()
                },
                onExit = {
                    viewModel.dismissPostSaveDialog()
                    onExitGame()
                }
            )
        }

        if (uiState.showPostWinSaveDialog) {
            PostWinSaveDialog(
                onNewGame = {
                    viewModel.dismissPostWinSaveDialog()
                    viewModel.startNewGame()
                },
                onExit = {
                    viewModel.dismissPostWinSaveDialog()
                    onExitGame()
                }
            )
        }
    }
}

@Composable
fun ScoreBox(label: String, score: Int, active: Boolean) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = if(active) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
        ),
        elevation = CardDefaults.cardElevation(if(active) 4.dp else 0.dp)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(label, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelMedium)
            Text("${score / 100}", style = MaterialTheme.typography.headlineMedium) // Mostramos pares (100pts = 1 par)
            Text("Pares", style = MaterialTheme.typography.bodySmall)
        }
    }
}

// ... StatItem, GameHeader, CardGrid, MemoryCard se mantienen IGUAL que tu versión anterior ...
// (Para ahorrar espacio, asumo que mantienes el código existente de esos componentes aquí)
// Solo asegúrate de copiar StatItem, GameHeader, CardGrid y MemoryCard del bloque anterior si borraste todo.

@Composable
private fun StatItem(label: String, value: String, modifier: Modifier = Modifier) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier.padding(horizontal = 4.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun GameHeader(
    moves: Int,
    matchedPairs: Int,
    score: Int,
    elapsedTime: Long,
    maxPairs: Int,
    onNewGame: () -> Unit,
    onExitGame: () -> Unit,
    onSaveClick: () -> Unit,
    onShowHistoryDialog: () -> Unit
) {
    val view = LocalView.current
    Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        Row(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp), horizontalArrangement = Arrangement.SpaceAround) {
            StatItem(label = "Puntuación", value = "$score")
            StatItem(label = "Movimientos", value = "$moves")
            StatItem(label = "Parejas", value = "$matchedPairs/$maxPairs")
            StatItem(label = "Tiempo", value = formatTime(elapsedTime))
        }
        Spacer(Modifier.height(16.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterHorizontally)) {
            Button(onClick = { view.playSoundEffect(SoundEffectConstants.CLICK); onNewGame() }) { Text("🔄 Nuevo") }
            Button(onClick = { view.playSoundEffect(SoundEffectConstants.CLICK); onSaveClick() }) { Text("💾 Guardar") }
        }
        Spacer(Modifier.height(8.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterHorizontally)) {
            Button(onClick = { view.playSoundEffect(SoundEffectConstants.CLICK); onShowHistoryDialog() }) { Text("📜 Historial") }
            Button(onClick = { view.playSoundEffect(SoundEffectConstants.CLICK); onExitGame() }) { Text("🚪 Salir") }
        }
    }
}

@Composable
fun CardGrid(cards: List<com.example.juegoks_memorama.model.Card>, columns: Int, onCardClick: (com.example.juegoks_memorama.model.Card) -> Unit, modifier: Modifier = Modifier) {
    LazyVerticalGrid(columns = GridCells.Fixed(columns), modifier = modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        items(cards) { card -> MemoryCard(card = card, onClick = { onCardClick(card) }) }
    }
}

@Composable
fun MemoryCard(card: com.example.juegoks_memorama.model.Card, onClick: () -> Unit) {
    val rotation = remember { Animatable(0f) }
    var isFaceUp by remember { mutableStateOf(card.isFaceUp) }
    LaunchedEffect(card.isFaceUp) {
        if (card.isFaceUp != isFaceUp) {
            if (card.isFaceUp) rotation.animateTo(180f, animationSpec = tween(500))
            else rotation.animateTo(0f, animationSpec = tween(500))
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
                    Icon(imageVector = Icons.Default.QuestionMark, contentDescription = "Carta", modifier = Modifier.size(32.dp))
                }
            }
        }
    }
}

@Composable
fun GameCompletedDialog(
    moves: Int,
    myScore: Int,
    opponentScore: Int,
    elapsedTime: Long,
    isMultiplayer: Boolean,
    onPlayAgain: () -> Unit,
    onSaveResult: () -> Unit,
    onExit: () -> Unit
) {
    val view = LocalView.current
    val won = myScore > opponentScore
    val tie = myScore == opponentScore

    AlertDialog(
        onDismissRequest = { if (!isMultiplayer) onPlayAgain() else onExit() },
        title = {
            if (isMultiplayer) {
                Text(if (tie) "¡Empate!" else if (won) "¡GANASTE! 🎉" else "Perdiste... 😢")
            } else {
                Text("¡Felicidades!")
            }
        },
        text = {
            Column {
                if (isMultiplayer) {
                    Text("Tiempo total: ${formatTime(elapsedTime)}")
                    Spacer(Modifier.height(8.dp))
                    Text("Tus Pares: ${myScore / 100}")
                    Text("Pares Rival: ${opponentScore / 100}")
                } else {
                    Text("Completaste el juego en $moves movimientos.")
                    Text("Puntuación Final: $myScore puntos.")
                }
            }
        },
        confirmButton = {
            Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (!isMultiplayer) {
                    Button(onClick = { view.playSoundEffect(SoundEffectConstants.CLICK); onSaveResult() }) { Text("💾 Guardar Resultado") }
                    Button(onClick = { view.playSoundEffect(SoundEffectConstants.CLICK); onPlayAgain() }) { Text("🔄 Jugar de nuevo") }
                }
                Button(onClick = { view.playSoundEffect(SoundEffectConstants.CLICK); onExit() }) { Text("🚪 Salir") }
            }
        },
        dismissButton = { }
    )
}

// ... (Resto de diálogos: SaveGameDialog, HistoryDialog, PostSaveDialog, PostWinSaveDialog) ...
// (Copia y pega los que ya tenías, son idénticos, solo necesitamos que estén presentes en el archivo)

@Composable
fun SaveGameDialog(existingSaveNames: List<String>, onSave: (String, SaveFormat) -> Unit, onDismiss: () -> Unit) {
    var selectedFormat by remember { mutableStateOf(SaveFormat.JSON) }
    var filename by rememberSaveable { mutableStateOf("") }
    val isBlank = filename.isBlank()
    val isDuplicate = existingSaveNames.any { it.equals(filename, ignoreCase = true) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Guardar Partida") },
        text = {
            Column {
                Text("Nombre de archivo:")
                OutlinedTextField(value = filename, onValueChange = { filename = it }, isError = isBlank || isDuplicate)
                Text("Formato:")
                SaveFormat.entries.forEach { format ->
                    Row(Modifier.clickable { selectedFormat = format }.fillMaxWidth().padding(4.dp)) {
                        Text("${if (selectedFormat == format) "●" else "○"} ${format.name}")
                    }
                }
            }
        },
        confirmButton = { Button(onClick = { onSave(filename, selectedFormat) }, enabled = !isBlank && !isDuplicate) { Text("Guardar") } },
        dismissButton = { Button(onClick = onDismiss) { Text("Cancelar") } }
    )
}

@Composable
fun HistoryDialog(historyItems: List<GameHistoryItem>, onLoad: (String, SaveFormat) -> Unit, onDismiss: () -> Unit) {
    var selectedFile by remember { mutableStateOf<Pair<String, SaveFormat>?>(null) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Historial") },
        text = {
            LazyColumn {
                items(historyItems) { item ->
                    val isSelected = selectedFile?.first == item.filename
                    Row(
                        Modifier.fillMaxWidth().clickable { selectedFile = item.filename to item.format }.padding(8.dp).background(if(isSelected) Color.LightGray else Color.Transparent)
                    ) {
                        Text(item.filename, Modifier.weight(1f))
                        Text(if(item.state.gameCompleted) "Win" else "Prog")
                    }
                }
            }
        },
        confirmButton = { Button(onClick = { selectedFile?.let { onLoad(it.first, it.second) } }, enabled = selectedFile != null) { Text("Cargar") } },
        dismissButton = { Button(onClick = onDismiss) { Text("Cancelar") } }
    )
}

@Composable
fun PostSaveDialog(onContinue: () -> Unit, onNewGame: () -> Unit, onExit: () -> Unit) {
    AlertDialog(
        onDismissRequest = onContinue,
        title = { Text("Guardado") },
        text = { Text("¿Qué deseas hacer?") },
        confirmButton = {
            Column {
                Button(onClick = onContinue) { Text("Continuar") }
                Button(onClick = onNewGame) { Text("Nuevo Juego") }
                Button(onClick = onExit) { Text("Salir") }
            }
        }
    )
}

@Composable
fun PostWinSaveDialog(onNewGame: () -> Unit, onExit: () -> Unit) {
    AlertDialog(
        onDismissRequest = onNewGame,
        title = { Text("Guardado") },
        text = { Text("¿Qué deseas hacer?") },
        confirmButton = {
            Column {
                Button(onClick = onNewGame) { Text("Nuevo Juego") }
                Button(onClick = onExit) { Text("Salir") }
            }
        }
    )
}