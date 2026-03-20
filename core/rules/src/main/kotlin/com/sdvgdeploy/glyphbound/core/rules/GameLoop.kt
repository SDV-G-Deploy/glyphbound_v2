package com.sdvgdeploy.glyphbound.core.rules

import com.sdvgdeploy.glyphbound.core.model.Direction
import com.sdvgdeploy.glyphbound.core.model.EnvEffect
import com.sdvgdeploy.glyphbound.core.model.EnvEffectType
import com.sdvgdeploy.glyphbound.core.model.GameState
import com.sdvgdeploy.glyphbound.core.model.Pos
import com.sdvgdeploy.glyphbound.core.model.Tile

fun step(state: GameState, direction: Direction): GameState {
    if (state.finished) return state

    val delta = when (direction) {
        Direction.UP -> Pos(0, -1)
        Direction.DOWN -> Pos(0, 1)
        Direction.LEFT -> Pos(-1, 0)
        Direction.RIGHT -> Pos(1, 0)
    }

    val target = Pos(state.player.x + delta.x, state.player.y + delta.y)
    if (!state.level.isWalkable(target)) {
        return state.copy(
            message = "Blocked",
            moves = state.moves + 1,
            messageLog = (state.messageLog + "Blocked").takeLast(8)
        )
    }

    val tile = state.level.tileAt(target)
    val profileEnv = state.profile.env

    val triggered = triggerEffects(state, target, tile)
    val advancedEffects = (state.envEffects + triggered).mapNotNull { effect ->
        if (effect.turnsLeft <= 0) null else effect.copy(turnsLeft = effect.turnsLeft - 1)
    }

    val tileDamage = when (tile) {
        Tile.RISK, Tile.SPARK -> 1
        Tile.FIRE -> 2
        else -> 0
    } * profileEnv.hazardDamageMultiplier

    val effectDamage = advancedEffects.sumOf { it.intensity }
    val hp = state.hp - tileDamage - effectDamage
    val atExit = target == state.level.exit
    val died = hp <= 0

    val events = buildList {
        if (tileDamage > 0) add("Hazard: -$tileDamage HP")
        if (triggered.any { it.type == EnvEffectType.IGNITION }) add("Ignition! Oil caught fire.")
        if (triggered.any { it.type == EnvEffectType.SHOCK }) add("Shock! Spark in water.")
        if (effectDamage > 0) add("Effects tick: -$effectDamage HP")
    }

    val message = when {
        atExit -> "Escaped"
        died -> "You collapsed on the path"
        events.isNotEmpty() -> events.joinToString(" ")
        else -> "Move"
    }

    return state.copy(
        player = target,
        hp = hp,
        moves = state.moves + 1,
        finished = atExit || died,
        won = atExit && !died,
        message = message,
        envEffects = advancedEffects,
        messageLog = (state.messageLog + message).takeLast(8)
    )
}

private fun triggerEffects(state: GameState, pos: Pos, tile: Tile): List<EnvEffect> {
    val neighbors = listOf(
        Pos(pos.x + 1, pos.y),
        Pos(pos.x - 1, pos.y),
        Pos(pos.x, pos.y + 1),
        Pos(pos.x, pos.y - 1)
    ).filter { state.level.inBounds(it) }
        .map { state.level.tileAt(it) }
        .toSet()

    val tuning = state.profile.env
    val effects = mutableListOf<EnvEffect>()
    val hasSpark = tile == Tile.SPARK || neighbors.contains(Tile.SPARK) || tile == Tile.FIRE

    if ((tile == Tile.OIL || neighbors.contains(Tile.OIL)) && hasSpark) {
        effects += EnvEffect(
            type = EnvEffectType.IGNITION,
            turnsLeft = tuning.ignitionTurns,
            intensity = tuning.ignitionTickDamage * tuning.hazardDamageMultiplier,
            source = "oil+s" // short marker for HUD/log
        )
    }

    if ((tile == Tile.WATER || neighbors.contains(Tile.WATER)) && hasSpark) {
        effects += EnvEffect(
            type = EnvEffectType.SHOCK,
            turnsLeft = tuning.shockTurns,
            intensity = tuning.shockTickDamage,
            source = "water+s"
        )
    }

    return effects
}
