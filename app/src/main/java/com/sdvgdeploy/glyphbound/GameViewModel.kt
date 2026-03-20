package com.sdvgdeploy.glyphbound

import androidx.lifecycle.ViewModel
import com.sdvgdeploy.glyphbound.core.model.DifficultyProfile
import com.sdvgdeploy.glyphbound.core.model.Direction
import com.sdvgdeploy.glyphbound.core.model.EnvEffect
import com.sdvgdeploy.glyphbound.core.model.GameState
import com.sdvgdeploy.glyphbound.core.model.GlyphRender
import com.sdvgdeploy.glyphbound.core.model.Pos
import com.sdvgdeploy.glyphbound.core.procgen.LevelGenerator
import com.sdvgdeploy.glyphbound.core.rules.step
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class GameUiState(
    val map: List<String>,
    val player: Pos,
    val hp: Int,
    val seed: Long,
    val profile: DifficultyProfile,
    val steps: Int,
    val messageLog: List<String>,
    val envEffects: List<EnvEffect>,
    val finished: Boolean,
    val won: Boolean,
    val highContrast: Boolean
)

sealed interface GameIntent {
    data class Start(val seed: Long, val profile: DifficultyProfile) : GameIntent
    data class Move(val direction: Direction) : GameIntent
    data object CycleProfile : GameIntent
    data class ToggleContrast(val enabled: Boolean) : GameIntent
    data object Restart : GameIntent
}

class GameViewModel : ViewModel() {
    private var baseSeed: Long = 1337L
    private var coreState: GameState = bootstrap(baseSeed, DifficultyProfile.NORMAL)

    private val _uiState = MutableStateFlow(toUiState(coreState, highContrast = false, seed = baseSeed))
    val uiState: StateFlow<GameUiState> = _uiState.asStateFlow()

    fun dispatch(intent: GameIntent) {
        when (intent) {
            is GameIntent.Start -> {
                baseSeed = intent.seed
                coreState = bootstrap(baseSeed, intent.profile)
                _uiState.value = toUiState(coreState, _uiState.value.highContrast, baseSeed)
            }

            is GameIntent.Move -> {
                coreState = step(coreState, intent.direction)
                _uiState.value = toUiState(coreState, _uiState.value.highContrast, baseSeed)
            }

            is GameIntent.ToggleContrast -> {
                _uiState.value = _uiState.value.copy(highContrast = intent.enabled)
            }

            GameIntent.CycleProfile -> {
                val next = DifficultyProfile.entries[(coreState.profile.ordinal + 1) % DifficultyProfile.entries.size]
                coreState = bootstrap(baseSeed, next)
                _uiState.value = toUiState(coreState, _uiState.value.highContrast, baseSeed)
            }

            GameIntent.Restart -> {
                coreState = bootstrap(baseSeed, coreState.profile)
                _uiState.value = toUiState(coreState, _uiState.value.highContrast, baseSeed)
            }
        }
    }

    private fun bootstrap(seed: Long, profile: DifficultyProfile): GameState {
        val level = LevelGenerator.generate(seed, profile)
        return GameState(level = level, player = level.entry, profile = profile, messageLog = listOf("Reach E"))
    }

    private fun toUiState(gameState: GameState, highContrast: Boolean, seed: Long): GameUiState {
        return GameUiState(
            map = GlyphRender.buildBuffer(gameState.level, gameState.player),
            player = gameState.player,
            hp = gameState.hp,
            seed = seed,
            profile = gameState.profile,
            steps = gameState.moves,
            messageLog = gameState.messageLog,
            envEffects = gameState.envEffects,
            finished = gameState.finished,
            won = gameState.won,
            highContrast = highContrast
        )
    }
}
