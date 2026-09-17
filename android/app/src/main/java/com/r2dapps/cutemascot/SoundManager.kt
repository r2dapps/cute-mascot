package com.r2dapps.cutemascot

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool
import kotlin.random.Random

/**
 * High-performance SoundPool audio manager for boop chimes and emotional reactions.
 * Zero latency, minimal memory, supports randomized pitch for lifelike anime sound effects.
 */
class SoundManager(private val context: Context) {

    private var soundPool: SoundPool? = null
    private val boopIds = mutableListOf<Int>()
    private var sparkleId = 0
    private var blushId = 0
    private var dizzyId = 0
    private var sleepyId = 0
    private var isReady = false

    init {
        val audioAttrs = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_GAME)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()

        soundPool = SoundPool.Builder()
            .setMaxStreams(4)
            .setAudioAttributes(audioAttrs)
            .build()

        soundPool?.setOnLoadCompleteListener { _, _, status ->
            if (status == 0) isReady = true
        }

        loadSounds()
    }

    private fun loadSounds() {
        val sp = soundPool ?: return
        for (i in 1..5) {
            try {
                val afd = context.assets.openFd("sounds/boop_$i.wav")
                val id = sp.load(afd, 1)
                boopIds.add(id)
            } catch (e: Exception) {
                // Ignore missing file
            }
        }
        sparkleId = loadSoundFd(sp, "sounds/sparkle.wav")
        blushId = loadSoundFd(sp, "sounds/blush.wav")
        dizzyId = loadSoundFd(sp, "sounds/dizzy.wav")
        sleepyId = loadSoundFd(sp, "sounds/sleepy.wav")
    }

    private fun loadSoundFd(sp: SoundPool, path: String): Int {
        return try {
            val afd = context.assets.openFd(path)
            sp.load(afd, 1)
        } catch (_: Exception) {
            0
        }
    }

    fun playBoop() {
        val sp = soundPool ?: return
        if (boopIds.isEmpty()) return
        val soundId = boopIds.random()
        val pitch = 0.96f + Random.nextFloat() * 0.12f
        sp.play(soundId, 1f, 1f, 1, 0, pitch)
    }

    fun playReaction(name: String) {
        val sp = soundPool ?: return
        when (name) {
            "sparkle", "heart" -> if (sparkleId != 0) sp.play(sparkleId, 1f, 1f, 1, 0, 1f) else playBoop()
            "blush" -> if (blushId != 0) sp.play(blushId, 1f, 1f, 1, 0, 1f) else playBoop()
            "dizzy" -> if (dizzyId != 0) sp.play(dizzyId, 1f, 1f, 1, 0, 1f) else playBoop()
            "sleepy" -> if (sleepyId != 0) sp.play(sleepyId, 1f, 1f, 1, 0, 1f) else playBoop()
            else -> playBoop()
        }
    }

    fun release() {
        soundPool?.release()
        soundPool = null
        boopIds.clear()
    }
}
