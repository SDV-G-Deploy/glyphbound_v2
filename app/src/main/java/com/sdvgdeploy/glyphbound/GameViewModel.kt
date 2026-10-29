package com.sdvgdeploy.glyphbound

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.sdvgdeploy.glyphbound.core.model.DifficultyProfile
import com.sdvgdeploy.glyphbound.core.model.Direction
import com.sdvgdeploy.glyphbound.core.model.DungeonNode
import com.sdvgdeploy.glyphbound.core.model.DungeonNodeType
import com.sdvgdeploy.glyphbound.core.model.Enemy
import com.sdvgdeploy.glyphbound.core.model.EnemyIntent
import com.sdvgdeploy.glyphbound.core.model.EnvEffect
import com.sdvgdeploy.glyphbound.core.model.GlyphRender
import com.sdvgdeploy.glyphbound.core.model.HazardVisualization
import com.sdvgdeploy.glyphbound.core.model.HazardVisualizationMapper
import com.sdvgdeploy.glyphbound.core.model.MetaProgressionState
import com.sdvgdeploy.glyphbound.core.model.Pos
import com.sdvgdeploy.glyphbound.core.model.RewardType
import com.sdvgdeploy.glyphbound.core.model.SaveGame
import com.sdvgdeploy.glyphbound.core.rules.RunDirector
import com.sdvgdeploy.glyphbound.core.rules.RunPhase
import com.sdvgdeploy.glyphbound.core.rules.RunSession
import com.sdvgdeploy.glyphbound.core.rules.displayName
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File

data class BranchChoiceUiModel(
    val nodeId: String,
    val label: String,
    val description: String
)

data class RewardChoiceUiModel(
    val type: RewardType,
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
    val draughts: Int,
    val canUseDraught: Boolean,
    val runOver: Boolean,
    val won: Boolean,
    val runSummary: String?,
    val highContrast: Boolean
)

sealed interface GameIntent {
    /** App launch: resume the autosaved run if there is one, otherwise start [seed]/[profile]. */
    data class ContinueOrStart(val seed: Long, val profile: DifficultyProfile) : GameIntent
    data class Start(val seed: Long, val profile: DifficultyProfile) : GameIntent
    data class Move(val direction: Direction) : GameIntent
    data class ChooseReward(val type: RewardType) : GameIntent
    data class ChooseBranch(val nodeId: String) : GameIntent
    data object UseDraught : GameIntent
    data object CycleProfile : GameIntent
    data class ToggleContrast(val enabled: Boolean) : GameIntent
    data object Restart : GameIntent
    data object NextSeed : GameIntent
}

class GameViewModel(application: Application) : AndroidViewModel(application) {
    private val store = SaveStore(File(application.filesDir, SAVE_FILE))
    private var meta: MetaProgressionState = MetaProgressionState()
    private var session: RunSession = RunDirector.start(DEFAULT_SEED, DifficultyProfile.NORMAL)
    private var booted = false

    private val _uiState = MutableStateFlow(toUiState(session, highContrast = false))
    val uiState: StateFlow<GameUiState> = _uiState.asStateFlow()

    fun dispatch(intent: GameIntent) {
        val before = session
        session = when (intent) {
            is GameIntent.ContinueOrStart -> {
                if (booted) return
                booted = true
                val save = store.load()
                meta = save?.meta ?: MetaProgressionState()
                save?.activeRun?.let(RunDirector::resume) ?: RunDirector.start(intent.seed, intent.profile)
            }
            is GameIntent.Start -> RunDirector.start(intent.seed, intent.profile)
            is GameIntent.Move -> RunDirector.move(session, intent.direction)
            is GameIntent.ChooseReward -> RunDirector.chooseReward(session, intent.type)
            is GameIntent.ChooseBranch -> RunDirector.chooseBranch(session, intent.nodeId)
            GameIntent.UseDraught -> RunDirector.useDraught(session)
            GameIntent.CycleProfile -> {
                val next = DifficultyProfile.entries[(session.profile.ordinal + 1) % DifficultyProfile.entries.size]
                RunDirector.start(session.run.baseSeed, next)
            }
            GameIntent.Restart -> RunDirector.start(session.run.baseSeed, session.profile)
            GameIntent.NextSeed -> RunDirector.start(session.run.baseSeed + 1, session.profile)
            is GameIntent.ToggleContrast -> {
                _uiState.value = _uiState.value.copy(highContrast = intent.enabled)
                return
            }
        }
        persist(before, session)
        _uiState.value = toUiState(session, _uiState.value.highContrast)
    }

    /** Autosave at node entry (new checkpoint) and record meta once when a run ends. */
    private fun persist(before: RunSession, after: RunSession) {
        val runEnded = after.isOver && !before.isOver && after.run.runId == before.run.runId
        when {
            runEnded -> {
                meta = meta.recordRun(after.run, after.outcome!!)
                store.write(SaveGame(activeRun = null, meta = meta))
            }
            !after.isOver && after.checkpoint != before.checkpoint -> store.write(SaveGame(activeRun = after.checkpoint, meta = meta))
        }
    }

    private fun toUiState(session: RunSession, highContrast: Boolean): GameUiState {
        val game = session.game
        val hazardVisualization: HazardVisualization = HazardVisualizationMapper.fromState(game)
        val rewardChoices = if (session.phase == RunPhase.CHOOSING_REWARD) {
            session.rewardChoices.map { RewardChoiceUiModel(it.type, it.label, it.description) }
        } else emptyList()
        val branchChoices = if (session.phase == RunPhase.CHOOSING_BRANCH) {
            session.branchChoices.map(::toBranchChoice)
        } else emptyList()
        return GameUiState(
            map = GlyphRender.buildBuffer(game.level, game.player, game.enemies, hazardVisualization.overlays),
            player = game.player,
            hp = game.hp,
            seed = session.run.baseSeed,
            profile = game.profile,
            steps = game.moves,
            messageLog = game.messageLog,
            envEffects = game.envEffects,
            enemies = game.enemies,
            hazardSummary = hazardVisualization.legendSummary(),
            runId = session.run.runId,
            nodeLabel = session.run.currentNode().title(),
            progressionSummary = summarizeProgression(session),
            enemyIntentSummary = summarizeEnemyIntents(game.enemies),
            rewardChoices = rewardChoices,
            branchChoices = branchChoices,
            draughts = session.draughts(),
            canUseDraught = session.phase == RunPhase.EXPLORING && session.draughts() > 0,
            runOver = session.isOver,
            won = session.phase == RunPhase.VICTORY,
            runSummary = if (session.isOver) "${session.summaryLine()} • record ${meta.completedRuns}/${meta.totalRuns}" else null,
            highContrast = highContrast
        )
    }

    private fun toBranchChoice(node: DungeonNode): BranchChoiceUiModel {
        val description = when (node.type) {
            DungeonNodeType.COMBAT -> "Fight for 1 of 2 rewards (theme shifts enemy style)"
            DungeonNodeType.ELITE -> "Two foes, pick 1 of 3 rewards"
            DungeonNodeType.TREASURE -> "Light pressure, find an Ember Draught"
            DungeonNodeType.REST -> "No foes, campfire heals +${RunDirector.REST_HEAL} HP"
            DungeonNodeType.BOSS -> "Warden pack guards the exit"
            DungeonNodeType.SHRINE -> "Event node"
            DungeonNodeType.ENTRY -> "Run start"
            DungeonNodeType.EXIT -> "Leave the run"
        }
        return BranchChoiceUiModel(
            nodeId = node.id,
            label = "${node.type.displayName()} • ${node.theme.displayName()}",
            description = description
        )
    }

    private fun summarizeEnemyIntents(enemies: List<Enemy>): String {
        if (enemies.isEmpty()) return "EN none"
        val melee = enemies.count { it.intent == EnemyIntent.MELEE_ATTACK }
        val ranged = enemies.count { it.intent == EnemyIntent.RANGED_ATTACK }
        val advance = enemies.count { it.intent == EnemyIntent.ADVANCE }
        return "EN M$melee R$ranged A$advance"
    }

    private fun summarizeProgression(session: RunSession): String {
        val run = session.run
        val depth = run.currentNode().depth
        return "D$depth/3 \$${run.progression.salvage} T${run.progression.unlockedTraits.size} " +
            "Dr${session.draughts()} NX ${run.pendingNodeRewardSummary()}"
    }

    companion object {
        const val DEFAULT_SEED: Long = 1337L
        private const val SAVE_FILE = "glyphbound-save.json"
    }
}
