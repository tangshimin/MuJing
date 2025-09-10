package fsrs

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.LocalDateTime

/**
 * FSRS 功能测试
 * 测试完整的学习流程和算法效果
 */
class FSRSFunctionalTest {

    private lateinit var fsrs: FSRS

    // FSRS-6 的默认参数
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
     * 功能测试：完整的学习流程
     * 模拟用户从新卡片到熟练掌握的完整学习过程
     */
    @Test
    fun testCompleteStudyWorkflow() {
        var card = FlashCard(phase = CardPhase.Added.value)

        // 第一次学习 - 选择 Good
        var grades = fsrs.calculate(card)
        card = card.copy(
            stability = grades[1].stability,
            difficulty = grades[1].difficulty,
            interval = grades[1].interval,
            phase = CardPhase.Review.value
        )

        // 模拟多次成功复习
        repeat(5) {
            grades = fsrs.calculate(card)
            card = card.copy(
                stability = grades[1].stability, // Good
                difficulty = grades[1].difficulty,
                interval = grades[1].interval
            )

            // 验证间隔递增
            assertTrue(card.interval > 0)
        }

        // 验证最终状态合理
        assertTrue(card.stability > 2.5) // 稳定性应该增长
        assertTrue(card.interval > 1) // 间隔应该增长
    }

    /**
     * 功能测试：遗忘和重新学习流程
     * 模拟用户遗忘后重新学习的过程
     */
    @Test
    fun testForgetAndRelearningWorkflow() {
        // 创建一个有一定基础的卡片
        var card = FlashCard(
            stability = 10.0,
            difficulty = 3.0,
            interval = 7,
            phase = CardPhase.Review.value
        )

        // 选择 Again（遗忘）
        var grades = fsrs.calculate(card)
        card = card.copy(
            stability = grades[3].stability,
            difficulty = grades[3].difficulty,
            phase = CardPhase.ReLearning.value
        )

        // 验证遗忘后的状态
        assertTrue(card.difficulty > 3.0) // 难度应该增加

        // 重新学习过程
        grades = fsrs.calculate(card)
        card = card.copy(
            stability = grades[1].stability, // Good
            difficulty = grades[1].difficulty,
            interval = grades[1].interval,
            phase = CardPhase.Review.value
        )

        // 验证重新学习后能恢复
        assertTrue(card.interval > 0)
    }

    /**
     * 功能测试：间隔时间的合理性
     * 验证不同评级产生的间隔时间符合记忆规律
     */
    @Test
    fun testIntervalProgression() {
        val card = FlashCard(
            stability = 5.0,
            difficulty = 4.0,
            interval = 3,
            phase = CardPhase.Review.value
        )

        val grades = fsrs.calculate(card)

        // Easy > Good > Hard 的间隔递减规律
        assertTrue(grades[0].interval >= grades[1].interval) // Easy >= Good
        assertTrue(grades[1].interval >= grades[2].interval) // Good >= Hard

        // Again 回到短期复习
        assertEquals("< 3 Min", grades[3].txt)
    }

    /**
     * 功能测试：模拟一张卡片每次都选择Good的学习过程
     * 记录每次复习的间隔变化，分析学习进度
     */
    @Test
    fun testCardProgressionWithGoodRating() {
        var card = FlashCard(phase = CardPhase.Added.value)
        val intervals = mutableListOf<Int>()
        val stabilities = mutableListOf<Double>()
        val difficulties = mutableListOf<Double>()

        println("=== 卡片每次选择Good的学习进度 ===")
        println("初始状态 - 阶段: ${card.phase}, 稳定性: ${card.stability}, 难度: ${card.difficulty}")

        // 第一次学习（新卡片阶段）
        var grades = fsrs.calculate(card)
        val goodGrade = grades.find { it.choice == Rating.Good }!!

        card = card.copy(
            stability = goodGrade.stability,
            difficulty = goodGrade.difficulty,
            interval = goodGrade.interval,
            phase = CardPhase.Review.value,
            reviewCount = card.reviewCount + 1
        )

        intervals.add(goodGrade.interval)
        stabilities.add(goodGrade.stability)
        difficulties.add(goodGrade.difficulty)

        println("第1次复习 - 间隔: ${if (goodGrade.interval == 0) goodGrade.txt else "${goodGrade.interval}天"}, 稳定性: ${"%.2f".format(goodGrade.stability)}, 难度: ${"%.2f".format(goodGrade.difficulty)}, 文本: ${goodGrade.txt}")

        // 继续复习15次，观察间隔变化
        for (i in 2..15) {
            grades = fsrs.calculate(card)
            val currentGood = grades.find { it.choice == Rating.Good }!!

            card = card.copy(
                stability = currentGood.stability,
                difficulty = currentGood.difficulty,
                interval = currentGood.interval,
                reviewCount = card.reviewCount + 1
            )

            intervals.add(currentGood.interval)
            stabilities.add(currentGood.stability)
            difficulties.add(currentGood.difficulty)

            println("第${i}次复习 - 间隔: ${currentGood.interval}天, 稳定性: ${"%.2f".format(currentGood.stability)}, 难度: ${"%.2f".format(currentGood.difficulty)}, 文本: ${currentGood.txt}")

            // 如果间隔超过1年，可以认为已经充分掌握
            if (currentGood.interval >= 365) {
                println("间隔已超过1年，可认为充分掌握")
                break
            }
        }

        println("\n=== 学习进度总结 ===")
        println("总复习次数: ${card.reviewCount}")
        println("最终间隔: ${card.interval}天")
        println("最终稳定性: ${"%.2f".format(card.stability)}")
        println("最终难度: ${"%.2f".format(card.difficulty)}")

        println("\n=== 间隔序列 ===")
        intervals.forEachIndexed { index, interval ->
            println("第${index + 1}次: ${interval}天")
        }

        // 验证间隔总体呈递增趋势
        for (i in 1 until intervals.size) {
            assertTrue(intervals[i] >= intervals[i - 1],
                "间隔应该递增或保持不变: 第${i}次(${intervals[i - 1]}天) -> 第${i + 1}次(${intervals[i]}天)")
        }

        // 验证稳定性总体递增
        assertTrue(stabilities.last() > stabilities.first(), "稳定性应该增长")

        // 验证最终间隔合理
        assertTrue(card.interval > 0, "最终间隔应该大于0")
    }

    /**
     * 功能测试：实际复习时间计算
     * 测试使用durationMillis计算具体的复习时间点
     */
    @Test
    fun testActualReviewTimeCalculation() {
        val card = FlashCard(phase = CardPhase.Added.value)
        val grades = fsrs.calculate(card)

        // 获取Good评分的结果
        val goodGrade = grades.find { it.choice == Rating.Good }!!

        // 使用FSRSTimeUtils计算下次复习时间
        val nextReviewTime = FSRSTimeUtils.addMillisToNow(goodGrade.durationMillis)
        val currentTime = LocalDateTime.now()

        println("当前时间: $currentTime")
        println("Good评分间隔: ${goodGrade.txt}")
        println("毫秒数: ${goodGrade.durationMillis}")
        println("下次复习时间: $nextReviewTime")

        // 验证时间计算的合理性
        assertTrue(nextReviewTime.isAfter(currentTime), "下次复习时间应该在当前时间之后")

        // 对于新卡片的Good评分，应该是10分钟后
        val expectedDuration = 10 * 60 * 1000L // 10分钟的毫秒数
        assertEquals(expectedDuration, goodGrade.durationMillis, "新卡片Good评分应该是10分钟")

        // 验证时间差大约是10分钟
        val timeDiff = java.time.Duration.between(currentTime, nextReviewTime)
        val diffInMinutes = timeDiff.toMinutes()
        assertTrue(diffInMinutes >= 9 && diffInMinutes <= 11,
            "时间差应该大约是10分钟，实际是${diffInMinutes}分钟")

        // 测试使用工具类更新卡片
        val updatedCard = FSRSTimeUtils.updateCardDueDate(card, goodGrade)
        assertTrue(updatedCard.reviewCount == 1, "复习次数应该增加")
        assertTrue(updatedCard.dueDate.isAfter(currentTime), "下次复习时间应该在未来")
        assertFalse(FSRSTimeUtils.isCardDue(updatedCard), "刚更新的卡片不应该立即到期")
    }

}
