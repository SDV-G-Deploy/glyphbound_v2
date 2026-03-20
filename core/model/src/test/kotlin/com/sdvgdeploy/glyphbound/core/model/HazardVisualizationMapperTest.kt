package com.sdvgdeploy.glyphbound.core.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class HazardVisualizationMapperTest {
    @Test
    fun mapperBuildsOverlayAndLegend() {
        val level = Level(
            width = 5,
            height = 3,
            seed = 1L,
            tiles = mutableListOf(
                mutableListOf(Tile.WALL, Tile.WALL, Tile.WALL, Tile.WALL, Tile.WALL),
                mutableListOf(Tile.WALL, Tile.ENTRY, Tile.FIRE, Tile.SHOCKED_WATER, Tile.WALL),
                mutableListOf(Tile.WALL, Tile.WALL, Tile.WALL, Tile.WALL, Tile.WALL)
            ),
            entry = Pos(1, 1),
            exit = Pos(3, 1)
        )
        val state = GameState(
            level = level,
            player = Pos(1, 1),
            hazardZones = listOf(
                HazardZone(Pos(2, 1), HazardType.FIRE_ZONE, ttl = 3, damage = 1, source = "t"),
                HazardZone(Pos(3, 1), HazardType.SHOCK_ZONE, ttl = 2, damage = 1, source = "t")
            )
        )

        val vm = HazardVisualizationMapper.fromState(state)

        assertEquals('^', vm.overlays[Pos(2, 1)])
        assertEquals('!', vm.overlays[Pos(3, 1)])
        assertTrue(vm.legendSummary().contains("F1"))
        assertTrue(vm.legendSummary().contains("S1"))
    }

    @Test
    fun mapper_marksMixedOverlay_whenTileHasFireAndShock() {
        val state = GameState(
            level = Level(
                width = 3,
                height = 3,
                seed = 1L,
                tiles = mutableListOf(
                    mutableListOf(Tile.WALL, Tile.WALL, Tile.WALL),
                    mutableListOf(Tile.WALL, Tile.FLOOR, Tile.WALL),
                    mutableListOf(Tile.WALL, Tile.WALL, Tile.WALL)
                ),
                entry = Pos(1, 1),
                exit = Pos(1, 1)
            ),
            player = Pos(1, 1),
            hazardZones = listOf(
                HazardZone(Pos(1, 1), HazardType.FIRE_ZONE, ttl = 3, damage = 1, source = "t"),
                HazardZone(Pos(1, 1), HazardType.SHOCK_ZONE, ttl = 2, damage = 1, source = "t")
            )
        )

        val vm = HazardVisualizationMapper.fromState(state)

        assertEquals('&', vm.overlays[Pos(1, 1)])
        assertEquals("HZ F1 S1 ttl:3", vm.legendSummary())
    }
}
