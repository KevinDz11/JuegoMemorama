package com.example.juegoks_memorama.data

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool
import com.example.juegoks_memorama.R
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SoundPlayer @Inject constructor(
    @ApplicationContext private val context: Context
) {
    // IDs de los sonidos que cargaremos
    private var flipSoundId: Int = 0
    private var matchSoundId: Int = 0
    private var noMatchSoundId: Int = 0
    private var winSoundId: Int = 0 // Sonido de victoria

    private val soundPool: SoundPool

    init {
        // Configurar el SoundPool
        val audioAttributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_GAME)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()

        soundPool = SoundPool.Builder()
            .setAudioAttributes(audioAttributes)
            .setMaxStreams(5) // Podemos reproducir hasta 5 sonidos a la vez
            .build()

        // Cargar los sonidos (¡asegúrate de que los nombres coincidan con tus archivos en res/raw!)
        flipSoundId = soundPool.load(context, R.raw.flip, 1)
        matchSoundId = soundPool.load(context, R.raw.match, 1)
        noMatchSoundId = soundPool.load(context, R.raw.no_match, 1)
        winSoundId = soundPool.load(context, R.raw.win, 1)

        // NOTA: Si no tienes los archivos, la app crasheará al iniciar.
        // Asegúrate de añadirlos a res/raw. Si no los tienes, comenta las líneas de load().
    }

    private fun playSound(soundId: Int) {
        if (soundId > 0) {
            soundPool.play(soundId, 1.0f, 1.0f, 1, 0, 1.0f)
        }
    }

    fun playFlipSound() {
        playSound(flipSoundId)
    }

    fun playMatchSound() {
        playSound(matchSoundId)
    }

    fun playNoMatchSound() {
        playSound(noMatchSoundId)
    }

    fun playWinSound() {
        playSound(winSoundId)
    }

    // (Opcional) Liberar recursos cuando ya no se necesiten
    fun release() {
        soundPool.release()
    }
}