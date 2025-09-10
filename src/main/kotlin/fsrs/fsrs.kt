/*
 * Copyright (c) 2025 tang shimin
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

import java.lang.Math.min
import kotlin.math.abs
import kotlin.math.exp
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.random.Random
import kotlin.ranges.coerceIn

/**
 * FSRS（Free Spaced Repetition Scheduler）算法实现类
 *
 * FSRS是一种基于遗忘曲线的间隔重复算法，用于优化记忆卡片的复习时间安排。
 * 该算法通过追踪每张卡片的稳定性(stability)和难度(difficulty)来预测最佳的复习间隔。
 *
 * @param requestRetention 目标记忆保持率（0.0-1.0），通常设置为0.9表示90%的保持率
 * @param params FSRS算法的21个参数列表，用于调整算法行为
 * @param isReview 是否为复习模式，影响模糊化(fuzz)的计算
 */
class FSRS(
    private val requestRetention: Double,
    private val params: List<Double>,
    private val isReview: Boolean = false,
) {

    /**
     * 初始状态数据类
     *
     * 用于存储卡片的难度和稳定性初始值
     *
     * @property difficulty 记忆难度值（1.0-10.0）
     * @property stability 记忆稳定性值（表示记忆保持的强度）
     */
    data class InitState(var difficulty: Double = 0.0, var stability: Double = 0.0)

    /** 遗忘曲线的衰减参数，从算法参数中计算得出 */
    private val decay = -params[20]

    /** 用于计算复习间隔的因子 */
    private val factor = 0.9.pow(1.0 / decay) - 1

    /** 是否启用间隔模糊化，用于避免复习时间过于集中 */
    private val enableFuzz = true

    /**
     * 评分选项列表
     *
     * 包含四种不同的复习评分选项，每个都有对应的颜色、标题和评分等级
     */
    var gradeList = mutableListOf<Grade>(
        // 使用颜色代码替代Android资源
        Grade("#2196F3", "Easy", 0, 0, "", Rating.Easy),
        Grade("#4CAF50", "Good", 0, 0, "", Rating.Good),
        Grade("#9C27B0", "Hard", 0, 0, "", Rating.Hard),
        Grade("#F44336", "Again", 0, 0, "", Rating.Again),
    )

    /**
     * 计算给定闪卡的所有评分选项
     *
     * 根据闪卡当前的学习阶段和历史数据，计算用户选择不同评分时的结果，
     * 包括下次复习的间隔时间、难度和稳定性等参数。
     *
     * @param flashCard 要计算的闪卡对象
     * @return 返回包含四种评分选项的列表（Easy, Good, Hard, Again）
     */
    fun calculate(flashCard: FlashCard): List<Grade> {
        var stateAgain: InitState
        var stateHard: InitState
        var stateGood: InitState
        var stateEasy: InitState

        var durationHard = 5 * 60 * 1000L //5min
        var durationGood: Long
        var durationEasy: Long

        var ivlHard = 0
        var ivlGood = 0
        var ivlEasy: Int

        var txtHard: String
        var txtGood: String
        var txtEasy: String

        val dayConvertor: Long = 24 * 60 * 60 * 1000

        when (flashCard.phase) {
            CardPhase.Added.value -> {
                // 修复：使用正确的初始化方法而不是空的InitState()
                stateAgain = initState(Rating.Again)
                stateHard = initState(Rating.Hard)
                stateGood = initState(Rating.Good)
                stateEasy = initState(Rating.Easy)

                ivlEasy = 1

                txtHard = "5 Min"
                txtGood = "10 Min"
                txtEasy = "1 day"

                durationGood = 10 * 60 * 1000L
                durationEasy = ivlEasy * dayConvertor
            }

            CardPhase.ReLearning.value -> {
                if (flashCard.difficulty == 0.0) {
                    stateAgain = initState(Rating.Again)
                    stateHard = initState(Rating.Hard)
                    stateGood = initState(Rating.Good)
                    stateEasy = initState(Rating.Easy)
                } else {
                    val lastD = flashCard.difficulty
                    val lastS = flashCard.stability

                    stateAgain = InitState(
                        difficulty = nextDifficulty(lastD, Rating.Again),
                        stability = nextShortTermStability(lastS, Rating.Again)
                    )
                    stateHard = InitState(
                        difficulty = nextDifficulty(lastD, Rating.Hard),
                        stability = nextShortTermStability(lastS, Rating.Hard)
                    )
                    stateGood = InitState(
                        difficulty = nextDifficulty(lastD, Rating.Good),
                        stability = nextShortTermStability(lastS, Rating.Good)
                    )
                    stateEasy = InitState(
                        difficulty = nextDifficulty(lastD, Rating.Easy),
                        stability = nextShortTermStability(lastS, Rating.Easy)
                    )
                }

                ivlGood = nextInterval(stateGood.stability)
                ivlEasy = nextInterval(stateEasy.stability)
                ivlEasy = max(ivlEasy, ivlGood + 1)

                txtHard = "10 Min"
                txtGood = convertDays(ivlGood)
                txtEasy = convertDays(ivlEasy)

                durationGood = ivlGood * dayConvertor
                durationEasy = ivlEasy * dayConvertor
            }

            else -> {
                val interval = flashCard.interval
                val lastD = flashCard.difficulty
                val lastS = flashCard.stability

                val retrievability = forgettingCurve(interval.toDouble(), lastS)

                stateAgain = InitState(
                    difficulty = nextDifficulty(lastD, Rating.Again),
                    stability = nextForgetStability(lastD, lastS, retrievability)
                )
                stateHard = InitState(
                    difficulty = nextDifficulty(lastD, Rating.Hard),
                    stability = nextRecallStability(lastD, lastS, retrievability, Rating.Hard)
                )
                stateGood = InitState(
                    difficulty = nextDifficulty(lastD, Rating.Good),
                    stability = nextRecallStability(lastD, lastS, retrievability, Rating.Good)
                )
                stateEasy = InitState(
                    difficulty = nextDifficulty(lastD, Rating.Easy),
                    stability = nextRecallStability(lastD, lastS, retrievability, Rating.Easy)
                )

                ivlHard = nextInterval(stateHard.stability)
                ivlGood = nextInterval(stateGood.stability)
                ivlEasy = nextInterval(stateEasy.stability)

                ivlHard = kotlin.math.min(ivlHard, ivlGood)
                ivlGood = kotlin.math.min(ivlGood, ivlHard + 1)
                ivlEasy = kotlin.math.min(ivlEasy, ivlGood + 1)

                txtHard = convertDays(ivlHard)
                txtGood = convertDays(ivlGood)
                txtEasy = convertDays(ivlEasy)

                durationHard = ivlHard * dayConvertor
                durationGood = ivlGood * dayConvertor
                durationEasy = ivlEasy * dayConvertor
            }
        }

        gradeList[0] = gradeList[0].copy(
            stability = stateEasy.stability, difficulty = stateEasy.difficulty,
            durationMillis = durationEasy, interval = ivlEasy, txt = txtEasy
        )
        gradeList[1] = gradeList[1].copy(
            stability = stateGood.stability, difficulty = stateGood.difficulty,
            durationMillis = durationGood, interval = ivlGood, txt = txtGood
        )
        gradeList[2] = gradeList[2].copy(
            stability = stateHard.stability, difficulty = stateHard.difficulty,
            durationMillis = durationHard, interval = ivlHard, txt = txtHard
        )
        gradeList[3] = gradeList[3].copy(
            stability = stateAgain.stability,
            difficulty = stateAgain.difficulty,
            interval = flashCard.interval,
            durationMillis = 3 * 60 * 1000L,
            txt = "< 3 Min"
        )

        return gradeList
    }

    /**
     * 将天数转换为可读的时间格式
     *
     * @param days 天数
     * @return 格式化的时间字符串（如"5 day"、"2.5 month"、"1.2 year"）
     */
    private fun convertDays(days: Int): String {
        return if (days > 365) "${days / 365.0} year"
        else if (days > 30) "${days / 30.0} month"
        else "$days day"
    }

    /**
     * 应用间隔模糊化
     *
     * 为避免复习时间过于集中，对计算出的间隔进行随机调整。
     * 在原始间隔的95%-105%范���内进行随机化。
     *
     * @param interval 原始间隔（天）
     * @param fuzzFactor 模糊化因子（0.0-1.0的随机数）
     * @param scheduledDays 已安排的复习天数
     * @return 模糊化后的间隔
     */
    private fun applyFuzz(
        interval: Double,
        fuzzFactor: Double,
        scheduledDays: Int = 0
    ): Double {
        if (!enableFuzz || interval < 2.5) return interval

        val ivl = interval.roundToInt()
        var minIvl = max(2, (ivl * 0.95 - 1).roundToInt())
        val maxIvl = (ivl * 1.05 + 1).roundToInt()

        if (isReview && ivl > scheduledDays)
            minIvl = max(minIvl, scheduledDays + 1)

        return floor(fuzzFactor * (maxIvl - minIvl + 1) + minIvl)
    }

    /**
     * 计算遗忘曲线
     *
     * 根据间隔时间和稳定性计算记忆的可提取性(retrievability)。
     * 使用指数衰减模型：R(t) = exp(-t/S)
     *
     * @param interval 距离上次复习的间隔时间
     * @param stability 记忆稳定性
     * @return 记忆可提取性（0.0-1.0）
     */
    private fun forgettingCurve(interval: Double, stability: Double): Double {
        return exp(-interval / stability)
    }

    /**
     * 生成模糊化因子
     *
     * 使用当前时间戳作为种子生成0.0-1.0之间的随机数，
     * 用于间隔时间的模糊化处理。
     *
     * @return 0.0-1.0之间的随机数
     */
    private fun generateFuzzFactor(): Double {
        val seed = System.currentTimeMillis()
        val random = Random(seed)
        return random.nextDouble()  // returns value between 0.0 and 1.0
    }

    /**
     * 计算初始难度
     *
     * 根据用户的首次评分计算卡片的初始难度值。
     * 使用算法参数4和5进行计算。
     *
     * @param rating 用户评分
     * @return 初始难度值（1.0-10.0）
     */
    private fun initDifficulty(rating: Rating): Double {
        val base = params[4]
        val exponent = params[5] * (rating.value - 1)
        val raw = base - exp(exponent) + 1
        return String.format("%.2f", raw.coerceIn(1.0, 10.0)).toDouble()
    }

    /**
     * 计算初始稳定性
     *
     * 根据用户的首次评分计算卡片的初始稳定性值。
     * 使用算法参数0-3（对应Again、Hard、Good、Easy）。
     *
     * @param rating 用户评分
     * @return 初始稳定性值
     */
    private fun initStability(rating: Rating): Double {
        val index = rating.value - 1
        val value = params.getOrElse(index) { 0.1 }
        return String.format("%.2f", value.coerceAtMost(0.1)).toDouble()
    }

    /**
     * 初始化卡片状态
     *
     * 根据用户评分创建新卡片的初始状态，包括难度和稳定性。
     *
     * @param rating 用户评分
     * @return 包含初始难度和稳定性的状态对象
     */
    private fun initState(rating: Rating): InitState {
        return InitState(
            difficulty = initDifficulty(rating),
            stability = initStability(rating)
        )
    }

    /**
     * 线性阻尼函数
     *
     * 对难度变化进行阻尼处理，避免难度变化过于剧烈。
     * 难度越高，变化的阻尼越大。
     *
     * @param delta 难度变化值
     * @param oldD 当前难度
     * @return 阻尼后的难度变化值
     */
    private fun linearDamping(delta: Double, oldD: Double): Double {
        return delta * (10 - oldD / 9)
    }

    /**
     * 均值回归
     *
     * 将难���值向初始Easy评分的难度值回归，避免难度过度偏离。
     * 使用算法参数7控制回归程度。
     *
     * @param initD 初始难度（Easy评分对应的难度）
     * @param nextD 计算出的下一个难度
     * @return 回归后的难度值
     */
    private fun meanReversion(initD: Double, nextD: Double): Double {
        return params[7] * initD + (1 - params[7]) * nextD
    }

    /**
     * 计算下次复习间隔
     *
     * 根据记忆稳定性和目标保持率计算下次复习的间隔时间。
     * 应用模糊化以避免复习时间过于集中。
     *
     * @param stability 记忆稳定性
     * @param maxInterval 最大间隔天数（默认36500天约100年）
     * @param lastInterval 上次间隔天数
     * @return 下次复习间隔（天）
     */
    private fun nextInterval(
        stability: Double,
        maxInterval: Int = 36500, lastInterval: Int = 0
    ): Int {
        val fuzzFactor = generateFuzzFactor()
        val rawInterval = stability / factor * (requestRetention.pow(1 / decay) - 1)
        val fuzzed = applyFuzz(rawInterval, fuzzFactor, scheduledDays = lastInterval)
        return fuzzed.roundToInt().coerceIn(1, maxInterval)
    }

    /**
     * 计算下一个难度值
     *
     * 根据当前难度和用户评分计算新的难度值。
     * 应用线性阻尼和均值回归来稳定难度变化。
     *
     * @param currentD 当前难度
     * @param rating 用户评分
     * @return 新的难度值（1.0-10.0）
     */
    private fun nextDifficulty(currentD: Double, rating: Rating): Double {
        val deltaD = -params[6] * (rating.value - 3)
        val damped = linearDamping(deltaD, currentD)
        val nextD = currentD + damped
        val reverted = meanReversion(initDifficulty(Rating.Easy), nextD)
        return String.format("%.2f", reverted.coerceIn(1.0, 10.0)).toDouble()
    }

    /**
     * 计算短期记忆稳定性
     *
     * 用于学习阶段和重新学习阶段，计算短期记忆的稳定性。
     * 使用算法参数17-19进行计算。
     *
     * @param currentS 当前稳定性
     * @param rating 用户评分
     * @return 新的短期稳定性值
     */
    private fun nextShortTermStability(currentS: Double, rating: Rating): Double {
        var sinc = exp(params[17] * (rating.value - 3 + params[18])) * currentS.pow(-params[19])
        if (rating.value >= 3) {
            sinc = max(sinc, 1.0)
        }
        return String.format("%.2f", abs(currentS * sinc)).toDouble()
    }

    /**
     * 计算遗忘后的稳定性
     *
     * 当用户评分为Again时，计算卡片遗忘后的新稳定性。
     * 使用算法参数11-14进行计算。
     *
     * @param difficulty 卡片难度
     * @param stability 当前稳定性
     * @param retrievability 记忆可提取性
     * @return 遗忘后的稳定性值
     */
    private fun nextForgetStability(
        difficulty: Double,
        stability: Double,
        retrievability: Double
    ): Double {
        val sMin = stability / exp(params[17] * params[18])

        val result = params[11] *
                difficulty.pow(-params[12]) *
                ((stability + 1).pow(params[13]) - 1) *
                exp((1 - retrievability) * params[14])

        return "%.2f".format(min(result, sMin)).toDouble()
    }

    /**
     * 计算成功回忆后的稳定性
     *
     * 当用户成功回忆（评分为Hard、Good或Easy）时，计算新的稳定性。
     * 对Hard评分应用惩罚因子，对Easy评分应用奖励因子。
     * 使用算法参数8-10、15-16进行计算。
     *
     * @param d 卡片难度
     * @param s 当前稳定性
     * @param r 记忆可提取性
     * @param rating 用户评分
     * @return 新的稳定性值
     */
    private fun nextRecallStability(d: Double, s: Double, r: Double, rating: Rating): Double {
        val hardPenalty = if (rating == Rating.Hard) params[15] else 1.0
        val easyBonus = if (rating == Rating.Easy) params[16] else 1.0

        val factor = exp(params[8]) *
                (11 - d) *
                s.pow(-params[9]) *
                (exp((1 - r) * params[10]) - 1) *
                hardPenalty *
                easyBonus

        val result = s * (1 + factor)
        return "%.2f".format(result).toDouble()
    }
}
