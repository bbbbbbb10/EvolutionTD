package com.bbb.evolutiontd

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool

object SoundManager {
    private var soundPool: SoundPool? = null
    private var gokuChargeSoundId: Int = 0

    fun init(context: Context) {
        val audioAttributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_GAME)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()

        soundPool = SoundPool.Builder()
            .setMaxStreams(5) // Одновременно 5 звуков
            .setAudioAttributes(audioAttributes)
            .build()

        // Загружаем звук из папки res/raw/
        // Если файла нет, код не упадет, просто звука не будет (try-catch)
        try {
            gokuChargeSoundId = soundPool?.load(context, R.raw.goku_charge, 1) ?: 0
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun playGokuCharge() {
        if (gokuChargeSoundId != 0) {
            // params: id, leftVol, rightVol, priority, loop, rate
            soundPool?.play(gokuChargeSoundId, 1f, 1f, 1, 0, 1f)
        }
    }
}