package com.sdvgdeploy.glyphbound

import android.os.Bundle
import android.view.MotionEvent
import android.widget.Button
import android.widget.Switch
import android.widget.TextView
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.sdvgdeploy.glyphbound.core.model.DifficultyProfile
import com.sdvgdeploy.glyphbound.core.model.Direction
import com.sdvgdeploy.glyphbound.core.model.HudRenderModel
import kotlinx.coroutines.launch
import kotlin.math.abs

class MainActivity : AppCompatActivity() {
    private val viewModel: GameViewModel by viewModels()

    private lateinit var mapView: GlyphMapView
    private lateinit var hudText: TextView
    private lateinit var messageText: TextView
    private lateinit var profileButton: Button
    private lateinit var rewardAButton: Button
    private lateinit var rewardBButton: Button
    private lateinit var branchAButton: Button
    private lateinit var branchBButton: Button

    private var touchStartX = 0f
    private var touchStartY = 0f
    private val swipeThresholdPx = 48f

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        mapView = findViewById(R.id.mapView)
        hudText = findViewById(R.id.hudText)
        messageText = findViewById(R.id.messageText)
        profileButton = findViewById(R.id.profileButton)
        rewardAButton = findViewById(R.id.rewardAButton)
        rewardBButton = findViewById(R.id.rewardBButton)
        branchAButton = findViewById(R.id.branchAButton)
        branchBButton = findViewById(R.id.branchBButton)

        val baseSeed = intent?.getLongExtra("seed", 1337L) ?: 1337L
        val profile = DifficultyProfile.fromRaw(intent?.getStringExtra("profile"))

        findViewById<Button>(R.id.upButton).setOnClickListener { viewModel.dispatch(GameIntent.Move(Direction.UP)) }
        findViewById<Button>(R.id.downButton).setOnClickListener { viewModel.dispatch(GameIntent.Move(Direction.DOWN)) }
        findViewById<Button>(R.id.leftButton).setOnClickListener { viewModel.dispatch(GameIntent.Move(Direction.LEFT)) }
        findViewById<Button>(R.id.rightButton).setOnClickListener { viewModel.dispatch(GameIntent.Move(Direction.RIGHT)) }

        findViewById<Switch>(R.id.highContrastSwitch).setOnCheckedChangeListener { _, checked ->
            viewModel.dispatch(GameIntent.ToggleContrast(checked))
        }

        profileButton.setOnClickListener { viewModel.dispatch(GameIntent.CycleProfile) }
        rewardAButton.setOnClickListener { chooseReward(0) }
        rewardBButton.setOnClickListener { chooseReward(1) }
        branchAButton.setOnClickListener { chooseBranch(0) }
        branchBButton.setOnClickListener { chooseBranch(1) }
        mapView.setOnTouchListener { _, event -> handleSwipe(event) }

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { ui ->
                    val effectSummary = if (ui.envEffects.isEmpty()) "none" else ui.envEffects.joinToString { "${it.type.name.lowercase()}:${it.turnsLeft}" }
                    val isSmallScreen = resources.configuration.smallestScreenWidthDp in 0..359
                    hudText.text = HudRenderModel.render(
                        HudRenderModel.Input(
                            hp = ui.hp,
                            seed = ui.seed,
                            profile = ui.profile.name,
                            configVersion = ui.profile.env.configVersion,
                            steps = ui.steps,
                            nodeLabel = ui.nodeLabel,
                            progressionSummary = ui.progressionSummary,
                            effectSummary = effectSummary,
                            hazardSummary = ui.hazardSummary,
                            enemyIntentSummary = ui.enemyIntentSummary,
                            highContrast = ui.highContrast,
                            smallScreen = isSmallScreen
                        )
                    )
                    profileButton.text = ui.profile.name
                    messageText.text = ui.messageLog.lastOrNull() ?: "Move"
                    bindRewardButtons(ui.rewardChoices)
                    bindBranchButtons(ui.branchChoices)
                    mapView.render(buffer = ui.map, highContrast = ui.highContrast)
                }
            }
        }

        viewModel.dispatch(GameIntent.Start(seed = baseSeed, profile = profile))
    }

    private fun bindBranchButtons(branchChoices: List<BranchChoiceUiModel>) {
        val buttons = listOf(branchAButton, branchBButton)
        buttons.forEachIndexed { index, button ->
            val choice = branchChoices.getOrNull(index)
            if (choice == null) {
                button.visibility = android.view.View.GONE
            } else {
                button.visibility = android.view.View.VISIBLE
                button.text = choice.label
                button.tag = choice.nodeId
            }
        }
    }

    private fun bindRewardButtons(rewardChoices: List<RewardChoiceUiModel>) {
        val buttons = listOf(rewardAButton, rewardBButton)
        buttons.forEachIndexed { index, button ->
            val choice = rewardChoices.getOrNull(index)
            if (choice == null) {
                button.visibility = android.view.View.GONE
            } else {
                button.visibility = android.view.View.VISIBLE
                button.text = "${choice.label}\n${choice.description}"
                button.tag = choice.type
            }
        }
    }

    private fun chooseReward(index: Int) {
        val button = if (index == 0) rewardAButton else rewardBButton
        val rewardType = button.tag as? com.sdvgdeploy.glyphbound.core.model.RewardType ?: return
        viewModel.dispatch(GameIntent.ChooseReward(rewardType))
    }

    private fun chooseBranch(index: Int) {
        val button = if (index == 0) branchAButton else branchBButton
        val nodeId = button.tag as? String ?: return
        viewModel.dispatch(GameIntent.ChooseBranch(nodeId))
    }

    private fun handleSwipe(event: MotionEvent): Boolean {
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                touchStartX = event.x
                touchStartY = event.y
                return true
            }

            MotionEvent.ACTION_UP -> {
                val dx = event.x - touchStartX
                val dy = event.y - touchStartY
                val absDx = abs(dx)
                val absDy = abs(dy)
                if (absDx < swipeThresholdPx && absDy < swipeThresholdPx) return true

                val direction = if (absDx > absDy) {
                    if (dx > 0) Direction.RIGHT else Direction.LEFT
                } else {
                    if (dy > 0) Direction.DOWN else Direction.UP
                }
                viewModel.dispatch(GameIntent.Move(direction))
                return true
            }
        }
        return false
    }
}
