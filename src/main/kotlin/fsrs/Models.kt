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

// 本文件基于原项目修改

package fsrs

import java.time.LocalDateTime

/**
 * 复习评分枚举
 *
 * 表示用户对闪卡复习的评分等级，用于FSRS算法计算下次复习时间和难度调整
 */
enum class Rating(val value: Int) {
    /** 完全不记得，需要重新学习 */
    Again(1),
    /** 记得困难，勉强回忆起来 */
    Hard(2),
    /** 记得良好，正常回忆 */
    Good(3),
    /** 记得很容易，轻松回忆 */
    Easy(4)
}

/**
 * 卡片学习阶段枚举
 *
 * 表示闪卡当前所处的学习阶段，影响复习算法的计算策略
 */
enum class CardPhase(val value: Int) {
    /** 新添加的卡片，尚未开始学习 */
    Added(0),
    /** 重新学习阶段，用于复习失败的卡片 */
    ReLearning(1),
    /** 正常复习阶段 */
    Review(2),
}

/**
 * 复习评分选项数据类
 *
 * 代表用户复习闪卡时可以选择的四个评分选项之一（Again、Hard、Good、Easy）。
 * 每个Grade对象包含了选择该评分后的预测结果，包括下次复习时间、算法参数变化等信息，
 * 用于在用户界面中展示不同评分选择的后果，帮助用户做出最佳的复习评分决定。
 *
 * @property color 评分选项的颜色代码，用于UI按钮显示
 * @property title 评分选项的标题文本（"Again"、"Hard"、"Good"、"Easy"）
 * @property durationMillis 选择此评分后距离下次复习的毫秒数
 * @property interval 选择此评分后距离下次复习的天数
 * @property txt interval 的用户友好显示格式（如"5 day"、"2.5 month"）
 * @property choice 对应的评分等级枚举值
 * @property stability 选择此评分后卡片的新稳定性参数
 * @property difficulty 选择此评分后卡片的新难度参数
 */
data class Grade(
    val color: String,
    val title: String,
    val durationMillis: Long = 0,
    val interval: Int = 0,
    val txt: String = "0",
    val choice: Rating,
    val stability: Double = 0.0,
    val difficulty: Double = 0.0
)

/**
 * 闪卡数据类
 *
 * 表示一张闪卡的完整信息，包含FSRS算法所需的所有参数和状态
 *
 * @property id 卡片唯一标识符（已移除Android Room的@PrimaryKey注解）
 * @property stability 记忆稳定性，表示记忆保持的强度（默认2.5）
 * @property difficulty 记忆难度，表示该卡片的学习难度（默认2.5）
 * @property interval 复习间隔天数，下次复习距离当前的天数
 * @property dueDate 到期复习日期，下次应该复习的具体时间
 * @property reviewCount 总复习次数，记录该卡片被复习的总次数
 * @property lastReview 上次复习时间，最近一次复习的具体时间
 * @property phase 当前学习阶段，对应CardPhase枚举的值
 */
data class FlashCard(
    val id: Long = 0, // 移除Android Room的@PrimaryKey注解

    var stability: Double = 2.5,
    var difficulty: Double = 2.5,
    var interval: Int = 0,
    var dueDate: LocalDateTime = LocalDateTime.now(),
    var reviewCount: Int = 0,
    var lastReview: LocalDateTime = LocalDateTime.now(),
    var phase: Int = 0,
)
