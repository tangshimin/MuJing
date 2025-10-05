/*
 * Copyright (c) 2023-2025 tang shimin
 *
 * This file is part of MuJing, which is licensed under GPL v3.
 */

package fsrs

import java.time.LocalDateTime

/**
 * FSRS 业务服务类
 *
 * 提供高级的闪卡学习业务逻辑，封装 FSRS 算法的使用细节
 *
 * @param requestRetention 目标记忆保持率（0.0-1.0），默认0.9表示90%保持率
 * @param customParams 自定义FSRS参数，为空则使用默认参数
 * @param isReview 是否为复习模式
 */
class FSRSService(
    private val requestRetention: Double = 0.9,
    private val customParams: List<Double>? = null,
    private val isReview: Boolean = false
) {

    /** FSRS-6 的默认参数 */
    private val defaultParams = listOf(
        0.212, 1.2931, 2.3065, 8.2956,
        6.4133, 0.8334, 3.0194, 0.001,
        1.8722, 0.1666, 0.796, 1.4835,
        0.0614, 0.2629, 1.6483, 0.6014,
        1.8729, 0.5425, 0.0912, 0.0658,
        0.1542
    )

    /** FSRS算法实例 */
    private val fsrs = FSRS(
        requestRetention = requestRetention,
        params = customParams ?: defaultParams,
        isReview = isReview
    )

    /**
     * 创建新闪卡
     *
     * @param id 卡片ID
     * @return 新创建的闪卡对象
     */
    fun createNewCard(id: Long = 0): FlashCard {
        return FlashCard(
            id = id,
            stability = 2.5,
            difficulty = 2.5,
            interval = 0,
            dueDate = LocalDateTime.now(),
            reviewCount = 0,
            lastReview = LocalDateTime.now(),
            phase = CardPhase.Added.value
        )
    }

    /**
     * 获取闪卡的评分选项
     *
     * @param card 要计算的闪卡
     * @return 四种评分选项的列表（Easy, Good, Hard, Again）
     */
    fun getGradeOptions(card: FlashCard): List<Grade> {
        return fsrs.calculate(card)
    }

    /**
     * 应用用户选择的评分到闪卡
     *
     * @param card 要更新的闪卡
     * @param selectedGrade 用户选择的评分
     * @return 更新后的闪卡
     */
    fun applyGrade(card: FlashCard, selectedGrade: Grade): FlashCard {
        return card.copy(
            stability = selectedGrade.stability,
            difficulty = selectedGrade.difficulty,
            interval = selectedGrade.interval,
            dueDate = FSRSTimeUtils.addMillisToNow(selectedGrade.durationMillis),
            reviewCount = card.reviewCount + 1,
            lastReview = LocalDateTime.now(),
            phase = when (selectedGrade.choice) {
                Rating.Again -> CardPhase.ReLearning.value
                else -> CardPhase.Review.value
            }
        )
    }

    /**
     * 检查闪卡是否到期需要复习
     *
     * @param card 要检查的闪卡
     * @param currentTime 当前时间，默认为系统当前时间
     * @return 是否到期
     */
    fun isDue(card: FlashCard, currentTime: LocalDateTime = LocalDateTime.now()): Boolean {
        return currentTime.isAfter(card.dueDate) || currentTime.isEqual(card.dueDate)
    }

    /**
     * 获取到期的闪卡列表
     *
     * @param cards 所有闪卡列表
     * @param currentTime 当前时间
     * @return 到期的闪卡列表
     */
    fun getDueCards(cards: List<FlashCard>, currentTime: LocalDateTime = LocalDateTime.now()): List<FlashCard> {
        return cards.filter { isDue(it, currentTime) }
    }

    /**
     * 批量处理多张闪卡的评分选项
     *
     * @param cards 闪卡列表
     * @return 每张卡片对应的评分选项映射
     */
    fun batchCalculateGrades(cards: List<FlashCard>): Map<FlashCard, List<Grade>> {
        return cards.associateWith { getGradeOptions(it) }
    }

    /**
     * 获取学习统计信息
     *
     * @param cards 所有闪卡列表
     * @return 学习统计数据
     */
    fun getLearningStat(cards: List<FlashCard>): LearningStat {
        val currentTime = LocalDateTime.now()
        val dueCards = getDueCards(cards, currentTime)
        val newCards = cards.filter { it.phase == CardPhase.Added.value }
        val reviewCards = cards.filter { it.phase == CardPhase.Review.value }
        val relearningCards = cards.filter { it.phase == CardPhase.ReLearning.value }

        return LearningStat(
            totalCards = cards.size,
            dueCards = dueCards.size,
            newCards = newCards.size,
            reviewCards = reviewCards.size,
            relearningCards = relearningCards.size,
            averageDifficulty = cards.map { it.difficulty }.average().takeIf { !it.isNaN() } ?: 0.0,
            averageStability = cards.map { it.stability }.average().takeIf { !it.isNaN() } ?: 0.0
        )
    }
}

/**
 * 学习统计数据类
 *
 * @property totalCards 总卡片数
 * @property dueCards 到期卡片数
 * @property newCards 新卡片数
 * @property reviewCards 复习阶段卡片数
 * @property relearningCards 重新学习阶段卡片数
 * @property averageDifficulty 平均难度
 * @property averageStability 平均稳定性
 */
data class LearningStat(
    val totalCards: Int,
    val dueCards: Int,
    val newCards: Int,
    val reviewCards: Int,
    val relearningCards: Int,
    val averageDifficulty: Double,
    val averageStability: Double
)
