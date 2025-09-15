package fsrs

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.nio.file.Path
import java.sql.DriverManager
import java.util.zip.ZipFile
import kotlinx.serialization.json.*

/**
 * APKG 创建器功能测试
 * 测试 ApkgCreator 类的各种功能
 */
class ApkgCreatorTest {

    @TempDir
    lateinit var tempDir: Path

    private lateinit var outputDir: File

    @BeforeEach
    fun setUp() {
        // 使用项目根目录下的 test-output 文件夹，而不是临时目录
        outputDir = File(System.getProperty("user.dir"), "test-output")
        outputDir.mkdirs() // 确保目录存在

        println("📁 测试输出目录: ${outputDir.absolutePath}")
        println("💡 生成的 APKG 文件将保存在此目录，不会被自动删除")
    }

    @AfterEach
    fun tearDown() {
        // 不删除文件，让用户可以手动测试
        println("📋 测试完成，APKG 文件已保存在: ${outputDir.absolutePath}")
        println("🔍 可以在 Finder 中导航到此路径查看文件")
    }

    /**
     * 测试创建基础单词学习包
     */
    @Test
    fun testCreateBasicVocabularyDeck() {
        val creator = ApkgCreator()

        // 1. 创建牌组
        val deckId = ApkgCreator.generateId()
        val deck = ApkgCreator.Deck(
            id = deckId,
            name = "基础英语词汇",
            desc = "包含常用英语单词的学习卡片"
        )
        creator.addDeck(deck)

        // 2. 创建模型
        val model = ApkgCreator.createBasicModel()
        creator.addModel(model)

        // 3. 添加测试单词数据
        val testWords = listOf(
            "apple" to "苹果",
            "book" to "书",
            "cat" to "猫",
            "dog" to "狗",
            "water" to "水"
        )

        testWords.forEach { (english, chinese) ->
            val note = ApkgCreator.Note(
                id = ApkgCreator.generateId(),
                mid = model.id,
                fields = listOf(english, chinese)
            )
            creator.addNote(note, deckId)
        }

        // 4. 生成文件
        val outputPath = File(outputDir, "test_basic_vocabulary.apkg").absolutePath
        creator.createApkg(outputPath)
        println("📦 生成的 APKG 文件: $outputPath")

        // 5. 验证文件创建成功
        val apkgFile = File(outputPath)
        assertTrue(apkgFile.exists(), "APKG 文件应该被成功创建")
        assertTrue(apkgFile.length() > 0, "APKG 文件应该不为空")
        println("📊 文件大小: ${apkgFile.length()} 字节")

        // 6. 验证 ZIP 结构
        verifyApkgStructure(apkgFile)

        // 7. 验证数据库内容
        verifyDatabaseContent(apkgFile, testWords.size, 1, 1)

        println("✅ 基础词汇包测试通过")
    }

    /**
     * 测试创建高级单词学习包（包含音频和例句）
     */
    @Test
    fun testCreateAdvancedVocabularyDeck() {
        val creator = ApkgCreator()

        // 1. 创建牌组
        val deckId = ApkgCreator.generateId()
        val deck = ApkgCreator.Deck(
            id = deckId,
            name = "高级英语词汇",
            desc = "包含音频和例句的英语单词学习"
        )
        creator.addDeck(deck)

        // 2. 使用高级单词模型
        val model = ApkgCreator.createWordModel()
        creator.addModel(model)

        // 3. 添加高级单词数据
        val advancedWords = listOf(
            WordData("sophisticated", "复杂的，精密的", "", "She has sophisticated taste in art."),
            WordData("magnificent", "壮丽的，宏伟的", "", "The view from the mountain top was magnificent."),
            WordData("fundamental", "基本的，根本的", "", "Education is fundamental to personal development.")
        )

        advancedWords.forEach { word ->
            val note = ApkgCreator.Note(
                id = ApkgCreator.generateId(),
                mid = model.id,
                fields = listOf(word.english, word.chinese, word.audio, word.example)
            )
            creator.addNote(note, deckId)
        }

        // 4. 生成文件
        val outputPath = File(outputDir, "test_advanced_vocabulary.apkg").absolutePath
        creator.createApkg(outputPath)
        println("📦 生成的高级 APKG 文件: $outputPath")

        // 5. 验证文件创建成功
        val apkgFile = File(outputPath)
        assertTrue(apkgFile.exists(), "高级 APKG 文件应该被成功创建")
        assertTrue(apkgFile.length() > 0, "高级 APKG 文件应该不为空")
        println("📊 文件大小: ${apkgFile.length()} 字节")

        // 6. 验证 ZIP 结构
        verifyApkgStructure(apkgFile)

        // 7. 验证数据库内容（高级模型有2个模板，所以每个笔记生成2张卡片）
        verifyDatabaseContent(apkgFile, advancedWords.size, 1, 1)

        // 8. 验证高级模型的字段数量
        verifyAdvancedModelFields(apkgFile)

        println("✅ 高级词汇包测试通过")
    }

    /**
     * 测试从现有词汇数据创建 APKG
     */
    @Test
    fun testCreateFromVocabularyData() {
        val creator = ApkgCreator()

        // 1. 创建牌组
        val deckId = ApkgCreator.generateId()
        val deck = ApkgCreator.Deck(
            id = deckId,
            name = "测试词汇导入",
            desc = "从测试数据导入的词汇"
        )
        creator.addDeck(deck)

        // 2. 创建模型
        val model = ApkgCreator.createWordModel()
        creator.addModel(model)

        // 3. 模拟从 JSON 数据导入（使用测试数据）
        val vocabularyData = listOf(
            mapOf(
                "word" to "example",
                "pronunciation" to "/ɪɡˈzæmpəl/",
                "definition" to "实例，例子",
                "exchange" to "examples",
                "translation" to "n. 例子，实例；榜样，典型",
                "pos" to "n.",
                "collins" to "5",
                "oxford" to "true",
                "tag" to "gre ielts toefl",
                "bnc" to "1015",
                "frq" to "20941"
            ),
            mapOf(
                "word" to "test",
                "pronunciation" to "/test/",
                "definition" to "测试，考试",
                "exchange" to "tests",
                "translation" to "n. 试验；检验 v. 试验；测试",
                "pos" to "n. v.",
                "collins" to "5",
                "oxford" to "true",
                "tag" to "gre ielts toefl",
                "bnc" to "578",
                "frq" to "52964"
            )
        )

        // 4. 添加词汇数据
        vocabularyData.forEach { wordMap ->
            val word = wordMap["word"] as String
            val definition = wordMap["definition"] as String
            val pronunciation = wordMap["pronunciation"] as String
            val translation = wordMap["translation"] as String

            val note = ApkgCreator.Note(
                id = ApkgCreator.generateId(),
                mid = model.id,
                fields = listOf(word, definition, "", "$pronunciation\n$translation"),
                tags = "imported vocabulary"
            )
            creator.addNote(note, deckId)
        }

        // 5. 生成文件
        val outputPath = File(outputDir, "test_imported_vocabulary.apkg").absolutePath
        creator.createApkg(outputPath)
        println("📦 生成的导入词汇 APKG 文件: $outputPath")

        // 6. 验证文件创建成功
        val apkgFile = File(outputPath)
        assertTrue(apkgFile.exists(), "导入词汇 APKG 文件应该被成功创建")
        assertTrue(apkgFile.length() > 0, "导入词汇 APKG 文件应该不为空")
        println("📊 文件大小: ${apkgFile.length()} 字节")

        // 7. 验证 ZIP 结构
        verifyApkgStructure(apkgFile)

        // 8. 验证数据库内容
        verifyDatabaseContent(apkgFile, vocabularyData.size, 1, 1)

        // 9. 验证标签
        verifyNoteTags(apkgFile, "imported vocabulary")

        println("✅ 词汇数据导入测试通过")
    }

    /**
     * 测试媒体文件处理
     */
    @Test
    fun testMediaFileHandling() {
        val creator = ApkgCreator()

        // 1. 创建牌组和模型
        val deckId = ApkgCreator.generateId()
        val deck = ApkgCreator.Deck(id = deckId, name = "媒体测试")
        creator.addDeck(deck)

        val model = ApkgCreator.createWordModel()
        creator.addModel(model)

        // 2. 添加测试媒体文件
        val audioData = "fake audio data".toByteArray()
        val imageData = "fake image data".toByteArray()

        creator.addMediaFile("test_audio.mp3", audioData)
        creator.addMediaFile("test_image.jpg", imageData)

        // 3. 添加引用媒体的笔记
        val note = ApkgCreator.Note(
            id = ApkgCreator.generateId(),
            mid = model.id,
            fields = listOf(
                "hello",
                "你好",
                "[sound:test_audio.mp3]",
                "Hello world! <img src=\"test_image.jpg\">"
            )
        )
        creator.addNote(note, deckId)

        // 4. 生成文件
        val outputPath = File(outputDir, "test_media.apkg").absolutePath
        creator.createApkg(outputPath)
        println("📦 生成的 APKG 文件: $outputPath")

        // 5. 验证媒体文件
        verifyMediaFiles(File(outputPath), mapOf(
            "test_audio.mp3" to audioData,
            "test_image.jpg" to imageData
        ))

        println("✅ 媒体文件处理测试通过")
    }

    /**
     * 测试多牌组支持
     */
    @Test
    fun testMultipleDeckSupport() {
        val creator = ApkgCreator()

        // 1. 创建多个牌组
        val deck1Id = ApkgCreator.generateId()
        val deck1 = ApkgCreator.Deck(id = deck1Id, name = "基础词汇")
        creator.addDeck(deck1)

        val deck2Id = ApkgCreator.generateId()
        val deck2 = ApkgCreator.Deck(id = deck2Id, name = "高级词汇")
        creator.addDeck(deck2)

        // 2. 创建模型
        val model = ApkgCreator.createBasicModel()
        creator.addModel(model)

        // 3. 向不同牌组添加笔记
        val basicNote = ApkgCreator.Note(
            id = ApkgCreator.generateId(),
            mid = model.id,
            fields = listOf("cat", "猫")
        )
        creator.addNote(basicNote, deck1Id)

        val advancedNote = ApkgCreator.Note(
            id = ApkgCreator.generateId(),
            mid = model.id,
            fields = listOf("sophisticated", "复杂的")
        )
        creator.addNote(advancedNote, deck2Id)

        // 4. 生成文件
        val outputPath = File(outputDir, "test_multiple_decks.apkg").absolutePath
        creator.createApkg(outputPath)
        println("📦 生成的 APKG 文件: $outputPath")

        // 5. 验证多牌组
        verifyMultipleDecks(File(outputPath), 2)

        println("✅ 多牌组支持测试通过")
    }

    // === 辅助验证方法 ===

    private fun verifyApkgStructure(apkgFile: File) {
        ZipFile(apkgFile).use { zipFile ->
            val entries = zipFile.entries().toList().map { it.name }

            assertTrue(entries.contains("collection.anki2"), "应该包含 collection.anki2 文件")
            assertTrue(entries.contains("media"), "应该包含 media 文件")
        }
    }

    private fun verifyDatabaseContent(apkgFile: File, expectedNotes: Int, expectedDecks: Int, expectedModels: Int) {
        ZipFile(apkgFile).use { zipFile ->
            val dbEntry = zipFile.getEntry("collection.anki2")
            assertNotNull(dbEntry, "collection.anki2 应该存在")

            val tempDbFile = File.createTempFile("test_db", ".anki2")
            try {
                zipFile.getInputStream(dbEntry).use { input ->
                    tempDbFile.outputStream().use { output ->
                        input.copyTo(output)
                    }
                }

                val url = "jdbc:sqlite:${tempDbFile.absolutePath}"
                DriverManager.getConnection(url).use { conn ->
                    // 检查笔记数量
                    conn.createStatement().use { stmt ->
                        val rs = stmt.executeQuery("SELECT COUNT(*) FROM notes")
                        rs.next()
                        assertEquals(expectedNotes, rs.getInt(1), "笔记数量应该匹配")
                    }

                    // 检查牌组数量
                    conn.createStatement().use { stmt ->
                        val rs = stmt.executeQuery("SELECT decks FROM col WHERE id = 1")
                        rs.next()
                        val decksJson = rs.getString(1)
                        val decks = Json.parseToJsonElement(decksJson).jsonObject
                        assertEquals(expectedDecks, decks.size, "牌组数量应该匹配")
                    }

                    // 检查模型数量
                    conn.createStatement().use { stmt ->
                        val rs = stmt.executeQuery("SELECT models FROM col WHERE id = 1")
                        rs.next()
                        val modelsJson = rs.getString(1)
                        val models = Json.parseToJsonElement(modelsJson).jsonObject
                        assertEquals(expectedModels, models.size, "模型数量应该匹配")
                    }
                }
            } finally {
                tempDbFile.delete()
            }
        }
    }

    private fun verifyAdvancedModelFields(apkgFile: File) {
        ZipFile(apkgFile).use { zipFile ->
            val dbEntry = zipFile.getEntry("collection.anki2")
            val tempDbFile = File.createTempFile("test_db", ".anki2")
            try {
                zipFile.getInputStream(dbEntry).use { input ->
                    tempDbFile.outputStream().use { output ->
                        input.copyTo(output)
                    }
                }

                val url = "jdbc:sqlite:${tempDbFile.absolutePath}"
                DriverManager.getConnection(url).use { conn ->
                    conn.createStatement().use { stmt ->
                        val rs = stmt.executeQuery("SELECT models FROM col WHERE id = 1")
                        rs.next()
                        val modelsJson = rs.getString(1)
                        val models = Json.parseToJsonElement(modelsJson).jsonObject

                        models.values.forEach { modelElement ->
                            val model = modelElement.jsonObject
                            val fields = model["flds"]?.jsonArray
                            assertNotNull(fields, "模型应该有字段定义")
                            assertTrue(fields!!.size >= 4, "高级模型应该至少有4个字段")
                        }
                    }
                }
            } finally {
                tempDbFile.delete()
            }
        }
    }

    private fun verifyNoteTags(apkgFile: File, expectedTag: String) {
        ZipFile(apkgFile).use { zipFile ->
            val dbEntry = zipFile.getEntry("collection.anki2")
            val tempDbFile = File.createTempFile("test_db", ".anki2")
            try {
                zipFile.getInputStream(dbEntry).use { input ->
                    tempDbFile.outputStream().use { output ->
                        input.copyTo(output)
                    }
                }

                val url = "jdbc:sqlite:${tempDbFile.absolutePath}"
                DriverManager.getConnection(url).use { conn ->
                    conn.createStatement().use { stmt ->
                        val rs = stmt.executeQuery("SELECT tags FROM notes")
                        while (rs.next()) {
                            val tags = rs.getString(1)
                            assertTrue(tags.contains(expectedTag), "笔记应该包含期望的标签")
                        }
                    }
                }
            } finally {
                tempDbFile.delete()
            }
        }
    }

    private fun verifyMediaFiles(apkgFile: File, expectedMedia: Map<String, ByteArray>) {
        ZipFile(apkgFile).use { zipFile ->
            // 验证 media 映射文件
            val mediaEntry = zipFile.getEntry("media")
            assertNotNull(mediaEntry, "media 文件应该存在")

            val mediaJson = zipFile.getInputStream(mediaEntry).use {
                it.readBytes().toString(Charsets.UTF_8)
            }
            val mediaMap = Json.parseToJsonElement(mediaJson).jsonObject

            // 验证每个媒体文件
            expectedMedia.forEach { (filename, expectedData) ->
                val found = mediaMap.values.any {
                    it.jsonPrimitive.content == filename
                }
                assertTrue(found, "媒体映射应该包含 $filename")

                // 找到对应的编号文件并验证内容
                val mediaNumber = mediaMap.entries.find {
                    it.value.jsonPrimitive.content == filename
                }?.key
                assertNotNull(mediaNumber, "应该找到 $filename 的编号")

                val mediaFileEntry = zipFile.getEntry(mediaNumber!!)
                assertNotNull(mediaFileEntry, "编号媒体文件应该存在")

                val actualData = zipFile.getInputStream(mediaFileEntry).use { it.readBytes() }
                assertArrayEquals(expectedData, actualData, "$filename 的内容应该匹配")
            }
        }
    }

    private fun verifyMultipleDecks(apkgFile: File, expectedDeckCount: Int) {
        ZipFile(apkgFile).use { zipFile ->
            val dbEntry = zipFile.getEntry("collection.anki2")
            val tempDbFile = File.createTempFile("test_db", ".anki2")
            try {
                zipFile.getInputStream(dbEntry).use { input ->
                    tempDbFile.outputStream().use { output ->
                        input.copyTo(output)
                    }
                }

                val url = "jdbc:sqlite:${tempDbFile.absolutePath}"
                DriverManager.getConnection(url).use { conn ->
                    conn.createStatement().use { stmt ->
                        val rs = stmt.executeQuery("SELECT decks FROM col WHERE id = 1")
                        rs.next()
                        val decksJson = rs.getString(1)
                        val decks = Json.parseToJsonElement(decksJson).jsonObject
                        assertEquals(expectedDeckCount, decks.size, "应该有 $expectedDeckCount 个牌组")
                    }
                }
            } finally {
                tempDbFile.delete()
            }
        }
    }

    private data class WordData(
        val english: String,
        val chinese: String,
        val audio: String,
        val example: String
    )
}
