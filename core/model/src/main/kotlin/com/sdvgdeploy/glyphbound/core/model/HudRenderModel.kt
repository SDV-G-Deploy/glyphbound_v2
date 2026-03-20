package com.sdvgdeploy.glyphbound.core.model

object HudRenderModel {
    data class Input(
        val hp: Int,
        val seed: Long,
        val profile: String,
        val configVersion: Int,
        val steps: Int,
        val effectSummary: String,
        val hazardSummary: String,
        val highContrast: Boolean,
        val smallScreen: Boolean
    )

    fun render(input: Input): String {
        val reproKey = "${input.seed}:${input.profile}:v${input.configVersion}"
        val contrastBadge = if (input.highContrast) "HC" else "STD"
        return if (input.smallScreen) {
            "HP ${input.hp} | ${input.profile} | $contrastBadge | S${input.steps} | ${input.hazardSummary} | FX ${input.effectSummary}"
        } else {
            "HP ${input.hp}   Seed $reproKey   Steps ${input.steps}   $contrastBadge   FX ${input.effectSummary}   ${input.hazardSummary}"
        }
    }
}
