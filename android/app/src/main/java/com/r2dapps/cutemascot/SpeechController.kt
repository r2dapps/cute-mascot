package com.r2dapps.cutemascot

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import java.io.File
import java.util.Locale

/**
 * Handles speech playback priority:
 *   1. voice_notes/ external folder (user's personal recordings)
 *   2. assets/voices_cache/ bundled MP3s
 *   3. Android TextToSpeech as final fallback (no internet needed)
 *
 * Returns estimated duration in ms for phoneme sync.
 */
class SpeechController(private val context: Context) {

    private var mediaPlayer: MediaPlayer? = null
    private var tts: TextToSpeech? = null
    private var ttsReady = false
    private var pendingVoiceType: String? = null

    private val voiceNotesDir: File
        get() = File(context.getExternalFilesDir(null), "voice_notes").also { it.mkdirs() }

    init {
        tts = TextToSpeech(context) { status ->
            ttsReady = (status == TextToSpeech.SUCCESS)
            if (ttsReady) {
                applyVoiceType(pendingVoiceType ?: "godavari")
            }
        }
    }

    // Called by MascotView when voice type changes
    fun applyVoiceType(voiceType: String) {
        pendingVoiceType = voiceType
        if (!ttsReady) return
        if (voiceType == "japanese") {
            tts?.language = Locale.JAPANESE
            tts?.setSpeechRate(0.85f)
            tts?.setPitch(1.28f)
        } else {
            tts?.language = Locale("te", "IN")
            tts?.setSpeechRate(1.1f)
            tts?.setPitch(1.1f)
        }
    }

    /**
     * Speaks [text] using the best available method.
     * [audioName] is the filename in voices_cache or voice_notes.
     * Returns estimated playback duration in ms.
     */
    fun speak(text: String, audioName: String? = null, onDone: () -> Unit = {}): Long {
        stopCurrent()

        // 1. Check voice_notes/ first
        if (audioName != null) {
            val voiceNote = File(voiceNotesDir, audioName)
            if (voiceNote.exists()) return playFile(voiceNote.absolutePath, onDone)
        }

        // 2. Check bundled voices_cache/
        if (audioName != null) {
            try {
                val afd = context.assets.openFd("voices_cache/$audioName")
                return playAssetFd(afd, onDone)
            } catch (_: Exception) {}
        }

        // 3. TTS fallback
        return speakTts(text, onDone)
    }

    private fun playFile(path: String, onDone: () -> Unit): Long {
        return try {
            val mp = createPlayer()
            mp.setDataSource(path)
            mp.prepare()
            val dur = mp.duration.toLong().coerceAtLeast(1000L)
            mp.setOnCompletionListener { onDone() }
            mp.start()
            mediaPlayer = mp
            dur
        } catch (e: Exception) {
            onDone()
            3000L
        }
    }

    private fun playAssetFd(afd: android.content.res.AssetFileDescriptor, onDone: () -> Unit): Long {
        return try {
            val mp = createPlayer()
            mp.setDataSource(afd.fileDescriptor, afd.startOffset, afd.length)
            afd.close()
            mp.prepare()
            val dur = mp.duration.toLong().coerceAtLeast(1000L)
            mp.setOnCompletionListener { onDone() }
            mp.start()
            mediaPlayer = mp
            dur
        } catch (e: Exception) {
            onDone()
            3500L
        }
    }

    private fun speakTts(text: String, onDone: () -> Unit): Long {
        if (!ttsReady) { onDone(); return 3000L }
        val uid = "mascot_${System.currentTimeMillis()}"
        tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(u: String?) {}
            override fun onDone(u: String?) { onDone() }
            override fun onError(u: String?) { onDone() }
        })
        tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, uid)
        return (text.length * 70L).coerceIn(2000L, 8000L)  // ~70ms per char estimate
    }

    private fun createPlayer(): MediaPlayer {
        return MediaPlayer().apply {
            setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                    .build()
            )
        }
    }

    fun pause() {
        try { mediaPlayer?.pause() } catch (_: Exception) {}
        tts?.stop()
    }

    fun resume() {
        try { mediaPlayer?.start() } catch (_: Exception) {}
    }

    fun stopCurrent() {
        try { mediaPlayer?.stop(); mediaPlayer?.release() } catch (_: Exception) {}
        mediaPlayer = null
        tts?.stop()
    }

    fun release() {
        stopCurrent()
        tts?.shutdown()
        tts = null
    }
}
