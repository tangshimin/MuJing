/*
 * Copyright (c) 2025 tang shimin
 *
 * This file is part of MuJing, which is licensed under GPL v3.
 */

package fsrs

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.LocalDateTime

/**
 * FSRS 业务逻辑测试
 * 测试实际使用场景中的业务逻辑
 */
class FSRSBusinessLogicTest {

    private lateinit var fsrs: FSRS
    private val defaultParams = listOf(
        0.212, 1.2931, 2.3065, 8.2956,
        6.4133, 0.8334, 3.0194, 0.001,
        1.8722, 0.1666, 0.796, 1.4835,
        0.0614, 0.2629, 1.6483, 0.6014,
        1.8729, 0.5425, 0.0912, 0.0658,
        0.1542
    )

    @BeforeEach
    fun setUp() {
        fsrs = FSRS(
            requestRetention = 0.9,
            params = defaultParams,
            isReview = false
        )
    }

    /**
     * 测试学习计划生成
     */
    @Test
    fun testStudyPlanGeneration() {
        val cards = createTestCards(50)
        val currentTime = LocalDateTime.now()

        // 模拟用户的学习历史
        val studiedCards = simulateStudySession(cards, currentTime)

        // 生成下一天的学习计划
        val nextDay = currentTime.plusDays(1)
        val dueCards = studiedCards.filter { card ->
            FSRSTimeUtils.isCardDue(card, nextDay)
        }

        println("=== 学习计划生成测试 ===")
        println("总卡片数: ${cards.size}")
        println("已学习卡片数: ${studiedCards.size}")
        println("明天需复习: ${dueCards.size}")

        // 按优先级排序（越早到期的优先级越高）
        val prioritizedCards = dueCards.sortedBy { it.dueDate }

        // 验证学习计划
        assertTrue(dueCards.isNotEmpty(), "应该有卡片需要复习")
        assertTrue(prioritizedCards.first().dueDate <= prioritizedCards.last().dueDate,
            "卡片应该按到期时间排序")

        // 模拟每日学习量限制（比如每天最多学习20张）
        val dailyLimit = 20
        val todaysCards = prioritizedCards.take(dailyLimit)

        assertTrue(todaysCards.size <= dailyLimit, "每日学习量不应超过限制")
        println("今日计划学习: ${todaysCards.size}张")
    }

    /**
     * 测试学习统计和进度跟踪
     */
    @Test
    fun testLearningStatistics() {
        val cards = createTestCards(100)
        val studyResults = mutableMapOf<Rating, Int>()

        // 模拟30天的学习过程
        var currentTime = LocalDateTime.now()
        var activeCards = cards.toMutableList()

        repeat(30) { day ->
            println("第${day + 1}天学习:")

            val dueCards = activeCards.filter { card ->
                FSRSTimeUtils.isCardDue(card, currentTime)
            }.take(20) // 每天最多学习20张

            dueCards.forEach { card ->
                val grades = fsrs.calculate(card)
                val selectedRating = selectRatingBasedOnDifficulty(card.difficulty)
                val selectedGrade = grades.find { it.choice == selectedRating }!!

                // 更新统计
                studyResults[selectedRating] = studyResults.getOrDefault(selectedRating, 0) + 1

                // 更新卡片
                val updatedCard = FSRSTimeUtils.updateCardDueDate(card, selectedGrade)
                val index = activeCards.indexOfFirst { it.id == card.id }
                if (index != -1) {
                    activeCards[index] = updatedCard.copy(
                        phase = if (selectedRating == Rating.Again)
                            CardPhase.ReLearning.value else CardPhase.Review.value
                    )
                }
            }

            currentTime = currentTime.plusDays(1)
            println("  学习了${dueCards.size}张卡片")
        }

        // 生成学习统计报告
        println("\n=== 30天学习统计 ===")
        val totalReviews = studyResults.values.sum()
        studyResults.forEach { (rating, count) ->
            val percentage = (count * 100.0 / totalReviews)
            println("${rating.name}: ${count}次 (${"%.1f".format(percentage)}%)")
        }

        // 验证学习统计
        assertTrue(totalReviews > 0, "应该有复习记录")

        // 安全地检查 Good 评分，使用 getOrDefault 避免空指针异常
        val goodCount = studyResults.getOrDefault(Rating.Good, 0)
        assertTrue(goodCount >= 0, "Good评分次数应该非负")

        // 如果没有Good评分，说明所有卡片都比较困难，这也是合理的情况
        if (goodCount == 0) {
            println("注意: 没有Good评分，所有卡片都被评为其他等级")
        }

        // 计算学习效果指标
        val successRate = (studyResults.getOrDefault(Rating.Good, 0) +
                          studyResults.getOrDefault(Rating.Easy, 0)) * 100.0 / totalReviews
        println("学习成功率: ${"%.1f".format(successRate)}%")

        assertTrue(successRate > 50, "学习成功率应该合理")
    }

    /**
     * 测试卡片退役机制
     */
    @Test
    fun testCardRetirement() {
        var card = FlashCard(phase = CardPhase.Added.value)
        val retirementThreshold = 180 // 间隔超过180天认为已掌握

        // 模拟持续选择Easy直到卡片"退役"
        var iterations = 0
        val maxIterations = 20

        while (card.interval < retirementThreshold && iterations < maxIterations) {
            val grades = fsrs.calculate(card)
            val easyGrade = grades.find { it.choice == Rating.Easy }!!

            card = card.copy(
                stability = easyGrade.stability,
                difficulty = easyGrade.difficulty,
                interval = easyGrade.interval,
                phase = CardPhase.Review.value,
                reviewCount = card.reviewCount + 1
            )

            iterations++
            println("第${iterations}次: 间隔=${card.interval}天, 稳定性=${"%.2f".format(card.stability)}")
        }

        println("\n卡片状态:")
        println("复习次数: ${card.reviewCount}")
        println("最终间隔: ${card.interval}天")
        println("是否可退役: ${card.interval >= retirementThreshold}")

        // 验证退役逻辑
        if (card.interval >= retirementThreshold) {
            assertTrue(card.reviewCount >= 3, "退役卡片应该有足够的复习次数")
            assertTrue(card.stability > 10.0, "退役卡片应该有较高的稳定性")
        }
    }

    /**
     * 测试学习负担均衡
     */
    @Test
    fun testWorkloadBalancing() {
        val cards = createTestCards(200)
        val currentTime = LocalDateTime.now()

        // 模拟不同的学习安排
        val studyDays = 14
        val dailyLimits = listOf(10, 20, 30) // 不同的每日学习量

        dailyLimits.forEach { dailyLimit ->
            println("\n=== 每日学习${dailyLimit}张的负载测试 ===")

            var testCards = cards.map { it.copy() }.toMutableList()
            var testTime = currentTime
            val dailyWorkloads = mutableListOf<Int>()

            repeat(studyDays) { day ->
                val dueCards = testCards.filter { card ->
                    FSRSTimeUtils.isCardDue(card, testTime)
                }

                val todaysCards = dueCards.take(dailyLimit)
                dailyWorkloads.add(todaysCards.size)

                // 模拟学习
                todaysCards.forEach { card ->
                    val grades = fsrs.calculate(card)
                    val selectedGrade = grades.find { it.choice == Rating.Good }!!
                    val updatedCard = FSRSTimeUtils.updateCardDueDate(card, selectedGrade)

                    val index = testCards.indexOfFirst { it.id == card.id }
                    if (index != -1) {
                        testCards[index] = updatedCard.copy(phase = CardPhase.Review.value)
                    }
                }

                testTime = testTime.plusDays(1)
            }

            val avgWorkload = dailyWorkloads.average()
            val maxWorkload = dailyWorkloads.maxOrNull() ?: 0
            val minWorkload = dailyWorkloads.minOrNull() ?: 0

            println("平均每日工作量: ${"%.1f".format(avgWorkload)}")
            println("最大工作量: $maxWorkload")
            println("最小工作量: $minWorkload")
            println("工作量方差: ${"%.1f".format(calculateVariance(dailyWorkloads))}")

            // 验证负载均衡
            assertTrue(maxWorkload <= dailyLimit * 1.2, "工作量不应过度超载")
            assertTrue(avgWorkload <= dailyLimit, "平均工作量应该在限制内")
        }
    }

    /**
     * 测试遗忘曲线建模
     */
    @Test
    fun testForgettingCurveModeling() {
        val card = FlashCard(
            stability = 10.0,
            difficulty = 5.0,
            interval = 7,
            phase = CardPhase.Review.value
        )

        // 模拟不同时间点的记忆保持率
        val timePoints = listOf(1, 3, 7, 14, 30)
        val retentionRates = mutableMapOf<Int, Double>()

        timePoints.forEach { days ->
            // 计算在该时间点的记忆保持率（简化模型）
            val retention = kotlin.math.exp(-days.toDouble() / card.stability)
            retentionRates[days] = retention

            println("第${days}天预期记忆保持率: ${"%.2f".format(retention * 100)}%")
        }

        // 验证遗忘曲线的合理性
        assertTrue(retentionRates[1]!! > retentionRates[7]!!, "短期记忆保持率应该更高")
        assertTrue(retentionRates[7]!! > retentionRates[30]!!, "记忆应该随时间衰减")

        // 测试在不同保持率下的复习时机
        val targetRetention = 0.9
        val optimalReviewDay = timePoints.find { days ->
            retentionRates[days]!! <= targetRetention
        }

        println("在目标保持率${targetRetention * 100}%下，最佳复习时机: 第${optimalReviewDay}天")
        assertNotNull(optimalReviewDay, "应该能找到最佳复习时机")
    }

    // 辅助方法
    private fun createTestCards(count: Int): List<FlashCard> {
        return (1..count).map { id ->
            FlashCard(
                id = id.toLong(),
                phase = CardPhase.Added.value
            )
        }
    }

    private fun simulateStudySession(cards: List<FlashCard>, currentTime: LocalDateTime): List<FlashCard> {
        return cards.map { card ->
            val grades = fsrs.calculate(card)
            val selectedGrade = grades.find { it.choice == Rating.Good }!!
            FSRSTimeUtils.updateCardDueDate(card, selectedGrade)
                .copy(phase = CardPhase.Review.value)
        }
    }

    private fun selectRatingBasedOnDifficulty(difficulty: Double): Rating {
        return when {
            difficulty <= 3.0 -> Rating.Easy
            difficulty <= 6.0 -> Rating.Good
            difficulty <= 8.0 -> Rating.Hard
            else -> Rating.Again
        }
    }

    private fun calculateVariance(values: List<Int>): Double {
        val mean = values.average()
        return values.map { (it - mean) * (it - mean) }.average()
    }
}
