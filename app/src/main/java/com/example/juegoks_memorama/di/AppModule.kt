package com.example.juegoks_memorama.di

import android.content.Context
import com.example.juegoks_memorama.data.SoundPlayer
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ViewModelComponent
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {
    @Provides
    @Singleton
    fun provideSoundPlayer(@ApplicationContext context: Context): SoundPlayer {
        return SoundPlayer(context)
    }
}