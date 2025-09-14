/*
 * Copyright (c) 2025 tang shimin
 *
 * This file is part of MuJing, which is licensed under GPL v3.
 */

package fsrs

import java.time.LocalDateTime
import java.time.temporal.ChronoUnit

/**
 * 学习会话管理器
 *
 * 管理用户的学习会话，包括学习进度、会话统计、学习建议等
 */
class LearningSessionManager(
    private val fsrsService: FSRSService = FSRSService()
) {

    /**
     * 开始新的学习会话
     *
     * @param allCards 所有可用的闪卡
     * @param maxNewCards 本次会话最多学习的新卡片数量
     * @param maxReviewCards 本次会话最多复习的卡片数量
     * @return 学习会话对象
     */
    fun startSession(
        allCards: List<FlashCard>,
        maxNewCards: Int = 10,
        maxReviewCards: Int = 50
    ): LearningSession {
        val currentTime = LocalDateTime.now()

        // 获取到期的复习卡片
        val dueReviewCards = fsrsService.getDueCards(allCards, currentTime)
            .filter { it.phase != CardPhase.Added.value }
            .take(maxReviewCards)

        // 获取新卡片
        val newCards = allCards
            .filter { it.phase == CardPhase.Added.value }
            .take(maxNewCards)

        // 组合本次会话的卡片
        val sessionCards = (dueReviewCards + newCards).shuffled()

        return LearningSession(
            sessionId = System.currentTimeMillis(),
            cards = sessionCards,
            startTime = currentTime,
            currentIndex = 0,
            completedCount = 0,
            correctCount = 0,
            sessionStats = mutableMapOf()
        )
    }

    /**
     * 处理用户对当前卡片的评分
     *
     * @param session 当前学习会话
     * @param selectedGrade 用户选择的评分
     * @return 更新后的会话和处理后的卡片
     */
    fun processCardReview(
        session: LearningSession,
        selectedGrade: Grade
    ): Pair<LearningSession, FlashCard> {
        val currentCard = session.getCurrentCard()
            ?: throw IllegalStateException("会话中没有当前卡片")

        // 应用评分到卡片
        val updatedCard = fsrsService.applyGrade(currentCard, selectedGrade)

        // 更新会话统计
        val updatedSession = session.copy(
            currentIndex = session.currentIndex + 1,
            completedCount = session.completedCount + 1,
            correctCount = session.correctCount + if (selectedGrade.choice != Rating.Again) 1 else 0,
            sessionStats = session.sessionStats.apply {
                put(currentCard.id, selectedGrade.choice)
            }
        )

        return updatedSession to updatedCard
    }

    /**
     * 获取学习建议
     *
     * @param cards 所有闪卡
     * @param recentSessions 最近的学习会话记录
     * @return 学习建议
     */
    fun getLearningRecommendations(
        cards: List<FlashCard>,
        recentSessions: List<LearningSession>
    ): LearningRecommendations {
        val stats = fsrsService.getLearningStat(cards)
        val currentTime = LocalDateTime.now()

        // 计算建议的学习时间
        val suggestedStudyTime = calculateSuggestedStudyTime(stats, recentSessions)

        // 计算优先级最高的卡片
        val priorityCards = getPriorityCards(cards, currentTime)

        // 计算学习负担
        val studyLoad = calculateStudyLoad(cards, currentTime)

        return LearningRecommendations(
            suggestedNewCards = calculateSuggestedNewCards(stats, studyLoad),
            suggestedReviewCards = stats.dueCards,
            estimatedStudyTime = suggestedStudyTime,
            priorityCards = priorityCards,
            studyLoad = studyLoad,
            recommendations = generateTextRecommendations(stats, studyLoad)
        )
    }

    /**
     * 计算优先级最高的卡片
     */
    private fun getPriorityCards(cards: List<FlashCard>, currentTime: LocalDateTime): List<FlashCard> {
        return cards
            .filter { fsrsService.isDue(it, currentTime) }
            .sortedBy {
                // 越久没复习的优先级越高
                ChronoUnit.DAYS.between(it.lastReview, currentTime) *
                // 难度越高的优先级越高
                it.difficulty
            }
            .take(10)
    }

    /**
     * 计算学习负担
     */
    private fun calculateStudyLoad(cards: List<FlashCard>, currentTime: LocalDateTime): StudyLoad {
        val today = fsrsService.getDueCards(cards, currentTime).size
        val tomorrow = fsrsService.getDueCards(cards, currentTime.plusDays(1)).size
        val thisWeek = fsrsService.getDueCards(cards, currentTime.plusDays(7)).size

        return StudyLoad(
            today = today,
            tomorrow = tomorrow,
            nextWeek = thisWeek,
            level = when {
                today > 50 -> StudyLoadLevel.HIGH
                today > 20 -> StudyLoadLevel.MEDIUM
                else -> StudyLoadLevel.LOW
            }
        )
    }

    /**
     * 计算建议的新卡片数量
     */
    private fun calculateSuggestedNewCards(stats: LearningStat, studyLoad: StudyLoad): Int {
        return when (studyLoad.level) {
            StudyLoadLevel.HIGH -> 0 // 高负担时不建议学新卡
            StudyLoadLevel.MEDIUM -> 5 // 中等负担时少学新卡
            StudyLoadLevel.LOW -> 10 // 低负担时正常学新卡
        }
    }

    /**
     * 计算建议的学习时间
     */
    private fun calculateSuggestedStudyTime(
        stats: LearningStat,
        recentSessions: List<LearningSession>
    ): Int {
        // 基于历史会话数据估算每张卡片的平均学习时间
        val avgTimePerCard = if (recentSessions.isNotEmpty()) {
            recentSessions.map { it.getAverageTimePerCard() }.average()
        } else {
            30.0 // 默认每张卡30秒
        }

        return ((stats.dueCards * avgTimePerCard) / 60).toInt() // 转换为分钟
    }

    /**
     * 生成文本建议
     */
    private fun generateTextRecommendations(stats: LearningStat, studyLoad: StudyLoad): List<String> {
        val recommendations = mutableListOf<String>()

        when (studyLoad.level) {
            StudyLoadLevel.HIGH -> {
                recommendations.add("你的复习负担较重，建议优先处理到期的卡片")
                recommendations.add("暂时停止学习新卡片，专注于复习")
            }
            StudyLoadLevel.MEDIUM -> {
                recommendations.add("学习负担适中，保持当前节奏")
                recommendations.add("可以适量学习新卡片")
            }
            StudyLoadLevel.LOW -> {
                recommendations.add("很好！你的学习进度领先")
                recommendations.add("可以增加新卡片的学习量")
            }
        }

        if (stats.averageDifficulty > 7.0) {
            recommendations.add("平均难度较高，建议放慢学习新卡片的速度")
        }

        if (stats.relearningCards > stats.totalCards * 0.2) {
            recommendations.add("有较多卡片需要重新学习，建议加强复习")
        }

        return recommendations
    }
}

/**
 * 学习会话数据类
 * 
 * 表示一次完整的学习会话，包含会话的基本信息、学习进度、统计数据等。
 * 会话从用户开始学习一组卡片时创建，到所有卡片学习完成时结束。
 *
 * @property sessionId 会话唯一标识符，使用创建时的时间戳
 * @property cards 本次会话要学习的卡片列表，包含新卡片和到期复习卡片
 * @property startTime 会话开始时间，记录用户开始学习的具体时刻
 * @property currentIndex 当前正在学习的卡片索引，从0开始，用于跟踪学习进度
 * @property completedCount 已完成学习的卡片数量，包括所有已评分的卡片
 * @property correctCount 正确回答的卡片数量，评分为Hard、Good、Easy的卡片数（排除Again）
 * @property sessionStats 会话统计数据，记录每张卡片的评分结果，key为卡片ID，value为用户选择的评分
 * @property endTime 会话结束时间，可选属性，会话完成时设置，用于计算总学习时间
 */
data class LearningSession(
    val sessionId: Long,
    val cards: List<FlashCard>,
    val startTime: LocalDateTime,
    val currentIndex: Int,
    val completedCount: Int,
    val correctCount: Int,
    val sessionStats: MutableMap<Long, Rating>,
    val endTime: LocalDateTime? = null
) {
    /**
     * 获取当前正在学习的卡片
     */
    fun getCurrentCard(): FlashCard? {
        return if (currentIndex < cards.size) cards[currentIndex] else null
    }

    /**
     * 检查会话是否完成
     */
    fun isCompleted(): Boolean = currentIndex >= cards.size

    /**
     * 获取会话进度百分比
     */
    fun getProgress(): Double {
        return if (cards.isEmpty()) 100.0 else (completedCount.toDouble() / cards.size * 100.0)
    }

    /**
     * 获取正确率
     */
    fun getAccuracy(): Double {
        return if (completedCount == 0) 0.0 else (correctCount.toDouble() / completedCount * 100.0)
    }

    /**
     * 获取平均每张卡片的学习时间（秒）
     */
    fun getAverageTimePerCard(): Double {
        if (completedCount == 0) return 0.0
        val endT = endTime ?: LocalDateTime.now()
        val totalSeconds = ChronoUnit.SECONDS.between(startTime, endT)
        return totalSeconds.toDouble() / completedCount
    }
}

/**
 * 学习建议数据类
 * 
 * 基于用户的学习历史、当前卡片状态和学习负担等因素，为用户提供个性化的学习建议。
 * 帮助用户合理安排学习计划，优化学习效果。
 *
 * @property suggestedNewCards 建议学习的新卡片数量，基于当前学习负担动态调整
 * @property suggestedReviewCards 建议复习的卡片数量，通常等于当前到期需要复习的卡片总数
 * @property estimatedStudyTime 预估学习时间（分钟），基于历史学习数据和待学习卡片数量计算
 * @property priorityCards 优先级最高的卡片列表，按紧急程度和难度排序，建议用户优先学习
 * @property studyLoad 当前学习负担评估，包含今天、明天、下周的待复习卡片数量和负担等级
 * @property recommendations 个性化文本建议列表，根据用户学习状态生成的具体指导建议
 */
data class LearningRecommendations(
    val suggestedNewCards: Int,
    val suggestedReviewCards: Int,
    val estimatedStudyTime: Int, // 分钟
    val priorityCards: List<FlashCard>,
    val studyLoad: StudyLoad,
    val recommendations: List<String>
)

/**
 * 学习负担数据类
 * 
 * 评估用户当前和未来的学习负担，帮助系统合理安排学习计划和提供负担管理建议。
 * 通过分析不同时间段的到期卡片数量，为用户提供学习压力的量化指标。
 *
 * @property today 今天需要复习的卡片数量，包括所有已到期和即将到期的卡片
 * @property tomorrow 明天需要复习的卡片数量，用于预测短期学习负担
 * @property nextWeek 下周需要复习的卡片数量，用于评估中期学习负担趋势
 * @property level 学习负担等级，基于今天的卡片数量自动计算（LOW/MEDIUM/HIGH）
 */
data class StudyLoad(
    val today: Int,
    val tomorrow: Int,
    val nextWeek: Int,
    val level: StudyLoadLevel
)

/**
 * 学习负担等级枚举
 */
enum class StudyLoadLevel {
    LOW,    // 轻松
    MEDIUM, // 适中
    HIGH    // 繁重
}
