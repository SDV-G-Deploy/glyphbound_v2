package com.sdvgdeploy.glyphbound

import androidx.lifecycle.ViewModel
import com.sdvgdeploy.glyphbound.core.model.DifficultyProfile
import com.sdvgdeploy.glyphbound.core.model.Direction
import com.sdvgdeploy.glyphbound.core.model.DungeonNode
import com.sdvgdeploy.glyphbound.core.model.DungeonNodeType
import com.sdvgdeploy.glyphbound.core.model.DungeonTheme
import com.sdvgdeploy.glyphbound.core.model.Enemy
import com.sdvgdeploy.glyphbound.core.model.EnemyDirector
import com.sdvgdeploy.glyphbound.core.model.EnvEffect
import com.sdvgdeploy.glyphbound.core.model.GameState
import com.sdvgdeploy.glyphbound.core.model.GlyphRender
import com.sdvgdeploy.glyphbound.core.model.HazardVisualization
import com.sdvgdeploy.glyphbound.core.model.HazardVisualizationMapper
import com.sdvgdeploy.glyphbound.core.model.Pos
import com.sdvgdeploy.glyphbound.core.model.RewardType
import com.sdvgdeploy.glyphbound.core.model.RunState
import com.sdvgdeploy.glyphbound.core.procgen.LevelGenerator
import com.sdvgdeploy.glyphbound.core.rules.refreshEnemyIntents
import com.sdvgdeploy.glyphbound.core.rules.step
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class BranchChoiceUiModel(
    val nodeId: String,
    val label: String,
    val description: String
)

data class GameUiState(
    val map: List<String>,
    val player: Pos,
    val hp: Int,
    val seed: Long,
    val profile: DifficultyProfile,
    val steps: Int,
    val messageLog: List<String>,
    val envEffects: List<EnvEffect>,
    val enemies: List<Enemy>,
    val hazardSummary: String,
    val runId: String,
    val nodeLabel: String,
    val progressionSummary: String,
    val enemyIntentSummary: String,
    val rewardChoices: List<RewardChoiceUiModel>,
    val branchChoices: List<BranchChoiceUiModel>,
    val finished: Boolean,
    val won: Boolean,
    val highContrast: Boolean
)

data class RewardChoiceUiModel(
    val type: RewardType,
    val label: String,
    val description: String
)

sealed interface GameIntent {
    data class Start(val seed: Long, val profile: DifficultyProfile) : GameIntent
    data class Move(val direction: Direction) : GameIntent
    data class ChooseReward(val type: RewardType) : GameIntent
    data class ChooseBranch(val nodeId: String) : GameIntent
    data object CycleProfile : GameIntent
    data class ToggleContrast(val enabled: Boolean) : GameIntent
    data object Restart : GameIntent
}

class GameViewModel : ViewModel() {
    private var baseSeed: Long = 1337L
    private var activeRun: RunState = RunState.initial(baseSeed)
    private var pendingRewardChoices: List<RewardChoiceUiModel> = emptyList()
    private var pendingBranchChoices: List<BranchChoiceUiModel> = emptyList()
    private var coreState: GameState = bootstrap(activeRun, DifficultyProfile.NORMAL, carryHp = DifficultyProfile.NORMAL.startingHp)

    private val _uiState = MutableStateFlow(toUiState(coreState, highContrast = false, seed = baseSeed))
    val uiState: StateFlow<GameUiState> = _uiState.asStateFlow()

    fun dispatch(intent: GameIntent) {
        when (intent) {
            is GameIntent.Start -> {
                baseSeed = intent.seed
                activeRun = RunState.initial(baseSeed)
                pendingRewardChoices = emptyList()
                pendingBranchChoices = emptyList()
                coreState = bootstrap(activeRun, intent.profile, carryHp = intent.profile.startingHp)
                _uiState.value = toUiState(coreState, _uiState.value.highContrast, baseSeed)
            }

            is GameIntent.Move -> {
                if (pendingRewardChoices.isEmpty() && pendingBranchChoices.isEmpty()) {
                    coreState = step(coreState, intent.direction)
                    if (coreState.won) {
                        coreState = onNodeCleared(coreState)
                    }
                    _uiState.value = toUiState(coreState, _uiState.value.highContrast, baseSeed)
                }
            }

            is GameIntent.ChooseReward -> {
                val selected = pendingRewardChoices.firstOrNull { it.type == intent.type } ?: return
                activeRun = activeRun.applyReward(selected.type)
                val pendingSummary = activeRun.pendingNodeRewardSummary()
                val rewardLine = if (pendingSummary == "none") {
                    "Reward: ${selected.label}"
                } else {
                    "Reward: ${selected.label} (next: $pendingSummary)"
                }
                val rewardedState = coreState.copy(
                    hp = activeRun.applyRewardToHp(selected.type, coreState.hp),
                    message = rewardLine,
                    messageLog = (coreState.messageLog + rewardLine).takeLast(8)
                )
                pendingRewardChoices = emptyList()
                coreState = advanceRun(rewardedState)
                _uiState.value = toUiState(coreState, _uiState.value.highContrast, baseSeed)
            }

            is GameIntent.ChooseBranch -> {
                val selected = pendingBranchChoices.firstOrNull { it.nodeId == intent.nodeId }
                if (selected != null) {
                    activeRun = activeRun.advanceTo(selected.nodeId)
                    pendingBranchChoices = emptyList()
                    coreState = bootstrap(activeRun, coreState.profile, carryHp = coreState.hp.coerceAtLeast(1))
                    coreState = coreState.copy(
                        message = "Entered ${selected.label}",
                        messageLog = (coreState.messageLog + "Entered ${selected.label}").takeLast(8)
                    )
                    _uiState.value = toUiState(coreState, _uiState.value.highContrast, baseSeed)
                }
            }

            is GameIntent.ToggleContrast -> {
                _uiState.value = _uiState.value.copy(highContrast = intent.enabled)
            }

            GameIntent.CycleProfile -> {
                val next = DifficultyProfile.entries[(coreState.profile.ordinal + 1) % DifficultyProfile.entries.size]
                activeRun = RunState.initial(baseSeed)
                pendingRewardChoices = emptyList()
                pendingBranchChoices = emptyList()
                coreState = bootstrap(activeRun, next, carryHp = next.startingHp)
                _uiState.value = toUiState(coreState, _uiState.value.highContrast, baseSeed)
            }

            GameIntent.Restart -> {
                activeRun = RunState.initial(baseSeed)
                pendingRewardChoices = emptyList()
                pendingBranchChoices = emptyList()
                coreState = bootstrap(activeRun, coreState.profile, carryHp = coreState.profile.startingHp)
                _uiState.value = toUiState(coreState, _uiState.value.highContrast, baseSeed)
            }
        }
    }

    private fun bootstrap(run: RunState, profile: DifficultyProfile, carryHp: Int): GameState {
        val nodeEntryRewards = run.consumePendingNodeRewardEffects()
        activeRun = nodeEntryRewards.updatedRun

        val node = activeRun.currentNode()
        val level = LevelGenerator.generate(node.floorSeed, profile)
        val intro = "${node.type.displayName()} • ${node.theme.displayName()}"
        val hpAtEntry = (carryHp + nodeEntryRewards.hpBonus).coerceAtLeast(1)

        val spawnedEnemies = EnemyDirector.spawnInitial(level, profile)
        val enemyReduction = reduceEnemiesForNodeStart(
            enemies = spawnedEnemies,
            reduction = nodeEntryRewards.enemyReduction,
            nodeType = node.type
        )

        val entryEffects = buildList {
            if (nodeEntryRewards.hpBonus > 0) add("Reward surge: +${nodeEntryRewards.hpBonus} HP")
            if (enemyReduction.removedCount > 0) add("Reward scouting: -${enemyReduction.removedCount} foe")
        }

        return refreshEnemyIntents(
            GameState(
                level = level,
                player = level.entry,
                profile = profile,
                hp = hpAtEntry,
                message = entryEffects.lastOrNull() ?: intro,
                messageLog = (listOf("Reach E", intro) + entryEffects).takeLast(8),
                enemies = enemyReduction.enemies
            )
        )
    }

    private data class EnemyReductionResult(
        val enemies: List<Enemy>,
        val removedCount: Int
    )

    private fun reduceEnemiesForNodeStart(
        enemies: List<Enemy>,
        reduction: Int,
        nodeType: DungeonNodeType
    ): EnemyReductionResult {
        if (reduction <= 0 || enemies.isEmpty()) return EnemyReductionResult(enemies = enemies, removedCount = 0)

        val minimumEnemies = if (nodeType in setOf(DungeonNodeType.COMBAT, DungeonNodeType.ELITE, DungeonNodeType.BOSS)) 1 else 0
        val removable = (enemies.size - minimumEnemies).coerceAtLeast(0)
        val toRemove = reduction.coerceAtMost(removable)
        if (toRemove <= 0) return EnemyReductionResult(enemies = enemies, removedCount = 0)

        val idsToRemove = enemies
            .sortedWith(
                compareByDescending<Enemy> { it.archetype.contactDamage }
                    .thenByDescending { it.archetype.maxHp }
                    .thenBy { it.id }
            )
            .take(toRemove)
            .map { it.id }
            .toSet()

        return EnemyReductionResult(
            enemies = enemies.filterNot { it.id in idsToRemove },
            removedCount = toRemove
        )
    }

    private fun advanceRun(state: GameState): GameState {
        val completed = activeRun.completeCurrent()
        val nextNodes = completed.graph.neighbors(completed.graph.currentNodeId).filter { it.id !in completed.completedNodeIds }
        if (nextNodes.isEmpty() || nextNodes.all { it.type == DungeonNodeType.EXIT }) {
            activeRun = completed
            pendingBranchChoices = emptyList()
            return state.copy(
                finished = true,
                won = true,
                message = "Run cleared",
                messageLog = (state.messageLog + "Run cleared").takeLast(8)
            )
        }

        if (nextNodes.size > 1) {
            activeRun = completed
            pendingBranchChoices = nextNodes.map(::toBranchChoice)
            return state.copy(
                finished = true,
                won = true,
                message = "Choose next path",
                messageLog = (state.messageLog + "Choose next path").takeLast(8)
            )
        }

        val nextNode = nextNodes.first()
        activeRun = completed.advanceTo(nextNode.id)
        pendingBranchChoices = emptyList()
        val nextState = bootstrap(activeRun, state.profile, carryHp = state.hp.coerceAtLeast(1))
        val transition = "Advanced to ${nextNode.type.displayName()} • ${nextNode.theme.displayName()}"
        return nextState.copy(message = transition, messageLog = (state.messageLog + transition).takeLast(8))
    }

    private fun toBranchChoice(node: DungeonNode): BranchChoiceUiModel {
        val label = "${node.type.displayName()} • ${node.theme.displayName()}"
        val description = when (node.type) {
            DungeonNodeType.COMBAT -> "Fight for a reward pick"
            DungeonNodeType.ELITE -> "Hard fight, stronger reward"
            DungeonNodeType.TREASURE -> "Safer detour, no combat reward"
            DungeonNodeType.REST -> "Lower pressure recovery node"
            DungeonNodeType.BOSS -> "Final fight before exit"
            DungeonNodeType.SHRINE -> "Event node"
            DungeonNodeType.ENTRY -> "Run start"
            DungeonNodeType.EXIT -> "Leave the run"
        }
        return BranchChoiceUiModel(nodeId = node.id, label = label, description = description)
    }

    private fun onNodeCleared(state: GameState): GameState {
        val rewards = activeRun.rewardChoicesForCurrent()
        if (rewards.isEmpty()) {
            return advanceRun(state)
        }

        pendingRewardChoices = rewards.map { RewardChoiceUiModel(it.type, it.label, it.description) }
        return state.copy(
            finished = true,
            won = true,
            message = "Choose reward",
            messageLog = (state.messageLog + "Choose reward").takeLast(8)
        )
    }

    private fun summarizeEnemyIntents(enemies: List<Enemy>): String {
        if (enemies.isEmpty()) return "EN none"
        val melee = enemies.count { it.intent == com.sdvgdeploy.glyphbound.core.model.EnemyIntent.MELEE_ATTACK }
        val ranged = enemies.count { it.intent == com.sdvgdeploy.glyphbound.core.model.EnemyIntent.RANGED_ATTACK }
        val advance = enemies.count { it.intent == com.sdvgdeploy.glyphbound.core.model.EnemyIntent.ADVANCE }
        return "EN M$melee R$ranged A$advance"
    }

    private fun summarizeProgression(run: RunState): String {
        val traits = run.progression.unlockedTraits.size
        return "PG L${run.progression.level} X${run.progression.experience} \$${run.progression.salvage} T$traits NX ${run.pendingNodeRewardSummary()}"
    }

    private fun toUiState(gameState: GameState, highContrast: Boolean, seed: Long): GameUiState {
        val hazardVisualization: HazardVisualization = HazardVisualizationMapper.fromState(gameState)
        return GameUiState(
            map = GlyphRender.buildBuffer(gameState.level, gameState.player, gameState.enemies, hazardVisualization.overlays),
            player = gameState.player,
            hp = gameState.hp,
            seed = seed,
            profile = gameState.profile,
            steps = gameState.moves,
            messageLog = gameState.messageLog,
            envEffects = gameState.envEffects,
            enemies = gameState.enemies,
            hazardSummary = hazardVisualization.legendSummary(),
            runId = activeRun.runId,
            nodeLabel = activeRun.currentNode().title(),
            progressionSummary = summarizeProgression(activeRun),
            enemyIntentSummary = summarizeEnemyIntents(gameState.enemies),
            rewardChoices = pendingRewardChoices,
            branchChoices = pendingBranchChoices,
            finished = gameState.finished,
            won = gameState.won,
            highContrast = highContrast
        )
    }

    private fun DungeonNodeType.displayName(): String = when (this) {
        DungeonNodeType.ENTRY -> "Entry"
        DungeonNodeType.COMBAT -> "Combat"
        DungeonNodeType.ELITE -> "Elite"
        DungeonNodeType.TREASURE -> "Treasure"
        DungeonNodeType.SHRINE -> "Shrine"
        DungeonNodeType.REST -> "Rest"
        DungeonNodeType.BOSS -> "Boss"
        DungeonNodeType.EXIT -> "Exit"
    }

    private fun DungeonTheme.displayName(): String = when (this) {
        DungeonTheme.NEUTRAL -> "Neutral"
        DungeonTheme.EMBER -> "Ember"
        DungeonTheme.FLOODED -> "Flooded"
        DungeonTheme.BASTION -> "Bastion"
        DungeonTheme.ROT -> "Rot"
    }
}
