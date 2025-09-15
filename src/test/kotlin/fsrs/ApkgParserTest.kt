package fsrs

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.nio.file.Path

/**
 * APKG 解析器测试套件
 * 测试 APKG 解析器与 Anki 格式的兼容性
 */
class ApkgParserTest {

    @TempDir
    lateinit var tempDir: Path

    private lateinit var parser: ApkgParser

    @BeforeEach
    fun setUp() {
        parser = ApkgParser()
    }

    /**
     * 测试解析基本的单词 APKG 文件
     */
    @Test
    fun testParseBasicWordApkg() {
        val testFile = File("src/test/resources/apkg/basic_word.apkg")
        assertTrue(testFile.exists(), "测试文件应该存在")

        val result = parser.parseApkg(testFile.absolutePath)

        // 验证基本结构
        assertTrue(result.notes.isNotEmpty(), "应该解析出笔记")
        assertTrue(result.cards.isNotEmpty(), "应该解析出卡片")
        assertTrue(result.decks.isNotEmpty(), "应该解析出牌组")
        assertTrue(result.models.isNotEmpty(), "应该解析出模型")

        // 验证数据库版本
        assertEquals(11, result.databaseVersion, "数据库版本应该为 11")
        assertTrue(result.creationTime > 0, "创建时间应该为正数")

        // 验证笔记数据
        val firstNote = result.notes.first()
        assertTrue(firstNote.id > 0, "笔记 ID 应该为正数")
        assertTrue(firstNote.guid.isNotBlank(), "笔记 GUID 不应该为空")
        assertTrue(firstNote.modelId > 0, "模型 ID 应该为正数")
        assertTrue(firstNote.fields.isNotEmpty(), "笔记字段不应该为空")

        // 验证卡片数据
        val firstCard = result.cards.first()
        assertTrue(firstCard.id > 0, "卡片 ID 应该为正数")
        assertTrue(firstCard.noteId > 0, "关联的笔记 ID 应该为正数")
        assertTrue(firstCard.deckId > 0, "牌组 ID 应该为正数")
        assertTrue(firstCard.templateOrdinal >= 0, "模板序号应该为非负数")

        // 验证牌组数据
        val firstDeck = result.decks.first()
        assertTrue(firstDeck.id > 0, "牌组 ID 应该为正数")
        assertTrue(firstDeck.name.isNotBlank(), "牌组名称不应该为空")

        // 验证模型数据
        val firstModel = result.models.first()
        assertTrue(firstModel.id > 0, "模型 ID 应该为正数")
        assertTrue(firstModel.name.isNotBlank(), "模型名称不应该为空")
        assertTrue(firstModel.templates.isNotEmpty(), "模型应该有模板")
        assertTrue(firstModel.fields.isNotEmpty(), "模型应该有字段")
        assertTrue(firstModel.css.isNotBlank(), "模型 CSS 不应该为空")
    }

    /**
     * 测试验证 APKG 文件有效性
     */
    @Test
    fun testIsValidApkg() {
        val validFile = File("src/test/resources/apkg/basic_word.apkg")
        assertTrue(parser.isValidApkg(validFile.absolutePath), "有效的 APKG 文件应该返回 true")

        val invalidFile = File("src/test/resources/apkg/apkg list.md")
        assertFalse(parser.isValidApkg(invalidFile.absolutePath), "无效的文件应该返回 false")
    }

    /**
     * 测试获取 APKG 文件基本信息
     */
    @Test
    fun testGetApkgInfo() {
        val testFile = File("src/test/resources/apkg/basic_word.apkg")
        val info = parser.getApkgInfo(testFile.absolutePath)

        assertTrue(info.isNotEmpty(), "应该返回基本信息")
        assertTrue(info.containsKey("noteCount"), "应该包含笔记数量")
        assertTrue(info.containsKey("cardCount"), "应该包含卡片数量")
        assertTrue(info.containsKey("deckCount"), "应该包含牌组数量")
        assertTrue(info.containsKey("databaseVersion"), "应该包含数据库版本")
        assertTrue(info.containsKey("creationTime"), "应该包含创建时间")

        val noteCount = info["noteCount"] as Int
        val cardCount = info["cardCount"] as Int
        val deckCount = info["deckCount"] as Int
        val dbVersion = info["databaseVersion"] as Int

        assertTrue(noteCount > 0, "笔记数量应该为正数")
        assertTrue(cardCount > 0, "卡片数量应该为正数")
        assertTrue(deckCount > 0, "牌组数量应该为正数")
        assertEquals(11, dbVersion, "数据库版本应该为 11")
    }

    /**
     * 测试解析笔记字段分隔符
     */
    @Test
    fun testNoteFieldParsing() {
        val testFile = File("src/test/resources/apkg/basic_word.apkg")
        val result = parser.parseApkg(testFile.absolutePath)

        result.notes.forEach { note ->
            // 验证字段数量合理（至少1个字段）
            assertTrue(note.fields.size >= 1, "笔记应该至少有一个字段")
            
            // 验证字段内容
            note.fields.forEach { field ->
                assertNotNull(field, "字段内容不应该为 null")
            }
        }
    }

    /**
     * 测试解析模型模板和字段
     */
    @Test
    fun testModelTemplateAndFieldParsing() {
        val testFile = File("src/test/resources/apkg/basic_word.apkg")
        val result = parser.parseApkg(testFile.absolutePath)

        result.models.forEach { model ->
            // 验证模板
            model.templates.forEach { template ->
                assertTrue(template.name.isNotBlank(), "模板名称不应该为空")
                assertTrue(template.ordinal >= 0, "模板序号应该为非负数")
                assertTrue(template.questionFormat.isNotBlank(), "问题格式不应该为空")
                assertTrue(template.answerFormat.isNotBlank(), "答案格式不应该为空")
            }

            // 验证字段
            model.fields.forEach { field ->
                assertTrue(field.name.isNotBlank(), "字段名称不应该为空")
                assertTrue(field.ordinal >= 0, "字段序号应该为非负数")
                assertTrue(field.font.isNotBlank(), "字段字体不应该为空")
                assertTrue(field.size > 0, "字段大小应该为正数")
            }
        }
    }

    /**
     * 测试解析多个 APKG 文件
     */
    @Test
    fun testParseMultipleApkgFiles() {
        val testFiles = listOf(
            "src/test/resources/apkg/basic_word.apkg",
            "src/test/resources/apkg/primary_school_words.apkg",
            "src/test/resources/apkg/English_Roots.apkg"
        )

        testFiles.forEach { filePath ->
            val file = File(filePath)
            if (file.exists()) {
                val result = parser.parseApkg(file.absolutePath)
                
                // 基本验证
                assertTrue(result.notes.isNotEmpty(), "$filePath 应该包含笔记")
                assertTrue(result.cards.isNotEmpty(), "$filePath 应该包含卡片")
                assertEquals(11, result.databaseVersion, "$filePath 数据库版本应该为 11")
            }
        }
    }

    /**
     * 测试错误处理 - 无效文件
     */
    @Test
    fun testErrorHandlingInvalidFile() {
        val invalidFile = File("nonexistent.apkg")
        
        assertThrows(Exception::class.java) {
            parser.parseApkg(invalidFile.absolutePath)
        }

        val textFile = File("src/test/resources/apkg/apkg list.md")
        assertThrows(Exception::class.java) {
            parser.parseApkg(textFile.absolutePath)
        }
    }

    /**
     * 测试解析媒体文件信息
     */
    @Test
    fun testMediaFileParsing() {
        val testFile = File("src/test/resources/apkg/basic_word.apkg")
        val result = parser.parseApkg(testFile.absolutePath)

        // 即使没有媒体文件，也应该返回空列表而不是异常
        assertNotNull(result.mediaFiles, "媒体文件列表不应该为 null")
        
        // 可以检查媒体文件的数量（某些测试文件可能包含媒体文件）
        // 对于 basic_word.apkg，可能没有媒体文件，所以只验证列表存在
    }

    /**
     * 测试笔记和卡片的关联关系
     */
    @Test
    fun testNoteCardRelationships() {
        val testFile = File("src/test/resources/apkg/basic_word.apkg")
        val result = parser.parseApkg(testFile.absolutePath)

        // 验证每个卡片都关联到一个存在的笔记
        result.cards.forEach { card ->
            val correspondingNote = result.notes.find { it.id == card.noteId }
            assertNotNull(correspondingNote, "卡片应该关联到一个存在的笔记")
        }

        // 验证每个笔记都关联到一个存在的模型
        result.notes.forEach { note ->
            val correspondingModel = result.models.find { it.id == note.modelId }
            assertNotNull(correspondingModel, "笔记应该关联到一个存在的模型")
        }

        // 验证每个卡片都关联到一个存在的牌组
        result.cards.forEach { card ->
            val correspondingDeck = result.decks.find { it.id == card.deckId }
            assertNotNull(correspondingDeck, "卡片应该关联到一个存在的牌组")
        }
    }

    /**
     * 测试性能 - 快速信息获取
     */
    @Test
    fun testPerformanceQuickInfo() {
        val testFile = File("src/test/resources/apkg/basic_word.apkg")
        
        // 测试快速信息获取（应该比完整解析快很多）
        val startTime = System.currentTimeMillis()
        val info = parser.getApkgInfo(testFile.absolutePath)
        val endTime = System.currentTimeMillis()
        
        assertTrue(info.isNotEmpty(), "快速信息应该包含数据")
        
        // 验证快速信息获取在合理时间内完成（小于1秒）
        val duration = endTime - startTime
        assertTrue(duration < 1000, "快速信息获取应该在1秒内完成，实际耗时: ${duration}ms")
    }

    /**
     * 测试 APKG 文件版本兼容性
     * 针对不同版本 APKG 文件的兼容性测试
     */
    @Test
    fun testVersionCompatibility() {
        val testFile = File("src/test/resources/apkg/primary_school_words.apkg")
        if (!testFile.exists()) {
            return  // 如果测试文件不存在，跳过测试
        }

        val result = parser.parseApkg(testFile.absolutePath)

        // 验证基本结构
        assertTrue(result.notes.isNotEmpty(), "应该解析出笔记")
        assertTrue(result.cards.isNotEmpty(), "应该解析出卡片")
        assertTrue(result.decks.isNotEmpty(), "应该解析出牌组")
        assertTrue(result.models.isNotEmpty(), "应该解析出模型")

        // 验证数据库版本（较老版本可能不是11）
        assertTrue(result.databaseVersion > 0, "数据库版本应该为正数")
        assertTrue(result.creationTime > 0, "创建时间应该为正数")

        // 验证笔记数据
        val firstNote = result.notes.first()
        assertTrue(firstNote.id > 0, "笔记 ID 应该为正数")
        assertTrue(firstNote.guid.isNotBlank(), "笔记 GUID 不应该为空")
        assertTrue(firstNote.modelId > 0, "模型 ID 应该为正数")
        assertTrue(firstNote.fields.isNotEmpty(), "笔记字段不应该为空")

        // 验证卡片数据
        val firstCard = result.cards.first()
        assertTrue(firstCard.id > 0, "卡片 ID 应该为正数")
        assertTrue(firstCard.noteId > 0, "关联的笔记 ID 应该为正数")
        assertTrue(firstCard.deckId > 0, "牌组 ID 应该为正数")
        assertTrue(firstCard.templateOrdinal >= 0, "模板序号应该为非负数")

        // 验证牌组数据
        val firstDeck = result.decks.first()
        assertTrue(firstDeck.id > 0, "牌组 ID 应该为正数")
        assertTrue(firstDeck.name.isNotBlank(), "牌组名称不应该为空")

        // 验证模型数据
        val firstModel = result.models.first()
        assertTrue(firstModel.id > 0, "模型 ID 应该为正数")
        assertTrue(firstModel.name.isNotBlank(), "模型名称不应该为空")
        assertTrue(firstModel.templates.isNotEmpty(), "模型应该有模板")
        assertTrue(firstModel.fields.isNotEmpty(), "模型应该有字段")

        // 验证关联关系
        result.cards.forEach { card ->
            val correspondingNote = result.notes.find { it.id == card.noteId }
            assertNotNull(correspondingNote, "卡片应该关联到一个存在的笔记")
        }

        result.notes.forEach { note ->
            val correspondingModel = result.models.find { it.id == note.modelId }
            assertNotNull(correspondingModel, "笔记应该关联到一个存在的模型")
        }

        result.cards.forEach { card ->
            val correspondingDeck = result.decks.find { it.id == card.deckId }
            assertNotNull(correspondingDeck, "卡片应该关联到一个存在的牌组")
        }

        // 输出数据库版本信息用于调试
        println("APKG 数据库版本: ${result.databaseVersion}")
        println("解析笔记数量: ${result.notes.size}")
        println("解析卡片数量: ${result.cards.size}")
    }
}