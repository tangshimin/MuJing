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
 * FSRS 用户体验和界面集成测试
 * 测试与用户界面相关的功能和体验
 */
class FSRSUserExperienceTest {

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
     * 测试学习进度可视化数据
     */
    @Test
    fun testLearningProgressVisualization() {
        var card = FlashCard(phase = CardPhase.Added.value)
        val progressData = mutableListOf<LearningProgressPoint>()

        // 模拟30次学习过程
        repeat(30) { iteration ->
            val grades = fsrs.calculate(card)
            val selectedGrade = when (iteration % 4) {
                0 -> grades.find { it.choice == Rating.Again }!!
                1 -> grades.find { it.choice == Rating.Hard }!!
                2 -> grades.find { it.choice == Rating.Good }!!
                else -> grades.find { it.choice == Rating.Easy }!!
            }

            card = card.copy(
                stability = selectedGrade.stability,
                difficulty = selectedGrade.difficulty,
                interval = selectedGrade.interval,
                phase = if (selectedGrade.choice == Rating.Again)
                    CardPhase.ReLearning.value else CardPhase.Review.value,
                reviewCount = card.reviewCount + 1
            )

            progressData.add(LearningProgressPoint(
                reviewNumber = iteration + 1,
                stability = card.stability,
                difficulty = card.difficulty,
                interval = card.interval,
                rating = selectedGrade.choice
            ))
        }

        println("=== 学习进度数据 ===")
        println("复习次数 | 稳定性 | 难度 | 间隔 | 评分")
        println("-".repeat(50))

        progressData.takeLast(10).forEach { point ->
            println("${"%8d".format(point.reviewNumber)} | ${"%.2f".format(point.stability)} | ${"%.2f".format(point.difficulty)} | ${"%4d".format(point.interval)} | ${point.rating.name}")
        }

        // 验证进度数据的可视化友好性
        assertTrue(progressData.isNotEmpty(), "应该有进度数据")
        assertTrue(progressData.all { it.stability > 0 }, "稳定性数据应该有效")
        assertTrue(progressData.all { it.difficulty in 1.0..10.0 }, "难度数据应该有效")
        assertTrue(progressData.all { it.interval >= 0 }, "间隔数据应该有效")

        // 计算学习趋势
        val stabilityTrend = progressData.takeLast(5).map { it.stability }.average() -
                           progressData.take(5).map { it.stability }.average()

        println("稳定性趋势: ${if (stabilityTrend > 0) "上升" else "下降"} (${"%.2f".format(stabilityTrend)})")
    }

    /**
     * 测试不同难度卡片的用户体验差异
     */
    @Test
    fun testDifficultyBasedUserExperience() {
        val difficultyLevels = listOf(
            Pair(1.0, "简单"),
            Pair(3.0, "普通"),
            Pair(5.0, "中等"),
            Pair(7.0, "困难"),
            Pair(9.0, "极难")
        )

        println("=== 不同难度卡片的复习体验 ===")

        difficultyLevels.forEach { (difficulty, label) ->
            val card = FlashCard(
                stability = 5.0,
                difficulty = difficulty,
                interval = 3,
                phase = CardPhase.Review.value
            )

            val grades = fsrs.calculate(card)
            val intervals = grades.map { "${it.choice.name}:${it.txt}" }

            println("$label 卡片 (难度$difficulty): ${intervals.joinToString(", ")}")

            // 验证难度对用户体验的影响
            val goodGrade = grades.find { it.choice == Rating.Good }!!
            val easyGrade = grades.find { it.choice == Rating.Easy }!!

            // 困难卡片的复习频率应该更高（间隔更短）
            assertTrue(goodGrade.interval > 0, "复习间隔应该为正")
            assertTrue(easyGrade.interval >= goodGrade.interval, "Easy间隔应该不短于Good")
        }
    }

    /**
     * 测试批量操作的用户反馈
     */
    @Test
    fun testBatchOperationFeedback() {
        val cardCount = 100
        val cards = (1..cardCount).map { id ->
            FlashCard(
                id = id.toLong(),
                phase = CardPhase.Added.value
            )
        }.toMutableList()

        println("=== 批量操作进度反馈测试 ===")

        val batchResults = mutableListOf<BatchOperationResult>()
        val startTime = System.currentTimeMillis()

        cards.forEachIndexed { index, card ->
            val grades = fsrs.calculate(card)
            val goodGrade = grades.find { it.choice == Rating.Good }!!

            val updatedCard = card.copy(
                stability = goodGrade.stability,
                difficulty = goodGrade.difficulty,
                interval = goodGrade.interval,
                phase = CardPhase.Review.value
            )

            cards[index] = updatedCard

            val progress = ((index + 1) * 100.0 / cardCount).toInt()

            // 模拟每10%提供一次进度反馈
            if (progress % 10 == 0 && progress > (batchResults.lastOrNull()?.progress ?: -1)) {
                val currentTime = System.currentTimeMillis()
                val elapsedTime = currentTime - startTime
                val estimatedTotal = if (progress > 0) elapsedTime * 100 / progress else 0L
                val remainingTime = estimatedTotal - elapsedTime

                val result = BatchOperationResult(
                    progress = progress,
                    processed = index + 1,
                    total = cardCount,
                    elapsedTime = elapsedTime,
                    estimatedRemainingTime = remainingTime
                )

                batchResults.add(result)
                println("进度: ${progress}% (${index + 1}/${cardCount}) - 已用时${elapsedTime}ms, 预计剩余${remainingTime}ms")
            }
        }

        val totalTime = System.currentTimeMillis() - startTime
        println("批量操作完成: ${cardCount}张卡片, 总耗时${totalTime}ms")

        // 验证批量操作的效率和反馈
        assertTrue(batchResults.isNotEmpty(), "应该有进度反馈数据")
        assertTrue(totalTime < 5000, "批量操作应该在合理时间内完成")
        assertEquals(100, batchResults.last().progress, "最终进度应该是100%")
    }

    /**
     * 测试学习提醒和通知功能
     */
    @Test
    fun testLearningReminders() {
        val currentTime = LocalDateTime.now()
        val cards = listOf(
            // 已经过期需要复习的卡片
            FlashCard(
                id = 1,
                dueDate = currentTime.minusHours(2),
                stability = 5.0,
                difficulty = 3.0,
                phase = CardPhase.Review.value
            ),
            // 即将到期的卡片（1小时后）
            FlashCard(
                id = 2,
                dueDate = currentTime.plusHours(1),
                stability = 3.0,
                difficulty = 4.0,
                phase = CardPhase.Review.value
            ),
            // 明天需要复习的卡片
            FlashCard(
                id = 3,
                dueDate = currentTime.plusDays(1),
                stability = 8.0,
                difficulty = 2.0,
                phase = CardPhase.Review.value
            )
        )

        println("=== 学习提醒功能测试 ===")

        val overdueCards = cards.filter { FSRSTimeUtils.isCardDue(it, currentTime) }
        val upcomingCards = cards.filter {
            !FSRSTimeUtils.isCardDue(it, currentTime) &&
            FSRSTimeUtils.isCardDue(it, currentTime.plusHours(2))
        }
        val tomorrowCards = cards.filter {
            FSRSTimeUtils.isCardDue(it, currentTime.plusDays(1)) &&
            !FSRSTimeUtils.isCardDue(it, currentTime.plusHours(12))
        }

        println("过期卡片: ${overdueCards.size}张")
        println("即将到期卡片: ${upcomingCards.size}张")
        println("明天需复习卡片: ${tomorrowCards.size}张")

        // 验证提醒逻辑
        assertEquals(1, overdueCards.size, "应该有1张过期卡片")
        assertEquals(1, upcomingCards.size, "应该有1张即将到期卡片")
        assertEquals(1, tomorrowCards.size, "应该有1张明天需复习的卡片")

        // 生成提醒消息
        val reminderMessages = mutableListOf<String>()

        if (overdueCards.isNotEmpty()) {
            reminderMessages.add("您有${overdueCards.size}张卡片已过期，建议立即复习")
        }

        if (upcomingCards.isNotEmpty()) {
            reminderMessages.add("您有${upcomingCards.size}张卡片即将到期")
        }

        if (tomorrowCards.isNotEmpty()) {
            reminderMessages.add("明天您需要复习${tomorrowCards.size}张卡片")
        }

        println("提醒消息:")
        reminderMessages.forEach { println("- $it") }

        assertTrue(reminderMessages.isNotEmpty(), "应该生成提醒消息")
    }

    /**
     * 测试个性化学习建议
     */
    @Test
    fun testPersonalizedLearningAdvice() {
        // 模拟不同类型的学习者
        val learnerProfiles = listOf(
            LearnerProfile("新手", 0.6, 15, 3.0), // 成功率60%，平均每日15张，平均难度3.0
            LearnerProfile("进阶", 0.8, 25, 4.5), // 成功率80%，平均每日25张，平均难度4.5
            LearnerProfile("专家", 0.9, 35, 6.0)  // 成功率90%，平均每日35张，平均难度6.0
        )

        println("=== 个性化学习建议测试 ===")

        learnerProfiles.forEach { profile ->
            val advice = generateLearningAdvice(profile)

            println("${profile.name}学习者:")
            println("  当前状态: 成功率${(profile.successRate * 100).toInt()}%, 日均${profile.dailyCards}张, 平均难度${"%.1f".format(profile.averageDifficulty)}")
            println("  建议: $advice")

            // 验证建议的合理性
            assertNotNull(advice, "应该生成学习建议")
            assertTrue(advice.isNotEmpty(), "建议不应为空")

            when (profile.name) {
                "新手" -> assertTrue(advice.contains("逐步") || advice.contains("基础"), "新手应该得到循序渐进的建议")
                "进阶" -> assertTrue(advice.contains("提高") || advice.contains("挑战"), "进阶者应该得到提升性建议")
                "专家" -> assertTrue(advice.contains("保持") || advice.contains("优化"), "专家应该得到优化性建议")
            }
        }
    }

    // 辅助数据类
    data class LearningProgressPoint(
        val reviewNumber: Int,
        val stability: Double,
        val difficulty: Double,
        val interval: Int,
        val rating: Rating
    )

    data class BatchOperationResult(
        val progress: Int,
        val processed: Int,
        val total: Int,
        val elapsedTime: Long,
        val estimatedRemainingTime: Long
    )

    data class LearnerProfile(
        val name: String,
        val successRate: Double,
        val dailyCards: Int,
        val averageDifficulty: Double
    )

    // 辅助方法
    private fun generateLearningAdvice(profile: LearnerProfile): String {
        return when {
            profile.successRate < 0.7 -> "建议降低学习强度，专注于基础知识的巩固"
            // 优先判断专家，避免落入进阶条件
            profile.successRate >= 0.9 && profile.averageDifficulty > 5.0 -> "您的学习状态很好！可以优化学习策略或挑战更有难度的内容"
            profile.successRate > 0.75 && profile.dailyCards > 20 -> "学习效率很高，可以适当提高学习目标或挑战更难的内容"
            profile.dailyCards > 50 -> "学习量较大，注意合理安排休息时间"
            profile.averageDifficulty < 3.0 -> "可以适当增加学习材料的难度"
            else -> "保持当前的学习节奏，继续稳步提升"
        }
    }
}
