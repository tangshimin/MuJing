package fsrs

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*
import java.io.File

/**
 * APKG 解析器示例测试
 * 演示如何使用 APKG 解析器
 */
class ApkgParserExampleTest {

    @Test
    fun testApkgParserExample() {
        val parser = ApkgParser()
        val testFile = File("src/test/resources/apkg/basic_word.apkg")
        
        if (testFile.exists()) {
            println("=== 测试 APKG 解析器 ===")
            
            // 测试快速信息获取
            val quickInfo = parser.getApkgInfo(testFile.absolutePath)
            println("快速信息:")
            println("  - 笔记数量: ${quickInfo["noteCount"]}")
            println("  - 卡片数量: ${quickInfo["cardCount"]}")
            println("  - 牌组数量: ${quickInfo["deckCount"]}")
            println("  - 数据库版本: ${quickInfo["databaseVersion"]}")
            
            // 验证快速信息包含必需字段
            assertTrue(quickInfo.containsKey("noteCount"))
            assertTrue(quickInfo.containsKey("cardCount"))
            assertTrue(quickInfo.containsKey("deckCount"))
            assertTrue(quickInfo.containsKey("databaseVersion"))
            assertTrue(quickInfo.containsKey("creationTime"))
            
            // 测试完整解析
            val parsedData = parser.parseApkg(testFile.absolutePath)
            
            println("\n详细解析结果:")
            println("  - 解析出 ${parsedData.notes.size} 个笔记")
            println("  - 解析出 ${parsedData.cards.size} 个卡片")
            println("  - 解析出 ${parsedData.decks.size} 个牌组")
            println("  - 解析出 ${parsedData.models.size} 个模型")
            
            // 验证解析结果
            assertTrue(parsedData.notes.isNotEmpty())
            assertTrue(parsedData.cards.isNotEmpty())
            assertTrue(parsedData.decks.isNotEmpty())
            assertTrue(parsedData.models.isNotEmpty())
            assertEquals(11, parsedData.databaseVersion)
            
            // 测试文本内容提取
            val allText = ApkgParserExample.extractAllTextContent(testFile.absolutePath)
            println("\n提取的文本内容数量: ${allText.size}")
            
            assertTrue(allText.isNotEmpty(), "应该提取到文本内容")
            
            println("\n测试完成!")
        } else {
            println("测试文件不存在，跳过测试")
        }
    }

    @Test
    fun testValidateApkgFile() {
        val validFile = File("src/test/resources/apkg/basic_word.apkg")
        if (validFile.exists()) {
            val isValid = ApkgParserExample.validateApkgFile(validFile.absolutePath)
            assertTrue(isValid, "有效的 APKG 文件应该返回 true")
        }
        
        val invalidFile = File("nonexistent.apkg")
        val isInvalid = ApkgParserExample.validateApkgFile(invalidFile.absolutePath)
        assertFalse(isInvalid, "无效的文件应该返回 false")
    }
}