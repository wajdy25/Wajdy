package com.animecharacter.managers

import android.content.Context
import com.animecharacter.models.Reward
import com.animecharacter.models.RewardType
import com.animecharacter.utils.PreferencesHelper
import com.animecharacter.services.Live2DAnimationEngine
import com.animecharacter.managers.Live2DManager
import android.util.Log

class RewardManager(
    private val context: Context,
    private val preferencesHelper: PreferencesHelper,
    private val live2DAnimationEngine: Live2DAnimationEngine,
    private val live2DManager: Live2DManager
) {

    private val TAG = "RewardManager"

    // قائمة المكافآت المتاحة (يمكن تحميلها من ملف JSON أو قاعدة بيانات لاحقًا)
    private val availableRewards = mutableListOf<Reward>()

    init {
        loadAvailableRewards()
    }

    private fun loadAvailableRewards() {
        // أمثلة على المكافآت
        availableRewards.add(Reward(
            id = "animation_happy_dance",
            name = "رقصة السعادة",
            description = "تفتح حركة رقصة جديدة للشخصية.",
            type = RewardType.ANIMATION,
            value = 0,
            unlockCondition = "points:100"
        ))
        availableRewards.add(Reward(
            id = "expression_wink",
            name = "غمزة",
            description = "تفتح تعبيراً جديداً للوجه.",
            type = RewardType.EXPRESSION,
            value = 0,
            unlockCondition = "points:200"
        ))
        availableRewards.add(Reward(
            id = "character_naruto_unlock",
            name = "شخصية ناروتو",
            description = "تفتح شخصية ناروتو.",
            type = RewardType.CHARACTER_UNLOCK,
            value = 0,
            unlockCondition = "points:500"
        ))
        availableRewards.add(Reward(
            id = "customization_window_color",
            name = "لون النافذة المخصص",
            description = "تفتح خياراً لتغيير لون النافذة العائمة.",
            type = RewardType.CUSTOMIZATION_OPTION,
            value = 0,
            unlockCondition = "points:300"
        ))
        availableRewards.add(Reward(
            id = "feature_daily_quiz",
            name = "اختبار يومي",
            description = "تفتح ميزة الاختبار اليومي.",
            type = RewardType.FEATURE_UNLOCK,
            value = 0,
            unlockCondition = "points:400"
        ))
    }

    fun addPoints(points: Int) {
        val currentPoints = preferencesHelper.getUserPoints()
        val newPoints = currentPoints + points
        preferencesHelper.setUserPoints(newPoints)
        Log.d(TAG, "Added $points points. Total points: $newPoints")
        checkAndUnlockRewards(newPoints)
    }

    fun spendPoints(points: Int): Boolean {
        val currentPoints = preferencesHelper.getUserPoints()
        return if (currentPoints >= points) {
            val newPoints = currentPoints - points
            preferencesHelper.setUserPoints(newPoints)
            Log.d(TAG, "Spent $points points. Remaining points: $newPoints")
            true
        } else {
            Log.d(TAG, "Not enough points to spend $points. Current points: $currentPoints")
            false
        }
    }

    private fun checkAndUnlockRewards(currentPoints: Int) {
        availableRewards.forEach { reward ->
            if (!isRewardUnlocked(reward) && canUnlockReward(reward, currentPoints)) {
                unlockReward(reward)
            }
        }
    }

    private fun isRewardUnlocked(reward: Reward): Boolean {
        // تحقق مما إذا كانت المكافأة مفتوحة بالفعل (يمكن تخزينها في SharedPreferences)
        return preferencesHelper.isRewardUnlocked(reward.id)
    }

    private fun canUnlockReward(reward: Reward, currentPoints: Int): Boolean {
        val conditionParts = reward.unlockCondition.split(":")
        if (conditionParts.size == 2) {
            val type = conditionParts[0]
            val value = conditionParts[1].toIntOrNull()

            if (value != null) {
                return when (type) {
                    "points" -> currentPoints >= value
                    // يمكن إضافة شروط أخرى هنا مثل "milestone"
                    else -> false
                }
            }
        }
        return false
    }

    private fun unlockReward(reward: Reward) {
        Log.d(TAG, "Unlocking reward: ${reward.name}")
        when (reward.type) {
            RewardType.ANIMATION -> {
                // تفعيل حركة جديدة
                live2DAnimationEngine.unlockAnimation(reward.id)
            }
            RewardType.EXPRESSION -> {
                // تفعيل تعبير جديد
                live2DAnimationEngine.unlockExpression(reward.id)
            }
            RewardType.CHARACTER_UNLOCK -> {
                // فتح شخصية جديدة
                live2DManager.unlockCharacter(reward.id)
            }
            RewardType.CUSTOMIZATION_OPTION -> {
                // فتح خيار تخصيص جديد
                // يجب أن يكون هناك منطق في Live2DManager أو PreferencesHelper للتعامل مع هذا
                preferencesHelper.unlockCustomizationOption(reward.id)
            }
            RewardType.FEATURE_UNLOCK -> {
                // فتح ميزة جديدة
                preferencesHelper.unlockFeature(reward.id)
            }
            RewardType.POINTS -> {
                // نقاط (لا تحتاج لفتح، هي نفسها المكافأة)
            }
        }
        preferencesHelper.setRewardUnlocked(reward.id, true)
        // يمكن إضافة إشعار للمستخدم هنا
    }

    fun getAvailableRewards(): List<Reward> {
        return availableRewards.toList()
    }

    fun getUnlockedRewards(): List<Reward> {
        return availableRewards.filter { isRewardUnlocked(it) }
    }
}


