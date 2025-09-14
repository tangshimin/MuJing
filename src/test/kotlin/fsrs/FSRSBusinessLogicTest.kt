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
 * 测试学习会话管理、卡片管理等高级业务场景
 */
class FSRSBusinessLogicTest {

    private lateinit var fsrsService: FSRSService
    private lateinit var cardManager: FlashCardManager
    private lateinit var sessionManager: LearningSessionManager

    @BeforeEach
    fun setUp() {
        fsrsService = FSRSService(requestRetention = 0.9)
        cardManager = FlashCardManager(fsrsService)
        sessionManager = LearningSessionManager(fsrsService)
    }

    /**
     * 测试学习会话的完整生命周期
     */
    @Test
    fun testLearningSessionLifecycle() {
        // 准备测试数据：5张新卡片和3张需要复习的卡片
        val newCards = cardManager.createCards(listOf("apple", "banana", "cherry", "dragon", "elephant"))
        val reviewCards = listOf(
            FlashCard(id = 101L, phase = CardPhase.Review.value, dueDate = LocalDateTime.now().minusHours(1)),
            FlashCard(id = 102L, phase = CardPhase.Review.value, dueDate = LocalDateTime.now().minusDays(1)),
            FlashCard(id = 103L, phase = CardPhase.ReLearning.value, dueDate = LocalDateTime.now().minusMinutes(30))
        )
        val allCards = newCards + reviewCards

        // 开始学习会话
        val session = sessionManager.startSession(allCards, maxNewCards = 3, maxReviewCards = 5)

        // 验证会话创建
        assertNotNull(session)
        assertTrue(session.cards.size <= 8) // 最多3新+5复习
        assertEquals(0, session.completedCount)
        assertEquals(0, session.correctCount)
        assertFalse(session.isCompleted())

        // 模拟完成会话中的所有卡片
        var currentSession = session
        val processedCards = mutableListOf<FlashCard>()

        while (!currentSession.isCompleted()) {
            val currentCard = currentSession.getCurrentCard()!!
            val gradeOptions = fsrsService.getGradeOptions(currentCard)

            // 模拟用户选择Good评分
            val selectedGrade = gradeOptions.find { it.choice == Rating.Good }!!
            val (updatedSession, updatedCard) = sessionManager.processCardReview(currentSession, selectedGrade)

            currentSession = updatedSession
            processedCards.add(updatedCard)
        }

        // 验证会话完成状态
        assertTrue(currentSession.isCompleted())
        assertEquals(100.0, currentSession.getProgress(), 0.1)
        assertEquals(processedCards.size, currentSession.completedCount)
        assertTrue(currentSession.getAccuracy() > 0) // 应该有正确率统计
    }

    /**
     * 测试学习建议生成
     */
    @Test
    fun testLearningRecommendations() {
        // 创建不同状态的卡片
        val cards = mutableListOf<FlashCard>()

        // 新卡片
        cards.addAll(cardManager.createCards(listOf("word1", "word2", "word3")))

        // 到期的复习卡片
        cards.add(FlashCard(id = 201L, phase = CardPhase.Review.value,
                           dueDate = LocalDateTime.now().minusDays(1), difficulty = 3.0))
        cards.add(FlashCard(id = 202L, phase = CardPhase.Review.value,
                           dueDate = LocalDateTime.now().minusHours(2), difficulty = 7.0))

        // 重新学习卡片
        cards.add(FlashCard(id = 203L, phase = CardPhase.ReLearning.value,
                           dueDate = LocalDateTime.now().minusMinutes(30)))

        // 创建模拟的学习会话历史
        val mockSession = LearningSession(
            sessionId = 1L,
            cards = cards.take(3),
            startTime = LocalDateTime.now().minusMinutes(15),
            currentIndex = 3,
            completedCount = 3,
            correctCount = 2,
            sessionStats = mutableMapOf(),
            endTime = LocalDateTime.now()
        )

        // 获取学习建议
        val recommendations = sessionManager.getLearningRecommendations(cards, listOf(mockSession))

        // 验证建议内容
        assertNotNull(recommendations)
        assertTrue(recommendations.suggestedReviewCards > 0) // 应该有到期卡片需要复习
        assertTrue(recommendations.estimatedStudyTime > 0) // 应该有学习时间估计
        assertNotNull(recommendations.studyLoad)
        assertTrue(recommendations.recommendations.isNotEmpty()) // 应该有文本建议
        assertTrue(recommendations.priorityCards.isNotEmpty()) // 应该有优先级卡片
    }

    /**
     * 测试卡片筛选和排序功能
     */
    @Test
    fun testCardFilteringAndSorting() {
        // 创建不同状态的卡片
        val cards = mutableListOf<FlashCard>()

        // 简单卡片
        cards.add(FlashCard(id = 1L, difficulty = 2.0, stability = 5.0, phase = CardPhase.Review.value))
        // 困难卡片
        cards.add(FlashCard(id = 2L, difficulty = 8.0, stability = 2.0, phase = CardPhase.Review.value))
        // 到期卡片
        cards.add(FlashCard(id = 3L, difficulty = 5.0, stability = 3.0, phase = CardPhase.Review.value,
                           dueDate = LocalDateTime.now().minusDays(1)))
        // 新卡片
        cards.add(FlashCard(id = 4L, difficulty = 2.5, stability = 2.5, phase = CardPhase.Added.value))

        // 测试筛选困难卡片
        val hardCards = cardManager.filterCards(cards, CardFilter(minDifficulty = 7.0))
        assertEquals(1, hardCards.size)
        assertEquals(2L, hardCards[0].id)

        // 测试筛选到期卡片
        val dueCards = cardManager.filterCards(cards, CardFilter(dueOnly = true))
        assertTrue(dueCards.isNotEmpty())

        // 测试筛选特定阶段
        val newCards = cardManager.filterCards(cards, CardFilter(phases = listOf(CardPhase.Added)))
        assertEquals(1, newCards.size)
        assertEquals(4L, newCards[0].id)

        // 测试按难度排序
        val sortedByDifficulty = cardManager.sortCards(cards, SortStrategy.DIFFICULTY_DESC)
        assertEquals(2L, sortedByDifficulty[0].id) // 最困难的应该排在前面

        // 测试智能优先级排序
        val prioritySorted = cardManager.sortCards(cards, SortStrategy.PRIORITY)
        assertNotNull(prioritySorted)
        assertEquals(cards.size, prioritySorted.size)
    }

    /**
     * 测试学习统计功能
     */
    @Test
    fun testLearningStatistics() {
        // 创建各种状态的卡片
        val cards = mutableListOf<FlashCard>()

        // 3张新卡片
        repeat(3) { i ->
            cards.add(FlashCard(id = i.toLong(), phase = CardPhase.Added.value))
        }

        // 5张复习卡片
        repeat(5) { i ->
            cards.add(FlashCard(id = (i + 10).toLong(), phase = CardPhase.Review.value,
                               difficulty = 3.0 + i, stability = 5.0 + i))
        }

        // 2张重新学习卡片
        repeat(2) { i ->
            cards.add(FlashCard(id = (i + 20).toLong(), phase = CardPhase.ReLearning.value,
                               dueDate = LocalDateTime.now().minusHours(1)))
        }

        // 获取学习统计
        val stats = fsrsService.getLearningStat(cards)

        assertEquals(10, stats.totalCards)
        assertEquals(3, stats.newCards)
        assertEquals(5, stats.reviewCards)
        assertEquals(2, stats.relearningCards)
        assertTrue(stats.dueCards >= 2) // 至少重新学习的卡片是到期的

        // 验证平均值计算
        assertTrue(stats.averageDifficulty > 0)
        assertTrue(stats.averageStability > 0)
    }

    /**
     * 测试批量卡片分析
     */
    @Test
    fun testBatchCardAnalysis() {
        // 创建具有不同特征的卡片
        val cards = listOf(
            FlashCard(id = 1L, difficulty = 2.0, stability = 8.0, phase = CardPhase.Review.value, reviewCount = 10),
            FlashCard(id = 2L, difficulty = 6.0, stability = 3.0, phase = CardPhase.Review.value, reviewCount = 3),
            FlashCard(id = 3L, difficulty = 9.0, stability = 1.5, phase = CardPhase.ReLearning.value, reviewCount = 15),
            FlashCard(id = 4L, difficulty = 2.5, stability = 2.5, phase = CardPhase.Added.value, reviewCount = 0)
        )

        val batchAnalysis = cardManager.batchAnalyzeCards(cards)

        assertEquals(4, batchAnalysis.totalCards)

        // 验证难度分布
        assertTrue(batchAnalysis.difficultyDistribution.containsKey("Easy"))
        assertTrue(batchAnalysis.difficultyDistribution.containsKey("Hard"))

        // 验证阶段分布
        assertTrue(batchAnalysis.phaseDistribution.containsKey("Review"))
        assertTrue(batchAnalysis.phaseDistribution.containsKey("Added"))

        // 验证平均值
        assertEquals((2.0 + 6.0 + 9.0 + 2.5) / 4, batchAnalysis.averageDifficulty, 0.01)
        assertEquals((8.0 + 3.0 + 1.5 + 2.5) / 4, batchAnalysis.averageStability, 0.01)
    }

    /**
     * 测试学习进度跟踪
     */
    @Test
    fun testLearningProgressTracking() {
        var card = fsrsService.createNewCard(1L)
        val progressSnapshots = mutableListOf<Pair<Int, Double>>() // (reviewCount, stability)

        // 模拟30次复习，记录进度
        repeat(30) { iteration ->
            val gradeOptions = fsrsService.getGradeOptions(card)

            // 模拟80%的时间选择Good，20%选择Easy
            val selectedGrade = if (iteration % 5 == 0) {
                gradeOptions.find { it.choice == Rating.Easy }!!
            } else {
                gradeOptions.find { it.choice == Rating.Good }!!
            }

            card = fsrsService.applyGrade(card, selectedGrade)
            progressSnapshots.add(card.reviewCount to card.stability)
        }

        // 验证学习进展
        assertEquals(30, card.reviewCount)
        assertTrue(card.stability > 2.5, "经过30次复习，稳定性应该显著提高")

        // 验证稳定性总体趋势向上
        val firstHalfAvgStability = progressSnapshots.take(15).map { it.second }.average()
        val secondHalfAvgStability = progressSnapshots.drop(15).map { it.second }.average()
        assertTrue(secondHalfAvgStability > firstHalfAvgStability,
                  "后半段的平均稳定性应该比前半段高")
    }

    /**
     * 测试学习负担管理
     */
    @Test
    fun testStudyLoadManagement() {
        // 创建大量到期卡片模拟高负担情况
        val heavyLoadCards = (1..60).map { i ->
            FlashCard(
                id = i.toLong(),
                phase = CardPhase.Review.value,
                dueDate = LocalDateTime.now().minusDays(1),
                difficulty = 5.0,
                stability = 3.0
            )
        }

        val recommendations = sessionManager.getLearningRecommendations(heavyLoadCards, emptyList())

        // 高负担情况下应该建议较少或不学新卡片
        assertTrue(recommendations.suggestedNewCards <= 5,
                  "高负担时新卡片建议数量应该较少")
        assertEquals(StudyLoadLevel.HIGH, recommendations.studyLoad.level)
        assertTrue(recommendations.recommendations.any { it.contains("负担") || it.contains("重") },
                  "应该包含负担相关的建议")

        // 测试低负担情况
        val lightLoadCards = (1..5).map { i ->
            FlashCard(
                id = i.toLong(),
                phase = CardPhase.Review.value,
                dueDate = LocalDateTime.now().plusDays(1), // 未到期
                difficulty = 3.0,
                stability = 10.0
            )
        }

        val lightRecommendations = sessionManager.getLearningRecommendations(lightLoadCards, emptyList())
        assertEquals(StudyLoadLevel.LOW, lightRecommendations.studyLoad.level)
        assertTrue(lightRecommendations.suggestedNewCards >= 5,
                  "低负担时应该建议学习更多新卡片")
    }
}
