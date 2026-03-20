package com.sdvgdeploy.glyphbound.core.procgen

import com.sdvgdeploy.glyphbound.core.model.DifficultyProfile
import com.sdvgdeploy.glyphbound.core.model.Level
import com.sdvgdeploy.glyphbound.core.model.Pos
import com.sdvgdeploy.glyphbound.core.model.Tile
import kotlin.random.Random

object LevelGenerator {
    data class Config(
        val width: Int = 24,
        val height: Int = 16,
        val wallChance: Double = 0.30,
        val ambientRiskChance: Double = 0.05,
        val oilChance: Double = 0.05,
        val waterChance: Double = 0.05,
        val sparkChance: Double = 0.05,
        val maxAttempts: Int = 200,
        val validator: PathValidationConfig = PathValidationConfig()
    )

    fun generate(seed: Long, profile: DifficultyProfile): Level {
        return generate(seedWithProfile(seed, profile), configFor(profile))
    }

    fun generate(seed: Long, config: Config = Config()): Level {
        repeat(config.maxAttempts) { attempt ->
            val level = build(seed + attempt, config)
            if (PathValidator.validate(level, config.validator).isValid) return level
        }
        return buildCorridorFallback(seed, config)
    }

    private fun build(seed: Long, config: Config): Level {
        val rng = Random(seed)
        val tiles = MutableList(config.height) { MutableList(config.width) { Tile.FLOOR } }

        for (y in 0 until config.height) {
            for (x in 0 until config.width) {
                val border = x == 0 || y == 0 || x == config.width - 1 || y == config.height - 1
                tiles[y][x] = when {
                    border -> Tile.WALL
                    rng.nextDouble() < config.wallChance -> Tile.WALL
                    else -> rollEnvironment(rng, config)
                }
            }
        }

        val entry = Pos(1, 1)
        val exit = Pos(config.width - 2, config.height - 2)
        tiles[entry.y][entry.x] = Tile.ENTRY
        tiles[exit.y][exit.x] = Tile.EXIT

        return Level(config.width, config.height, seed, tiles, entry, exit)
    }

    private fun rollEnvironment(rng: Random, config: Config): Tile {
        val roll = rng.nextDouble()
        var acc = config.ambientRiskChance
        if (roll < acc) return Tile.RISK
        acc += config.oilChance
        if (roll < acc) return Tile.OIL
        acc += config.waterChance
        if (roll < acc) return Tile.WATER
        acc += config.sparkChance
        if (roll < acc) return Tile.SPARK
        return Tile.FLOOR
    }

    private fun buildCorridorFallback(seed: Long, config: Config): Level {
        val tiles = MutableList(config.height) { MutableList(config.width) { Tile.WALL } }
        val entry = Pos(1, 1)
        val exit = Pos(config.width - 2, config.height - 2)

        for (x in 1 until config.width - 1) {
            tiles[1][x] = Tile.FLOOR
            tiles[config.height - 2][x] = Tile.FLOOR
        }
        for (y in 1 until config.height - 1) {
            tiles[y][1] = Tile.FLOOR
            tiles[y][config.width - 2] = Tile.FLOOR
        }

        for (x in 2 until config.width - 2 step 4) {
            for (y in 2 until config.height - 2) {
                tiles[y][x] = when ((x + y) % 9) {
                    0 -> Tile.OIL
                    3 -> Tile.WATER
                    6 -> Tile.SPARK
                    else -> Tile.RISK
                }
            }
        }

        tiles[entry.y][entry.x] = Tile.ENTRY
        tiles[exit.y][exit.x] = Tile.EXIT
        return Level(config.width, config.height, seed, tiles, entry, exit)
    }

    fun seedWithProfile(seed: Long, profile: DifficultyProfile): Long {
        return seed xor ((profile.ordinal.toLong() + 1L) shl 48)
    }

    fun configFor(profile: DifficultyProfile): Config = Config(
        wallChance = profile.wallChance,
        ambientRiskChance = profile.env.ambientRiskChance,
        oilChance = profile.env.oilChance,
        waterChance = profile.env.waterChance,
        sparkChance = profile.env.sparkChance,
        validator = PathValidationConfig(
            minDisjointPaths = profile.minDisjointPaths,
            mode = if (profile.useNodeDisjoint) DisjointMode.NODE else DisjointMode.EDGE
        )
    )
}
