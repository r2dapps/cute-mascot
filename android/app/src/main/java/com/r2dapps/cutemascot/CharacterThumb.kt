package com.r2dapps.cutemascot

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory

/** Loads the center tile of a character directions sheet for grid previews. */
object CharacterThumb {

    data class Option(val id: String, val label: String, val assetCandidates: List<String>)

    private fun formatLabel(id: String): String {
        return id.split("-", "_").joinToString(" ") { part ->
            part.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
        }
    }

    fun getAllOptions(context: Context): List<Option> {
        val list = mutableListOf<Option>()

        // 1. Scan assets/characters/
        try {
            val assetDirs = context.assets.list("characters") ?: emptyArray()
            for (id in assetDirs.sorted()) {
                if (id.startsWith(".")) continue
                val candidates = listOf(
                    "characters/$id/directions.png",
                    "characters/$id/directions.jpg"
                )
                var exists = false
                for (p in candidates) {
                    try {
                        context.assets.open(p).close()
                        exists = true
                        break
                    } catch (_: Exception) {}
                }
                if (exists) {
                    list.add(Option(id, formatLabel(id), candidates))
                }
            }
        } catch (_: Exception) {}

        // Fallback for legacy mascot if in root assets and not already listed
        if (list.none { it.id == "mascot" }) {
            try {
                context.assets.open("mascot-directions.png").close()
                list.add(0, Option("mascot", "Fox", listOf("mascot-directions.png")))
            } catch (_: Exception) {}
        }

        // 2. Custom characters in app storage
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
            val candidates = listOf(
                "characters/$characterId/directions.png",
                "characters/$characterId/directions.jpg",
                "mascot-directions.png"
            )
            candidates.firstNotNullOfOrNull { path ->
                try {
                    context.assets.open(path).use { BitmapFactory.decodeStream(it) }
                } catch (_: Exception) {
                    null
                }
            }
        }) ?: return null

        val tileW = src.width / 3
        val tileH = src.height / 3
        if (tileW <= 0 || tileH <= 0) {
            src.recycle()
            return null
        }
        val tile = Bitmap.createBitmap(src, tileW, tileH, tileW, tileH)
        if (src !== tile) src.recycle()
        return if (tile.width > maxPx) {
            val scaled = Bitmap.createScaledBitmap(tile, maxPx, maxPx, true)
            if (scaled !== tile) tile.recycle()
            scaled
        } else tile
    }
}

