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
 * FSRS 性能和压力测试
 * 测试算法在大规模数据下的表现
 */
class FSRSPerformanceTest {

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
     * 大量卡片批量处理测试
     */
    @Test
    fun testLargeScaleBatchProcessing() {
        val cardCount = 10000
        val cards = mutableListOf<FlashCard>()

        // 创建10000张卡片
        val startTime = System.currentTimeMillis()

        repeat(cardCount) { index ->
            cards.add(FlashCard(
                id = index.toLong(),
                stability = kotlin.random.Random.nextDouble(0.1, 100.0), // 修正：使用Random.nextDouble()
                difficulty = kotlin.random.Random.nextDouble(1.0, 10.0), // 修正：使用Random.nextDouble()
                interval = (0..100).random(),
                phase = CardPhase.values().random().value
            ))
        }

        val creationTime = System.currentTimeMillis() - startTime
        println("创建${cardCount}张卡片耗时: ${creationTime}ms")

        // 批量计算评分
        val calculationStartTime = System.currentTimeMillis()
        var totalGrades = 0

        cards.forEach { card ->
            val grades = fsrs.calculate(card)
            totalGrades += grades.size

            // 验证每个计算结果都是有效的
            grades.forEach { grade ->
                assertTrue(grade.stability > 0, "稳定性应该大于0")
                assertTrue(grade.difficulty in 1.0..10.0, "难度应该在有效范围内")
                assertTrue(grade.interval >= 0, "间隔应该非负")
            }
        }

        val calculationTime = System.currentTimeMillis() - calculationStartTime
        println("计算${totalGrades}个评分选项耗时: ${calculationTime}ms")
        println("平均每张卡片计算耗时: ${calculationTime.toDouble() / cardCount}ms")

        // 性能断言
        assertTrue(calculationTime < 10000, "大量计算应该在10秒内完成")
        assertEquals(cardCount * 4, totalGrades, "每张卡片应该产生4个评分选项")
    }

    /**
     * 内存使用测试
     */
    @Test
    fun testMemoryUsage() {
        val runtime = Runtime.getRuntime()
        val initialMemory = runtime.totalMemory() - runtime.freeMemory()

        // 创建大量对象并进行计算
        val cards = mutableListOf<FlashCard>()
        val allGrades = mutableListOf<Grade>()

        repeat(5000) { index ->
            val card = FlashCard(
                id = index.toLong(),
                phase = CardPhase.Added.value
            )
            cards.add(card)

            val grades = fsrs.calculate(card)
            allGrades.addAll(grades)
        }

        val peakMemory = runtime.totalMemory() - runtime.freeMemory()
        val memoryUsed = peakMemory - initialMemory

        println("内存使用量: ${memoryUsed / 1024 / 1024}MB")
        println("创建对象数量: ${cards.size}张卡片, ${allGrades.size}个评分")

        // 清理并触发GC
        cards.clear()
        allGrades.clear()
        System.gc()

        val afterCleanupMemory = runtime.totalMemory() - runtime.freeMemory()
        val memoryReclaimed = peakMemory - afterCleanupMemory

        println("回收内存: ${memoryReclaimed / 1024 / 1024}MB")

        // 内存使用应该合理
        assertTrue(memoryUsed < 500 * 1024 * 1024, "内存使用不应超过500MB")
    }

    /**
     * 并发安全测试
     */
    @Test
    fun testConcurrentAccess() {
        val cardCount = 1000
        val threadCount = 10
        val results = mutableListOf<MutableList<Grade>>()

        // 准备测试数据
        val testCards = (0 until cardCount).map { index ->
            FlashCard(
                id = index.toLong(),
                stability = 5.0,
                difficulty = 3.0,
                interval = 3,
                phase = CardPhase.Review.value
            )
        }

        // 多线程并发计算
        val threads = (0 until threadCount).map { threadIndex ->
            Thread {
                val threadResults = mutableListOf<Grade>()
                val startIndex = threadIndex * (cardCount / threadCount)
                val endIndex = minOf(startIndex + (cardCount / threadCount), cardCount)

                for (i in startIndex until endIndex) {
                    val grades = fsrs.calculate(testCards[i])
                    threadResults.addAll(grades)
                }

                synchronized(results) {
                    results.add(threadResults)
                }
            }
        }

        val startTime = System.currentTimeMillis()
        threads.forEach { it.start() }
        threads.forEach { it.join() }
        val endTime = System.currentTimeMillis()

        println("并发计算耗时: ${endTime - startTime}ms")

        // 验证结果
        val totalResults = results.flatten()
        assertEquals(cardCount * 4, totalResults.size, "并发计算应该产生正确数量的结果")

        // 验证结果的一致性
        totalResults.forEach { grade ->
            assertTrue(grade.stability > 0, "并发计算的稳定性应该有效")
            assertTrue(grade.difficulty in 1.0..10.0, "并发计算的难度应该有效")
        }
    }

    /**
     * 长时��运行稳定性测试
     */
    @Test
    fun testLongRunningStability() {
        var card = FlashCard(phase = CardPhase.Added.value)
        val iterations = 1000
        val stabilities = mutableListOf<Double>()
        val difficulties = mutableListOf<Double>()

        val startTime = System.currentTimeMillis()

        repeat(iterations) { iteration ->
            val grades = fsrs.calculate(card)
            val selectedGrade = grades.random() // 随机选择一个评分

            card = card.copy(
                stability = selectedGrade.stability,
                difficulty = selectedGrade.difficulty,
                interval = selectedGrade.interval,
                phase = if (selectedGrade.choice == Rating.Again)
                    CardPhase.ReLearning.value else CardPhase.Review.value,
                reviewCount = card.reviewCount + 1
            )

            stabilities.add(card.stability)
            difficulties.add(card.difficulty)

            // 每100次迭代验证一次
            if (iteration % 100 == 0) {
                assertTrue(card.stability > 0, "长期运行后稳定性仍应有效")
                assertTrue(card.difficulty in 1.0..10.0, "长期运行后难度仍应有效")
                assertFalse(card.stability.isNaN(), "长期运行后稳定性不应为NaN")
                assertFalse(card.difficulty.isNaN(), "长期运行后难度不应为NaN")
            }
        }

        val endTime = System.currentTimeMillis()
        println("长时间运行测试完成: ${iterations}次迭代，耗时${endTime - startTime}ms")

        // 统计分析
        val avgStability = stabilities.average()
        val avgDifficulty = difficulties.average()

        println("平均稳定性: ${"%.2f".format(avgStability)}")
        println("平均难度: ${"%.2f".format(avgDifficulty)}")

        // 验证长期稳定性
        assertTrue(avgStability > 0, "长期平均稳定性应该有效")
        assertTrue(avgDifficulty in 1.0..10.0, "长期平均难度应该有效")
    }
}
