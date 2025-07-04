package com.animecharacter.models

data class Reward(
    val id: String,
    val name: String,
    val description: String,
    val type: RewardType,
    val value: Int,
    val unlockCondition: String // مثلاً: "points:100", "milestone:5"
)

enum class RewardType {
    POINTS,
    ANIMATION,
    EXPRESSION,
    CHARACTER_UNLOCK,
    CUSTOMIZATION_OPTION,
    FEATURE_UNLOCK
}


