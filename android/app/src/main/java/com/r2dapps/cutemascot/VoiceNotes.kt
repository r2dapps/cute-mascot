package com.r2dapps.cutemascot

import android.content.Context
import android.net.Uri
import java.io.File
import java.io.FileOutputStream

object VoiceNotes {
    fun dir(context: Context): File =
        File(context.getExternalFilesDir(null), "voice_notes").also { it.mkdirs() }

    fun listFiles(context: Context): List<File> {
        val d = dir(context)
        return d.listFiles { f ->
            f.isFile && f.extension.lowercase() in setOf("mp3", "wav", "m4a", "ogg", "aac")
        }?.sortedBy { it.name.lowercase() } ?: emptyList()
    }

    fun listAssetVoices(context: Context): List<String> {
        return try {
            context.assets.list("voices_cache")?.filter {
                it.endsWith(".mp3", true) || it.endsWith(".wav", true) || it.endsWith(".m4a", true)
            }?.sorted() ?: emptyList()
        } catch (_: Exception) {
            emptyList()
        }
    }

    /** Copy picked audio into app voice_notes. Returns saved file name or null. */
    fun importFromUri(context: Context, uri: Uri): String? {
        return try {
            val nameHint = uri.lastPathSegment?.substringAfterLast('/') ?: "note_${System.currentTimeMillis()}.mp3"
            val safe = nameHint.replace(Regex("[^a-zA-Z0-9._-]"), "_")
            val outName = if (safe.contains('.')) safe else "$safe.mp3"
            val out = File(dir(context), outName)
            context.contentResolver.openInputStream(uri)?.use { input ->
                FileOutputStream(out).use { output -> input.copyTo(output) }
            } ?: return null
            out.name
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}
