package dev.ujhhgtg.aimassistant.models

import com.google.mlkit.vision.pose.Pose

data class PoseResult(
    val pose: List<PoseLandmark>,
    val imageWidth: Int,
    val imageHeight: Int
)
