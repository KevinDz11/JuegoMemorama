package com.example.juegoks_memorama.data

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothServerSocket
import android.bluetooth.BluetoothSocket
import android.util.Log
import com.example.juegoks_memorama.model.BluetoothMessage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BluetoothService @Inject constructor() {

    private val TAG = "BluetoothService"
    private val bluetoothAdapter: BluetoothAdapter? = BluetoothAdapter.getDefaultAdapter()
    private val APP_UUID: UUID = UUID.fromString("fa87c0d0-afac-11de-8a39-0800200c9a66")
    private val APP_NAME = "MemoramaKS"

    private var socket: BluetoothSocket? = null

    // REPLAY = 1: Crucial para que el mensaje StartGame no se pierda si la UI tarda en cargar
    private val _incomingMessages = MutableSharedFlow<BluetoothMessage>(replay = 1)
    val incomingMessages: SharedFlow<BluetoothMessage> = _incomingMessages

    // MODO SERVIDOR
    @SuppressLint("MissingPermission")
    suspend fun startServer() = withContext(Dispatchers.IO) {
        var serverSocket: BluetoothServerSocket? = null
        try {
            serverSocket = bluetoothAdapter?.listenUsingRfcommWithServiceRecord(APP_NAME, APP_UUID)
            val clientSocket = serverSocket?.accept() // Bloquea hasta que alguien se conecta
            serverSocket?.close() // Cerramos el socket del servidor, ya tenemos la conexión
            handleConnection(clientSocket)
        } catch (e: IOException) {
            Log.e(TAG, "Error iniciando servidor", e)
            throw e // Re-lanzar para manejar en la UI
        }
    }

    // MODO CLIENTE
    @SuppressLint("MissingPermission")
    suspend fun connectToDevice(device: BluetoothDevice) = withContext(Dispatchers.IO) {
        try {
            val clientSocket = device.createRfcommSocketToServiceRecord(APP_UUID)
            clientSocket.connect() // Bloquea hasta conectar
            handleConnection(clientSocket)
        } catch (e: IOException) {
            Log.e(TAG, "Error conectando a dispositivo", e)
            throw e // Re-lanzar para manejar en la UI
        }
    }

    private fun handleConnection(connectedSocket: BluetoothSocket?) {
        if (connectedSocket == null) return
        socket = connectedSocket
        Log.d(TAG, "Conexión establecida")

        // IMPORTANTE: Lanzamos el bucle de escucha en una corrutina separada
        CoroutineScope(Dispatchers.IO).launch {
            listenForMessages()
        }
    }

    private suspend fun listenForMessages() {
        val reader = BufferedReader(InputStreamReader(socket?.inputStream))
        while (true) {
            try {
                // Leer línea por línea
                val messageJson = reader.readLine() ?: break
                Log.d(TAG, "Recibido: $messageJson")

                try {
                    val message = Json.decodeFromString<BluetoothMessage>(messageJson)
                    _incomingMessages.emit(message)
                } catch (e: Exception) {
                    Log.e(TAG, "Error decodificando JSON", e)
                }
            } catch (e: IOException) {
                Log.e(TAG, "Error leyendo stream", e)
                break
            }
        }
    }

    fun sendMessage(message: BluetoothMessage) {
        if (socket == null) return
        try {
            // AGREGAMOS UN SALTO DE LÍNEA AL FINAL para separar mensajes
            val json = Json.encodeToString(message) + "\n"
            socket?.outputStream?.write(json.toByteArray())
            socket?.outputStream?.flush()
            Log.d(TAG, "Enviado: $json")
        } catch (e: IOException) {
            Log.e(TAG, "Error enviando mensaje", e)
        }
    }

    fun disconnect() {
        try {
            socket?.close()
            socket = null
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    @SuppressLint("MissingPermission")
    fun getPairedDevices(): List<BluetoothDevice> {
        return bluetoothAdapter?.bondedDevices?.toList() ?: emptyList()
    }
}