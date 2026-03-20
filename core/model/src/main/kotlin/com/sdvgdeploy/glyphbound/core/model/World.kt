package com.sdvgdeploy.glyphbound.core.model

enum class Tile(val glyph: Char, val walkable: Boolean, val risk: Int = 0) {
    FLOOR('.', true),
    WALL('#', false),
    ENTRY('S', true),
    EXIT('E', true),
    RISK('~', true, risk = 1),
    OIL('o', true, risk = 0),
    WATER('w', true, risk = 0),
    SPARK('*', true, risk = 1),
    FIRE('f', true, risk = 2)
}

data class Pos(val x: Int, val y: Int)

data class Level(
    val width: Int,
    val height: Int,
    val seed: Long,
    val tiles: List<MutableList<Tile>>,
    val entry: Pos,
    val exit: Pos
) {
    fun tileAt(pos: Pos): Tile = tiles[pos.y][pos.x]
    fun inBounds(pos: Pos): Boolean = pos.x in 0 until width && pos.y in 0 until height
    fun isWalkable(pos: Pos): Boolean = inBounds(pos) && tileAt(pos).walkable
}

enum class EnvEffectType { IGNITION, SHOCK }

data class EnvEffect(
    val type: EnvEffectType,
    val turnsLeft: Int,
    val intensity: Int,
    val source: String
)

data class GameState(
    val level: Level,
    val player: Pos,
    val profile: DifficultyProfile = DifficultyProfile.NORMAL,
    val hp: Int = profile.startingHp,
    val moves: Int = 0,
    val finished: Boolean = false,
    val won: Boolean = false,
    val message: String = "Reach E",
    val messageLog: List<String> = listOf("Reach E"),
    val envEffects: List<EnvEffect> = emptyList()
)

enum class Direction { UP, DOWN, LEFT, RIGHT }
