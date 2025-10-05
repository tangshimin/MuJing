/*
 * Copyright (c) 2023-2025 tang shimin
 *
 * This file is part of MuJing, which is licensed under GPL v3.
 */

package fsrs

import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId

/**
 * FSRS 时间工具类
 *
 * 提供与 FSRS 算法配套的时间计算和转换功能
 */
object FSRSTimeUtils {

    /**
     * 在当前时间基础上添加指定毫秒数
     *
     * 用于根据 FSRS 计算出的 durationMillis 确定下次复习的具体时间点
     *
     * @param millis 要添加的毫秒数（通常来自 Grade.durationMillis）
     * @return 计算后的本地时间
     */
    fun addMillisToNow(millis: Long): LocalDateTime {
        val nowInstant = Instant.now()
        val newInstant = nowInstant.plusMillis(millis)
        return LocalDateTime.ofInstant(newInstant, ZoneId.systemDefault())
    }

    /**
     * 在指定时间基础上添加毫秒数
     *
     * @param baseTime 基准时间
     * @param millis 要添加的毫秒数
     * @return 计算后的时间
     */
    fun addMillisToTime(baseTime: LocalDateTime, millis: Long): LocalDateTime {
        val baseInstant = baseTime.atZone(ZoneId.systemDefault()).toInstant()
        val newInstant = baseInstant.plusMillis(millis)
        return LocalDateTime.ofInstant(newInstant, ZoneId.systemDefault())
    }

    /**
     * 根据 FSRS Grade 结果更新闪卡的下次复习时间
     *
     * @param card 要更新的闪卡
     * @param selectedGrade 用户选择的评分结果
     * @return 更新了 dueDate 的闪卡副本
     */
    fun updateCardDueDate(card: FlashCard, selectedGrade: Grade): FlashCard {
        val nextReviewTime = addMillisToNow(selectedGrade.durationMillis)
        return card.copy(
            dueDate = nextReviewTime,
            stability = selectedGrade.stability,
            difficulty = selectedGrade.difficulty,
            interval = selectedGrade.interval,
            lastReview = LocalDateTime.now(),
            reviewCount = card.reviewCount + 1
        )
    }

    /**
     * 检查闪卡是否到期需要复习
     *
     * @param card 要检查的闪卡
     * @param currentTime 当前时间（默认为系统当前时间）
     * @return 如果到期返回 true，否则返回 false
     */
    fun isCardDue(card: FlashCard, currentTime: LocalDateTime = LocalDateTime.now()): Boolean {
        return currentTime.isAfter(card.dueDate) || currentTime.isEqual(card.dueDate)
    }

    /**
     * 计算距离下次复习还有多长时间
     *
     * @param card 闪卡
     * @param currentTime 当前时间（默认为系统当前时间）
     * @return 剩余时间的毫秒数，如果已经到期则返回 0
     */
    fun timeUntilDue(card: FlashCard, currentTime: LocalDateTime = LocalDateTime.now()): Long {
        if (isCardDue(card, currentTime)) {
            return 0L
        }

        val currentInstant = currentTime.atZone(ZoneId.systemDefault()).toInstant()
        val dueInstant = card.dueDate.atZone(ZoneId.systemDefault()).toInstant()

        return dueInstant.toEpochMilli() - currentInstant.toEpochMilli()
    }
}
