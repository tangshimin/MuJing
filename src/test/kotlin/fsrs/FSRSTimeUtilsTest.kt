/*
 * Copyright (c) 2023-2025 tang shimin
 *
 * This file is part of MuJing, which is licensed under GPL v3.
 */

package fsrs

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit

/**
 * FSRSTimeUtils 测试类
 * 测试时间工具函数的正确性
 */
class FSRSTimeUtilsTest {

    @Test
    fun testAddMillisToNow() {
        val beforeTime = LocalDateTime.now()
        val millis = 10 * 60 * 1000L // 10分钟

        val result = FSRSTimeUtils.addMillisToNow(millis)
        val afterTime = LocalDateTime.now()

        // 验证结果时间在合理范围内
        assertTrue(result.isAfter(beforeTime))
        assertTrue(result.isBefore(afterTime.plus(11, ChronoUnit.MINUTES)))

        // 验证时间差大约是10分钟
        val diffMinutes = ChronoUnit.MINUTES.between(beforeTime, result)
        assertTrue(diffMinutes >= 9 && diffMinutes <= 11)
    }

    @Test
    fun testAddMillisToTime() {
        val baseTime = LocalDateTime.of(2025, 9, 11, 10, 0, 0)
        val millis = 2 * 60 * 60 * 1000L // 2小时

        val result = FSRSTimeUtils.addMillisToTime(baseTime, millis)
        val expected = LocalDateTime.of(2025, 9, 11, 12, 0, 0)

        assertEquals(expected, result)
    }

    @Test
    fun testUpdateCardDueDate() {
        val card = FlashCard(
            id = 1,
            stability = 2.5,
            difficulty = 2.5,
            interval = 0,
            phase = CardPhase.Added.value
        )

        val grade = Grade(
            color = "#4CAF50",
            title = "Good",
            durationMillis = 10 * 60 * 1000L, // 10分钟
            interval = 1,
            txt = "10 Min",
            choice = Rating.Good,
            stability = 3.0,
            difficulty = 2.8
        )

        val beforeUpdate = LocalDateTime.now()
        val updatedCard = FSRSTimeUtils.updateCardDueDate(card, grade)
        val afterUpdate = LocalDateTime.now()

        // 验证卡片属性被正确更新
        assertEquals(grade.stability, updatedCard.stability)
        assertEquals(grade.difficulty, updatedCard.difficulty)
        assertEquals(grade.interval, updatedCard.interval)
        assertEquals(card.reviewCount + 1, updatedCard.reviewCount)

        // 验证dueDate在合理范围内
        assertTrue(updatedCard.dueDate.isAfter(beforeUpdate))
        assertTrue(updatedCard.dueDate.isBefore(afterUpdate.plus(11, ChronoUnit.MINUTES)))

        // 验证lastReview被更新
        assertTrue(updatedCard.lastReview.isAfter(beforeUpdate.minus(1, ChronoUnit.SECONDS)))
        assertTrue(updatedCard.lastReview.isBefore(afterUpdate.plus(1, ChronoUnit.SECONDS)))
    }

    @Test
    fun testIsCardDue() {
        val currentTime = LocalDateTime.of(2025, 9, 11, 12, 0, 0)

        // 已经到期的卡片
        val overdueCard = FlashCard(
            dueDate = LocalDateTime.of(2025, 9, 11, 11, 0, 0)
        )
        assertTrue(FSRSTimeUtils.isCardDue(overdueCard, currentTime))

        // 正好到期的卡片
        val exactlyDueCard = FlashCard(
            dueDate = currentTime
        )
        assertTrue(FSRSTimeUtils.isCardDue(exactlyDueCard, currentTime))

        // 还没到期的卡片
        val futureCard = FlashCard(
            dueDate = LocalDateTime.of(2025, 9, 11, 13, 0, 0)
        )
        assertFalse(FSRSTimeUtils.isCardDue(futureCard, currentTime))
    }

    @Test
    fun testTimeUntilDue() {
        val currentTime = LocalDateTime.of(2025, 9, 11, 12, 0, 0)

        // 已经到期的卡片应该返回0
        val overdueCard = FlashCard(
            dueDate = LocalDateTime.of(2025, 9, 11, 11, 0, 0)
        )
        assertEquals(0L, FSRSTimeUtils.timeUntilDue(overdueCard, currentTime))

        // 未来1小时到期的卡片
        val futureCard = FlashCard(
            dueDate = LocalDateTime.of(2025, 9, 11, 13, 0, 0)
        )
        val oneHourInMillis = 60 * 60 * 1000L
        assertEquals(oneHourInMillis, FSRSTimeUtils.timeUntilDue(futureCard, currentTime))

        // 正好到期的卡片应该返回0
        val exactlyDueCard = FlashCard(
            dueDate = currentTime
        )
        assertEquals(0L, FSRSTimeUtils.timeUntilDue(exactlyDueCard, currentTime))
    }

    @Test
    fun testIntegrationWithFSRS() {
        // 集成测试：完整的工作流程
        val fsrs = FSRS(
            requestRetention = 0.9,
            params = listOf(
                0.212, 1.2931, 2.3065, 8.2956,
                6.4133, 0.8334, 3.0194, 0.001,
                1.8722, 0.1666, 0.796, 1.4835,
                0.0614, 0.2629, 1.6483, 0.6014,
                1.8729, 0.5425, 0.0912, 0.0658,
                0.1542
            ),
            isReview = false
        )

        var card = FlashCard(phase = CardPhase.Added.value)

        // 1. 计算评分选项
        val grades = fsrs.calculate(card)
        val goodGrade = grades.find { it.choice == Rating.Good }!!

        // 2. 更新卡片
        card = FSRSTimeUtils.updateCardDueDate(card, goodGrade)

        // 3. 验证卡片状态
        assertFalse(FSRSTimeUtils.isCardDue(card)) // 新更新的卡片不应该立即到期
        assertTrue(FSRSTimeUtils.timeUntilDue(card) > 0) // 应该有剩余时间

        // 4. 验证10分钟后到期
        val timeIn10Minutes = LocalDateTime.now().plus(10, ChronoUnit.MINUTES)
        assertTrue(FSRSTimeUtils.isCardDue(card, timeIn10Minutes))
    }
}
