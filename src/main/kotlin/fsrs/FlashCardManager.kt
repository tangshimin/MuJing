/*
 * Copyright (c) 2025 tang shimin
 *
 * This file is part of MuJing, which is licensed under GPL v3.
 */

package fsrs

import java.time.LocalDateTime

/**
 * 闪卡管理器
 *
 * 提供闪卡的创建、更新、删除和批量操作功能
 */
class FlashCardManager(
    private val fsrsService: FSRSService = FSRSService()
) {

    /**
     * 创建新的闪卡
     *
     * @param content 卡片内容（如单词、问题等）
     * @param tags 卡片标签
     * @param category 卡片类别
     * @return 新创建的闪卡
     */
    fun createCard(
        content: String,
        tags: List<String> = emptyList(),
        category: String = "default"
    ): FlashCard {
        return fsrsService.createNewCard(System.currentTimeMillis())
    }

    /**
     * 批量创建闪卡
     *
     * @param contents 卡片内容列表
     * @param category 统一类别
     * @return 创建的闪卡列表
     */
    fun createCards(contents: List<String>, category: String = "default"): List<FlashCard> {
        val baseTime = System.currentTimeMillis()
        return contents.mapIndexed { index, content ->
            // 使用基础时间戳+索引确保ID唯一性
            fsrsService.createNewCard(baseTime + index.toLong())
        }
    }

    /**
     * 重置卡片学习进度
     *
     * @param card 要重置的卡片
     * @return 重置后的卡片
     */
    fun resetCard(card: FlashCard): FlashCard {
        return card.copy(
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
     * 批量重置卡片
     *
     * @param cards 要重置的卡片列表
     * @return 重置后的卡片列表
     */
    fun resetCards(cards: List<FlashCard>): List<FlashCard> {
        return cards.map { resetCard(it) }
    }

    /**
     * 暂停卡片（设置为很久以后复习）
     *
     * @param card 要暂停的卡片
     * @param days 暂停天数
     * @return 暂停后的卡片
     */
    fun suspendCard(card: FlashCard, days: Int = 365): FlashCard {
        return card.copy(
            dueDate = LocalDateTime.now().plusDays(days.toLong())
        )
    }

    /**
     * 恢复暂停的卡片
     *
     * @param card 要恢复的卡片
     * @return 恢复后的卡片
     */
    fun resumeCard(card: FlashCard): FlashCard {
        return card.copy(
            dueDate = LocalDateTime.now()
        )
    }

    /**
     * 获取卡片学习统计
     *
     * @param card 要分析的卡片
     * @return 卡片统计信息
     */
    fun getCardAnalytics(card: FlashCard): CardAnalytics {
        val nextReviewOptions = fsrsService.getGradeOptions(card)

        return CardAnalytics(
            cardId = card.id,
            currentPhase = CardPhase.entries.find { it.value == card.phase }!!,
            stability = card.stability,
            difficulty = card.difficulty,
            reviewCount = card.reviewCount,
            daysSinceLastReview = java.time.temporal.ChronoUnit.DAYS.between(card.lastReview, LocalDateTime.now()),
            nextReviewIn = java.time.temporal.ChronoUnit.DAYS.between(LocalDateTime.now(), card.dueDate),
            estimatedRetention = calculateCurrentRetention(card),
            nextReviewOptions = nextReviewOptions
        )
    }

    /**
     * 批量分析卡片
     *
     * @param cards 要分析的卡片列表
     * @return 批量分析结果
     */
    fun batchAnalyzeCards(cards: List<FlashCard>): BatchAnalysisResult {
        val analytics = cards.map { getCardAnalytics(it) }

        val difficultyDistribution = analytics.groupBy {
            when {
                it.difficulty < 3.0 -> "Easy"
                it.difficulty < 7.0 -> "Medium"
                else -> "Hard"
            }
        }.mapValues { it.value.size }

        val phaseDistribution = analytics.groupBy { it.currentPhase.name }
            .mapValues { it.value.size }

        val retentionStats = analytics.map { it.estimatedRetention }

        return BatchAnalysisResult(
            totalCards = cards.size,
            averageDifficulty = analytics.map { it.difficulty }.average(),
            averageStability = analytics.map { it.stability }.average(),
            averageRetention = retentionStats.average(),
            difficultyDistribution = difficultyDistribution,
            phaseDistribution = phaseDistribution,
            cardsNeedingReview = analytics.filter { it.nextReviewIn <= 0 }.size,
            overdueCards = analytics.filter { it.nextReviewIn < -1 }.size
        )
    }

    /**
     * 根据条件筛选卡片
     *
     * @param cards 所有卡片
     * @param filter 筛选条件
     * @return 筛选后的卡片列表
     */
    fun filterCards(cards: List<FlashCard>, filter: CardFilter): List<FlashCard> {
        return cards.filter { card ->
            val analytics = getCardAnalytics(card)

            (filter.phases.isEmpty() || analytics.currentPhase in filter.phases) &&
            (filter.minDifficulty == null || analytics.difficulty >= filter.minDifficulty) &&
            (filter.maxDifficulty == null || analytics.difficulty <= filter.maxDifficulty) &&
            (filter.minStability == null || analytics.stability >= filter.minStability) &&
            (filter.maxStability == null || analytics.stability <= filter.maxStability) &&
            (filter.dueOnly == null || (filter.dueOnly && analytics.nextReviewIn <= 0)) &&
            (filter.overdueOnly == null || (filter.overdueOnly && analytics.nextReviewIn < -1))
        }
    }

    /**
     * 计算当前记忆保持率
     */
    private fun calculateCurrentRetention(card: FlashCard): Double {
        if (card.stability <= 0) return 0.0

        val daysSinceLastReview = java.time.temporal.ChronoUnit.DAYS.between(card.lastReview, LocalDateTime.now())
        return kotlin.math.exp(-daysSinceLastReview.toDouble() / card.stability)
    }

    /**
     * 智能排序卡片
     *
     * @param cards 要排序的卡片列表
     * @param strategy 排序策略
     * @return 排序后的卡片列表
     */
    fun sortCards(cards: List<FlashCard>, strategy: SortStrategy): List<FlashCard> {
        return when (strategy) {
            SortStrategy.DUE_DATE_ASC -> cards.sortedBy { it.dueDate }
            SortStrategy.DUE_DATE_DESC -> cards.sortedByDescending { it.dueDate }
            SortStrategy.DIFFICULTY_ASC -> cards.sortedBy { it.difficulty }
            SortStrategy.DIFFICULTY_DESC -> cards.sortedByDescending { it.difficulty }
            SortStrategy.STABILITY_ASC -> cards.sortedBy { it.stability }
            SortStrategy.STABILITY_DESC -> cards.sortedByDescending { it.stability }
            SortStrategy.REVIEW_COUNT_ASC -> cards.sortedBy { it.reviewCount }
            SortStrategy.REVIEW_COUNT_DESC -> cards.sortedByDescending { it.reviewCount }
            SortStrategy.PRIORITY -> {
                // 智能优先级排序：优先级 = 到期程度 + 难度权重
                cards.sortedBy { card ->
                    val daysPastDue = java.time.temporal.ChronoUnit.DAYS.between(card.dueDate, LocalDateTime.now())
                    val difficultyWeight = card.difficulty / 10.0
                    -(daysPastDue.toDouble() + difficultyWeight) // 负号表示降序
                }
            }
        }
    }
}

/**
 * 卡片分析结果数据类
 *
 * 提供单张闪卡的详细分析数据，包括学习状态、性能指标和下次复习预测。
 * 用于生成学习报告、优化复习计划和展示学习进度。
 *
 * @property cardId 卡片唯一标识符
 * @property currentPhase 卡片当前学习阶段（新卡片、复习、重新学习）
 * @property stability 当前记忆稳定性，数值越高表示记忆越牢固
 * @property difficulty 卡片难度系数，范围1.0-10.0，数值越高表示越难记忆
 * @property reviewCount 累计复习次数，反映用户对该卡片的学习投入
 * @property daysSinceLastReview 距离上次复习的天数，用于计算遗忘程度
 * @property nextReviewIn 距离下次复习的天数，负数表示已过期需要立即复习
 * @property estimatedRetention 当前估算的记忆保持率（0.0-1.0），基于FSRS算法计算
 * @property nextReviewOptions 下次复习的四种评分选项及其预测结果
 */
data class CardAnalytics(
    val cardId: Long,
    val currentPhase: CardPhase,
    val stability: Double,
    val difficulty: Double,
    val reviewCount: Int,
    val daysSinceLastReview: Long,
    val nextReviewIn: Long, // 负数表示已过期
    val estimatedRetention: Double, // 当前估计的记忆保持率
    val nextReviewOptions: List<Grade>
)

/**
 * 批量分析结果数据类
 *
 * 提供多张闪卡的统计分析结果，用于生成学习报告和整体学习状态评估。
 * 包含平均值统计、分布情况和需要关注的卡片数量等关键指标。
 *
 * @property totalCards 分析的卡片总数
 * @property averageDifficulty 平均难度系数，反映整体学习难度水平
 * @property averageStability 平均稳定性，反映整体记忆牢固程度
 * @property averageRetention 平均记忆保持率，反映当前整体记忆状态
 * @property difficultyDistribution 难度分布统计，按Easy/Medium/Hard分组统计卡片数量
 * @property phaseDistribution 学习阶段分布，按Added/Review/ReLearning分组统计
 * @property cardsNeedingReview 需要复习的卡片数量（包括到期和过期）
 * @property overdueCards 已过期超过1天的卡片数量，需要优先处理
 */
data class BatchAnalysisResult(
    val totalCards: Int,
    val averageDifficulty: Double,
    val averageStability: Double,
    val averageRetention: Double,
    val difficultyDistribution: Map<String, Int>,
    val phaseDistribution: Map<String, Int>,
    val cardsNeedingReview: Int,
    val overdueCards: Int
)

/**
 * 卡片筛选条件数据类
 *
 * 定义多维度的卡片筛选条件，支持按学习阶段、难度范围、稳定性范围等进行过滤。
 * 用于实现高级搜索、生成专门的复习列表和数据分析。
 *
 * @property phases 筛选的学习阶段列表，为空时不限制阶段
 * @property minDifficulty 最小难度阈值，筛选难度≥此值的卡片
 * @property maxDifficulty 最大难度阈值，筛选难度≤此值的卡片
 * @property minStability 最小稳定性阈值，筛选稳定性≥此值的卡片
 * @property maxStability 最大稳定性阈值，筛选稳定性≤此值的卡片
 * @property dueOnly 是否只筛选到期卡片，true时只返回需要复习的卡片
 * @property overdueOnly 是否只筛选过期卡片，true时只返回已过期的卡片
 */
data class CardFilter(
    val phases: List<CardPhase> = emptyList(),
    val minDifficulty: Double? = null,
    val maxDifficulty: Double? = null,
    val minStability: Double? = null,
    val maxStability: Double? = null,
    val dueOnly: Boolean? = null,
    val overdueOnly: Boolean? = null
)

/**
 * 排序策略枚举
 *
 * 定义卡片排序的各种策略，支持按不同属性进行升序或降序排列。
 * 用于在卡片管理和学习过程中按需要对卡片进行排序，提高学习效率。
 */
enum class SortStrategy {
    /** 按到期时间升序排列，最早到期的卡片排在前面，适合优先处理紧急复习 */
    DUE_DATE_ASC,

    /** 按到期时间降序排列，最晚到期的卡片排在前面，适合查看学习进度 */
    DUE_DATE_DESC,

    /** 按难度升序排列，最简单的卡片排在前面，适合从易到难的学习策略 */
    DIFFICULTY_ASC,

    /** 按难度降序排列，最困难的卡片排在前面，适合挑战性学习 */
    DIFFICULTY_DESC,

    /** 按稳定性升序排列，记忆最不稳定的卡片排在前面，适合强化薄弱环节 */
    STABILITY_ASC,

    /** 按稳定性降序排列，记忆最稳定的卡片排在前面，适合复习巩固 */
    STABILITY_DESC,

    /** 按复习次数升序排列，最少复习的卡片排在前面，适合均衡学习进度 */
    REVIEW_COUNT_ASC,

    /** 按复习次数降序排列，最多复习的卡片排在前面，适合查看学习投入 */
    REVIEW_COUNT_DESC,

    /** 智能优先级排序，综合考虑到期程度和难度权重，系统推荐的最优学习顺序 */
    PRIORITY
}
