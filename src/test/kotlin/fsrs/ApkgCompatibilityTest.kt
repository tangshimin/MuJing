package fsrs

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.nio.file.Path
import java.sql.DriverManager
import java.util.zip.ZipFile
import kotlinx.serialization.json.*

/**
 * APKG 兼容性测试
 * 测试生成的 APKG 文件与不同版本的 Anki 兼容性
 */
class ApkgCompatibilityTest {

    @TempDir
    lateinit var tempDir: Path

    /**
     * 测试生成的 APKG 文件包含所有必需的字段
     */
    @Test
    fun testGeneratedApkgContainsRequiredFields() {
        val creator = ApkgCreator()

        // 创建牌组和模型
        val deckId = ApkgCreator.generateId()
        val deck = ApkgCreator.Deck(
            id = deckId,
            name = "测试兼容性牌组"
        )
        creator.addDeck(deck)

        val model = ApkgCreator.createBasicModel()
        creator.addModel(model)

        // 添加测试笔记
        val note = ApkgCreator.Note(
            id = ApkgCreator.generateId(),
            mid = model.id,
            fields = listOf("test", "测试")
        )
        creator.addNote(note, deckId)

        // 生成文件
        val outputFile = File(tempDir.toFile(), "compatibility_test.apkg")
        creator.createApkg(outputFile.absolutePath)

        // 验证文件结构
        ZipFile(outputFile).use { zipFile ->
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
                    // 验证必需的 JSON 字段存在
                    verifyRequiredJsonFields(conn)
                }
            } finally {
                tempDbFile.delete()
            }
        }
    }

    private fun verifyRequiredJsonFields(conn: java.sql.Connection) {
        // 验证 decks JSON 包含必需字段
        val decksRs = conn.createStatement().executeQuery("SELECT decks FROM col WHERE id = 1")
        decksRs.next()
        val decksJson = decksRs.getString(1)
        val decks = Json.parseToJsonElement(decksJson).jsonObject

        val firstDeck = decks.values.first().jsonObject
        val requiredDeckFields = listOf(
            "id", "mod", "name", "usn", "lrnToday", "revToday",
            "newToday", "timeToday", "collapsed", "browserCollapsed",
            "desc", "dyn", "conf"
        )

        requiredDeckFields.forEach { field ->
            assertTrue(firstDeck.containsKey(field), "牌组应该包含 $field 字段")
        }

        // 验证 models JSON 包含必需字段
        val modelsRs = conn.createStatement().executeQuery("SELECT models FROM col WHERE id = 1")
        modelsRs.next()
        val modelsJson = modelsRs.getString(1)
        val models = Json.parseToJsonElement(modelsJson).jsonObject

        val firstModel = models.values.first().jsonObject
        val requiredModelFields = listOf(
            "id", "name", "type", "mod", "usn", "sortf",
            "tmpls", "flds", "css"
        )

        requiredModelFields.forEach { field ->
            assertTrue(firstModel.containsKey(field), "模型应该包含 $field 字段")
        }

        // 验证 dconf JSON 包含必需字段
        val dconfRs = conn.createStatement().executeQuery("SELECT dconf FROM col WHERE id = 1")
        dconfRs.next()
        val dconfJson = dconfRs.getString(1)
        val dconf = Json.parseToJsonElement(dconfJson).jsonObject

        val firstDconf = dconf.values.first().jsonObject
        val requiredDconfFields = listOf(
            "id", "mod", "name", "usn", "maxTaken", "autoplay",
            "timer", "replayq", "new", "rev", "lapse"
        )

        requiredDconfFields.forEach { field ->
            assertTrue(firstDconf.containsKey(field), "牌组配置应该包含 $field 字段")
        }
    }

    /**
     * 测试数据库版本兼容性
     */
    @Test
    fun testDatabaseVersionCompatibility() {
        val creator = ApkgCreator()
        val deckId = ApkgCreator.generateId()
        val deck = ApkgCreator.Deck(id = deckId, name = "版本测试")
        creator.addDeck(deck)

        val model = ApkgCreator.createBasicModel()
        creator.addModel(model)

        val outputFile = File(tempDir.toFile(), "version_test.apkg")
        creator.createApkg(outputFile.absolutePath)

        ZipFile(outputFile).use { zipFile ->
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
                    val rs = conn.createStatement().executeQuery("SELECT ver FROM col WHERE id = 1")
                    rs.next()
                    val version = rs.getInt(1)
                    
                    // Anki 2.1.x 使用版本 11
                    assertEquals(11, version, "数据库版本应该为 11 (Anki 2.1.x)")
                }
            } finally {
                tempDbFile.delete()
            }
        }
    }
}