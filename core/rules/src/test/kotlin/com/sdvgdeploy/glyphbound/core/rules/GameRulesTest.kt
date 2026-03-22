package com.sdvgdeploy.glyphbound.core.rules

import com.sdvgdeploy.glyphbound.core.model.Direction
import com.sdvgdeploy.glyphbound.core.model.Enemy
import com.sdvgdeploy.glyphbound.core.model.EnemyArchetype
import com.sdvgdeploy.glyphbound.core.model.DifficultyProfile
import com.sdvgdeploy.glyphbound.core.model.EnemyIntent
import com.sdvgdeploy.glyphbound.core.model.GameState
import com.sdvgdeploy.glyphbound.core.model.HazardType
import com.sdvgdeploy.glyphbound.core.model.HazardZone
import com.sdvgdeploy.glyphbound.core.model.Level
import com.sdvgdeploy.glyphbound.core.model.Pos
import com.sdvgdeploy.glyphbound.core.model.Tile
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class GameRulesTest {


    @Test
    fun refreshEnemyIntents_marksImmediateThreats() {
        val state = stateFrom(
            rows = listOf(
                "########",
                "#S.gs.E#",
                "########"
            ),
            profile = DifficultyProfile.NORMAL,
            enemies = listOf(
                Enemy(id = "m0", pos = Pos(3, 1), archetype = EnemyArchetype.STALKER),
                Enemy(id = "r0", pos = Pos(4, 1), archetype = EnemyArchetype.SPITTER)
            )
        )

        val refreshed = refreshEnemyIntents(state)

        assertEquals(EnemyIntent.ADVANCE, refreshed.enemies.first { it.id == "m0" }.intent)
        assertEquals(EnemyIntent.RANGED_ATTACK, refreshed.enemies.first { it.id == "r0" }.intent)
    }

    @Test
    fun movingIntoEnemy_attacksInsteadOfMoving() {
        val initial = stateFrom(
            rows = listOf(
                "#####",
                "#S.E#",
                "#####"
            ),
            profile = DifficultyProfile.NORMAL,
            enemies = listOf(Enemy(id = "e0", pos = Pos(2, 1), archetype = EnemyArchetype.STALKER, hp = 1))
        )

        val after = step(initial, Direction.RIGHT)

        assertEquals(Pos(1, 1), after.player)
        assertTrue(after.enemies.isEmpty())
        assertTrue(after.message.contains("Slain stalker"))
    }

    @Test
    fun enemyPhase_movesAndDealsContactDamage() {
        val initial = stateFrom(
            rows = listOf(
                "#######",
                "#S...E#",
                "#.....#",
                "#######"
            ),
            profile = DifficultyProfile.NORMAL,
            enemies = listOf(Enemy(id = "e0", pos = Pos(3, 1), archetype = EnemyArchetype.STALKER))
        )

        val afterFirst = step(initial, Direction.DOWN)
        val afterSecond = step(afterFirst, Direction.UP)

        assertTrue(afterFirst.enemies.any { it.pos == Pos(2, 1) })
        assertEquals(afterFirst.hp - EnemyArchetype.STALKER.contactDamage, afterSecond.hp)
        assertTrue(afterSecond.message.contains("Enemy pressure"))
    }


    @Test
    fun spitterUsesLineAttack_whenPlayerInSight() {
        val initial = stateFrom(
            rows = listOf(
                "#######",
                "#S..sE#",
                "#######"
            ),
            profile = DifficultyProfile.NORMAL,
            enemies = listOf(Enemy(id = "e0", pos = Pos(4, 1), archetype = EnemyArchetype.SPITTER))
        )

        val after = step(initial, Direction.RIGHT)

        assertEquals(initial.hp - EnemyArchetype.SPITTER.contactDamage, after.hp)
        assertTrue(after.enemies.any { it.pos == Pos(4, 1) })
        assertTrue(after.message.contains("spitter spits"))
    }

    @Test
    fun reducerMove_transitionIsPure() {
        val initial = stateFrom(
            listOf(
                "#####",
                "#SoE#",
                "#####"
            ),
            profile = DifficultyProfile.NORMAL
        )

        val reduced = reduce(initial, Direction.RIGHT)

        assertEquals(Pos(2, 1), reduced.state.player)
        assertTrue(reduced.events.isEmpty())
    }

    @Test
    fun pipelineStages_areComposable() {
        val initial = stateFrom(
            listOf(
                "#####",
                "#Sof#",
                "#####"
            ),
            profile = DifficultyProfile.EASY,
            player = Pos(1, 1)
        )

        val reduced = reduce(initial, Direction.RIGHT)
        val reacted = applyReactions(reduced)
        val ticked = tickHazards(reacted)
        val resolved = resolveDamage(ticked)
        val final = buildCombatLog(resolved)

        assertEquals(Pos(2, 1), final.player)
        assertTrue(final.messageLog.isNotEmpty())
    }

    @Test
    fun hazardTtlDecay_fireToAsh() {
        val initialBase = stateFrom(
            listOf(
                "#####",
                "#SfE#",
                "#####"
            ),
            profile = DifficultyProfile.EASY,
            player = Pos(1, 1)
        )
        val initial = initialBase.copy(
            hazardZones = listOf(
                HazardZone(
                    pos = Pos(2, 1),
                    type = HazardType.FIRE_ZONE,
                    ttl = 1,
                    damage = 1,
                    source = "test"
                )
            )
        )

        val after = step(initial, Direction.RIGHT)

        assertTrue(after.hazardZones.isEmpty())
        assertEquals(Tile.ASH, after.level.tileAt(Pos(2, 1)))
    }

    @Test
    fun tileTransitions_waterToShockedBackToWater() {
        val initial = stateFrom(
            listOf(
                "#####",
                "#S*w#",
                "#####"
            ),
            profile = DifficultyProfile.EASY
        )

        val afterSpark = step(initial, Direction.RIGHT)
        assertEquals(Tile.SHOCKED_WATER, afterSpark.level.tileAt(Pos(3, 1)))

        val cooled = step(afterSpark, Direction.LEFT)
        assertEquals(Tile.WATER, cooled.level.tileAt(Pos(3, 1)))
    }

    @Test
    fun chainReaction_isBoundedByProfile() {
        val initial = stateFrom(
            listOf(
                "#######",
                "#Sooo*#",
                "#######"
            ),
            profile = DifficultyProfile.HARD
        )

        val reduced = reduce(initial, Direction.RIGHT)
        val ignition = reduced.effects.filterIsInstance<GameEffect.IgniteOil>().flatMap { it.positions }

        assertTrue(ignition.size <= DifficultyProfile.HARD.env.fireSpreadProfile.maxTargets)
    }

    @Test
    fun deterministicSequence_sameSeedAndIntentsSameResult() {
        val intents = listOf(Direction.RIGHT, Direction.RIGHT, Direction.LEFT, Direction.RIGHT)
        val s1 = intents.fold(deterministicState()) { s, d -> step(s, d) }
        val s2 = intents.fold(deterministicState()) { s, d -> step(s, d) }

        assertEquals(s1.hp, s2.hp)
        assertEquals(s1.player, s2.player)
        assertEquals(s1.messageLog, s2.messageLog)
        assertEquals(s1.hazardZones, s2.hazardZones)
    }

    @Test
    fun longSequenceInvariants_noRunawayAndFiniteStats() {
        val intents = fixedLongSequence()
        var state = stateFrom(
            listOf(
                "#########",
                "#Sooow*E#",
                "#...w...#",
                "#########"
            ),
            profile = DifficultyProfile.HARD,
            seed = 20260320L
        )

        intents.forEach { direction ->
            state = step(state, direction)
            assertTrue(state.hazardZones.size <= 32, "hazard zones runaway: ${state.hazardZones.size}")
            assertTrue(state.hazardZones.all { it.ttl in 1..state.profile.env.fireZoneTtl.coerceAtLeast(state.profile.env.shockZoneTtl) })
            assertTrue(state.hp in -200..state.profile.startingHp)
        }
    }

    @Test
    fun deterministicFuzzSweep_seedSpaceInvariants() {
        val seedsPerProfile = 500
        val intents = listOf(Direction.RIGHT, Direction.RIGHT, Direction.LEFT, Direction.DOWN, Direction.UP, Direction.LEFT)

        DifficultyProfile.entries.forEach { profile ->
            fixedSeeds(profile, seedsPerProfile).forEach { seed ->
                var a = stateFrom(
                    rows = listOf(
                        "#########",
                        "#Sooow*E#",
                        "#...w...#",
                        "#########"
                    ),
                    profile = profile,
                    seed = seed
                )
                var b = a.copy()

                intents.forEach { direction ->
                    a = step(a, direction)
                    b = step(b, direction)
                }

                val maxConfiguredTtl = profile.env.fireZoneTtl.coerceAtLeast(profile.env.shockZoneTtl)
                assertTrue(a.hazardZones.size <= 40, "hazard zones runaway: ${a.hazardZones.size} for $profile/$seed")
                assertTrue(a.hazardZones.all { it.ttl in 1..maxConfiguredTtl }, "ttl out of bounds for $profile/$seed")
                assertTrue(a.hp in -200..profile.startingHp, "hp out of bounds: ${a.hp} for $profile/$seed")
                assertEquals(a.hazardZones, b.hazardZones, "repro mismatch hazards $profile/$seed/v${profile.env.configVersion}")
                assertEquals(a.hp, b.hp, "repro mismatch hp $profile/$seed/v${profile.env.configVersion}")
            }
        }
    }

    @Test
    fun mixedHazardDamage_appliesBonusAndCap() {
        val initial = stateFrom(
            rows = listOf(
                "#####",
                "#S.E#",
                "#####"
            ),
            profile = DifficultyProfile.HARD,
            player = Pos(1, 1)
        ).copy(
            hazardZones = listOf(
                HazardZone(Pos(1, 1), HazardType.FIRE_ZONE, ttl = 3, damage = 3, source = "t"),
                HazardZone(Pos(1, 1), HazardType.SHOCK_ZONE, ttl = 3, damage = 2, source = "t")
            )
        )

        val after = step(initial, Direction.UP)

        assertEquals(initial.hp - DifficultyProfile.HARD.env.persistentDamageCapPerTurn, after.hp)
        assertTrue(after.message.contains("Persistent hazard"))
    }

    @Test
    fun edgeSeedSuite_extremeProfilesStayBounded() {
        val edgeSeeds = listOf(Long.MIN_VALUE, -1L, 0L, 1L, 42L, 999_999_999L, Long.MAX_VALUE)
        val intents = listOf(Direction.RIGHT, Direction.DOWN, Direction.LEFT, Direction.UP, Direction.RIGHT)

        DifficultyProfile.entries.forEach { profile ->
            edgeSeeds.forEach { seed ->
                var state = stateFrom(
                    rows = listOf(
                        "#########",
                        "#Sooow*E#",
                        "#~oow~..#",
                        "#########"
                    ),
                    profile = profile,
                    seed = seed
                )

                intents.forEach { direction ->
                    state = step(state, direction)
                }

                val maxConfiguredTtl = profile.env.fireZoneTtl.coerceAtLeast(profile.env.shockZoneTtl)
                assertTrue(state.hazardZones.size <= 40, "hazard zones runaway edge $profile/$seed")
                assertTrue(state.hazardZones.all { it.ttl in 1..maxConfiguredTtl }, "ttl out of bounds edge $profile/$seed")
                assertTrue(state.hp in -200..profile.startingHp, "hp out of bounds edge $profile/$seed")
            }
        }
    }

    private fun fixedLongSequence(): List<Direction> {
        val base = listOf(Direction.RIGHT, Direction.RIGHT, Direction.LEFT, Direction.DOWN, Direction.UP)
        return List(60) { base[it % base.size] }
    }

    private fun fixedSeeds(profile: DifficultyProfile, count: Int): List<Long> {
        val profileOffset = (profile.ordinal + 1L) * 10_000L
        return List(count) { i -> profileOffset + i * 97L + 23L }
    }

    private fun deterministicState(): GameState = stateFrom(
        listOf(
            "########",
            "#So*wE##",
            "########"
        ),
        profile = DifficultyProfile.NORMAL,
        seed = 42L
    )

    private fun stateFrom(
        rows: List<String>,
        profile: DifficultyProfile,
        seed: Long = 1L,
        player: Pos? = null,
        enemies: List<Enemy> = emptyList()
    ): GameState {
        val tiles = rows.map { row ->
            row.map { ch ->
                when (ch) {
                    '#' -> Tile.WALL
                    'S' -> Tile.ENTRY
                    'E' -> Tile.EXIT
                    '.' -> Tile.FLOOR
                    '~' -> Tile.RISK
                    'o' -> Tile.OIL
                    'w' -> Tile.WATER
                    '*' -> Tile.SPARK
                    'f' -> Tile.FIRE
                    'a' -> Tile.ASH
                    'z' -> Tile.SHOCKED_WATER
                    else -> Tile.FLOOR
                }
            }.toMutableList()
        }.toMutableList()

        var entry = Pos(1, 1)
        var exit = Pos(rows[0].length - 2, rows.size - 2)
        rows.forEachIndexed { y, row ->
            row.forEachIndexed { x, ch ->
                if (ch == 'S') entry = Pos(x, y)
                if (ch == 'E') exit = Pos(x, y)
            }
        }

        val level = Level(rows[0].length, rows.size, seed, tiles, entry, exit)
        return GameState(level = level, player = player ?: entry, profile = profile, hp = profile.startingHp, enemies = enemies)
    }
}
