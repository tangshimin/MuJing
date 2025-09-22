package fsrs

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.nio.file.Path
import java.sql.DriverManager
import java.util.zip.ZipFile
import kotlinx.serialization.json.*

/**
 * APKG 格式验证测试套件
 * 专门用于验证生成的 APKG 文件格式是否符合 Anki 标准
 */
class ApkgFormatValidatorTest {

    @TempDir
    lateinit var tempDir: Path

    private lateinit var outputDir: File
    private lateinit var testApkgFile: File

    @BeforeEach
    fun setUp() {
//        outputDir = File(System.getProperty("user.dir"), "test-output")

        // 使用临时目录
        outputDir = tempDir.toFile()
        outputDir.mkdirs()

        // 创建一个测试用的 APKG 文件（使用默认旧格式）
        val creator = ApkgCreator()
        val deckId = ApkgCreator.generateId()
        val deck = ApkgCreator.Deck(
            id = deckId,
            name = "格式验证测试牌组",
            description = "用于验证 APKG 格式正确性"
        )
        creator.addDeck(deck)

        val model = ApkgCreator.createBasicModel()
        creator.addModel(model)

        // 添加测试笔记
        val note = ApkgCreator.Note(
            id = ApkgCreator.generateId(),
            modelId = model.id,
            fields = listOf("test", "测试")
        )
        creator.addNote(note, deckId)

        // 生成文件
        testApkgFile = File(outputDir, "format_validation_test.apkg")
        creator.createApkg(testApkgFile.absolutePath)
    }

    /**
     * 测试 APKG 文件基本结构（旧格式）
     */
    @Test
    fun testApkgBasicStructureLegacy() {
        assertTrue(testApkgFile.exists(), "APKG 文件应该存在")
        assertTrue(testApkgFile.length() > 0, "APKG 文件应该不为空")

        ZipFile(testApkgFile).use { zipFile ->
            val entries = zipFile.entries().toList().map { it.name }

            // 验证必需的文件
            assertTrue(entries.contains("collection.anki2"), "应该包含 collection.anki2 文件")
            assertTrue(entries.contains("media"), "应该包含 media 文件")

            // 验证文件大小
            val dbEntry = zipFile.getEntry("collection.anki2")
            assertTrue(dbEntry.size > 0, "数据库文件应该不为空")

            val mediaEntry = zipFile.getEntry("media")
            assertTrue(mediaEntry.size >= 0, "media 文件应该存在")
        }
    }

    /**
     * 测试新格式 APKG 文件结构
     */
    @Test
    fun testNewFormatApkgStructure() {
        val newFormatFile = File(outputDir, "new_format_validation_test.apkg")
        
        val creator = ApkgCreator()
        val deckId = ApkgCreator.generateId()
        val deck = ApkgCreator.Deck(id = deckId, name = "新格式测试牌组")
        creator.addDeck(deck)

        val model = ApkgCreator.createBasicModel()
        creator.addModel(model)

        val note = ApkgCreator.Note(
            id = ApkgCreator.generateId(),
            modelId = model.id,
            fields = listOf("new", "格式")
        )
        creator.addNote(note, deckId)

        // 生成新格式文件
        creator.setFormat(ApkgFormat.LATEST)
        creator.createApkg(newFormatFile.absolutePath)

        assertTrue(newFormatFile.exists(), "新格式 APKG 文件应该存在")

        ZipFile(newFormatFile).use { zipFile ->
            val entries = zipFile.entries().toList().map { it.name }

            // 验证新格式文件
            assertTrue(entries.contains("collection.anki21b"), "应该包含 collection.anki21b 文件")
            assertTrue(entries.contains("media"), "应该包含 media 文件")

            val dbEntry = zipFile.getEntry("collection.anki21b")
            assertTrue(dbEntry.size > 0, "新格式数据库文件应该不为空")
        }
    }

    /**
     * 测试双格式 APKG 文件结构
     */
    @Test
    fun testDualFormatApkgStructure() {
        val dualFormatFile = File(outputDir, "dual_format_validation_test.apkg")
        
        val creator = ApkgCreator()
        val deckId = ApkgCreator.generateId()
        val deck = ApkgCreator.Deck(id = deckId, name = "双格式测试牌组")
        creator.addDeck(deck)

        val model = ApkgCreator.createBasicModel()
        creator.addModel(model)

        val note = ApkgCreator.Note(
            id = ApkgCreator.generateId(),
            modelId = model.id,
            fields = listOf("dual", "格式")
        )
        creator.addNote(note, deckId)

        // 生成双格式文件
        creator.createApkg(dualFormatFile.absolutePath, dualFormat = true)

        assertTrue(dualFormatFile.exists(), "双格式 APKG 文件应该存在")

        ZipFile(dualFormatFile).use { zipFile ->
            val entries = zipFile.entries().toList().map { it.name }

            // 验证同时包含新旧格式
            assertTrue(entries.contains("collection.anki2"), "应该包含旧格式 collection.anki2")
            assertTrue(entries.contains("collection.anki21b"), "应该包含新格式 collection.anki21b")
            assertTrue(entries.contains("media"), "应该包含 media 文件")

            // 验证两个数据库文件都不为空
            val legacyDbEntry = zipFile.getEntry("collection.anki2")
            val newDbEntry = zipFile.getEntry("collection.anki21b")
            assertTrue(legacyDbEntry.size > 0, "旧格式数据库文件应该不为空")
            assertTrue(newDbEntry.size > 0, "新格式数据库文件应该不为空")
        }
    }

    /**
     * 测试数据库表结构
     */
    @Test
    fun testDatabaseSchema() {
        extractAndVerifyDatabase(testApkgFile) { conn ->
            // 验证必需的表存在
            val tables = listOf("col", "notes", "cards", "revlog", "graves")
            tables.forEach { tableName ->
                val rs = conn.createStatement().executeQuery("SELECT name FROM sqlite_master WHERE type='table' AND name='$tableName'")
                assertTrue(rs.next(), "表 $tableName 应该存在")
            }

            // 验证 col 表结构
            verifyTableSchema(conn, "col", listOf(
                "id", "crt", "mod", "scm", "ver", "dty", "usn", "ls", "conf", "models", "decks", "dconf", "tags"
            ))

            // 验证 notes 表结构
            verifyTableSchema(conn, "notes", listOf(
                "id", "guid", "mid", "mod", "usn", "tags", "flds", "sfld", "csum", "flags", "data"
            ))

            // 验证 cards 表结构
            verifyTableSchema(conn, "cards", listOf(
                "id", "nid", "did", "ord", "mod", "usn", "type", "queue", "due", "ivl", "factor",
                "reps", "lapses", "left", "odue", "odid", "flags", "data"
            ))
        }
    }

    /**
     * 测试集合配置 JSON 结构
     */
    @Test
    fun testCollectionConfigStructure() {
        extractAndVerifyDatabase(testApkgFile) { conn ->
            val rs = conn.createStatement().executeQuery("SELECT conf FROM col WHERE id = 1")
            assertTrue(rs.next(), "应该能找到集合配置")

            val confJson = rs.getString(1)
            val config = Json.parseToJsonElement(confJson).jsonObject

            // 验证必需的配置字段
            val requiredFields = listOf(
                "nextPos", "estTimes", "activeDecks", "sortType", "timeLim",
                "sortBackwards", "addToCur", "curDeck", "newBury", "newSpread",
                "dueCounts", "curModel", "collapseTime"
            )

            requiredFields.forEach { field ->
                assertTrue(config.containsKey(field), "配置应该包含 $field 字段")
            }
        }
    }

    /**
     * 测试牌组 JSON 结构
     */
    @Test
    fun testDecksJsonStructure() {
        extractAndVerifyDatabase(testApkgFile) { conn ->
            val rs = conn.createStatement().executeQuery("SELECT decks FROM col WHERE id = 1")
            assertTrue(rs.next(), "应该能找到牌组配置")

            val decksJson = rs.getString(1)
            val decks = Json.parseToJsonElement(decksJson).jsonObject

            // 验证至少有一个牌组
            assertTrue(decks.size > 0, "应该至少有一个牌组")

            // 验证牌组结构
            decks.values.forEach { deckElement ->
                val deck = deckElement.jsonObject
                val requiredFields = listOf(
                    "id", "mod", "name", "usn", "lrnToday", "revToday",
                    "newToday", "timeToday", "collapsed", "browserCollapsed",
                    "desc", "dyn", "conf", "extendNew", "extendRev",
                    "reviewLimit", "newLimit", "reviewLimitToday", "newLimitToday"
                )

                requiredFields.forEach { field ->
                    assertTrue(deck.containsKey(field), "牌组应该包含 $field 字段")
                }

                // 验证特定字段类型
                assertEquals(JsonPrimitive(0).jsonPrimitive.content, deck["usn"]?.jsonPrimitive?.content, "usn 应该为 0")
                assertEquals(JsonPrimitive(1).jsonPrimitive.content, deck["conf"]?.jsonPrimitive?.content, "conf 应该为 1")
            }
        }
    }

    /**
     * 测试模型 JSON 结构
     */
    @Test
    fun testModelsJsonStructure() {
        extractAndVerifyDatabase(testApkgFile) { conn ->
            val rs = conn.createStatement().executeQuery("SELECT models FROM col WHERE id = 1")
            assertTrue(rs.next(), "应该能找到模型配置")

            val modelsJson = rs.getString(1)
            val models = Json.parseToJsonElement(modelsJson).jsonObject

            // 验证至少有一个模型
            assertTrue(models.size > 0, "应该至少有一个模型")

            // 验证模型结构
            models.values.forEach { modelElement ->
                val model = modelElement.jsonObject
                val requiredFields = listOf(
                    "id", "name", "type", "mod", "usn", "sortf", "did",
                    "tmpls", "flds", "css"
                )

                requiredFields.forEach { field ->
                    assertTrue(model.containsKey(field), "模型应该包含 $field 字段")
                }

                // 验证模板和字段结构
                val templates = model["tmpls"]?.jsonArray
                assertNotNull(templates, "模型应该有模板")
                assertTrue(templates!!.size > 0, "模型应该至少有一个模板")

                val fields = model["flds"]?.jsonArray
                assertNotNull(fields, "模型应该有字段")
                assertTrue(fields!!.size >= 2, "模型应该至少有两个字段")
            }
        }
    }

    /**
     * 测试笔记数据完整性
     */
    @Test
    fun testNotesDataIntegrity() {
        extractAndVerifyDatabase(testApkgFile) { conn ->
            // 验证笔记数量
            val countRs = conn.createStatement().executeQuery("SELECT COUNT(*) FROM notes")
            countRs.next()
            assertTrue(countRs.getInt(1) > 0, "应该至少有一个笔记")

            // 验证笔记字段
            val notesRs = conn.createStatement().executeQuery("SELECT * FROM notes")
            while (notesRs.next()) {
                val id = notesRs.getLong("id")
                val guid = notesRs.getString("guid")
                val mid = notesRs.getLong("mid")
                val fields = notesRs.getString("flds")

                assertTrue(id > 0, "笔记 ID 应该为正数")
                assertNotNull(guid, "笔记应该有 GUID")
                assertTrue(guid.isNotBlank(), "GUID 不应该为空")
                assertTrue(mid > 0, "模型 ID 应该为正数")
                assertNotNull(fields, "笔记字段不应该为空")
                assertTrue(fields.contains("\u001f"), "笔记字段应该包含分隔符")
            }
        }
    }

    /**
     * 测试卡片数据完整性
     */
    @Test
    fun testCardsDataIntegrity() {
        extractAndVerifyDatabase(testApkgFile) { conn ->
            // 验证卡片数量
            val countRs = conn.createStatement().executeQuery("SELECT COUNT(*) FROM cards")
            countRs.next()
            assertTrue(countRs.getInt(1) > 0, "应该至少有一个卡片")

            // 验证卡片与笔记的关联
            val cardsRs = conn.createStatement().executeQuery("""
                SELECT c.*, n.id as note_id 
                FROM cards c 
                JOIN notes n ON c.nid = n.id
            """)
            
            while (cardsRs.next()) {
                val cardId = cardsRs.getLong("id")
                val noteId = cardsRs.getLong("note_id")
                val deckId = cardsRs.getLong("did")
                val ordinal = cardsRs.getInt("ord")

                assertTrue(cardId > 0, "卡片 ID 应该为正数")
                assertTrue(noteId > 0, "关联的笔记 ID 应该为正数")
                assertTrue(deckId > 0, "牌组 ID 应该为正数")
                assertTrue(ordinal >= 0, "卡片序号应该为非负数")
            }
        }
    }

    /**
     * 测试媒体文件处理
     */
    @Test
    fun testMediaFileHandling() {
        ZipFile(testApkgFile).use { zipFile ->
            // 验证 media 文件存在
            val mediaEntry = zipFile.getEntry("media")
            assertNotNull(mediaEntry, "media 文件应该存在")

            // 读取 media JSON
            val mediaJson = zipFile.getInputStream(mediaEntry).use {
                it.readBytes().toString(Charsets.UTF_8)
            }
            
            val mediaMap = Json.parseToJsonElement(mediaJson).jsonObject
            
            // media 映射可以为空（如果没有媒体文件）
            assertNotNull(mediaMap, "media 映射应该存在")
        }
    }

    /**
     * 测试数据库版本兼容性
     */
    @Test
    fun testDatabaseVersionCompatibility() {
        extractAndVerifyDatabase(testApkgFile) { conn ->
            val rs = conn.createStatement().executeQuery("SELECT ver FROM col WHERE id = 1")
            assertTrue(rs.next(), "应该能找到版本号")
            
            val version = rs.getInt(1)
            // Anki 2.1.x 使用版本 11
            assertEquals(11, version, "数据库版本应该为 11 (Anki 2.1.x)")
        }
    }

    // ===== 辅助方法 =====

    private fun extractAndVerifyDatabase(apkgFile: File, verification: (java.sql.Connection) -> Unit) {
        ZipFile(apkgFile).use { zipFile ->
            // 尝试检测数据库文件格式（优先新格式，回退旧格式）
            val dbEntry = zipFile.getEntry("collection.anki21b") ?: 
                         zipFile.getEntry("collection.anki21") ?: 
                         zipFile.getEntry("collection.anki2")
            assertNotNull(dbEntry, "应该包含至少一种数据库格式")

            val tempDbFile = File.createTempFile("test_db", ".anki2")
            try {
                zipFile.getInputStream(dbEntry).use { input ->
                    tempDbFile.outputStream().use { output ->
                        input.copyTo(output)
                    }
                }

                val url = "jdbc:sqlite:${tempDbFile.absolutePath}"
                DriverManager.getConnection(url).use { conn ->
                    verification(conn)
                }
            } finally {
                tempDbFile.delete()
            }
        }
    }

    private fun verifyTableSchema(conn: java.sql.Connection, tableName: String, expectedColumns: List<String>) {
        val rs = conn.createStatement().executeQuery("PRAGMA table_info($tableName)")
        val actualColumns = mutableListOf<String>()
        
        while (rs.next()) {
            actualColumns.add(rs.getString("name"))
        }

        expectedColumns.forEach { column ->
            assertTrue(actualColumns.contains(column), "表 $tableName 应该包含列 $column")
        }
    }
}