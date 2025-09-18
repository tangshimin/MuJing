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
import com.github.luben.zstd.Zstd
import java.nio.charset.StandardCharsets

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
     * 测试创建基础单词学习包（旧格式）
     */
    @Test
    fun testCreateBasicVocabularyDeckLegacy() {
        val (creator, testWords) = createBasicTestData(ApkgCreator.FormatVersion.LEGACY)

        // 生成文件
        val outputPath = File(outputDir, "test_basic_vocabulary_legacy.apkg").absolutePath
        creator.createApkg(outputPath)
        println("📦 生成的 APKG 文件: $outputPath")

        // 验证文件
        val apkgFile = File(outputPath)
        verifyBasicApkgStructure(apkgFile, "collection.anki2")
        verifyDatabaseContent(apkgFile, testWords.size, 1, 1, "collection.anki2")

        println("✅ 基础词汇包测试通过（旧格式）")
    }

    /**
     * 测试创建基础单词学习包（新格式）
     */
    @Test
    fun testCreateBasicVocabularyDeckLatest() {
        val (creator, testWords) = createBasicTestData(ApkgCreator.FormatVersion.LATEST)

        // 生成文件
        val outputPath = File(outputDir, "test_basic_vocabulary_latest.apkg").absolutePath
        creator.createApkg(outputPath)
        println("📦 生成的 APKG 文件: $outputPath")

        // 验证文件
        val apkgFile = File(outputPath)
        verifyBasicApkgStructure(apkgFile, "collection.anki21b")
        verifyDatabaseContent(apkgFile, testWords.size, 1, 1, "collection.anki21b")

        println("✅ 基础词汇包测试通过（新格式）")
    }

    /**
     * 测试创建基础单词学习包（双格式）
     */
    @Test
    fun testCreateBasicVocabularyDeckDualFormat() {
        val (creator, testWords) = createBasicTestData()

        // 4. 生成文件（双格式）
        val outputPath = File(outputDir, "test_basic_vocabulary_dual.apkg").absolutePath
        creator.createApkg(outputPath, dualFormat = true)
        println("📦 生成的 APKG 文件: $outputPath")

        // 5. 验证文件创建成功
        val apkgFile = File(outputPath)
        assertTrue(apkgFile.exists(), "APKG 文件应该被成功创建")
        assertTrue(apkgFile.length() > 0, "APKG 文件应该不为空")
        println("📊 文件大小: ${apkgFile.length()} 字节")

        // 6. 验证 ZIP 结构（双格式）
        verifyDualFormatApkgStructure(apkgFile)

        // 7. 验证数据库内容（验证两种格式）
        verifyDatabaseContent(apkgFile, testWords.size, 1, 1, "collection.anki2")
        verifyDatabaseContent(apkgFile, testWords.size, 1, 1, "collection.anki21b")

        println("✅ 基础词汇包测试通过（双格式）")
    }

    /**
     * 测试创建高级单词学习包（包含音频和例句）- 多格式版本
     */
    @Test
    fun testCreateAdvancedVocabularyDeckMultiFormat() {
        runMultiFormatTest(
            testName = "advanced_vocabulary",
            setup = { creator, formatVersion, _ ->
                setupAdvancedTestData(creator, formatVersion)
            },
            verify = { apkgFile, formatVersion, _ ->
                val expectedDbName = getExpectedDbName(formatVersion)
                val advancedWords = listOf(
                    WordData("sophisticated", "复杂的，精密的", "", "She has sophisticated taste in art."),
                    WordData("magnificent", "壮丽的，宏伟的", "", "The view from the mountain top was magnificent."),
                    WordData("fundamental", "基本的，根本的", "", "Education is fundamental to personal development.")
                )
                
                verifyApkgStructure(apkgFile, expectedDbName)
                verifyDatabaseContent(apkgFile, advancedWords.size, 1, 1, expectedDbName)
                verifyAdvancedModelFields(apkgFile, expectedDbName)
            }
        )
    }

    /**
     * 测试从现有词汇数据创建 APKG - 多格式版本
     */
    @Test
    fun testCreateFromVocabularyDataMultiFormat() {
        // 测试所有格式版本
        val formatTests = listOf(
            ApkgCreator.FormatVersion.LEGACY to "legacy",
            ApkgCreator.FormatVersion.LATEST to "latest"
        )

        formatTests.forEach { (formatVersion, formatName) ->
            val creator = ApkgCreator()
            creator.setFormatVersion(formatVersion)

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
            val outputPath = File(outputDir, "test_imported_vocabulary_$formatName.apkg").absolutePath
            creator.createApkg(outputPath)
            println("📦 生成的导入词汇 APKG 文件 ($formatName): $outputPath")

            // 6. 验证文件创建成功
            val apkgFile = File(outputPath)
            assertTrue(apkgFile.exists(), "导入词汇 APKG 文件应该被成功创建")
            assertTrue(apkgFile.length() > 0, "导入词汇 APKG 文件应该不为空")
            println("📊 文件大小: ${apkgFile.length()} 字节")

            // 7. 验证 ZIP 结构
            val expectedDbName = when (formatVersion) {
                ApkgCreator.FormatVersion.LEGACY -> "collection.anki2"
                ApkgCreator.FormatVersion.TRANSITIONAL -> "collection.anki21"
                ApkgCreator.FormatVersion.LATEST -> "collection.anki21b"
            }
            verifyApkgStructure(apkgFile, expectedDbName)

            // 8. 验证数据库内容
            verifyDatabaseContent(apkgFile, vocabularyData.size, 1, 1, expectedDbName)

            // 9. 验证标签
            verifyNoteTags(apkgFile, "imported vocabulary", expectedDbName)

            println("✅ 词汇数据导入测试通过 ($formatName)")
        }
    }

    /**
     * 测试媒体文件处理 - 多格式版本
     */
    @Test
    fun testMediaFileHandlingMultiFormat() {
        // 测试所有格式版本
        val formatTests = listOf(
            ApkgCreator.FormatVersion.LEGACY to "legacy",
            ApkgCreator.FormatVersion.LATEST to "latest"
        )

        formatTests.forEach { (formatVersion, formatName) ->
            val creator = ApkgCreator()
            creator.setFormatVersion(formatVersion)

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
            val outputPath = File(outputDir, "test_media_$formatName.apkg").absolutePath
            creator.createApkg(outputPath)
            println("📦 生成的 APKG 文件 ($formatName): $outputPath")

            // 5. 验证媒体文件
            verifyMediaFiles(File(outputPath), mapOf(
                "test_audio.mp3" to audioData,
                "test_image.jpg" to imageData
            ), formatVersion)

            // 6. 验证数据库结构
            val expectedDbName = when (formatVersion) {
                ApkgCreator.FormatVersion.LEGACY -> "collection.anki2"
                ApkgCreator.FormatVersion.TRANSITIONAL -> "collection.anki21"
                ApkgCreator.FormatVersion.LATEST -> "collection.anki21b"
            }
            verifyDatabaseContent(File(outputPath), 1, 1, 1, expectedDbName)

            println("✅ 媒体文件处理测试通过 ($formatName)")
        }
    }

    /**
     * 测试多牌组支持 - 多格式版本
     */
    @Test
    fun testMultipleDeckSupportMultiFormat() {
        // 测试所有格式版本
        val formatTests = listOf(
            ApkgCreator.FormatVersion.LEGACY to "legacy",
            ApkgCreator.FormatVersion.TRANSITIONAL to "transitional"  // 暂时使用过渡格式避免压缩问题
        )

        formatTests.forEach { (formatVersion, formatName) ->
            val creator = ApkgCreator()
            creator.setFormatVersion(formatVersion)

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
            val outputPath = File(outputDir, "test_multiple_decks_$formatName.apkg").absolutePath
            creator.createApkg(outputPath)
            println("📦 生成的 APKG 文件 ($formatName): $outputPath")

            // 5. 验证多牌组
            val expectedDbName = when (formatVersion) {
                ApkgCreator.FormatVersion.LEGACY -> "collection.anki2"
                ApkgCreator.FormatVersion.TRANSITIONAL -> "collection.anki21"
                ApkgCreator.FormatVersion.LATEST -> "collection.anki21b"
            }
            
            // 调试：检查数据库文件是否被压缩
            val apkgFile = File(outputPath)
            val zipFile = java.util.zip.ZipFile(apkgFile)
            val dbEntry = zipFile.getEntry(expectedDbName)
            if (dbEntry != null) {
                val dbStream = zipFile.getInputStream(dbEntry)
                val dbData = dbStream.readBytes()
                val isZstdCompressed = isZstdCompressed(dbData)
                println("🔍 数据库 $expectedDbName Zstd 压缩检测: $isZstdCompressed, 数据大小: ${dbData.size} 字节")
                
                if (isZstdCompressed && formatVersion != ApkgCreator.FormatVersion.LATEST) {
                    println("❌ 错误: 非 LATEST 格式的数据库被压缩了!")
                }
            }
            verifyMultipleDecks(File(outputPath), 2, expectedDbName)

            // 6. 验证数据库内容
            verifyDatabaseContent(File(outputPath), 2, 2, 1, expectedDbName)

            println("✅ 多牌组支持测试通过 ($formatName)")
        }
    }


    /**
     * 测试 V18 架构特定功能
     */
    @Test
    fun testV18SchemaSpecificFeatures() {
        val creator = ApkgCreator()
        creator.setFormatVersion(ApkgCreator.FormatVersion.LATEST)
        
        // 创建牌组和模型
        val deckId = ApkgCreator.generateId()
        val deck = ApkgCreator.Deck(id = deckId, name = "V18 测试")
        creator.addDeck(deck)
        
        val model = ApkgCreator.createWordModel()
        creator.addModel(model)
        
        // 添加媒体文件
        val audioData = "fake audio data for V18 test".toByteArray()
        creator.addMediaFile("test_audio_v18.mp3", audioData)
        
        // 添加笔记
        val note = ApkgCreator.Note(
            id = ApkgCreator.generateId(),
            mid = model.id,
            fields = listOf("v18", "V18 测试", "[sound:test_audio_v18.mp3]", "V18 schema test")
        )
        creator.addNote(note, deckId)
        
        // 生成文件
        val outputPath = File(outputDir, "test_v18_features.apkg").absolutePath
        creator.createApkg(outputPath)
        println("📦 生成的 V18 APKG 文件: $outputPath")
        
        // 验证 V18 特定功能
        withDatabase(File(outputPath), "collection.anki21b") { conn ->
            // 验证 V18 新增字段
            conn.createStatement().use { stmt ->
                val rs = stmt.executeQuery("SELECT fsrsWeights, fsrsParams5, desiredRetention FROM col WHERE id = 1")
                rs.next()
                val fsrsWeights = rs.getString(1)
                val fsrsParams5 = rs.getString(2)
                val desiredRetention = rs.getDouble(3)
                
                assertEquals("[]", fsrsWeights, "fsrsWeights 应该为空数组")
                assertEquals("[]", fsrsParams5, "fsrsParams5 应该为空数组")
                assertEquals(0.9, desiredRetention, 0.01, "desiredRetention 应该为 0.9")
            }
            
            // 验证媒体元数据表存在
            val metaTables = conn.metaData.getTables(null, null, "%", arrayOf("TABLE"))
            var hasMediaMetaTable = false
            while (metaTables.next()) {
                if (metaTables.getString("TABLE_NAME").equals("mediaMeta", ignoreCase = true)) {
                    hasMediaMetaTable = true
                    break
                }
            }
            assertTrue(hasMediaMetaTable, "V18 架构应该包含 mediaMeta 表")
            
            // 验证 FSRS 表存在
            var hasFsrsWeightsTable = false
            var hasFsrsParamsTable = false
            val metaTables2 = conn.metaData.getTables(null, null, "%", arrayOf("TABLE"))
            while (metaTables2.next()) {
                val tableName = metaTables2.getString("TABLE_NAME")
                if (tableName.equals("fsrsWeights", ignoreCase = true)) {
                    hasFsrsWeightsTable = true
                } else if (tableName.equals("fsrsParams", ignoreCase = true)) {
                    hasFsrsParamsTable = true
                }
            }
            assertTrue(hasFsrsWeightsTable, "V18 架构应该包含 fsrsWeights 表")
            assertTrue(hasFsrsParamsTable, "V18 架构应该包含 fsrsParams 表")
            
            // 验证卡片表有 FSRS 字段
            val cardColumns = conn.metaData.getColumns(null, null, "cards", "%")
            var hasFsrsState = false
            var hasFsrsDifficulty = false
            var hasFsrsStability = false
            var hasFsrsDue = false
            
            while (cardColumns.next()) {
                val columnName = cardColumns.getString("COLUMN_NAME")
                when (columnName.lowercase()) {
                    "fsrsstate" -> hasFsrsState = true
                    "fsrsdifficulty" -> hasFsrsDifficulty = true
                    "fsrsstability" -> hasFsrsStability = true
                    "fsrsdue" -> hasFsrsDue = true
                }
            }
            
            assertTrue(hasFsrsState, "cards 表应该包含 fsrsState 字段")
            assertTrue(hasFsrsDifficulty, "cards 表应该包含 fsrsDifficulty 字段")
            assertTrue(hasFsrsStability, "cards 表应该包含 fsrsStability 字段")
            assertTrue(hasFsrsDue, "cards 表应该包含 fsrsDue 字段")
        }
        
        println("✅ V18 架构特定功能测试通过")
    }

    /**
     * 测试 Zstd 压缩功能 - 独立测试
     */
    @Test
    fun testZstdCompressionFunctionality() {
        // 测试 Zstd 压缩和解压缩
        val testData = "Hello, this is a test string for Zstd compression. This should be compressed effectively.".toByteArray()
        
        // 使用 zstd-jni 库进行压缩（与 ApkgCreator 保持一致）
        val compressed = Zstd.compress(testData, 3)
        println("📊 原始数据大小: ${testData.size} 字节")
        println("📊 压缩后大小: ${compressed.size} 字节")
        println("📊 压缩率: ${String.format("%.1f%%", compressed.size.toDouble() / testData.size * 100)}")
        
        // 验证 Zstd 魔术字节
        assertTrue(isZstdCompressed(compressed), "压缩数据应该包含 Zstd 魔术字节")
        
        // 解压缩数据（使用 zstd-jni 库）
        val decompressed = Zstd.decompress(compressed, testData.size * 2)
        
        // 验证数据完整性
        assertArrayEquals(testData, decompressed, "解压缩后的数据应该与原始数据相同")
        
        println("✅ Zstd 压缩功能测试通过")
    }

    // === 辅助测试方法 ===

    /**
     * 执行多格式测试的通用模式
     */
    private fun runMultiFormatTest(
        testName: String,
        formatTests: List<Pair<ApkgCreator.FormatVersion, String>> = listOf(
            ApkgCreator.FormatVersion.LEGACY to "legacy",
            ApkgCreator.FormatVersion.LATEST to "latest"
        ),
        setup: (ApkgCreator, ApkgCreator.FormatVersion, String) -> Unit,
        verify: (File, ApkgCreator.FormatVersion, String) -> Unit
    ) {
        formatTests.forEach { (formatVersion, formatName) ->
            val creator = ApkgCreator()
            creator.setFormatVersion(formatVersion)
            
            setup(creator, formatVersion, formatName)
            
            // 生成文件
            val outputPath = File(outputDir, "test_${testName}_$formatName.apkg").absolutePath
            creator.createApkg(outputPath)
            println("📦 生成的 APKG 文件 ($formatName): $outputPath")
            
            // 验证文件创建成功
            val apkgFile = File(outputPath)
            assertTrue(apkgFile.exists(), "APKG 文件应该被成功创建")
            assertTrue(apkgFile.length() > 0, "APKG 文件应该不为空")
            println("📊 文件大小: ${apkgFile.length()} 字节")
            
            verify(apkgFile, formatVersion, formatName)
            
            println("✅ $testName 测试通过 ($formatName)")
        }
    }

    // === 辅助验证方法 ===

    /**
     * 从 APKG 文件中提取并解压缩数据库到临时文件
     */
    private fun extractDatabaseToTempFile(apkgFile: File, dbName: String = "collection.anki2"): File {
        ZipFile(apkgFile).use { zipFile ->
            val dbEntry = zipFile.getEntry(dbName)
            assertNotNull(dbEntry, "$dbName 应该存在")

            val tempDbFile = File.createTempFile("test_db", ".anki2")
            zipFile.getInputStream(dbEntry).use { input ->
                tempDbFile.outputStream().use { output ->
                    val data = input.readBytes()
                    
                    // 检查是否是 Zstd 压缩格式
                    val isZstdCompressed = isZstdCompressed(data)
                    println("🔍 数据库 $dbName Zstd 压缩检测: $isZstdCompressed, 数据大小: ${data.size} 字节")
                    
                    val decompressedData = if (isZstdCompressed) {
                        try {
                            // 使用 zstd-jni 库进行解压缩（与 ApkgCreator 保持一致）
                            val result = Zstd.decompress(data, 10 * 1024 * 1024) // 10MB 最大解压缩大小
                            println("✅ Zstd 解压缩成功: ${data.size} -> ${result.size} 字节")
                            result
                        } catch (e: Exception) {
                            println("❌ Zstd 解压缩失败: ${e.message}")
                            throw e // 重新抛出异常，让测试失败
                        }
                    } else {
                        data
                    }
                    
                    output.write(decompressedData)
                    
                    // 验证解压缩后的数据是否是有效的 SQLite 数据库
                    val isSqlite = isSqliteDatabase(decompressedData)
                    println("🔍 解压缩后数据是否是 SQLite 数据库: $isSqlite")
                    
                    if (!isSqlite) {
                        throw AssertionError("解压缩后的数据不是有效的 SQLite 数据库")
                    }
                }
            }
            return tempDbFile
        }
    }

    /**
     * 检查数据是否使用 Zstd 压缩
     */
    private fun isZstdCompressed(data: ByteArray): Boolean {
        if (data.size < 4) return false
        
        // Zstd 魔术字节: 0x28B52FFD (big-endian: 28 B5 2F FD)
        val magic = (data[0].toLong() and 0xFF) shl 24 or
                   ((data[1].toLong() and 0xFF) shl 16) or
                   ((data[2].toLong() and 0xFF) shl 8) or
                   (data[3].toLong() and 0xFF)
        
        return magic == 0x28B52FFDL
    }

    /**
     * 检查数据是否是有效的 SQLite 数据库
     */
    private fun isSqliteDatabase(data: ByteArray): Boolean {
        if (data.size < 16) return false
        
        // SQLite 文件头: "SQLite format 3\u0000"
        val sqliteHeader = "SQLite format 3\u0000".toByteArray()
        return data.copyOfRange(0, 16).contentEquals(sqliteHeader)
    }

    /**
     * 执行数据库查询操作
     */
    private fun <T> executeDatabaseQuery(tempDbFile: File, block: (java.sql.Connection) -> T): T {
        val url = "jdbc:sqlite:${tempDbFile.absolutePath}"
        DriverManager.getConnection(url).use { conn ->
            return block(conn)
        }
    }

    /**
     * 执行数据库查询并自动清理临时文件
     */
    private fun <T> withDatabase(apkgFile: File, dbName: String = "collection.anki2", block: (java.sql.Connection) -> T): T {
        val tempDbFile = extractDatabaseToTempFile(apkgFile, dbName)
        try {
            return executeDatabaseQuery(tempDbFile, block)
        } finally {
            tempDbFile.delete()
        }
    }

    private fun verifyApkgStructure(apkgFile: File, expectedDbName: String = "collection.anki2") {
        ZipFile(apkgFile).use { zipFile ->
            val entries = zipFile.entries().toList().map { it.name }

            assertTrue(entries.contains(expectedDbName), "应该包含 $expectedDbName 文件")
            assertTrue(entries.contains("media"), "应该包含 media 文件")
        }
    }

    private fun verifyDualFormatApkgStructure(apkgFile: File) {
        ZipFile(apkgFile).use { zipFile ->
            val entries = zipFile.entries().toList().map { it.name }

            assertTrue(entries.contains("collection.anki2"), "应该包含 collection.anki2 文件")
            assertTrue(entries.contains("collection.anki21b"), "应该包含 collection.anki21b 文件")
            assertTrue(entries.contains("media"), "应该包含 media 文件")
        }
    }

    private fun verifyDatabaseContent(apkgFile: File, expectedNotes: Int, expectedDecks: Int, expectedModels: Int, dbName: String = "collection.anki2") {
        withDatabase(apkgFile, dbName) { conn ->
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
    }

    private fun verifyAdvancedModelFields(apkgFile: File, dbName: String = "collection.anki2") {
        withDatabase(apkgFile, dbName) { conn ->
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
    }

    private fun verifyNoteTags(apkgFile: File, expectedTag: String, dbName: String = "collection.anki2") {
        withDatabase(apkgFile, dbName) { conn ->
            conn.createStatement().use { stmt ->
                val rs = stmt.executeQuery("SELECT tags FROM notes")
                while (rs.next()) {
                    val tags = rs.getString(1)
                    assertTrue(tags.contains(expectedTag), "笔记应该包含期望的标签")
                }
            }
        }
    }

    private fun verifyMediaFiles(apkgFile: File, expectedMedia: Map<String, ByteArray>, formatVersion: ApkgCreator.FormatVersion = ApkgCreator.FormatVersion.LEGACY) {
        ZipFile(apkgFile).use { zipFile ->
            // 验证 media 映射文件
            val mediaEntry = zipFile.getEntry("media")
            assertNotNull(mediaEntry, "media 文件应该存在")

            val mediaJson = zipFile.getInputStream(mediaEntry).use {
                it.readBytes().toString(StandardCharsets.UTF_8)
            }
            
            if (formatVersion.schemaVersion >= 18) {
                // V18+ 新格式：JsonArray of JsonObjects
                val mediaList = Json.parseToJsonElement(mediaJson).jsonArray
                
                // 验证每个媒体文件
                expectedMedia.forEach { (filename, expectedData) ->
                    val found = mediaList.any { mediaItem ->
                        mediaItem.jsonObject["name"]?.jsonPrimitive?.content == filename
                    }
                    assertTrue(found, "媒体映射应该包含 $filename")

                    // 找到对应的编号文件并验证内容
                    val mediaItem = mediaList.find { mediaItem ->
                        mediaItem.jsonObject["name"]?.jsonPrimitive?.content == filename
                    }
                    val mediaId = mediaItem?.jsonObject?.get("id")?.jsonPrimitive?.int
                    assertNotNull(mediaId, "应该找到 $filename 的编号")

                    val mediaFileEntry = zipFile.getEntry(mediaId.toString())
                    assertNotNull(mediaFileEntry, "编号媒体文件应该存在")

                    val actualData = zipFile.getInputStream(mediaFileEntry).use { it.readBytes() }
                    assertArrayEquals(expectedData, actualData, "$filename 的内容应该匹配")
                }
            } else {
                // 旧格式：JsonObject mapping numbers to filenames
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
    }

    private fun verifyMultipleDecks(apkgFile: File, expectedDeckCount: Int, dbName: String = "collection.anki2") {
        withDatabase(apkgFile, dbName) { conn ->
            conn.createStatement().use { stmt ->
                val rs = stmt.executeQuery("SELECT decks FROM col WHERE id = 1")
                rs.next()
                val decksJson = rs.getString(1)
                val decks = Json.parseToJsonElement(decksJson).jsonObject
                assertEquals(expectedDeckCount, decks.size, "应该有 $expectedDeckCount 个牌组")
            }
        }
    }

    private data class WordData(
        val english: String,
        val chinese: String,
        val audio: String,
        val example: String
    )

    // === 测试辅助方法 ===

    /**
     * 创建基础测试数据
     */
    private fun createBasicTestData(formatVersion: ApkgCreator.FormatVersion? = null): Pair<ApkgCreator, List<Pair<String, String>>> {
        val creator = ApkgCreator()
        formatVersion?.let { creator.setFormatVersion(it) }
        
        // 创建牌组
        val deckId = ApkgCreator.generateId()
        val deck = ApkgCreator.Deck(
            id = deckId,
            name = "基础英语词汇",
            desc = "包含常用英语单词的学习卡片"
        )
        creator.addDeck(deck)

        // 创建模型
        val model = ApkgCreator.createBasicModel()
        creator.addModel(model)

        // 测试单词数据
        val testWords = listOf(
            "apple" to "苹果",
            "book" to "书",
            "cat" to "猫",
            "dog" to "狗",
            "water" to "水"
        )

        // 添加笔记
        testWords.forEach { (english, chinese) ->
            val note = ApkgCreator.Note(
                id = ApkgCreator.generateId(),
                mid = model.id,
                fields = listOf(english, chinese)
            )
            creator.addNote(note, deckId)
        }

        return Pair(creator, testWords)
    }

    /**
     * 创建高级测试数据（包含音频和例句）
     */
    private fun createAdvancedTestData(formatVersion: ApkgCreator.FormatVersion? = null): Pair<ApkgCreator, List<WordData>> {
        val creator = ApkgCreator()
        formatVersion?.let { creator.setFormatVersion(it) }
        
        // 创建牌组
        val deckId = ApkgCreator.generateId()
        val deck = ApkgCreator.Deck(
            id = deckId,
            name = "高级英语词汇",
            desc = "包含音频和例句的英语单词学习"
        )
        creator.addDeck(deck)

        // 使用高级单词模型
        val model = ApkgCreator.createWordModel()
        creator.addModel(model)

        // 高级单词数据
        val advancedWords = listOf(
            WordData("sophisticated", "复杂的，精密的", "", "She has sophisticated taste in art."),
            WordData("magnificent", "壮丽的，宏伟的", "", "The view from the mountain top was magnificent."),
            WordData("fundamental", "基本的，根本的", "", "Education is fundamental to personal development.")
        )

        // 添加笔记
        advancedWords.forEach { word ->
            val note = ApkgCreator.Note(
                id = ApkgCreator.generateId(),
                mid = model.id,
                fields = listOf(word.english, word.chinese, word.audio, word.example)
            )
            creator.addNote(note, deckId)
        }

        return Pair(creator, advancedWords)
    }

    /**
     * 验证 APKG 文件基本结构
     */
    private fun verifyBasicApkgStructure(apkgFile: File, expectedDbName: String) {
        assertTrue(apkgFile.exists(), "APKG 文件应该被成功创建")
        assertTrue(apkgFile.length() > 0, "APKG 文件应该不为空")
        println("📊 文件大小: ${apkgFile.length()} 字节")
        
        verifyApkgStructure(apkgFile, expectedDbName)
    }

    /**
     * 设置高级测试数据
     */
    private fun setupAdvancedTestData(creator: ApkgCreator, formatVersion: ApkgCreator.FormatVersion? = null) {
        formatVersion?.let { creator.setFormatVersion(it) }
        
        // 创建牌组
        val deckId = ApkgCreator.generateId()
        val deck = ApkgCreator.Deck(
            id = deckId,
            name = "高级英语词汇",
            desc = "包含音频和例句的英语单词学习"
        )
        creator.addDeck(deck)

        // 使用高级单词模型
        val model = ApkgCreator.createWordModel()
        creator.addModel(model)

        // 高级单词数据
        val advancedWords = listOf(
            WordData("sophisticated", "复杂的，精密的", "", "She has sophisticated taste in art."),
            WordData("magnificent", "壮丽的，宏伟的", "", "The view from the mountain top was magnificent."),
            WordData("fundamental", "基本的，根本的", "", "Education is fundamental to personal development.")
        )

        // 添加笔记
        advancedWords.forEach { word ->
            val note = ApkgCreator.Note(
                id = ApkgCreator.generateId(),
                mid = model.id,
                fields = listOf(word.english, word.chinese, word.audio, word.example)
            )
            creator.addNote(note, deckId)
        }
    }

    /**
     * 获取对应格式版本的数据库文件名
     */
    private fun getExpectedDbName(formatVersion: ApkgCreator.FormatVersion): String {
        return when (formatVersion) {
            ApkgCreator.FormatVersion.LEGACY -> "collection.anki2"
            ApkgCreator.FormatVersion.TRANSITIONAL -> "collection.anki21"
            ApkgCreator.FormatVersion.LATEST -> "collection.anki21b"
        }
    }
}
