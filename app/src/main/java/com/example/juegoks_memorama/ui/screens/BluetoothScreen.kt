package com.example.juegoks_memorama.ui.screens

import android.Manifest
import android.annotation.SuppressLint
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.juegoks_memorama.viewmodel.MemoryGameViewModel
import com.example.juegoks_memorama.model.Difficulty
import com.example.juegoks_memorama.model.GameMode
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BluetoothScreen(
    onGameStart: () -> Unit,
    viewModel: MemoryGameViewModel = hiltViewModel(),
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var permissionGranted by remember { mutableStateOf(false) }

    // Lista de dispositivos emparejados
    var pairedDevices by remember { mutableStateOf(emptyList<android.bluetooth.BluetoothDevice>()) }

    // --- LÓGICA DE PERMISOS INTELIGENTE ---
    val permissionsToRequest = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        arrayOf(
            Manifest.permission.BLUETOOTH_SCAN,
            Manifest.permission.BLUETOOTH_CONNECT
        )
    } else {
        arrayOf(
            Manifest.permission.ACCESS_FINE_LOCATION
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { perms ->
        permissionGranted = perms.values.all { it }
        if (permissionGranted) {
            try {
                pairedDevices = viewModel.bluetoothService.getPairedDevices()
            } catch (e: SecurityException) {
                // Manejar excepción
            }
        }
    }

    LaunchedEffect(Unit) {
        permissionLauncher.launch(permissionsToRequest)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Multijugador Bluetooth") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .padding(16.dp)
        ) {
            if (!permissionGranted) {
                Text("Se requieren permisos de Bluetooth/Ubicación para jugar.")
                Spacer(modifier = Modifier.height(8.dp))
                Button(onClick = { permissionLauncher.launch(permissionsToRequest) }) {
                    Text("Dar Permisos")
                }
            } else {
                // BOTÓN SERVIDOR (HOST)
                Button(
                    onClick = {
                        if (permissionGranted) {
                            // Configuramos el modo ANTES de conectar
                            viewModel.setGameMode(GameMode.BLUETOOTH)
                            scope.launch {
                                try {
                                    Toast.makeText(context, "Esperando conexión...", Toast.LENGTH_SHORT).show()
                                    viewModel.bluetoothService.startServer()
                                    // Al conectar, inicia como HOST
                                    viewModel.startMultiplayerGame(isHost = true, Difficulty.MEDIUM)
                                    onGameStart() // Navegar SOLO cuando termine la conexión
                                } catch (e: Exception) {
                                    Toast.makeText(context, "Error al conectar: ${e.message}", Toast.LENGTH_SHORT).show()
                                }
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Crear Partida (Esperar conexión)")
                }

                Spacer(modifier = Modifier.height(20.dp))
                Text("O conéctate a un dispositivo emparejado:", style = MaterialTheme.typography.titleMedium)

                LazyColumn {
                    items(pairedDevices) { device ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                                .clickable {
                                    if (permissionGranted) {
                                        // Configuramos el modo ANTES de conectar para limpiar el tablero
                                        viewModel.setGameMode(GameMode.BLUETOOTH)

                                        scope.launch {
                                            try {
                                                Toast.makeText(context, "Conectando...", Toast.LENGTH_SHORT).show()
                                                viewModel.bluetoothService.connectToDevice(device)
                                                // SOLO navegamos si la conexión fue exitosa
                                                onGameStart()
                                            } catch (e: Exception) {
                                                Toast.makeText(context, "No se pudo conectar", Toast.LENGTH_SHORT).show()
                                                // Si falla, regresamos a modo single player o nos quedamos aquí
                                            }
                                        }
                                    }
                                }
                        ) {
                            @SuppressLint("MissingPermission")
                            val deviceName = device.name ?: "Desconocido"
                            Text(
                                text = deviceName,
                                modifier = Modifier.padding(16.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}