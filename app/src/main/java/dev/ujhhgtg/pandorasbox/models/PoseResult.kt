package dev.ujhhgtg.pandorasbox.models

data class PoseResult(
    val pose: List<PoseLandmark>,
    val imageWidth: Int,
    val imageHeight: Int
)
