package com.example.pulseworkout.ui

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.ln
import kotlin.math.sin

/** Вибро-отклик и звуковой сигнал (аналог navigator.vibrate и playRestCue из app.js). */
class Feedback(context: Context) {
    private val handler = Handler(Looper.getMainLooper())
    private val vibrator: Vibrator? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        (context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager)?.defaultVibrator
    } else {
        @Suppress("DEPRECATION")
        context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
    }

    fun haptic(ms: Long = 8) {
        val v = vibrator ?: return
        runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) v.vibrate(VibrationEffect.createOneShot(ms, VibrationEffect.DEFAULT_AMPLITUDE))
            else @Suppress("DEPRECATION") v.vibrate(ms)
        }
    }

    /** Сигнал каждой полной минуты отдыха: три коротких вибрации и тон 660→880 Гц. */
    fun restMinuteCue() {
        listOf(0L, 90L, 180L).forEach { delay -> handler.postDelayed({ haptic(18) }, delay) }
        Thread { runCatching { playTone() } }.start()
    }

    private fun playTone() {
        val rate = 44100
        val duration = 0.24
        val samples = (rate * duration).toInt()
        val pcm = ShortArray(samples)
        var phase = 0.0
        for (i in 0 until samples) {
            val t = i.toDouble() / rate
            val freq = if (t < 0.16) 660 * exp(ln(880.0 / 660.0) * t / 0.16) else 880.0
            phase += 2 * PI * freq / rate
            val gain = when {
                t < 0.025 -> 0.0001 * exp(ln(0.045 / 0.0001) * t / 0.025)
                t < 0.22 -> 0.045 * exp(ln(0.0001 / 0.045) * (t - 0.025) / 0.195)
                else -> 0.0001
            }
            pcm[i] = (sin(phase) * gain * 4 * Short.MAX_VALUE).toInt().coerceIn(-32767, 32767).toShort()
        }
        val track = AudioTrack.Builder()
            .setAudioAttributes(AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION).setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION).build())
            .setAudioFormat(AudioFormat.Builder().setEncoding(AudioFormat.ENCODING_PCM_16BIT).setSampleRate(rate).setChannelMask(AudioFormat.CHANNEL_OUT_MONO).build())
            .setBufferSizeInBytes(pcm.size * 2)
            .setTransferMode(AudioTrack.MODE_STATIC)
            .build()
        track.write(pcm, 0, pcm.size)
        track.play()
        Thread.sleep(400)
        track.release()
    }
}
