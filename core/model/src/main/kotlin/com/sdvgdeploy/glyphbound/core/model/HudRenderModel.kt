package com.sdvgdeploy.glyphbound.core.model

object HudRenderModel {
    data class Input(
        val hp: Int,
        val seed: Long,
        val profile: String,
        val configVersion: Int,
        val steps: Int,
        val nodeLabel: String,
        val progressionSummary: String,
        val effectSummary: String,
        val hazardSummary: String,
        val enemyIntentSummary: String,
        val highContrast: Boolean,
        val smallScreen: Boolean
    )

    fun render(input: Input): String {
        val reproKey = "${input.seed}:${input.profile}:v${input.configVersion}"
        val contrastBadge = if (input.highContrast) "HC" else "STD"
        return if (input.smallScreen) {
            "HP ${input.hp} | ${input.profile} | ${input.nodeLabel} | ${input.progressionSummary} | $contrastBadge | S${input.steps} | ${input.hazardSummary} | ${input.enemyIntentSummary} | FX ${input.effectSummary}"
        } else {
            "HP ${input.hp}   Seed $reproKey   Node ${input.nodeLabel}   ${input.progressionSummary}   Steps ${input.steps}   $contrastBadge   FX ${input.effectSummary}   ${input.hazardSummary}   ${input.enemyIntentSummary}"
        }
    }
}
