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
import java.time.LocalDateTime

class ModelsTest {

    /**
     * 测试 Rating 枚举的基本功能
     * 验证各个评级的数值和枚举总数
     */
    @Test
    fun testRatingEnum() {
        assertEquals(1, Rating.Again.value)
        assertEquals(2, Rating.Hard.value)
        assertEquals(3, Rating.Good.value)
        assertEquals(4, Rating.Easy.value)

        // 测试枚举数量
        assertEquals(4, Rating.entries.size)
    }

    /**
     * 测试 CardPhase 枚举的基本功能
     * 验证各个卡片阶段的数值和枚举总数
     */
    @Test
    fun testCardPhaseEnum() {
        assertEquals(0, CardPhase.Added.value)
        assertEquals(1, CardPhase.ReLearning.value)
        assertEquals(2, CardPhase.Review.value)

        // 测试枚举数量
        assertEquals(3, CardPhase.entries.size)
    }

    /**
     * 测试 Grade 数据类的完整构造
     * 验证使用所有参数创建 Grade 对象时各属性的正确性
     */
    @Test
    fun testGradeDataClass() {
        val grade = Grade(
            color = "#FF0000",
            title = "Test Grade",
            durationMillis = 1000L,
            interval = 5,
            txt = "5 days",
            choice = Rating.Good,
            stability = 3.5,
            difficulty = 4.2
        )

        assertEquals("#FF0000", grade.color)
        assertEquals("Test Grade", grade.title)
        assertEquals(1000L, grade.durationMillis)
        assertEquals(5, grade.interval)
        assertEquals("5 days", grade.txt)
        assertEquals(Rating.Good, grade.choice)
        assertEquals(3.5, grade.stability)
        assertEquals(4.2, grade.difficulty)
    }

    /**
     * 测试 Grade 数据类的默认值
     * 验证只传入必需参数时，其他属性使用正确的默认值
     */
    @Test
    fun testGradeDataClassDefaults() {
        val grade = Grade(
            color = "#FF0000",
            title = "Test Grade",
            choice = Rating.Easy
        )

        assertEquals(0L, grade.durationMillis)
        assertEquals(0, grade.interval)
        assertEquals("0", grade.txt)
        assertEquals(0.0, grade.stability)
        assertEquals(0.0, grade.difficulty)
    }

    /**
     * 测试 Grade 数据类的复制功能
     * 验证 copy() 方法能够正确复制对象并修改指定属性
     */
    @Test
    fun testGradeCopy() {
        val original = Grade(
            color = "#FF0000",
            title = "Original",
            choice = Rating.Good,
            stability = 2.0,
            difficulty = 3.0
        )

        val copied = original.copy(
            title = "Copied",
            stability = 4.0
        )

        assertEquals("#FF0000", copied.color) // 保持不变
        assertEquals("Copied", copied.title) // 已更改
        assertEquals(Rating.Good, copied.choice) // 保持不变
        assertEquals(4.0, copied.stability) // 已更改
        assertEquals(3.0, copied.difficulty) // 保持不变
    }

    /**
     * 测试 FlashCard 数据类的完整构造
     * 验证使用所有参数创建 FlashCard 对象时各属性的正确性
     */
    @Test
    fun testFlashCardDataClass() {
        val now = LocalDateTime.now()
        val card = FlashCard(
            id = 123L,
            stability = 5.5,
            difficulty = 3.8,
            interval = 10,
            dueDate = now,
            reviewCount = 7,
            lastReview = now.minusDays(1),
            phase = CardPhase.Review.value
        )

        assertEquals(123L, card.id)
        assertEquals(5.5, card.stability)
        assertEquals(3.8, card.difficulty)
        assertEquals(10, card.interval)
        assertEquals(now, card.dueDate)
        assertEquals(7, card.reviewCount)
        assertEquals(now.minusDays(1), card.lastReview)
        assertEquals(CardPhase.Review.value, card.phase)
    }

    /**
     * 测试 FlashCard 数据类的默认值
     * 验证使用默认构造函数时各属性的初始值
     */
    @Test
    fun testFlashCardDefaults() {
        val card = FlashCard()

        assertEquals(0L, card.id)
        assertEquals(2.5, card.stability)
        assertEquals(2.5, card.difficulty)
        assertEquals(0, card.interval)
        assertEquals(0, card.reviewCount)
        assertEquals(0, card.phase)

        // dueDate 和 lastReview 应该接近当前时间
        val now = LocalDateTime.now()
        assertTrue(card.dueDate.isAfter(now.minusSeconds(1)))
        assertTrue(card.dueDate.isBefore(now.plusSeconds(1)))
        assertTrue(card.lastReview.isAfter(now.minusSeconds(1)))
        assertTrue(card.lastReview.isBefore(now.plusSeconds(1)))
    }

    /**
     * 测试 FlashCard 可变属性的修改
     * 验证 var 声明的属性可以被正确修改
     */
    @Test
    fun testFlashCardMutableProperties() {
        val card = FlashCard()

        // 测试可变属性
        card.stability = 10.0
        card.difficulty = 8.5
        card.interval = 30
        card.reviewCount = 15
        card.phase = CardPhase.ReLearning.value

        assertEquals(10.0, card.stability)
        assertEquals(8.5, card.difficulty)
        assertEquals(30, card.interval)
        assertEquals(15, card.reviewCount)
        assertEquals(CardPhase.ReLearning.value, card.phase)
    }

    /**
     * 测试 FlashCard 中日期相关操作
     * 验证 LocalDateTime 字段的设置和修改功能
     */
    @Test
    fun testFlashCardDateOperations() {
        val baseDate = LocalDateTime.of(2023, 1, 1, 12, 0, 0)
        val card = FlashCard(
            dueDate = baseDate,
            lastReview = baseDate.minusDays(7)
        )

        assertEquals(baseDate, card.dueDate)
        assertEquals(baseDate.minusDays(7), card.lastReview)

        // 测试日期修改
        card.dueDate = baseDate.plusDays(10)
        card.lastReview = baseDate

        assertEquals(baseDate.plusDays(10), card.dueDate)
        assertEquals(baseDate, card.lastReview)
    }

    /**
     * 测试 FlashCard 与 CardPhase 枚举的集成
     * 验证卡片阶段字段与枚举值的对应关系
     */
    @Test
    fun testCardPhaseWithFlashCard() {
        val addedCard = FlashCard(phase = CardPhase.Added.value)
        val relearningCard = FlashCard(phase = CardPhase.ReLearning.value)
        val reviewCard = FlashCard(phase = CardPhase.Review.value)

        assertEquals(0, addedCard.phase)
        assertEquals(1, relearningCard.phase)
        assertEquals(2, reviewCard.phase)
    }

    /**
     * 测试 Rating 枚举值的范围和顺序
     * 验证评级数值在预期范围内且保持正确的递增顺序
     */
    @Test
    fun testRatingValueRange() {
        // 验证评级值在预期范围内
        assertTrue(Rating.Again.value >= 1)
        assertTrue(Rating.Hard.value >= 1)
        assertTrue(Rating.Good.value >= 1)
        assertTrue(Rating.Easy.value >= 1)

        assertTrue(Rating.Again.value <= 4)
        assertTrue(Rating.Hard.value <= 4)
        assertTrue(Rating.Good.value <= 4)
        assertTrue(Rating.Easy.value <= 4)

        // 验证值的递增顺序
        assertTrue(Rating.Again.value < Rating.Hard.value)
        assertTrue(Rating.Hard.value < Rating.Good.value)
        assertTrue(Rating.Good.value < Rating.Easy.value)
    }
}
