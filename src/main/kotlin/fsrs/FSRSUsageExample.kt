/*
 * Copyright (c) 2023-2025 tang shimin
 *
 * This file is part of MuJing, which is licensed under GPL v3.
 */

package fsrs

/**
 * FSRS 使用示例
 *
 * 展示如何在实际应用中使用 FSRS 业务逻辑
 */
object FSRSUsageExample {

    /**
     * 完整的学习流程示例
     */
    fun demonstrateCompleteWorkflow() {
        println("=== FSRS 闪卡学习系统演示 ===\n")

        // 1. 初始化服务
        val fsrsService = FSRSService(requestRetention = 0.9)
        val cardManager = FlashCardManager(fsrsService)
        val sessionManager = LearningSessionManager(fsrsService)

        // 2. 创建示例卡片
        val words = listOf("apple", "banana", "cherry", "dragon", "elephant")
        val cards = cardManager.createCards(words, "vocabulary")
        println("创建了 ${cards.size} 张卡片")

        // 3. 模拟学习过程
        var updatedCards = cards.toMutableList()

        // 开始学习会话
        val session = sessionManager.startSession(updatedCards, maxNewCards = 3, maxReviewCards = 10)
        println("开始学习会话，本次会话包含 ${session.cards.size} 张卡片\n")

        // 模拟用户复习每张卡片
        var currentSession = session
        while (!currentSession.isCompleted()) {
            val currentCard = currentSession.getCurrentCard()!!
            println("正在复习卡片 #${currentCard.id}")

            // 获取评分选项
            val gradeOptions = fsrsService.getGradeOptions(currentCard)
            println("评分选项：")
            gradeOptions.forEach { grade ->
                println("  ${grade.title}: ${grade.txt} (难度: %.2f, 稳定性: %.2f)".format(grade.difficulty, grade.stability))
            }

            // 模拟用户选择（这里随机选择Good或Easy）
            val selectedGrade = if (Math.random() > 0.3) gradeOptions[1] else gradeOptions[0] // Good or Easy
            println("用户选择: ${selectedGrade.title}\n")

            // 应用评分
            val (updatedSession, updatedCard) = sessionManager.processCardReview(currentSession, selectedGrade)
            currentSession = updatedSession

            // 更新卡片列表
            val cardIndex = updatedCards.indexOfFirst { it.id == updatedCard.id }
            if (cardIndex >= 0) {
                updatedCards[cardIndex] = updatedCard
            }
        }

        // 4. 显示学习统计
        println("=== 学习会话完成 ===")
        println("完成进度: %.1f%%".format(currentSession.getProgress()))
        println("正确率: %.1f%%".format(currentSession.getAccuracy()))
        println()

        // 5. 分析卡片状态
        val analysisResult = cardManager.batchAnalyzeCards(updatedCards)
        println("=== 卡片分析结果 ===")
        println("总卡片数: ${analysisResult.totalCards}")
        println("平均难度: %.2f".format(analysisResult.averageDifficulty))
        println("平均稳定性: %.2f".format(analysisResult.averageStability))
        println("平均记忆保持率: %.2f".format(analysisResult.averageRetention))
        println("需要复习的卡片: ${analysisResult.cardsNeedingReview}")
        println("难度分布: ${analysisResult.difficultyDistribution}")
        println()

        // 6. 获取学习建议
        val recommendations = sessionManager.getLearningRecommendations(updatedCards, listOf(currentSession))
        println("=== 学习建议 ===")
        println("建议新卡片数: ${recommendations.suggestedNewCards}")
        println("建议复习卡片数: ${recommendations.suggestedReviewCards}")
        println("预计学习时间: ${recommendations.estimatedStudyTime} 分钟")
        println("学习负担等级: ${recommendations.studyLoad.level}")
        println("建议:")
        recommendations.recommendations.forEach {
            println("  • $it")
        }
        println()

        // 7. 演示高级功能
        demonstrateAdvancedFeatures(cardManager, updatedCards)
    }

    /**
     * 演示高级功能
     */
    private fun demonstrateAdvancedFeatures(cardManager: FlashCardManager, cards: List<FlashCard>) {
        println("=== 高级功能演示 ===")

        // 筛选功能
        val hardCards = cardManager.filterCards(cards, CardFilter(
            minDifficulty = 5.0,
            dueOnly = true
        ))
        println("困难且到期的卡片: ${hardCards.size} 张")

        // 排序功能
        val prioritySortedCards = cardManager.sortCards(cards, SortStrategy.PRIORITY)
        println("按优先级排序的前3张卡片:")
        prioritySortedCards.take(3).forEach { card ->
            val analytics = cardManager.getCardAnalytics(card)
            println("  卡片 #${card.id}: 难度 ${analytics.difficulty}, 距离复习 ${analytics.nextReviewIn} 天")
        }

        // 暂停和恢复功能
        if (cards.isNotEmpty()) {
            val cardToSuspend = cards.first()
            val suspendedCard = cardManager.suspendCard(cardToSuspend, 30)
            println("暂停了卡片 #${cardToSuspend.id}，30天后复习")

            val resumedCard = cardManager.resumeCard(suspendedCard)
            println("恢复了卡片 #${suspendedCard.id}，立即可复习")
        }
    }

    /**
     * 简单的单卡片学习示例
     */
    fun demonstrateSimpleCardReview() {
        println("\n=== 简单卡片复习示例 ===")

        val fsrsService = FSRSService()

        // 创建新卡片
        var card = fsrsService.createNewCard(1L)
        println("创建新卡片 #${card.id}")

        // 第一次学习
        val gradeOptions = fsrsService.getGradeOptions(card)
        println("首次学习的评分选项:")
        gradeOptions.forEach { grade ->
            println("  ${grade.title}: ${grade.txt}")
        }

        // 用户选择 "Good"
        val selectedGrade = gradeOptions.find { it.choice == Rating.Good }!!
        card = fsrsService.applyGrade(card, selectedGrade)

        println("选择 'Good' 后:")
        println("  下次复习: ${card.dueDate}")
        println("  难度: ${card.difficulty}")
        println("  稳定性: ${card.stability}")
        println("  复习次数: ${card.reviewCount}")
    }

    /**
     * 批量操作示例
     */
    fun demonstrateBatchOperations() {
        println("\n=== 批量操作示例 ===")

        val fsrsService = FSRSService()
        val cardManager = FlashCardManager(fsrsService)

        // 批量创建卡片
        val subjects = listOf("数学", "物理", "化学", "生物", "历史")
        val cards = cardManager.createCards(subjects, "学科")

        // 批量计算评分选项
        val batchGrades = fsrsService.batchCalculateGrades(cards)
        println("为 ${batchGrades.size} 张卡片计算了评分选项")

        // 获取学习统计
        val stats = fsrsService.getLearningStat(cards)
        println("学习统计:")
        println("  总卡片: ${stats.totalCards}")
        println("  新卡片: ${stats.newCards}")
        println("  到期卡片: ${stats.dueCards}")
        println("  平均难度: %.2f".format(stats.averageDifficulty))
    }
}

/**
 * 主函数 - 运行所有示例
 */
fun main() {
    FSRSUsageExample.demonstrateCompleteWorkflow()
    FSRSUsageExample.demonstrateSimpleCardReview()
    FSRSUsageExample.demonstrateBatchOperations()
}
