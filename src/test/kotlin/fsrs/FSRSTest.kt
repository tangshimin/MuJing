package fsrs

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import java.time.LocalDateTime

class FSRSTest {

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
     * 测试 FSRS 实例的基本初始化
     * 验证实例创建成功，评级列表包含 4 个正确的评级选项
     */
    @Test
    fun testInitialization() {
        assertNotNull(fsrs)
        assertEquals(4, fsrs.gradeList.size)

        // 验证评级类型
        assertEquals(Rating.Easy, fsrs.gradeList[0].choice)
        assertEquals(Rating.Good, fsrs.gradeList[1].choice)
        assertEquals(Rating.Hard, fsrs.gradeList[2].choice)
        assertEquals(Rating.Again, fsrs.gradeList[3].choice)
    }

    /**
     * 测试新卡片的间隔计算
     * 验证新添加的卡片能够正确计算出各种评级对应的复习时间
     */
    @Test
    fun testNewCardCalculation() {
        val newCard = FlashCard(
            id = 1,
            stability = 2.5,
            difficulty = 2.5,
            interval = 0,
            dueDate = LocalDateTime.now(),
            reviewCount = 0,
            lastReview = LocalDateTime.now(),
            phase = CardPhase.Added.value
        )

        val grades = fsrs.calculate(newCard)

        assertEquals(4, grades.size)

        // 验证新卡片的时间安排
        assertEquals("1 day", grades[0].txt) // Easy
        assertEquals("10 Min", grades[1].txt) // Good
        assertEquals("5 Min", grades[2].txt) // Hard
        assertEquals("< 3 Min", grades[3].txt) // Again

        // 验证稳定性和难度值不为零
        assertTrue(grades[0].stability > 0) // Easy
        assertTrue(grades[1].stability > 0) // Good
        assertTrue(grades[2].stability > 0) // Hard
        assertTrue(grades[3].stability >= 0) // Again
    }

    /**
     * 测试重新学习阶段卡片的间隔计算
     * 验证失败后进入重新学习状态的卡片能够正确计算复习间隔
     */
    @Test
    fun testRelearningCardCalculation() {
        val relearningCard = FlashCard(
            id = 2,
            stability = 1.5,
            difficulty = 5.0,
            interval = 1,
            dueDate = LocalDateTime.now().minusDays(1),
            reviewCount = 2,
            lastReview = LocalDateTime.now().minusDays(1),
            phase = CardPhase.ReLearning.value
        )

        val grades = fsrs.calculate(relearningCard)

        assertEquals(4, grades.size)

        // 验证重新学习卡片有合理的间隔
        assertTrue(grades[0].interval >= 1) // Easy
        assertTrue(grades[1].interval >= 1) // Good
        assertEquals("10 Min", grades[2].txt) // Hard
        assertEquals("< 3 Min", grades[3].txt) // Again
    }

    /**
     * 测试复习阶段卡片的间隔计算
     * 验证正常复习流程中卡片的间隔递增逻辑
     */
    @Test
    fun testReviewCardCalculation() {
        val reviewCard = FlashCard(
            id = 3,
            stability = 10.0,
            difficulty = 3.0,
            interval = 7,
            dueDate = LocalDateTime.now().minusDays(7),
            reviewCount = 5,
            lastReview = LocalDateTime.now().minusDays(7),
            phase = CardPhase.Review.value
        )

        val grades = fsrs.calculate(reviewCard)

        assertEquals(4, grades.size)

        // 验证复习卡片的间隔递增
        assertTrue(grades[0].interval > grades[1].interval) // Easy > Good
        assertTrue(grades[1].interval >= grades[2].interval) // Good >= Hard

        // 验证稳定性值合理
        assertTrue(grades[0].stability > 0)
        assertTrue(grades[1].stability > 0)
        assertTrue(grades[2].stability > 0)
        assertTrue(grades[3].stability > 0)
    }

    /**
     * 测试难度值的变化规律
     * 验证不同评级对卡片难度的影响（Again 增加难度，Easy 降低难度）
     */
    @Test
    fun testDifficultyProgression() {
        val card = FlashCard(
            id = 4,
            stability = 5.0,
            difficulty = 5.0,
            interval = 3,
            dueDate = LocalDateTime.now().minusDays(3),
            reviewCount = 3,
            lastReview = LocalDateTime.now().minusDays(3),
            phase = CardPhase.Review.value
        )

        val grades = fsrs.calculate(card)

        // Again 应该增加难度，Easy 应该降低难度
        assertTrue(grades[3].difficulty > card.difficulty) // Again increases difficulty
        assertTrue(grades[0].difficulty < card.difficulty) // Easy decreases difficulty

        // 难度值应该在合理范围内 (1-10)
        grades.forEach { grade ->
            assertTrue(grade.difficulty >= 1.0 && grade.difficulty <= 10.0)
        }
    }

    /**
     * 测试评级选项的颜色配置
     * 验证四个评级选项使用了正确的颜色代码
     */
    @Test
    fun testGradeColors() {
        val grades = fsrs.gradeList

        assertEquals("#2196F3", grades[0].color) // Easy - Blue
        assertEquals("#4CAF50", grades[1].color) // Good - Green
        assertEquals("#9C27B0", grades[2].color) // Hard - Purple
        assertEquals("#F44336", grades[3].color) // Again - Red
    }

    /**
     * 测试评级选项的标题文本
     * 验证四个评级选项显示了正确的标题文字
     */
    @Test
    fun testGradeTitles() {
        val grades = fsrs.gradeList

        assertEquals("Easy", grades[0].title)
        assertEquals("Good", grades[1].title)
        assertEquals("Hard", grades[2].title)
        assertEquals("Again", grades[3].title)
    }

    /**
     * 测试卡片阶段枚举的数值
     * 验证 CardPhase 枚举各阶段的数值正确性
     */
    @Test
    fun testCardPhaseEnum() {
        assertEquals(0, CardPhase.Added.value)
        assertEquals(1, CardPhase.ReLearning.value)
        assertEquals(2, CardPhase.Review.value)
    }

    /**
     * 测试评级枚举的数值
     * 验证 Rating 枚举各评级的数值正确性
     */
    @Test
    fun testRatingEnum() {
        assertEquals(1, Rating.Again.value)
        assertEquals(2, Rating.Hard.value)
        assertEquals(3, Rating.Good.value)
        assertEquals(4, Rating.Easy.value)
    }

    /**
     * 测试参数验证和异常处理
     * 验证当传入无效参数时能够正确抛出异常
     */
    @Test
    fun testParameterValidation() {
        // 测试参数数量不足的情况
        assertThrows(IndexOutOfBoundsException::class.java) {
            val invalidFsrs = FSRS(
                requestRetention = 0.9,
                params = listOf(1.0, 2.0), // 参数不足
                isReview = false
            )
            // 这会在访问 params[20] 时抛出异常
            val card = FlashCard(phase = CardPhase.Added.value)
            invalidFsrs.calculate(card)
        }
    }

    /**
     * 测试稳定性和难度值的边界限制
     * 验证计算结果中的稳定性和难度值都在合理范围内
     */
    @Test
    fun testStabilityAndDifficultyBounds() {
        val card = FlashCard(
            stability = 100.0,
            difficulty = 9.5,
            interval = 30,
            phase = CardPhase.Review.value
        )

        val grades = fsrs.calculate(card)

        // 验证所有难度值都在 1-10 范围内
        grades.forEach { grade ->
            assertTrue(grade.difficulty >= 1.0, "Difficulty should be >= 1.0, but was ${grade.difficulty}")
            assertTrue(grade.difficulty <= 10.0, "Difficulty should be <= 10.0, but was ${grade.difficulty}")
        }

        // 验证稳定性值为正数
        grades.forEach { grade ->
            assertTrue(grade.stability > 0, "Stability should be positive, but was ${grade.stability}")
        }
    }
}
