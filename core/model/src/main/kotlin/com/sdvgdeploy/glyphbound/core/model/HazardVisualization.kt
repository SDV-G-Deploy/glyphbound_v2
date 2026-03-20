package com.sdvgdeploy.glyphbound.core.model

data class HazardLegendItem(
    val type: HazardType,
    val count: Int,
    val maxTtl: Int
)

data class HazardVisualization(
    val overlays: Map<Pos, Char>,
    val legend: List<HazardLegendItem>
) {
    fun legendSummary(): String {
        if (legend.isEmpty()) return "HZ none"
        val fire = legend.firstOrNull { it.type == HazardType.FIRE_ZONE }
        val shock = legend.firstOrNull { it.type == HazardType.SHOCK_ZONE }
        val maxTtl = legend.maxOfOrNull { it.maxTtl } ?: 0
        return "HZ F${fire?.count ?: 0} S${shock?.count ?: 0} ttl:$maxTtl"
    }
}

object HazardVisualizationMapper {
    fun fromState(state: GameState): HazardVisualization {
        val overlays = state.hazardZones
            .groupBy { it.pos }
            .mapValues { (_, zones) ->
                val hasFire = zones.any { it.type == HazardType.FIRE_ZONE }
                val hasShock = zones.any { it.type == HazardType.SHOCK_ZONE }
                when {
                    hasFire && hasShock -> '&'
                    hasFire -> '^'
                    hasShock -> '!'
                    else -> '?'
                }
            }

        val legend = state.hazardZones
            .groupBy { it.type }
            .map { (type, zones) ->
                HazardLegendItem(type = type, count = zones.size, maxTtl = zones.maxOf { it.ttl })
            }
            .sortedBy { if (it.type == HazardType.FIRE_ZONE) 0 else 1 }

        return HazardVisualization(overlays = overlays, legend = legend)
    }
}
