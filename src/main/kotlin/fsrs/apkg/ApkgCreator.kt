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

import fsrs.zstd.ZstdNative
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.security.MessageDigest
import java.time.Instant
import java.util.zip.CRC32
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

/**
 * APKG 创建器
 * 支持所有格式版本
 */
class ApkgCreator {

    /**
     * 创建上下文
     */
    private data class CreateContext(
        val format: ApkgFormat,
        val meta: ApkgMeta
    )


    companion object {
        private var nextId = System.currentTimeMillis()

        fun generateId(): Long = ++nextId
        fun generateGuid(): String = "${System.currentTimeMillis()}.${(Math.random() * 1000000).toInt()}"

        /**
         * 创建基本的单词学习模型
         */
        fun createBasicModel(): Model {
            val modelId = generateId()
            return Model(
                id = modelId,
                name = "Basic",
                templates = listOf(
                    CardTemplate(
                        name = "Card 1",
                        ordinal = 0,
                        questionFormat = "{{Front}}",
                        answerFormat = "{{FrontSide}}\n\n<hr id=answer>\n\n{{Back}}"
                    )
                ),
                fields = listOf(
                    Field(name = "Front", ordinal = 0),
                    Field(name = "Back", ordinal = 1)
                )
            )
        }

        /**
         * 创建单词学习专用模型（支持音频）
         */
        fun createWordModel(): Model {
            val modelId = generateId()
            return Model(
                id = modelId,
                name = "Word Learning",
                templates = listOf(
                    CardTemplate(
                        name = "Recognition",
                        ordinal = 0,
                        questionFormat = "{{Word}}\n{{#Audio}}{{Audio}}{{/Audio}}",
                        answerFormat = "{{FrontSide}}\n\n<hr id=answer>\n\n{{Meaning}}\n{{#Example}}{{Example}}{{/Example}}"
                    ),
                    CardTemplate(
                        name = "Recall",
                        ordinal = 1,
                        questionFormat = "{{Meaning}}",
                        answerFormat = "{{FrontSide}}\n\n<hr id=answer>\n\n{{Word}}\n{{#Audio}}{{Audio}}{{/Audio}}\n{{#Example}}{{Example}}{{/Example}}"
                    )
                ),
                fields = listOf(
                    Field(name = "Word", ordinal = 0),
                    Field(name = "Meaning", ordinal = 1),
                    Field(name = "Audio", ordinal = 2),
                    Field(name = "Example", ordinal = 3)
                ),
                css = """
                    .card {
                        font-family: Arial, sans-serif;
                        font-size: 20px;
                        text-align: center;
                        color: black;
                        background-color: white;
                        padding: 20px;
                    }
                    
                    .word {
                        font-size: 24px;
                        font-weight: bold;
                        color: #2196F3;
                        margin-bottom: 10px;
                    }
                    
                    .meaning {
                        font-size: 18px;
                        color: #333;
                        margin: 10px 0;
                    }
                    
                    .example {
                        font-style: italic;
                        color: #666;
                        margin-top: 15px;
                    }
                """.trimIndent()
            )
        }
    }

    private val notes = mutableListOf<Note>()
    private val cards = mutableListOf<Card>()
    private val decks = mutableMapOf<Long, Deck>()
    private val models = mutableMapOf<Long, Model>()
    private val mediaFiles = mutableMapOf<String, ByteArray>()
    private var format: ApkgFormat = ApkgFormat.LEGACY
    private val databaseCreator = ApkgDatabaseCreator()

    /**
     * 添加牌组
     */
    fun addDeck(deck: Deck): ApkgCreator {
        decks[deck.id] = deck
        return this
    }

    /**
     * 添加模型
     */
    fun addModel(model: Model): ApkgCreator {
        models[model.id] = model
        return this
    }

    /**
     * 添加笔记和卡片
     */
    fun addNote(note: Note, deckId: Long): ApkgCreator {
        notes.add(note)

        // 为笔记创建卡片（根据模型的模板数量）
        val model = models[note.modelId] ?: throw IllegalArgumentException("Model not found: ${note.modelId}")
        model.templates.forEachIndexed { index, _ ->
            cards.add(
                Card(
                    id = generateId(),
                    noteId = note.id,
                    deckId = deckId,
                    templateOrdinal = index
                )
            )
        }
        return this
    }

    /**
     * 添加媒体文件
     */
    fun addMediaFile(filename: String, data: ByteArray): ApkgCreator {
        mediaFiles[filename] = data
        return this
    }

    /**
     * 设置 APKG 格式版本
     */
    fun setFormat(format: ApkgFormat): ApkgCreator {
        this.format = format
        return this
    }

    /**
     * 创建 APKG 文件
     * @param outputPath 输出文件路径
     * @param dualFormat 是否同时生成新旧两种格式（默认 false）
     */
    fun createApkg(outputPath: String, dualFormat: Boolean = false) {
        val context = CreateContext(format, createMetaForFormat(format))
        val tempDbFiles = mutableListOf<File>()
        try {
            // 创建 SQLite 数据库
            val dbFilesWithFormat = if (dualFormat) {
                // 生成双格式：旧格式和新格式
                listOf(
                    createDatabase(ApkgFormat.LEGACY) to ApkgFormat.LEGACY,
                    createDatabase(ApkgFormat.LATEST) to ApkgFormat.LATEST
                )
            } else {
                // 生成单格式
                listOf(createDatabase(format) to format)
            }
            tempDbFiles.addAll(dbFilesWithFormat.map { it.first })

            // 构建规范化后的媒体清单，确保名称安全唯一
            val mediaList: List<Pair<String, ByteArray>> = buildNormalizedMediaList(context)

            // 创建 ZIP 文件
            FileOutputStream(outputPath).use { fos ->
                ZipOutputStream(fos).use { zos ->
                    // 添加数据库文件
                    dbFilesWithFormat.forEach { (dbFile, dbFormat) ->
                        val dbName = dbFormat.databaseFileName
                        println("🔧 数据库文件检测: 原始文件名=${dbFile.name}, ZIP 条目名=$dbName")

                        // 使用 STORED 方式写入数据库，避免 ZIP 再压缩
                        val bytes = dbFile.readBytes()
                        val crc32 = CRC32()
                        crc32.update(bytes)
                        val entry = ZipEntry(dbName).apply {
                            method = ZipEntry.STORED
                            size = bytes.size.toLong()
                            crc = crc32.value
                        }
                        zos.putNextEntry(entry)
                        zos.write(bytes)
                        zos.closeEntry()
                    }

                    // 添加 meta 文件（Anki 23.10+ 要求）
                    zos.putNextEntry(ZipEntry("meta"))
                    zos.write(context.meta.toByteArray())
                    zos.closeEntry()

                    // 添加媒体映射文件
                    zos.putNextEntry(ZipEntry("media"))
                    val mediaBytes = createMediaFileData(context, mediaList)
                    zos.write(mediaBytes)
                    zos.closeEntry()

                    // 添加媒体文件（使用编号命名）
                    mediaList.forEachIndexed { index, pair ->
                        val (_, data) = pair
                        zos.putNextEntry(ZipEntry(index.toString()))
                        val toWrite = if (context.format.useZstdCompression) {
                            // LATEST: 每个媒体文件内容单独用 Zstd 压缩
                            ZstdNative().compress(data, 0)
                        } else {
                            data
                        }
                        zos.write(toWrite)
                        zos.closeEntry()
                    }
                }
            }
        } finally {
            tempDbFiles.forEach { it.delete() }
        }
    }

    // 创建 SQLite 数据库文件（使用 ApkgDatabaseCreator）
    private fun createDatabase(format: ApkgFormat): File {
        return databaseCreator.createDatabase(
            format = format,
            notes = notes,
            cards = cards,
            decks = decks,
            models = models,
            mediaFiles = mediaFiles
        )
    }




    // 基于当前格式需要，构建安全、唯一的媒体清单（name,data）
    private fun buildNormalizedMediaList(context: CreateContext): List<Pair<String, ByteArray>> {
        if (mediaFiles.isEmpty()) return emptyList()
        val normalized = mutableListOf<Pair<String, ByteArray>>()
        val used = mutableSetOf<String>()
        mediaFiles.forEach { (origName, data) ->
            var name = if (context.format.schemaVersion >= 18) normalizeFilename(origName) else origName
            if (name.isEmpty()) name = "media_${System.nanoTime()}"
            // 去重：如重名，追加短哈希后缀
            if (name in used) {
                val short = sha1Bytes(data).joinToString("") { "%02x".format(it) }.take(8)
                var candidate = addSuffixBeforeExtension(name, "-$short")
                var i = 1
                while (candidate in used) {
                    candidate = addSuffixBeforeExtension(name, "-$short-$i")
                    i++
                }
                name = candidate
            }
            used.add(name)
            normalized.add(name to data)
        }
        return normalized
    }

    private fun addSuffixBeforeExtension(name: String, suffix: String): String {
        val idx = name.lastIndexOf('.')
        return if (idx > 0) {
            name.substring(0, idx) + suffix + name.substring(idx)
        } else name + suffix
    }

    // 近似对齐 Anki 的安全文件名规则（避免目录穿越、非法字符、Windows 保留名等）
    private fun normalizeFilename(input: String): String {
        var s = input.replace('\\', '/').replace('/', '_')
        s = s.replace(Regex("[\\n\\r\\t\\u0000]"), "")
        s = s.replace(Regex("[:*?\"<>|]"), "_")
        // 去掉尾随空格与点，避免 Windows 问题
        s = s.trim().trimEnd('.', ' ')
        if (s.isEmpty()) return s
        // 防止隐藏路径组件
        if (s.startsWith("../") || s.startsWith("..")) s = s.replace("..", "_")
        // Windows 保留名处理（不区分大小写，含扩展名也不允许）
        val lower = s.lowercase()
        val dot = lower.indexOf('.')
        val stem = if (dot >= 0) lower.substring(0, dot) else lower
        val reserved = setOf(
            "con","prn","aux","nul",
            "com1","com2","com3","com4","com5","com6","com7","com8","com9",
            "lpt1","lpt2","lpt3","lpt4","lpt5","lpt6","lpt7","lpt8","lpt9"
        )
        if (stem in reserved) s += "_"
        if (s.length > 255) s = s.take(255)
        return s
    }

    /**
     * 创建媒体文件数据，遵循 Anki 的格式要求
     */
    private fun createMediaFileData(context: CreateContext, mediaList: List<Pair<String, ByteArray>>): ByteArray {
        return if (context.format.useZstdCompression) {
            // LATEST: Protobuf(MediaEntries) + Zstd 压缩
            val entriesBytes = buildMediaEntriesProtobuf(mediaList)
            ZstdNative().compress(entriesBytes, 0)
        } else {
            // 旧格式：JSON（未压缩）
            createLegacyMediaJson(mediaList).toByteArray()
        }
    }

    private fun createLegacyMediaJson(mediaList: List<Pair<String, ByteArray>>): String {
        val map = mediaList.mapIndexed { index, (name, _) -> index.toString() to name }.toMap()
        return Json.encodeToString(map)
    }

    // 构建 LATEST 所需的 Protobuf(MediaEntries)
    private fun buildMediaEntriesProtobuf(mediaList: List<Pair<String, ByteArray>>): ByteArray {
        val out = ByteArrayOutputStream()
        // MediaEntries: field 1 (entries), wire type 2 (length-delimited)
        mediaList.forEach { (filename, data) ->
            val entryBytes = encodeMediaEntry(
                name = filename,
                size = data.size,
                sha1 = sha1Bytes(data)
            )
            out.write(0x0A) // tag for field 1, wire type 2
            writeVarint(out, entryBytes.size.toLong())
            out.write(entryBytes)
        }
        return out.toByteArray()
    }

    // 编码单个 MediaEntry 子消息: name=1(string), size=2(uint32), sha1=3(bytes)
    private fun encodeMediaEntry(name: String, size: Int, sha1: ByteArray): ByteArray {
        val out = ByteArrayOutputStream()
        val nameBytes = name.toByteArray(Charsets.UTF_8)
        // field 1: name (length-delimited)
        out.write(0x0A)
        writeVarint(out, nameBytes.size.toLong())
        out.write(nameBytes)
        // field 2: size (varint)
        out.write(0x10)
        writeVarint(out, size.toLong() and 0xFFFFFFFFL)
        // field 3: sha1 (length-delimited)
        out.write(0x1A)
        writeVarint(out, sha1.size.toLong())
        out.write(sha1)
        return out.toByteArray()
    }

    /**
     * 根据格式创建对应的 meta 数据
     */
    private fun createMetaForFormat(format: ApkgFormat): ApkgMeta {
        val version = when (format) {
            ApkgFormat.LEGACY -> 1
            ApkgFormat.TRANSITIONAL -> 2
            ApkgFormat.LATEST -> 3
        }
        return ApkgMeta(version)
    }

    private fun sha1Bytes(data: ByteArray): ByteArray {
        val md = MessageDigest.getInstance("SHA-1")
        return md.digest(data)
    }

    // Protobuf varint 编码（无符号）
    private fun writeVarint(out: ByteArrayOutputStream, value: Long) {
        var v = value
        while (true) {
            if ((v and -128L) == 0L) {
                out.write(v.toInt())
                return
            }
            out.write(((v and 0x7FL) or 0x80L).toInt())
            v = v ushr 7
        }
    }

}



data class Note(
    val id: Long,
    val modelId: Long,
    val fields: List<String>,
    val tags: String = "",
    val guid: String = ApkgCreator.Companion.generateGuid()
)

data class Card(
    val id: Long,
    val noteId: Long,
    val deckId: Long,
    val templateOrdinal: Int,
    val cardType: Int = 0, // 0=new, 1=learning, 2=review
    val queueType: Int = 0, // same as type
    val dueTime: Int = 1,
    val interval: Int = 0,
    val easeFactor: Int = 2500,
    val repetitions: Int = 0,
    val lapses: Int = 0,
    val remainingSteps: Int = 0
)

data class Deck(
    val id: Long,
    val modificationTime: Long = 0,
    val name: String,
    val updateSequenceNumber: Int = 0,
    val learnToday: List<Int> = listOf(0, 0),
    val reviewToday: List<Int> = listOf(0, 0),
    val newToday: List<Int> = listOf(0, 0),
    val timeToday: List<Int> = listOf(0, 0),
    val collapsed: Boolean = false,
    val browserCollapsed: Boolean = true,
    val description: String = "",
    val isDynamic: Int = 0,
    val configurationId: Long = 1,
    val extendNew: Int = 0,
    val extendRev: Int = 0,
    val reviewLimit: Int? = null,
    val newLimit: Int? = null,
    val reviewLimitToday: Int? = null,
    val newLimitToday: Int? = null
)

data class Model(
    val id: Long,
    val name: String,
    val type: Int = 0,
    val modificationTime: Long = Instant.now().epochSecond,
    val updateSequenceNumber: Int = -1,
    val sortField: Int = 0,
    val deckId: Long? = null,
    val templates: List<CardTemplate>,
    val fields: List<Field>,
    val css: String = ".card {\n font-family: arial;\n font-size: 20px;\n text-align: center;\n color: black;\n background-color: white;\n}"
)

data class CardTemplate(
    val name: String,
    val ordinal: Int,
    val questionFormat: String,
    val answerFormat: String,
    val deckId: Long? = null,
    val browserQuestionFormat: String = "",
    val browserAnswerFormat: String = ""
)

data class Field(
    val name: String,
    val ordinal: Int,
    val sticky: Boolean = false,
    val rightToLeft: Boolean = false,
    val font: String = "Arial",
    val size: Int = 20
)

data class MediaFile(
    val index: Int,
    val filename: String,
    val data: ByteArray,
    val size: Int? = null,
    val sha1: ByteArray? = null
)