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
 * FSRS 边界情况和异常测试
 * 测试算法在极端情况下的表现
 */
class FSRSEdgeCaseTest {

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
     * 测试极端稳定性值的处理
     */
    @Test
    fun testExtremeStabilityValues() {
        // 测试非常高的稳定性
        var card = FlashCard(
            stability = 10000.0,
            difficulty = 5.0,
            interval = 1000,
            phase = CardPhase.Review.value
        )

        val grades = fsrs.calculate(card)
        val goodGrade = grades.find { it.choice == Rating.Good }!!

        // 高稳定性应该产生较长间隔，但应该有上限
        assertTrue(goodGrade.interval > 0, "高稳定性应该产生正数间隔")
        assertTrue(goodGrade.interval <= 36500, "间隔不应该超过最大值(约100年)")

        // 测试非常低的稳定性
        card = card.copy(stability = 0.01)
        val gradesLowStability = fsrs.calculate(card)
        val goodGradeLowStability = gradesLowStability.find { it.choice == Rating.Good }!!

        assertTrue(goodGradeLowStability.interval > 0, "低稳定性仍应产生正数间隔")
    }

    /**
     * 测试极端难度值的处理
     */
    @Test
    fun testExtremeDifficultyValues() {
        // 测试最大难度
        var card = FlashCard(
            stability = 5.0,
            difficulty = 10.0,
            interval = 5,
            phase = CardPhase.Review.value
        )

        val grades = fsrs.calculate(card)
        grades.forEach { grade ->
            assertTrue(grade.difficulty in 1.0..10.0, "难度应该保持在1.0-10.0范围内")
        }

        // 测试最小难度
        card = card.copy(difficulty = 1.0)
        val gradesMinDifficulty = fsrs.calculate(card)
        gradesMinDifficulty.forEach { grade ->
            assertTrue(grade.difficulty in 1.0..10.0, "难度应该保持在1.0-10.0范围内")
        }
    }

    /**
     * 测试非常长的复习间隔
     */
    @Test
    fun testVeryLongIntervals() {
        val card = FlashCard(
            stability = 5.0,
            difficulty = 3.0,
            interval = 1000, // 1000天
            phase = CardPhase.Review.value
        )

        val grades = fsrs.calculate(card)
        val easyGrade = grades.find { it.choice == Rating.Easy }!!

        // 即使间隔很长，算法仍应该正常工作
        assertTrue(easyGrade.interval > 0, "长间隔卡片仍应产生有效的下次间隔")
        assertTrue(easyGrade.stability > 0, "稳定性应该为正数")
    }

    /**
     * 测试零间隔情况
     */
    @Test
    fun testZeroInterval() {
        val card = FlashCard(
            stability = 2.0,
            difficulty = 5.0,
            interval = 0,
            phase = CardPhase.Review.value
        )

        val grades = fsrs.calculate(card)
        grades.forEach { grade ->
            assertTrue(grade.interval >= 0, "间隔不应该为负数")
            assertTrue(grade.durationMillis >= 0, "持续时间不应该为负数")
        }
    }

    /**
     * 测试极端目标保持率
     */
    @Test
    fun testExtremeRequestRetention() {
        // 测试非常高的目标保持率（99.9%）
        val highRetentionFsrs = FSRS(
            requestRetention = 0.999,
            params = defaultParams,
            isReview = false
        )

        val card = FlashCard(
            stability = 5.0,
            difficulty = 3.0,
            interval = 3,
            phase = CardPhase.Review.value
        )

        val gradesHigh = highRetentionFsrs.calculate(card)
        val goodGradeHigh = gradesHigh.find { it.choice == Rating.Good }!!

        // 测试非常低的目标保持率（50%）
        val lowRetentionFsrs = FSRS(
            requestRetention = 0.5,
            params = defaultParams,
            isReview = false
        )

        val gradesLow = lowRetentionFsrs.calculate(card)
        val goodGradeLow = gradesLow.find { it.choice == Rating.Good }!!

        // 高目标保持率应该产生更短的间隔
        assertTrue(goodGradeHigh.interval <= goodGradeLow.interval,
            "高目标保持率应该产生更短的复习间隔")
    }

    /**
     * 测试连续多次失败（Again）的情况
     */
    @Test
    fun testConsecutiveFailures() {
        var card = FlashCard(
            stability = 10.0,
            difficulty = 3.0,
            interval = 7,
            phase = CardPhase.Review.value
        )

        println("=== 连续失败测试 ===")

        // 连续5次选择Again
        repeat(5) { attempt ->
            val grades = fsrs.calculate(card)
            val againGrade = grades.find { it.choice == Rating.Again }!!

            card = card.copy(
                stability = againGrade.stability,
                difficulty = againGrade.difficulty,
                interval = againGrade.interval,
                phase = CardPhase.ReLearning.value,
                reviewCount = card.reviewCount + 1
            )

            println("第${attempt + 1}次失败 - 难度: ${"%.2f".format(card.difficulty)}, 稳定性: ${"%.2f".format(card.stability)}")

            // 验证失败后的状态
            assertTrue(card.difficulty <= 10.0, "难度不应超过最大值")
            assertTrue(card.stability > 0, "稳定性应该始终为正")
        }

        // 连续失败后，难度应该显著增加
        assertTrue(card.difficulty > 3.0, "连续失败应该增加难度")
    }

    /**
     * 测试数值精度和舍入
     */
    @Test
    fun testNumericalPrecision() {
        val card = FlashCard(
            stability = 2.999999999,
            difficulty = 4.000000001,
            interval = 3,
            phase = CardPhase.Review.value
        )

        val grades = fsrs.calculate(card)
        grades.forEach { grade ->
            // 验证数值在合理范围内且没有异常精度问题
            assertTrue(grade.stability.isFinite(), "稳定性应该是有限数值")
            assertTrue(grade.difficulty.isFinite(), "难度应该是有限数值")
            assertFalse(grade.stability.isNaN(), "稳定性不应该是NaN")
            assertFalse(grade.difficulty.isNaN(), "难度不应该是NaN")
        }
    }

    /**
     * 测试复习计数边界情况
     */
    @Test
    fun testReviewCountBoundaries() {
        // 测试复习次数很多的卡片
        val card = FlashCard(
            stability = 5.0,
            difficulty = 3.0,
            interval = 30,
            phase = CardPhase.Review.value,
            reviewCount = 1000 // 复习1000次
        )

        val grades = fsrs.calculate(card)
        val goodGrade = grades.find { it.choice == Rating.Good }!!

        // 即使复习次数很多，算法仍应正常工作
        assertTrue(goodGrade.interval > 0, "高复习次数卡片仍应产生有效间隔")
        assertTrue(goodGrade.stability > 0, "高复习次数卡片稳定性应为正")
    }

    /**
     * 测试时间边界情况
     */
    @Test
    fun testTimeBoundaries() {
        val card = FlashCard(phase = CardPhase.Added.value)
        val grades = fsrs.calculate(card)

        grades.forEach { grade ->
            // 验证时间相关的值
            assertTrue(grade.durationMillis >= 0, "持续时间不应为负")
            assertTrue(grade.interval >= 0, "间隔不应为负")

            // 验证时间格式化
            assertNotNull(grade.txt, "时间文本不应为null")
            assertTrue(grade.txt.isNotEmpty(), "时间文本不应为空")
        }
    }
}
