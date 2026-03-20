package com.sdvgdeploy.glyphbound.core.model

data class GlyphPalette(
    val player: Int,
    val entry: Int,
    val exit: Int,
    val wall: Int,
    val floor: Int,
    val risk: Int,
    val oil: Int,
    val water: Int,
    val spark: Int,
    val fire: Int,
    val fallback: Int = 0xFFFFFFFF.toInt()
) {
    fun colorFor(glyph: Char): Int = when (glyph) {
        '@' -> player
        'S' -> entry
        'E' -> exit
        '#' -> wall
        '.' -> floor
        '~' -> risk
        'o' -> oil
        'w' -> water
        '*' -> spark
        'f' -> fire
        else -> fallback
    }
}

object GlyphRender {
    val defaultPalette = GlyphPalette(
        player = 0xFFDDEEFF.toInt(),
        entry = 0xFF90CAF9.toInt(),
        exit = 0xFF81C784.toInt(),
        wall = 0xFF9E9E9E.toInt(),
        floor = 0xFFB0BEC5.toInt(),
        risk = 0xFFEF9A9A.toInt(),
        oil = 0xFF795548.toInt(),
        water = 0xFF64B5F6.toInt(),
        spark = 0xFFFFF176.toInt(),
        fire = 0xFFFF7043.toInt()
    )

    val highContrastPalette = GlyphPalette(
        player = 0xFFFFFFFF.toInt(),
        entry = 0xFF00FFFF.toInt(),
        exit = 0xFF00FF00.toInt(),
        wall = 0xFFD3D3D3.toInt(),
        floor = 0xFFFFD740.toInt(),
        risk = 0xFFFF0000.toInt(),
        oil = 0xFF8D6E63.toInt(),
        water = 0xFF00B0FF.toInt(),
        spark = 0xFFFFFF00.toInt(),
        fire = 0xFFFF3D00.toInt()
    )

    fun buildBuffer(level: Level, player: Pos): List<String> {
        return level.tiles.mapIndexed { y, row ->
            buildString(level.width) {
                row.forEachIndexed { x, tile ->
                    append(if (player == Pos(x, y)) '@' else tile.glyph)
                }
            }
        }
    }
}
