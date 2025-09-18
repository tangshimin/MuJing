package fsrs

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import java.io.File
import java.util.zip.ZipFile
import kotlinx.serialization.json.*
import com.github.luben.zstd.Zstd

/**
 * APKG 兼容性测试
 * 专门测试与 Anki 24.11 的兼容性问题
 * 修复 "500: Unknown frame descriptor" 错误
 */
class ApkgCompatibilityTest {

    private lateinit var outputDir: File

    @BeforeEach
    fun setUp() {
        outputDir = File(System.getProperty("user.dir"), "test-output")
        outputDir.mkdirs()
    }

    /**
     * 测试修复后的新格式兼容性
     * 解决 "500: Unknown frame descriptor" 错误
     */
    @Test
    fun testAnki24_11CompatibilityFix() {
        println("🧪 开始 Anki 24.11 兼容性测试")

        val creator = ApkgCreator()
        creator.setFormatVersion(ApkgCreator.FormatVersion.LATEST)

        // 创建测试数据
        val deckId = ApkgCreator.generateId()
        val deck = ApkgCreator.Deck(
            id = deckId,
            name = "兼容性测试牌组",
            desc = "测试与 Anki 24.11 的兼容性",
            mod = System.currentTimeMillis() / 1000
        )
        creator.addDeck(deck)

        val model = ApkgCreator.createBasicModel()
        creator.addModel(model)

        // 添加测试笔记
        val note = ApkgCreator.Note(
            id = ApkgCreator.generateId(),
            mid = model.id,
            fields = listOf("compatibility test", "兼容性测试"),
            tags = "anki24.11 compatibility"
        )
        creator.addNote(note, deckId)

        // 添加媒体文件
        val audioData = "test audio data for compatibility".toByteArray()
        creator.addMediaFile("compatibility_test.mp3", audioData)

        // 生成 APKG 文件
        val outputPath = File(outputDir, "anki_24_11_compatibility_test.apkg").absolutePath
        creator.createApkg(outputPath)
        println("📦 生成的兼容性测试 APKG 文件: $outputPath")

        // 验证文件结构
        val apkgFile = File(outputPath)
        assertTrue(apkgFile.exists(), "APKG 文件应该存在")
        assertTrue(apkgFile.length() > 0, "APKG 文件应该不为空")

        // 验证 ZIP 结构和内容
        ZipFile(apkgFile).use { zipFile ->
            val entries = zipFile.entries().toList().map { it.name }.sorted()
            println("📋 ZIP 条目: $entries")

            // 检查必要的文件存在
            assertTrue(entries.contains("collection.anki21b"), "应该包含 collection.anki21b")
            assertTrue(entries.contains("media"), "应该包含 media 文件")
            assertTrue(entries.contains("meta"), "应该包含 meta 文件")
            assertTrue(entries.contains("0"), "应该包含媒体文件 0")

            // 验证 meta 文件
            val metaEntry = zipFile.getEntry("meta")
            val metaData = zipFile.getInputStream(metaEntry).readBytes()
            assertEquals(2, metaData.size, "Meta 文件应该是 2 字节")
            assertEquals(0x08.toByte(), metaData[0], "Meta 文件第一字节应该是 0x08")
            assertEquals(0x03.toByte(), metaData[1], "Meta 文件第二字节应该是 0x03 (VERSION_LATEST)")
            println("✅ Meta 文件验证通过: ${metaData.map { "0x%02X".format(it) }}")

            // 验证媒体文件格式（JSON 数组格式）
            val mediaEntry = zipFile.getEntry("media")
            val mediaJson = zipFile.getInputStream(mediaEntry).readBytes().toString(Charsets.UTF_8)
            val mediaArray = Json.parseToJsonElement(mediaJson).jsonArray
            assertTrue(mediaArray.size > 0, "媒体数组应该包含元素")
            val firstMedia = mediaArray[0].jsonObject
            assertEquals("compatibility_test.mp3", firstMedia["name"]?.jsonPrimitive?.content, "媒体名称应该正确")
            println("✅ 媒体文件格式验证通过: $mediaJson")

            // 验证数据库文件是否正确压缩
            val dbEntry = zipFile.getEntry("collection.anki21b")
            val dbData = zipFile.getInputStream(dbEntry).readBytes()

            // 检查 Zstd 压缩
            val isZstdCompressed = isZstdCompressed(dbData)
            assertTrue(isZstdCompressed, "数据库文件应该使用 Zstd 压缩")
            println("✅ Zstd 压缩检测通过")

            // 尝试解压缩验证格式正确性
            try {
                val decompressed = Zstd.decompress(dbData, 10 * 1024 * 1024)
                assertTrue(decompressed.isNotEmpty(), "解压缩后的数据不应为空")

                // 验证是否是有效的 SQLite 数据库
                val sqliteHeader = "SQLite format 3\u0000".toByteArray()
                assertTrue(decompressed.size >= 16, "解压缩数据应该足够大")
                val actualHeader = decompressed.copyOfRange(0, 16)
                assertArrayEquals(
                    sqliteHeader,
                    actualHeader,
                    "解压缩后应该是有效的 SQLite 数据库，实际头部: ${actualHeader.map { it.toInt().toChar() }}"
                )

                println("✅ Zstd 解压缩验证成功: ${dbData.size} -> ${decompressed.size} 字节")
            } catch (e: Exception) {
                fail("Zstd 解压缩失败: ${e.message}")
            }
        }

        println("🎉 Anki 24.11 兼容性测试通过")
    }

    /**
     * 测试格式验证 - 确保所有格式都能正确生成
     */
    @Test
    fun testAllFormatsValidation() {
        println("🧪 开始所有格式验证测试")

        val formatTests = listOf(
            ApkgCreator.FormatVersion.LEGACY to "legacy",
            ApkgCreator.FormatVersion.TRANSITIONAL to "transitional",
            ApkgCreator.FormatVersion.LATEST to "latest"
        )

        formatTests.forEach { (formatVersion, formatName) ->
            println("📝 测试 $formatName 格式...")

            val creator = ApkgCreator()
            creator.setFormatVersion(formatVersion)

            // 创建测试数据
            val deckId = ApkgCreator.generateId()
            val deck = ApkgCreator.Deck(
                id = deckId,
                name = "格式验证测试 $formatName",
                desc = "$formatName 格式兼容性测试"
            )
            creator.addDeck(deck)

            val model = ApkgCreator.createBasicModel()
            creator.addModel(model)

            val note = ApkgCreator.Note(
                id = ApkgCreator.generateId(),
                mid = model.id,
                fields = listOf("word $formatName", "词汇 $formatName")
            )
            creator.addNote(note, deckId)

            // 生成文件
            val outputPath = File(outputDir, "format_validation_test_$formatName.apkg").absolutePath
            creator.createApkg(outputPath)

            // 验证文件
            val apkgFile = File(outputPath)
            assertTrue(apkgFile.exists(), "$formatName 格式文件应该存在")
            assertTrue(apkgFile.length() > 0, "$formatName 格式文件应该不为空")

            // 验证数据库文件名
            ZipFile(apkgFile).use { zipFile ->
                val expectedDbName = when (formatVersion) {
                    ApkgCreator.FormatVersion.LEGACY -> "collection.anki2"
                    ApkgCreator.FormatVersion.TRANSITIONAL -> "collection.anki21"
                    ApkgCreator.FormatVersion.LATEST -> "collection.anki21b"
                }

                val dbEntry = zipFile.getEntry(expectedDbName)
                assertNotNull(dbEntry, "$formatName 格式应该包含 $expectedDbName")

                // 验证压缩状态
                if (formatVersion.useZstdCompression) {
                    val dbData = zipFile.getInputStream(dbEntry).readBytes()
                    assertTrue(isZstdCompressed(dbData), "$formatName 格式应该使用 Zstd 压缩")
                    println("✅ $formatName 格式压缩验证通过")
                } else {
                    val dbData = zipFile.getInputStream(dbEntry).readBytes()
                    assertFalse(isZstdCompressed(dbData), "$formatName 格式不应该使用 Zstd 压缩")
                    println("✅ $formatName 格式未压缩验证通过")
                }
            }

            println("✅ $formatName 格式验证通过")
        }

        println("🎉 所有格式验证测试通过")
    }

    /**
     * 测试双格式生成
     */
    @Test
    fun testDualFormatGeneration() {
        println("🧪 开始双格式生成测试")

        val creator = ApkgCreator()

        // 创建测试数据
        val deckId = ApkgCreator.generateId()
        val deck = ApkgCreator.Deck(
            id = deckId,
            name = "双格式测试牌组",
            desc = "测试同时生成新旧格式"
        )
        creator.addDeck(deck)

        val model = ApkgCreator.createBasicModel()
        creator.addModel(model)

        val note = ApkgCreator.Note(
            id = ApkgCreator.generateId(),
            mid = model.id,
            fields = listOf("dual format", "双格式测试")
        )
        creator.addNote(note, deckId)

        // 添加媒体文件
        val imageData = "fake image data for dual format test".toByteArray()
        creator.addMediaFile("dual_test.jpg", imageData)

        // 生成双格式文件
        val outputPath = File(outputDir, "dual_format_compatibility_test.apkg").absolutePath
        creator.createApkg(outputPath, dualFormat = true)
        println("📦 生成的双格式测试文件: $outputPath")

        // 验证双格式文件
        val apkgFile = File(outputPath)
        assertTrue(apkgFile.exists(), "双格式文件应该存在")

        ZipFile(apkgFile).use { zipFile ->
            val entries = zipFile.entries().toList().map { it.name }.sorted()
            println("📋 双格式 ZIP 条目: $entries")

            assertTrue(entries.contains("collection.anki2"), "应该包含旧格式数据库")
            assertTrue(entries.contains("collection.anki21b"), "应该包含新格式数据库")
            assertTrue(entries.contains("media"), "应该包含媒体文件")
            assertTrue(entries.contains("meta"), "应该包含元数据文件")
            assertTrue(entries.contains("0"), "应该包含媒体文件")

            // 验证新格式数据库是压缩的
            val newDbEntry = zipFile.getEntry("collection.anki21b")
            val newDbData = zipFile.getInputStream(newDbEntry).readBytes()
            assertTrue(isZstdCompressed(newDbData), "新格式数据库应该是压缩的")
            println("✅ 新格式数据库压缩验证通过")

            // 验证旧格式数据库是未压缩的
            val oldDbEntry = zipFile.getEntry("collection.anki2")
            val oldDbData = zipFile.getInputStream(oldDbEntry).readBytes()
            assertFalse(isZstdCompressed(oldDbData), "旧格式数据库不应该是压缩的")

            // 验证旧格式是有效的 SQLite
            val sqliteHeader = "SQLite format 3\u0000".toByteArray()
            val actualHeader = oldDbData.copyOfRange(0, 16)
            assertArrayEquals(sqliteHeader, actualHeader, "旧格式应该是有效的 SQLite 数据库")
            println("✅ 旧格式数据库验证通过")
        }

        println("🎉 双格式生成验证通过")
    }

    /**
     * 测试媒体文件处理
     */
    @Test
    fun testMediaFileHandling() {
        println("🧪 开始媒体文件处理测试")

        val creator = ApkgCreator()
        creator.setFormatVersion(ApkgCreator.FormatVersion.LATEST)

        // 创建测试数据
        val deckId = ApkgCreator.generateId()
        val deck = ApkgCreator.Deck(id = deckId, name = "媒体测试牌组")
        creator.addDeck(deck)

        val model = ApkgCreator.createWordModel() // 使用支持媒体的模型
        creator.addModel(model)

        // 添加多个媒体文件
        val audioData = "fake mp3 audio data".toByteArray()
        val imageData = "fake jpg image data".toByteArray()
        val videoData = "fake mp4 video data".toByteArray()

        creator.addMediaFile("word_audio.mp3", audioData)
        creator.addMediaFile("word_image.jpg", imageData)
        creator.addMediaFile("example_video.mp4", videoData)

        // 添加带媒体引用的笔记
        val note = ApkgCreator.Note(
            id = ApkgCreator.generateId(),
            mid = model.id,
            fields = listOf(
                "test word",
                "测试单词",
                "[sound:word_audio.mp3]",
                "This is an example sentence with <img src=\"word_image.jpg\">."
            )
        )
        creator.addNote(note, deckId)

        // 生成文件
        val outputPath = File(outputDir, "media_handling_test.apkg").absolutePath
        creator.createApkg(outputPath)
        println("📦 生成的媒体测试文件: $outputPath")

        // 验证媒体文件
        ZipFile(File(outputPath)).use { zipFile ->
            val entries = zipFile.entries().toList().map { it.name }.sorted()
            println("📋 媒体测试 ZIP 条目: $entries")

            // 验证媒体映射（JSON 数组格式）
            val mediaEntry = zipFile.getEntry("media")
            val mediaJson = zipFile.getInputStream(mediaEntry).readBytes().toString(Charsets.UTF_8)
            val mediaArray = Json.parseToJsonElement(mediaJson).jsonArray

            assertEquals(3, mediaArray.size, "应该有 3 个媒体文件")
            val mediaNames = mediaArray.map { it.jsonObject["name"]?.jsonPrimitive?.content ?: "" }
            assertTrue(mediaNames.contains("word_audio.mp3"), "应该包含 word_audio.mp3")
            assertTrue(mediaNames.contains("word_image.jpg"), "应该包含 word_image.jpg")
            assertTrue(mediaNames.contains("example_video.mp4"), "应该包含 example_video.mp4")
            println("✅ 媒体映射验证通过: $mediaJson")

            // 验证媒体文件数据
            (0..2).forEach { index ->
                val mediaFileEntry = zipFile.getEntry(index.toString())
                assertNotNull(mediaFileEntry, "媒体文件 $index 应该存在")
                val data = zipFile.getInputStream(mediaFileEntry).readBytes()
                assertTrue(data.isNotEmpty(), "媒体文件 $index 不应为空")
            }
            println("✅ 媒体文件数据验证通过")
        }

        println("🎉 媒体文件处理测试通过")
    }

    /**
     * 测试 FSRS 特性支持
     */
    @Test
    fun testFSRSFeatures() {
        println("🧪 开始 FSRS 特性测试")

        val creator = ApkgCreator()
        creator.setFormatVersion(ApkgCreator.FormatVersion.LATEST) // 只有新格式支持 FSRS

        // 创建测试数据
        val deckId = ApkgCreator.generateId()
        val deck = ApkgCreator.Deck(id = deckId, name = "FSRS 测试牌组")
        creator.addDeck(deck)

        val model = ApkgCreator.createBasicModel()
        creator.addModel(model)

        val note = ApkgCreator.Note(
            id = ApkgCreator.generateId(),
            mid = model.id,
            fields = listOf("FSRS test", "FSRS 测试")
        )
        creator.addNote(note, deckId)

        // 生成文件
        val outputPath = File(outputDir, "fsrs_features_test.apkg").absolutePath
        creator.createApkg(outputPath)
        println("📦 生成的 FSRS 测试文件: $outputPath")

        // 验证 FSRS 数据库结构
        ZipFile(File(outputPath)).use { zipFile ->
            val dbEntry = zipFile.getEntry("collection.anki21b")
            val dbData = zipFile.getInputStream(dbEntry).readBytes()

            // 解压并验证数据库
            val decompressed = Zstd.decompress(dbData, 10 * 1024 * 1024)

            // 创建临时文件来检查数据库结构
            val tempDb = File.createTempFile("fsrs_test", ".db")
            try {
                tempDb.writeBytes(decompressed)

                // 验证 FSRS 相关表和字段是否存在
                val url = "jdbc:sqlite:${tempDb.absolutePath}"
                java.sql.DriverManager.getConnection(url).use { conn ->
                    // 检查 col 表是否有 FSRS 字段
                    val colMetadata = conn.metaData.getColumns(null, null, "col", null)
                    val colColumns = mutableListOf<String>()
                    while (colMetadata.next()) {
                        colColumns.add(colMetadata.getString("COLUMN_NAME"))
                    }

                    assertTrue(colColumns.contains("fsrsWeights"), "col 表应该包含 fsrsWeights 字段")
                    assertTrue(colColumns.contains("fsrsParams5"), "col 表应该包含 fsrsParams5 字段")
                    assertTrue(colColumns.contains("desiredRetention"), "col 表应该包含 desiredRetention 字段")
                    println("✅ FSRS col 表字段验证通过")

                    // 检查 cards 表是否有 FSRS 字段
                    val cardsMetadata = conn.metaData.getColumns(null, null, "cards", null)
                    val cardsColumns = mutableListOf<String>()
                    while (cardsMetadata.next()) {
                        cardsColumns.add(cardsMetadata.getString("COLUMN_NAME"))
                    }

                    assertTrue(cardsColumns.contains("fsrsState"), "cards 表应该包含 fsrsState 字段")
                    assertTrue(cardsColumns.contains("fsrsDifficulty"), "cards 表应该包含 fsrsDifficulty 字段")
                    assertTrue(cardsColumns.contains("fsrsStability"), "cards 表应该包含 fsrsStability 字段")
                    println("✅ FSRS cards 表字段验证通过")
                }
            } finally {
                tempDb.delete()
            }
        }

        println("🎉 FSRS 特性测试通过")
    }

    /**
     * 检查数据是否使用 Zstd 压缩
     */
    private fun isZstdCompressed(data: ByteArray): Boolean {
        if (data.size < 4) return false

        // Zstd 魔术字节: 0x28B52FFD
        val magic = (data[0].toLong() and 0xFF) shl 24 or
                   ((data[1].toLong() and 0xFF) shl 16) or
                   ((data[2].toLong() and 0xFF) shl 8) or
                   (data[3].toLong() and 0xFF)

        return magic == 0x28B52FFDL
    }
}
