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
import kotlinx.coroutines.launch
import kotlin.math.abs

class MainActivity : AppCompatActivity() {
    private val viewModel: GameViewModel by viewModels()

    private lateinit var mapView: GlyphMapView
    private lateinit var hudText: TextView
    private lateinit var messageText: TextView
    private lateinit var profileButton: Button

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
        mapView.setOnTouchListener { _, event -> handleSwipe(event) }

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { ui ->
                    val reproKey = "${ui.seed}:${ui.profile.name}:v${ui.profile.env.configVersion}"
                    val effectSummary = if (ui.envEffects.isEmpty()) "none" else ui.envEffects.joinToString { "${it.type.name.lowercase()}:${it.turnsLeft}" }
                    hudText.text = "HP ${ui.hp}   Seed $reproKey   Steps ${ui.steps}   FX $effectSummary   ${ui.hazardSummary}"
                    profileButton.text = ui.profile.name
                    messageText.text = ui.messageLog.lastOrNull() ?: "Move"
                    mapView.render(buffer = ui.map, highContrast = ui.highContrast)
                }
            }
        }

        viewModel.dispatch(GameIntent.Start(seed = baseSeed, profile = profile))
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
