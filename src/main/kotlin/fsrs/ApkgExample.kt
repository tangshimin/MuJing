package fsrs


/**
 * APKG 使用示例
 * 演示如何创建不同类型的 Anki 包
 */
object ApkgExample {

    /**
     * 创建基础单词学习包
     */
    fun createBasicVocabularyDeck(): String {
        val creator = ApkgCreator()

        // 1. 创建牌组
        val deckId = ApkgCreator.generateId()
        val deck = Deck(
            id = deckId,
            name = "基础英语词汇",
            description = "包含常用英语单词的学习卡片"
        )
        creator.addDeck(deck)

        // 2. 创建模型（卡片模板）
        val model = ApkgCreator.createBasicModel()
        creator.addModel(model)

        // 3. 添加单词数据
        val basicWords = listOf(
            "apple" to "苹果",
            "book" to "书",
            "cat" to "猫",
            "dog" to "狗",
            "water" to "水",
            "house" to "房子",
            "car" to "汽车",
            "tree" to "树",
            "sun" to "太阳",
            "moon" to "月亮"
        )

        basicWords.forEach { (english, chinese) ->
            val note = Note(
                id = ApkgCreator.generateId(),
                modelId = model.id,
                fields = listOf(english, chinese)
            )
            creator.addNote(note, deckId)
        }

        // 4. 生成文件
        val outputPath = "basic_vocabulary.apkg"
        creator.createApkg(outputPath)
        return outputPath
    }

    /**
     * 创建高级单词学习包（包含音频和例句）
     */
    fun createAdvancedVocabularyDeck(): String {
        val creator = ApkgCreator()

        // 1. 创建牌组
        val deckId = ApkgCreator.generateId()
        val deck = Deck(
            id = deckId,
            name = "高级英语词汇",
            description = "包含音频和例句的英语单词学习"
        )
        creator.addDeck(deck)

        // 2. 使用高级单词模型
        val model = ApkgCreator.createWordModel()
        creator.addModel(model)

        // 3. 添加高级单词数据
        val advancedWords = listOf(
            WordData("sophisticated", "复杂的，精密的", "", "She has sophisticated taste in art."),
            WordData("magnificent", "壮丽的，宏伟的", "", "The view from the mountain top was magnificent."),
            WordData("fundamental", "基本的，根本的", "", "Education is fundamental to personal development."),
            WordData("demonstrate", "证明，演示", "", "The teacher will demonstrate how to solve this problem."),
            WordData("significant", "重要的，显著的", "", "This discovery has significant implications for science.")
        )

        advancedWords.forEach { word ->
            val note = Note(
                id = ApkgCreator.generateId(),
                modelId = model.id,
                fields = listOf(word.english, word.chinese, word.audio, word.example)
            )
            creator.addNote(note, deckId)
        }

        // 4. 生成文件
        val outputPath = "advanced_vocabulary.apkg"
        creator.createApkg(outputPath)
        return outputPath
    }

    /**
     * 从 JSON 文件创建 APKG
     */
    fun createFromJsonFile(jsonPath: String): String {
        // 这里可以读取你现有的词汇 JSON 文件
        // 参考 build/resources/test/Vocabulary.json 的格式
        val creator = ApkgCreator()

        val deckId = ApkgCreator.generateId()
        val deck = Deck(
            id = deckId,
            name = "导入的词汇",
            description = "从 JSON 文件导入的词汇数据"
        )
        creator.addDeck(deck)

        val model = ApkgCreator.createWordModel()
        creator.addModel(model)

        // TODO: 实现 JSON 解析逻辑
        // val vocabularyData = Json.decodeFromString<VocabularyData>(File(jsonPath).readText())

        val outputPath = "imported_vocabulary.apkg"
        creator.createApkg(outputPath)
        return outputPath
    }

    private data class WordData(
        val english: String,
        val chinese: String,
        val audio: String,
        val example: String
    )
}

// 使用示例
fun main() {
    println("创建基础词汇包...")
    val basicDeck = ApkgExample.createBasicVocabularyDeck()
    println("创建完成: $basicDeck")

    println("创建高级词汇包...")
    val advancedDeck = ApkgExample.createAdvancedVocabularyDeck()
    println("创建完成: $advancedDeck")

    println("所有 APKG 文件创建完成！")
}
