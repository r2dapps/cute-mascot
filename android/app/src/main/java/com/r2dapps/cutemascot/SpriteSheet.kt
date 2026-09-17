package com.r2dapps.cutemascot

import android.content.Context
import android.graphics.*

/**
 * Loads and slices sprite sheets for a given character.
 *
 * Character asset layout in assets/:
 *   Default char1:  mascot-directions.png  /  mascot-reactions.png
 *   char2+:         characters/<name>/directions.png  /  reactions.jpg
 *
 * Grid layout (same as desktop):
 *   directions: 3x3 grid → 9 tiles (up-left, up, up-right, left, center, right, down-left, down, down-right)
 *   reactions:  3x3 grid → 9 tiles (calm, heart, sparkle, surprise, starstruck, blush, sleep, dizzy, grin)
 */
class SpriteSheet(private val context: Context, private val characterId: String = "char1") {

    // Direction tile mapping — 3x3 grid row-major
    private val directionKeys = listOf(
        "upleft", "up", "upright",
        "left", "center", "right",
        "downleft", "down", "downright"
    )
    private val reactionKeys = listOf(
        "calm", "heart", "sparkle",
        "surprise", "starstruck", "blush",
        "sleep", "dizzy", "grin"
    )

    private val dirTiles = HashMap<String, Bitmap>()
    private val reactTiles = HashMap<String, Bitmap>()

    init {
        loadDirections()
        loadReactions()
    }

    private fun dirPath() = if (characterId == "char1") "mascot-directions.png"
                            else "characters/$characterId/directions.png"

    private fun reactPath() = if (characterId == "char1") "mascot-reactions.png"
                              else "characters/$characterId/reactions.jpg"

    private fun loadDirections() {
        val bmp = try {
            context.assets.open(dirPath()).use { BitmapFactory.decodeStream(it) }
        } catch (e: Exception) { null } ?: return
        sliceGrid(bmp, 3, 3, directionKeys, dirTiles)
        bmp.recycle()
    }

    private fun loadReactions() {
        val bmp = try {
            context.assets.open(reactPath()).use { BitmapFactory.decodeStream(it) }
        } catch (e: Exception) { null } ?: return
        sliceGrid(bmp, 3, 3, reactionKeys, reactTiles)
        bmp.recycle()
    }

    private fun sliceGrid(src: Bitmap, cols: Int, rows: Int, keys: List<String>, map: HashMap<String, Bitmap>) {
        val tileW = src.width / cols
        val tileH = src.height / rows
        keys.forEachIndexed { i, key ->
            val col = i % cols
            val row = i / cols
            map[key] = Bitmap.createBitmap(src, col * tileW, row * tileH, tileW, tileH)
        }
    }

    fun loadPhonemeTiles(): Map<String, Bitmap> {
        val phonemes = listOf("A", "I", "U", "E", "O", "M")
        return phonemes.associateWith { p ->
            try { context.assets.open("phonemes/$p.png").use { BitmapFactory.decodeStream(it) } }
            catch (e: Exception) { null }
        }.filterValues { it != null } as Map<String, Bitmap>
    }

    fun getDirectionTile(direction: String): Bitmap? = dirTiles[direction] ?: dirTiles["center"]
    fun getReactionTile(reaction: String): Bitmap? = reactTiles[reaction] ?: reactTiles["calm"]

    fun recycle() {
        dirTiles.values.forEach { it.recycle() }
        reactTiles.values.forEach { it.recycle() }
        dirTiles.clear()
        reactTiles.clear()
    }
}

