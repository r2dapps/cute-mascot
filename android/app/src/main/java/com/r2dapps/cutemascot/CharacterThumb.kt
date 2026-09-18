package com.r2dapps.cutemascot

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory

/** Loads the center tile of a character directions sheet for grid previews. */
object CharacterThumb {

    data class Option(val id: String, val label: String, val assetCandidates: List<String>)

    val OPTIONS = listOf(
        Option("chibi", "Chibi", listOf("characters/chibi/directions.png")),
        Option("mascot", "Fox", listOf("mascot-directions.png", "characters/mascot/directions.png")),
        Option("guy", "Guy", listOf("characters/guy/directions.png")),
        Option("pixel", "Pixel", listOf("characters/pixel/directions.png")),
        Option("ink", "Ink", listOf("characters/ink/directions.png"))
    )

    fun getAllOptions(context: Context): List<Option> {
        val list = OPTIONS.toMutableList()
        val customDir = java.io.File(context.getExternalFilesDir(null), "custom_characters")
        if (customDir.exists() && customDir.isDirectory) {
            customDir.listFiles()?.filter { it.isDirectory }?.forEach { dir ->
                val id = dir.name
                val label = id.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
                if (list.none { it.id == id }) {
                    list.add(Option(id, "⭐ $label", emptyList()))
                }
            }
        }
        return list
    }

    fun loadCenter(context: Context, characterId: String, maxPx: Int = 160): Bitmap? {
        val customDir = java.io.File(context.getExternalFilesDir(null), "custom_characters/$characterId")
        val customFile = java.io.File(customDir, "directions.png").takeIf { it.exists() }
            ?: java.io.File(customDir, "directions.jpg").takeIf { it.exists() }

        val src = (if (customFile != null) {
            BitmapFactory.decodeFile(customFile.absolutePath)
        } else {
            val opt = OPTIONS.find { it.id == characterId } ?: return null
            opt.assetCandidates.firstNotNullOfOrNull { path ->
                try {
                    context.assets.open(path).use { BitmapFactory.decodeStream(it) }
                } catch (_: Exception) {
                    null
                }
            }
        }) ?: return null

        val tileW = src.width / 3
        val tileH = src.height / 3
        val tile = Bitmap.createBitmap(src, tileW, tileH, tileW, tileH)
        if (src !== tile) src.recycle()
        return if (tile.width > maxPx) {
            val scaled = Bitmap.createScaledBitmap(tile, maxPx, maxPx, true)
            if (scaled !== tile) tile.recycle()
            scaled
        } else tile
    }
}
