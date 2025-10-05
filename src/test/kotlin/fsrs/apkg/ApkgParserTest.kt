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

package fsrs.apkg

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
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

    @Nested
    @DisplayName("基本功能测试")
    inner class BasicFunctionalityTests {

        @Test
        @DisplayName("测试解析基本的单词 APKG 文件")
        fun testParseBasicWordApkg() {
            val testFile = File("src/test/resources/apkg/basic_word.apkg")
            assertTrue(testFile.exists(), "测试文件应该存在")

            val result = parser.parseApkg(testFile.absolutePath)

            // 验证基本结构
            assertTrue(result.notes.isNotEmpty(), "应该解析出笔记")
            assertTrue(result.cards.isNotEmpty(), "应该解析出卡片")
            assertTrue(result.decks.isNotEmpty(), "应该解析出牌组")
            assertTrue(result.models.isNotEmpty(), "应该解析出模型")

            // 验证数据库版本和格式信息
            assertTrue(result.databaseVersion > 0, "数据库版本应该为正数")
            assertTrue(result.creationTime > 0, "创建时间应该为正数")
            assertNotNull(result.format, "格式不应该为 null")
            assertTrue(result.schemaVersion > 0, "架构版本应该为正数")

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

        @Test
        @DisplayName("测试验证 APKG 文件有效性")
        fun testIsValidApkg() {
            val validFile = File("src/test/resources/apkg/basic_word.apkg")
            assertTrue(parser.isValidApkg(validFile.absolutePath), "有效的 APKG 文件应该返回 true")

            val invalidFile = File("src/test/resources/apkg/apkg list.md")
            assertFalse(parser.isValidApkg(invalidFile.absolutePath), "无效的文件应该返回 false")
        }

        @Test
        @DisplayName("测试获取 APKG 文件基本信息")
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
    }

    @Nested
    @DisplayName("数据结构验证测试")
    inner class DataStructureTests {

        @Test
        @DisplayName("测试解析笔记字段分隔符")
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

        @Test
        @DisplayName("测试解析模型模板和字段")
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

        @Test
        @DisplayName("测试笔记和卡片的关联关系")
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

        @Test
        @DisplayName("测试解析媒体文件信息")
        fun testMediaFileParsing() {
            val testFile = File("src/test/resources/apkg/basic_word.apkg")
            val result = parser.parseApkg(testFile.absolutePath)

            // 即使没有媒体文件，也应该返回空列表而不是异常
            assertNotNull(result.mediaFiles, "媒体文件列表不应该为 null")
            
            // 可以检查媒体文件的数量（某些测试文件可能包含媒体文件）
            // 对于 basic_word.apkg，可能没有媒体文件，所以只验证列表存在
        }
    }

    @Nested
    @DisplayName("兼容性测试")
    inner class CompatibilityTests {

        @Test
        @DisplayName("测试解析多个 APKG 文件")
        fun testParseMultipleApkgFiles() {
            val testFiles = listOf(
                "src/test/resources/apkg/basic_word.apkg",
                "src/test/resources/apkg/primary_school_words.apkg",
                "src/test/resources/apkg/basic_word_new_version.apkg",
                "src/test/resources/apkg/Alphabet Word.apkg"
            )

            testFiles.forEach { filePath ->
                val file = File(filePath)
                if (file.exists()) {
                    try {
                        val result = parser.parseApkg(file.absolutePath)
                        
                        // 基本验证
                        assertTrue(result.notes.isNotEmpty(), "$filePath 应该包含笔记")
                        assertTrue(result.cards.isNotEmpty(), "$filePath 应该包含卡片")
                        assertTrue(result.databaseVersion > 0, "$filePath 数据库版本应该为正数")
                        
                        // 输出格式信息用于调试
                        println("文件: ${file.name}")
                        println("  - 笔记数量: ${result.notes.size}")
                        println("  - 卡片数量: ${result.cards.size}")
                        println("  - 牌组数量: ${result.decks.size}")
                        println("  - 模型数量: ${result.models.size}")
                        println("  - 数据库版本: ${result.databaseVersion}")
                    } catch (e: Exception) {
                        if (file.name.contains("new_version") || file.name.contains("Alphabet")) {
                            // 新格式文件可能无法解析，提供警告信息
                            println("文件: ${file.name} (新格式 - 需要特殊处理)")
                            println("  - 状态: 新格式 collection.anki21b + collection.anki2")
                            println("  - 错误: ${e.message}")
                        } else {
                            // 其他文件应该正常解析
                            throw e
                        }
                    }
                }
            }
        }

        @Test
        @DisplayName("测试 APKG 文件版本兼容性 - 原始格式 (collection.anki2)")
        fun testOldFormatCompatibility() {
            val testFile = File("src/test/resources/apkg/primary_school_words.apkg")
            if (!testFile.exists()) {
                return  // 如果测试文件不存在，跳过测试
            }

            val result = parser.parseApkg(testFile.absolutePath)

            // 验证基本结构
            assertTrue(result.notes.isNotEmpty(), "原始格式应该解析出笔记")
            assertTrue(result.cards.isNotEmpty(), "原始格式应该解析出卡片")
            assertTrue(result.decks.isNotEmpty(), "原始格式应该解析出牌组")
            assertTrue(result.models.isNotEmpty(), "原始格式应该解析出模型")

            // 验证数据库版本
            assertTrue(result.databaseVersion > 0, "数据库版本应该为正数")
            assertTrue(result.creationTime > 0, "创建时间应该为正数")

            // 输出格式信息
            println("原始格式 (collection.anki2):")
            println("  - 数据库版本: ${result.databaseVersion}")
            println("  - 笔记数量: ${result.notes.size}")
            println("  - 卡片数量: ${result.cards.size}")
        }

        @Test
        @DisplayName("测试 APKG 文件版本兼容性 - 过渡格式 (collection.anki21)")
        fun testTransitionFormatCompatibility() {
            val testFile = File("src/test/resources/apkg/basic_word.apkg")
            if (!testFile.exists()) {
                return
            }

            val result = parser.parseApkg(testFile.absolutePath)

            // 验证基本结构
            assertTrue(result.notes.isNotEmpty(), "过渡格式应该解析出笔记")
            assertTrue(result.cards.isNotEmpty(), "过渡格式应该解析出卡片")
            assertTrue(result.decks.isNotEmpty(), "过渡格式应该解析出牌组")
            assertTrue(result.models.isNotEmpty(), "过渡格式应该解析出模型")

            // 验证数据库版本和格式信息
            assertTrue(result.databaseVersion > 0, "数据库版本应该为正数")
            assertTrue(result.creationTime > 0, "创建时间应该为正数")
            assertEquals(ApkgFormat.TRANSITIONAL, result.format, "格式应该为 TRANSITIONAL")

            // 输出格式信息
            println("过渡格式 (collection.anki21):")
            println("  - 数据库版本: ${result.databaseVersion}")
            println("  - 架构版本: ${result.schemaVersion}")
            println("  - 格式: ${result.format}")
            println("  - 笔记数量: ${result.notes.size}")
            println("  - 卡片数量: ${result.cards.size}")
        }

        @Test
        @DisplayName("测试 APKG 文件版本兼容性 - 新格式 (包含兼容性数据库)")
        fun testNewFormatCompatibility() {
            val testFiles = listOf(
                "src/test/resources/apkg/basic_word_new_version.apkg",
                "src/test/resources/apkg/Alphabet Word.apkg"
            )

            testFiles.forEach { filePath ->
                val file = File(filePath)
                if (file.exists()) {

                    val result = parser.parseApkg(file.absolutePath)

                    // 验证基本结构
                    assertTrue(result.notes.isNotEmpty(), "新格式 $filePath 应该解析出笔记")
                    assertTrue(result.cards.isNotEmpty(), "新格式 $filePath 应该解析出卡片")

                    // 验证数据库版本和格式信息
                    assertTrue(result.databaseVersion > 0, "新格式数据库版本应该为正数")
                    assertTrue(result.creationTime > 0, "创建时间应该为正数")
                    assertEquals(ApkgFormat.LATEST, result.format, "格式应该为 LATEST")
                    assertTrue(result.schemaVersion >= 18, "新格式架构版本应该 >= 18")

                    // 验证新格式特有特性
                    if (result.schemaVersion >= 18) {
                        println("检测到新格式 (V18+): ${file.name}")
                        // 新格式特有字段验证已移除，因为数据类已统一
                    }

                    // 输出格式信息
                    println("新格式 (${file.name}):")
                    println("  - 数据库版本: ${result.databaseVersion}")
                    println("  - 架构版本: ${result.schemaVersion}")
                    println("  - 格式: ${result.format}")
                    println("  - 笔记数量: ${result.notes.size}")
                    println("  - 卡片数量: ${result.cards.size}")
                    println("  - 文件大小: ${file.length()} bytes")
                    println("  - 格式类型: collection.anki21b")


                }
            }
        }

        @Test
        @DisplayName("测试所有格式的快速信息获取")
        fun testAllFormatsQuickInfo() {
            val testFiles = listOf(
                "src/test/resources/apkg/primary_school_words.apkg",
                "src/test/resources/apkg/basic_word.apkg",
                "src/test/resources/apkg/basic_word_new_version.apkg",
                "src/test/resources/apkg/Alphabet Word.apkg"
            )

            testFiles.forEach { filePath ->
                val file = File(filePath)
                if (file.exists()) {
                    try {
                        val info = parser.getApkgInfo(file.absolutePath)

                        assertTrue(info.isNotEmpty(), "$filePath 应该返回基本信息")
                        
                        // 对于新格式文件，可能无法获取完整信息，只验证能获取到的信息
                        if (!info.containsKey("error")) {
                            assertTrue(info.containsKey("format") || info.containsKey("mediaError"), "应该包含格式或媒体错误信息")
                            assertTrue(info.containsKey("schemaVersion") || info.containsKey("mediaError"), "应该包含架构版本或媒体错误信息")
                            assertTrue(info.containsKey("noteCount") || info.containsKey("mediaError"), "应该包含笔记数量或媒体错误信息")
                            assertTrue(info.containsKey("cardCount") || info.containsKey("mediaError"), "应该包含卡片数量或媒体错误信息")
                            assertTrue(info.containsKey("databaseVersion") || info.containsKey("mediaError"), "应该包含数据库版本或媒体错误信息")
                            
                            // 只验证能获取到的信息
                            if (info.containsKey("format")) {
                                val format = info["format"] as String
                                assertTrue(format.isNotBlank(), "格式不应该为空")
                            }
                            if (info.containsKey("schemaVersion")) {
                                val schemaVersion = info["schemaVersion"] as Int
                                assertTrue(schemaVersion > 0, "架构版本应该为正数")
                            }
                            if (info.containsKey("noteCount")) {
                                val noteCount = info["noteCount"] as Int
                                assertTrue(noteCount > 0, "笔记数量应该为正数")
                            }
                            if (info.containsKey("cardCount")) {
                                val cardCount = info["cardCount"] as Int
                                assertTrue(cardCount > 0, "卡片数量应该为正数")
                            }
                            if (info.containsKey("deckCount")) {
                                val deckCount = info["deckCount"] as Int
                                assertTrue(deckCount > 0, "牌组数量应该为正数")
                            }
                            if (info.containsKey("databaseVersion")) {
                                val dbVersion = info["databaseVersion"] as Int
                                assertTrue(dbVersion > 0, "数据库版本应该为正数")
                            }
                        } else {
                            // 对于有错误的文件，只验证错误信息存在
                            assertTrue(info["error"] is String, "错误信息应该是字符串")
                        }

                        println("快速信息 - ${file.name}:")
                        println("  - 格式: ${info["format"] ?: "N/A"}, 架构: ${info["schemaVersion"] ?: "N/A"}")
                        println("  - 笔记: ${info["noteCount"] ?: "N/A"}, 卡片: ${info["cardCount"] ?: "N/A"}, 牌组: ${info["deckCount"] ?: "N/A"}, 版本: ${info["databaseVersion"] ?: "N/A"}")
                    } catch (e: Exception) {
                        if (file.name.contains("new_version") || file.name.contains("Alphabet")) {
                            // 新格式文件可能无法获取快速信息
                            println("快速信息 - ${file.name} (新格式):")
                            println("  - 状态: 需要特殊处理新格式 collection.anki21b")
                            println("  - 错误: ${e.message}")
                        } else {
                            // 其他文件应该正常处理
                            throw e
                        }
                    }
                }
            }
        }
    }

    @Nested
    @DisplayName("格式信息测试")
    inner class FormatInfoTests {

        @Test
        @DisplayName("测试获取 APKG 文件格式信息")
        fun testGetFormatInfo() {
            val testFile = File("src/test/resources/apkg/basic_word.apkg")
            val formatInfo = parser.getFormatInfo(testFile.absolutePath)

            assertTrue(formatInfo.isNotEmpty(), "应该返回格式信息")
            assertTrue(formatInfo.containsKey("format"), "应该包含格式")
            assertTrue(formatInfo.containsKey("databaseFormat"), "应该包含数据库格式")
            assertTrue(formatInfo.containsKey("hasMetaFile"), "应该包含 meta 文件信息")
            assertTrue(formatInfo.containsKey("hasMediaFile"), "应该包含媒体文件信息")

            val format = formatInfo["format"] as String
            val databaseFormat = formatInfo["databaseFormat"] as String
            val hasMetaFile = formatInfo["hasMetaFile"] as Boolean
            val hasMediaFile = formatInfo["hasMediaFile"] as Boolean

            assertTrue(format.isNotBlank(), "格式不应该为空")
            assertTrue(databaseFormat.isNotBlank(), "数据库格式不应该为空")

            println("格式信息 - ${testFile.name}:")
            println("  - 格式: $format")
            println("  - 数据库格式: $databaseFormat")
            println("  - 有 meta 文件: $hasMetaFile")
            println("  - 有媒体文件: $hasMediaFile")
        }
    }

    @Nested
    @DisplayName("错误处理和性能测试")
    inner class ErrorHandlingAndPerformanceTests {

        @Test
        @DisplayName("测试错误处理 - 无效文件")
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

        @Test
        @DisplayName("测试性能 - 快速信息获取")
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
    }
}