/*
 * Copyright (c) 2023-2025 tang shimin
 *
 * This file is part of MuJing, which is licensed under GPL v3.
 *
 * This file contains code based on FSRS-Kotlin (https://github.com/open-spaced-repetition/FSRS-Kotlin)
 * Original work Copyright (c) 2025 khordady
 * Original work licensed under MIT License
 *
 * The original MIT License text:
 *
 * MIT License
 *
 * Copyright (c) 2025 khordady
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package fsrs

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import java.time.LocalDateTime

class FSRSTest {

    private lateinit var fsrsService: FSRSService
    private lateinit var cardManager: FlashCardManager

    @BeforeEach
    fun setUp() {
        fsrsService = FSRSService(
            requestRetention = 0.9,
            customParams = null, // 使用默认参数
            isReview = false
        )
        cardManager = FlashCardManager(fsrsService)
    }

    /**
     * 测试 FSRS 服务的基本初始化
     * 验证服务创建成功，能够正确创建新卡片
     */
    @Test
    fun testServiceInitialization() {
        assertNotNull(fsrsService)
        assertNotNull(cardManager)

        // 测试创建新卡片
        val newCard = fsrsService.createNewCard(1L)
        assertNotNull(newCard)
        assertEquals(1L, newCard.id)
        assertEquals(CardPhase.Added.value, newCard.phase)
        assertEquals(2.5, newCard.stability, 0.01)
        assertEquals(2.5, newCard.difficulty, 0.01)
    }

    /**
     * 测试新卡片的间隔计算
     * 验证新添加的卡片能够正确计算出各种评级对应的复习时间
     */
    @Test
    fun testNewCardGradeOptions() {
        val newCard = fsrsService.createNewCard(1L)
        val gradeOptions = fsrsService.getGradeOptions(newCard)

        assertEquals(4, gradeOptions.size)

        // 验证评级类型
        val easyGrade = gradeOptions.find { it.choice == Rating.Easy }!!
        val goodGrade = gradeOptions.find { it.choice == Rating.Good }!!
        val hardGrade = gradeOptions.find { it.choice == Rating.Hard }!!
        val againGrade = gradeOptions.find { it.choice == Rating.Again }!!

        // 验证新卡片的时间间隔
        assertTrue(easyGrade.interval >= 1, "Easy应该至少1天")
        assertTrue(hardGrade.txt.contains("Min"), "Hard应该是分钟级别")
        assertTrue(goodGrade.txt.contains("Min"), "Good应该是分钟级别")
        assertTrue(againGrade.txt.contains("Min"), "Again应该是分钟级别")
    }

    /**
     * 测试评分应用功能
     * 验证用户选择评分后卡片状态的正确更新
     */
    @Test
    fun testApplyGrade() {
        val originalCard = fsrsService.createNewCard(1L)
        val gradeOptions = fsrsService.getGradeOptions(originalCard)
        val goodGrade = gradeOptions.find { it.choice == Rating.Good }!!

        val updatedCard = fsrsService.applyGrade(originalCard, goodGrade)

        // 验证卡片状态更新
        assertEquals(goodGrade.stability, updatedCard.stability, 0.01)
        assertEquals(goodGrade.difficulty, updatedCard.difficulty, 0.01)
        assertEquals(1, updatedCard.reviewCount)
        assertTrue(updatedCard.lastReview.isAfter(originalCard.lastReview) ||
                  updatedCard.lastReview.isEqual(originalCard.lastReview))
        assertTrue(updatedCard.dueDate.isAfter(LocalDateTime.now()))
    }

    /**
     * 测试连续学习流程
     * 模拟用户连续复习同一张卡片的过程
     */
    @Test
    fun testContinuousLearning() {
        var card = fsrsService.createNewCard(1L)

        // 第一次复习 - Good
        var gradeOptions = fsrsService.getGradeOptions(card)
        var selectedGrade = gradeOptions.find { it.choice == Rating.Good }!!
        card = fsrsService.applyGrade(card, selectedGrade)
        assertEquals(1, card.reviewCount)

        // 模拟时间间隔：设置卡片为已到期状态（模拟经过了足够的时间）
        card = card.copy(
            dueDate = LocalDateTime.now().minusDays(1), // 设置为昨天到期
            lastReview = LocalDateTime.now().minusDays(2) // 设置上次复习为2天前
        )

        // 第二次复习 - Easy（现在应该会增加稳定性）
        gradeOptions = fsrsService.getGradeOptions(card)
        selectedGrade = gradeOptions.find { it.choice == Rating.Easy }!!
        val oldStability = card.stability
        card = fsrsService.applyGrade(card, selectedGrade)
        assertEquals(2, card.reviewCount)

        // 验证稳定性应该增加
        assertTrue(card.stability >= oldStability, "连续好评在合适间隔后应该维持或增加稳定性")

        // 第三次复习 - Again (模拟遗忘)
        gradeOptions = fsrsService.getGradeOptions(card)
        selectedGrade = gradeOptions.find { it.choice == Rating.Again }!!
        card = fsrsService.applyGrade(card, selectedGrade)
        assertEquals(3, card.reviewCount)
        assertEquals(CardPhase.ReLearning.value, card.phase)
    }

    /**
     * 测试到期判断
     * 验证卡片到期判断逻辑的正确性
     */
    @Test
    fun testDueDetection() {
        // 创建一个已过期的卡片
        val pastDueCard = FlashCard(
            id = 1L,
            dueDate = LocalDateTime.now().minusDays(1),
            phase = CardPhase.Review.value
        )
        assertTrue(fsrsService.isDue(pastDueCard))

        // 创建一个未到期的卡片
        val futureDueCard = FlashCard(
            id = 2L,
            dueDate = LocalDateTime.now().plusDays(1),
            phase = CardPhase.Review.value
        )
        assertFalse(fsrsService.isDue(futureDueCard))

        // 创建一个正好到期的卡片
        val nowDueCard = FlashCard(
            id = 3L,
            dueDate = LocalDateTime.now(),
            phase = CardPhase.Review.value
        )
        assertTrue(fsrsService.isDue(nowDueCard))
    }

    /**
     * 测试批量操作
     * 验证批量处理卡片的功能
     */
    @Test
    fun testBatchOperations() {
        // 创建多张卡片
        val cards = (1..5).map { fsrsService.createNewCard(it.toLong()) }

        // 批量计算评分选项
        val batchGrades = fsrsService.batchCalculateGrades(cards)
        assertEquals(5, batchGrades.size)
        batchGrades.values.forEach { gradeOptions ->
            assertEquals(4, gradeOptions.size)
        }

        // 获取学习统计
        val stats = fsrsService.getLearningStat(cards)
        assertEquals(5, stats.totalCards)
        assertEquals(5, stats.newCards)
        assertEquals(0, stats.reviewCards)
        assertEquals(0, stats.relearningCards)
    }

    /**
     * 测试卡片管理器功能
     * 验证卡片创建、重置、暂停等功能
     */
    @Test
    fun testCardManager() {
        // 测试创建卡片
        val card = cardManager.createCard("test word", listOf("vocabulary"), "english")
        assertNotNull(card)

        // 测试批量创建
        val words = listOf("apple", "banana", "cherry")
        val cards = cardManager.createCards(words, "fruits")
        assertEquals(3, cards.size)

        // 测试重置卡片
        var testCard = fsrsService.createNewCard(100L)
        testCard = testCard.copy(reviewCount = 5, difficulty = 8.0)
        val resetCard = cardManager.resetCard(testCard)
        assertEquals(0, resetCard.reviewCount)
        assertEquals(2.5, resetCard.difficulty, 0.01)
        assertEquals(CardPhase.Added.value, resetCard.phase)

        // 测试暂停和恢复
        val suspendedCard = cardManager.suspendCard(testCard, 30)
        assertTrue(suspendedCard.dueDate.isAfter(LocalDateTime.now().plusDays(29)))

        val resumedCard = cardManager.resumeCard(suspendedCard)
        assertTrue(resumedCard.dueDate.isBefore(LocalDateTime.now().plusMinutes(1)))
    }

    /**
     * 测试卡片分析功能
     * 验证单卡片和批量分析的准确性
     */
    @Test
    fun testCardAnalytics() {
        val card = fsrsService.createNewCard(1L)
        val analytics = cardManager.getCardAnalytics(card)

        assertEquals(card.id, analytics.cardId)
        assertEquals(CardPhase.Added, analytics.currentPhase)
        assertEquals(card.stability, analytics.stability, 0.01)
        assertEquals(card.difficulty, analytics.difficulty, 0.01)
        assertEquals(4, analytics.nextReviewOptions.size)

        // 测试批量分析
        val cards = (1..10).map { fsrsService.createNewCard(it.toLong()) }
        val batchAnalysis = cardManager.batchAnalyzeCards(cards)

        assertEquals(10, batchAnalysis.totalCards)
        assertEquals(2.5, batchAnalysis.averageDifficulty, 0.01)
        assertEquals(2.5, batchAnalysis.averageStability, 0.01)
        assertTrue(batchAnalysis.phaseDistribution.containsKey("Added"))
        assertEquals(10, batchAnalysis.phaseDistribution["Added"])
    }
}
