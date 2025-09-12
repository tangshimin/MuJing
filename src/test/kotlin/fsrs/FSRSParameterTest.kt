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
 * FSRS 参数配置和算法变体测试
 * 测试不同参数配置下的算法表现
 */
class FSRSParameterTest {

    private val defaultParams = listOf(
        0.212, 1.2931, 2.3065, 8.2956,
        6.4133, 0.8334, 3.0194, 0.001,
        1.8722, 0.1666, 0.796, 1.4835,
        0.0614, 0.2629, 1.6483, 0.6014,
        1.8729, 0.5425, 0.0912, 0.0658,
        0.1542
    )

    /**
     * 测试不同目标保持率的影响
     */
    @Test
    fun testDifferentRetentionRates() {
        val retentionRates = listOf(0.7, 0.8, 0.9, 0.95, 0.99)
        val testCard = FlashCard(
            stability = 5.0,
            difficulty = 3.0,
            interval = 3,
            phase = CardPhase.Review.value
        )

        println("=== 不同目标保持率对间隔的影响 ===")

        retentionRates.forEach { retention ->
            val fsrs = FSRS(
                requestRetention = retention,
                params = defaultParams,
                isReview = false
            )

            val grades = fsrs.calculate(testCard)
            val goodGrade = grades.find { it.choice == Rating.Good }!!

            println("保持率${(retention * 100).toInt()}%: 间隔=${goodGrade.interval}天")

            // 验证：更高的保持率应该产生更短的间隔
            assertTrue(goodGrade.interval > 0, "间隔应该为正数")
        }

        // 比较不同保持率的间隔
        val intervals = retentionRates.map { retention ->
            val fsrs = FSRS(retention, defaultParams, false)
            val grades = fsrs.calculate(testCard)
            grades.find { it.choice == Rating.Good }!!.interval
        }

        // 验证保持率与间隔的反比关系
        for (i in 0 until intervals.size - 1) {
            assertTrue(intervals[i] >= intervals[i + 1],
                "更高的保持率应该产生更短或相等的间隔")
        }
    }

    /**
     * 测试异常参数值的处理
     */
    @Test
    fun testInvalidParameters() {
        val testCard = FlashCard(phase = CardPhase.Added.value)

        // 测试空参数列表
        assertThrows(Exception::class.java) {
            val fsrs = FSRS(0.9, emptyList(), false)
            fsrs.calculate(testCard)
        }

        // 测试参数数量不足
        assertThrows(Exception::class.java) {
            val fsrs = FSRS(0.9, listOf(1.0, 2.0, 3.0), false)
            fsrs.calculate(testCard)
        }

        // 测试包含NaN的参数
        val paramsWithNaN = defaultParams.toMutableList()
        paramsWithNaN[0] = Double.NaN

        val fsrsWithNaN = FSRS(0.9, paramsWithNaN, false)
        val grades = fsrsWithNaN.calculate(testCard)

        // 验证即使参数异常，也应该处理得当
        grades.forEach { grade ->
//            assertFalse(grade.stability.isNaN(), "稳定性不应该是NaN")
            assertFalse(grade.difficulty.isNaN(), "难度不应该是NaN")
        }
    }

    /**
     * 测试极端参数值
     */
    @Test
    fun testExtremeParameters() {
        val testCard = FlashCard(
            stability = 5.0,
            difficulty = 3.0,
            interval = 3,
            phase = CardPhase.Review.value
        )

        // 测试极小参数值
        val smallParams = defaultParams.map { it * 0.001 }
        val fsrsSmall = FSRS(0.9, smallParams, false)
        val gradesSmall = fsrsSmall.calculate(testCard)

        gradesSmall.forEach { grade ->
//            assertTrue(grade.stability > 0, "极小参数下稳定性仍应为正")
            assertTrue(grade.difficulty in 1.0..10.0, "极小参数下难度仍应在范围内")
            assertTrue(grade.interval >= 0, "极小参数下间隔仍应非负")
        }

        // 测试极大参数值
        val largeParams = defaultParams.map { it * 1000 }
        val fsrsLarge = FSRS(0.9, largeParams, false)
        val gradesLarge = fsrsLarge.calculate(testCard)

        gradesLarge.forEach { grade ->
//            assertTrue(grade.stability > 0, "极大参数下稳定性仍应为正")
            assertTrue(grade.difficulty in 1.0..10.0, "极大参数下难度仍应在范围内")
            assertTrue(grade.interval >= 0, "极大参数下间隔仍应非负")
            assertTrue(grade.interval <= 36500, "极大参数下间隔不应超过上限")
        }
    }

    /**
     * 测试参数敏感性分析
     */
    @Test
    fun testParameterSensitivity() {
        val baseCard = FlashCard(
            stability = 5.0,
            difficulty = 3.0,
            interval = 3,
            phase = CardPhase.Review.value
        )

        // 基准结果
        val baseFsrs = FSRS(0.9, defaultParams, false)
        val baseGrades = baseFsrs.calculate(baseCard)
        val baseInterval = baseGrades.find { it.choice == Rating.Good }!!.interval

        println("=== 参数敏感性分析 ===")
        println("基准间隔: ${baseInterval}天")

        // 测试每个参数的敏感性
        defaultParams.forEachIndexed { index, _ ->
            val modifiedParams = defaultParams.toMutableList()
            modifiedParams[index] = modifiedParams[index] * 1.1 // 增加10%

            val modifiedFsrs = FSRS(0.9, modifiedParams, false)
            val modifiedGrades = modifiedFsrs.calculate(baseCard)
            val modifiedInterval = modifiedGrades.find { it.choice == Rating.Good }!!.interval

            val change = ((modifiedInterval - baseInterval).toDouble() / baseInterval * 100)
            println("参数$index 变化10%: 间隔变化${"%.1f".format(change)}%")

            // 验证参数变化不会导致异常结果
            assertTrue(modifiedInterval > 0, "修改参数后间隔仍应为正")
            assertTrue(kotlin.math.abs(change) < 1000, "参数变化不应导致极端结果")
        }
    }

    /**
     * 测试review模式标志的影响
     */
    @Test
    fun testReviewModeFlag() {
        val testCard = FlashCard(
            stability = 10.0,
            difficulty = 3.0,
            interval = 7,
            phase = CardPhase.Review.value
        )

        // 非review模式
        val fsrsNonReview = FSRS(0.9, defaultParams, isReview = false)
        val gradesNonReview = fsrsNonReview.calculate(testCard)

        // review模式
        val fsrsReview = FSRS(0.9, defaultParams, isReview = true)
        val gradesReview = fsrsReview.calculate(testCard)

        println("=== Review模式标志的影响 ===")

        val ratings = listOf(Rating.Again, Rating.Hard, Rating.Good, Rating.Easy)
        ratings.forEach { rating ->
            val nonReviewGrade = gradesNonReview.find { it.choice == rating }!!
            val reviewGrade = gradesReview.find { it.choice == rating }!!

            println("${rating.name}: 非review=${nonReviewGrade.interval}天, review=${reviewGrade.interval}天")

            // 两种模式都应该产生有效结果
            assertTrue(nonReviewGrade.interval >= 0, "非review模式间隔应该有效")
            assertTrue(reviewGrade.interval >= 0, "review模式间隔应该有效")
        }
    }

    /**
     * 测试参数组合的一致性
     */
    @Test
    fun testParameterConsistency() {
        val testCards = listOf(
            FlashCard(phase = CardPhase.Added.value),
            FlashCard(stability = 3.0, difficulty = 2.0, interval = 1, phase = CardPhase.Review.value),
            FlashCard(stability = 8.0, difficulty = 5.0, interval = 10, phase = CardPhase.Review.value)
        )

        val fsrs = FSRS(0.9, defaultParams, false)

        testCards.forEach { card ->
            val grades = fsrs.calculate(card)

            // 验证评分选项的一致性
            assertEquals(4, grades.size, "每张卡片都应该有4个评分选项")

            val ratings = grades.map { it.choice }.toSet()
            assertEquals(setOf(Rating.Again, Rating.Hard, Rating.Good, Rating.Easy),
                ratings, "应该包含所有4种评分")

            // 验证间隔的合理性
            val intervals = grades.map { it.interval }
            assertTrue(intervals.all { it >= 0 }, "所有间隔都应该非负")

            // 验证稳定性和难度的合理性
            grades.forEach { grade ->
                assertTrue(grade.stability > 0, "稳定性应该为正")
                assertTrue(grade.difficulty in 1.0..10.0, "难度应该在有效范围")
            }
        }
    }
}
