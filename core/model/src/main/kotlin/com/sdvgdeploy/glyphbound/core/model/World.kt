package com.sdvgdeploy.glyphbound.core.model

import kotlin.math.absoluteValue

enum class Tile(val glyph: Char, val walkable: Boolean, val risk: Int = 0) {
    FLOOR('.', true),
    WALL('#', false),
    ENTRY('S', true),
    EXIT('E', true),
    RISK('~', true, risk = 1),
    OIL('o', true, risk = 0),
    WATER('w', true, risk = 0),
    SPARK('*', true, risk = 1),
    FIRE('f', true, risk = 2),
    SHOCKED_WATER('z', true, risk = 1),
    ASH('a', true, risk = 0)
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

enum class HazardType { FIRE_ZONE, SHOCK_ZONE }

data class HazardZone(
    val pos: Pos,
    val type: HazardType,
    val ttl: Int,
    val damage: Int,
    val source: String
)

enum class EnemyArchetype(val glyph: Char, val maxHp: Int, val contactDamage: Int, val attackRange: Int = 1) {
    STALKER('g', maxHp = 2, contactDamage = 1),
    BRUTE('O', maxHp = 3, contactDamage = 2),
    SPITTER('s', maxHp = 1, contactDamage = 1, attackRange = 3)
}

enum class EnemyIntent { HOLD, ADVANCE, MELEE_ATTACK, RANGED_ATTACK }

data class Enemy(
    val id: String,
    val pos: Pos,
    val archetype: EnemyArchetype,
    val hp: Int = archetype.maxHp,
    val intent: EnemyIntent = EnemyIntent.HOLD
) {
    val isAlive: Boolean get() = hp > 0
}

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
    val envEffects: List<EnvEffect> = emptyList(),
    val hazardZones: List<HazardZone> = emptyList(),
    val enemies: List<Enemy> = emptyList()
)

enum class Direction { UP, DOWN, LEFT, RIGHT }

object EnemyDirector {
    fun spawnInitial(
        level: Level,
        profile: DifficultyProfile,
        nodeType: DungeonNodeType = DungeonNodeType.COMBAT,
        theme: DungeonTheme = DungeonTheme.NEUTRAL
    ): List<Enemy> {
        val candidates = buildList {
            for (y in 1 until level.height - 1) {
                for (x in 1 until level.width - 1) {
                    val pos = Pos(x, y)
                    if (!level.isWalkable(pos) || pos == level.entry || pos == level.exit) continue
                    add(pos)
                }
            }
        }

        if (candidates.isEmpty()) return emptyList()

        val archetypes = archetypePlan(level, profile, nodeType, theme)
        if (archetypes.isEmpty()) return emptyList()

        val ordered = orderedCandidates(candidates, level, nodeType)
        val spawnPool = preferredSpawnPool(
            ordered = ordered,
            level = level,
            required = archetypes.size,
            minimumDistance = minimumDistance(nodeType)
        )

        return archetypes.mapIndexedNotNull { index, archetype ->
            spawnPool.getOrNull(index)?.let { spawnPos ->
                Enemy(id = "enemy-$index", pos = spawnPos, archetype = archetype)
            }
        }
    }

    private fun archetypePlan(
        level: Level,
        profile: DifficultyProfile,
        nodeType: DungeonNodeType,
        theme: DungeonTheme
    ): List<EnemyArchetype> {
        if (nodeType == DungeonNodeType.TREASURE) return listOf(EnemyArchetype.STALKER)

        val fallbackPrimary = if ((level.seed.absoluteValue % 2L) == 0L) EnemyArchetype.STALKER else EnemyArchetype.SPITTER
        val primary = when (theme) {
            DungeonTheme.EMBER -> EnemyArchetype.BRUTE
            DungeonTheme.FLOODED -> EnemyArchetype.SPITTER
            DungeonTheme.BASTION -> EnemyArchetype.STALKER
            DungeonTheme.ROT -> EnemyArchetype.STALKER
            DungeonTheme.NEUTRAL -> fallbackPrimary
        }

        val support = when (theme) {
            DungeonTheme.EMBER -> EnemyArchetype.STALKER
            DungeonTheme.FLOODED -> EnemyArchetype.SPITTER
            DungeonTheme.BASTION -> EnemyArchetype.BRUTE
            DungeonTheme.ROT -> EnemyArchetype.SPITTER
            DungeonTheme.NEUTRAL -> EnemyArchetype.BRUTE
        }

        return if (profile == DifficultyProfile.HARD && nodeType in setOf(DungeonNodeType.COMBAT, DungeonNodeType.ELITE, DungeonNodeType.BOSS)) {
            listOf(primary, support)
        } else {
            listOf(primary)
        }
    }

    private fun orderedCandidates(candidates: List<Pos>, level: Level, nodeType: DungeonNodeType): List<Pos> {
        val comparator = when (nodeType) {
            DungeonNodeType.COMBAT, DungeonNodeType.ELITE, DungeonNodeType.BOSS ->
                compareBy<Pos> { manhattan(it, level.entry) }
                    .thenBy { manhattan(it, level.exit) }
                    .thenBy { it.y }
                    .thenBy { it.x }

            DungeonNodeType.TREASURE, DungeonNodeType.REST ->
                compareByDescending<Pos> { manhattan(it, level.entry) }
                    .thenBy { it.y }
                    .thenBy { it.x }

            else ->
                compareByDescending<Pos> { manhattan(it, level.entry) }
                    .thenBy { it.y }
                    .thenBy { it.x }
        }
        return candidates.sortedWith(comparator)
    }

    private fun preferredSpawnPool(
        ordered: List<Pos>,
        level: Level,
        required: Int,
        minimumDistance: Int
    ): List<Pos> {
        val strict = ordered.filter { manhattan(it, level.entry) >= minimumDistance }
        if (strict.size >= required) return strict

        val relaxed = ordered.filter { manhattan(it, level.entry) >= 3 }
        if (relaxed.size >= required) return relaxed

        return ordered
    }

    private fun minimumDistance(nodeType: DungeonNodeType): Int = when (nodeType) {
        DungeonNodeType.TREASURE, DungeonNodeType.REST -> 6
        DungeonNodeType.COMBAT, DungeonNodeType.ELITE, DungeonNodeType.BOSS -> 3
        else -> 4
    }

    private fun manhattan(a: Pos, b: Pos): Int = kotlin.math.abs(a.x - b.x) + kotlin.math.abs(a.y - b.y)
}
