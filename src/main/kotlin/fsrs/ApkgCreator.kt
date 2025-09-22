package fsrs


import fsrs.zstd.ZstdNative
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File
import java.io.FileOutputStream
import java.security.MessageDigest
import java.util.zip.CRC32
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

/**
 * APKG 创建器
 * 用于创建 Anki 包格式文件
 * 支持多种格式：collection.anki2 (旧格式), collection.anki21 (过渡格式), collection.anki21b (新格式)
 */
class ApkgCreator {

    /**
     * APKG 格式版本（使用共享的 ApkgFormat 枚举）
     */
    enum class FormatVersion {
        LEGACY {          // collection.anki2 (Anki 2.1.x 之前)
            override val apkgFormat = ApkgFormat.LEGACY
        },
        TRANSITIONAL {    // collection.anki21 (Anki 2.1.x)
            override val apkgFormat = ApkgFormat.TRANSITIONAL
        },
        LATEST {          // collection.anki21b (Anki 23.10+)
            override val apkgFormat = ApkgFormat.LATEST
        };
        
        abstract val apkgFormat: ApkgFormat
        
        val schemaVersion: Int get() = apkgFormat.schemaVersion
        val databaseVersion: Int get() = apkgFormat.databaseVersion
        val useZstdCompression: Boolean get() = apkgFormat.useZstdCompression
        val databaseFileName: String get() = apkgFormat.databaseFileName
    }

    data class Note(
        val id: Long,
        val mid: Long, // model id
        val fields: List<String>,
        val tags: String = "",
        val guid: String = generateGuid()
    )

    data class Card(
        val id: Long,
        val nid: Long, // note id
        val did: Long, // deck id
        val ord: Int, // ordinal (card template)
        val type: Int = 0, // 0=new, 1=learning, 2=review
        val queue: Int = 0, // same as type
        val due: Int = 1,
        val ivl: Int = 0, // interval
        val factor: Int = 2500, // ease factor
        val reps: Int = 0, // repetitions
        val lapses: Int = 0,
        val left: Int = 0
    )

    data class Deck(
        val id: Long,
        val mod: Long = 0,
        val name: String,
        val usn: Int = 0,
        val lrnToday: List<Int> = listOf(0, 0),
        val revToday: List<Int> = listOf(0, 0),
        val newToday: List<Int> = listOf(0, 0),
        val timeToday: List<Int> = listOf(0, 0),
        val collapsed: Boolean = false,
        val browserCollapsed: Boolean = true,
        val desc: String = "",
        val dyn: Int = 0,
        val conf: Long = 1,
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
        val mod: Long = java.time.Instant.now().epochSecond,
        val usn: Int = -1,
        val sortf: Int = 0,
        val did: Long? = null,
        val tmpls: List<CardTemplate>,
        val flds: List<Field>,
        val css: String = ".card {\n font-family: arial;\n font-size: 20px;\n text-align: center;\n color: black;\n background-color: white;\n}"
    )

    data class CardTemplate(
        val name: String,
        val ord: Int,
        val qfmt: String, // question format
        val afmt: String, // answer format
        val did: Long? = null,
        val bqfmt: String = "",
        val bafmt: String = ""
    )

    data class Field(
        val name: String,
        val ord: Int,
        val sticky: Boolean = false,
        val rtl: Boolean = false,
        val font: String = "Arial",
        val size: Int = 20
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
                tmpls = listOf(
                    CardTemplate(
                        name = "Card 1",
                        ord = 0,
                        qfmt = "{{Front}}",
                        afmt = "{{FrontSide}}\n\n<hr id=answer>\n\n{{Back}}"
                    )
                ),
                flds = listOf(
                    Field(name = "Front", ord = 0),
                    Field(name = "Back", ord = 1)
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
                tmpls = listOf(
                    CardTemplate(
                        name = "Recognition",
                        ord = 0,
                        qfmt = "{{Word}}\n{{#Audio}}{{Audio}}{{/Audio}}",
                        afmt = "{{FrontSide}}\n\n<hr id=answer>\n\n{{Meaning}}\n{{#Example}}{{Example}}{{/Example}}"
                    ),
                    CardTemplate(
                        name = "Recall",
                        ord = 1,
                        qfmt = "{{Meaning}}",
                        afmt = "{{FrontSide}}\n\n<hr id=answer>\n\n{{Word}}\n{{#Audio}}{{Audio}}{{/Audio}}\n{{#Example}}{{Example}}{{/Example}}"
                    )
                ),
                flds = listOf(
                    Field(name = "Word", ord = 0),
                    Field(name = "Meaning", ord = 1),
                    Field(name = "Audio", ord = 2),
                    Field(name = "Example", ord = 3)
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
    private var formatVersion: FormatVersion = FormatVersion.LEGACY
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
        val model = models[note.mid] ?: throw IllegalArgumentException("Model not found: ${note.mid}")
        model.tmpls.forEachIndexed { index, _ ->
            cards.add(
                Card(
                    id = generateId(),
                    nid = note.id,
                    did = deckId,
                    ord = index
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
    fun setFormatVersion(version: FormatVersion): ApkgCreator {
        formatVersion = version
        return this
    }

    /**
     * 创建 APKG 文件
     * @param outputPath 输出文件路径
     * @param dualFormat 是否同时生成新旧两种格式（默认 false）
     */
    fun createApkg(outputPath: String, dualFormat: Boolean = false) {
        val tempDbFiles = mutableListOf<File>()
        try {
            // 创建 SQLite 数据库
            val dbFiles = if (dualFormat) {
                // 生成双格式：旧格式和新格式
                listOf(
                    createDatabase(FormatVersion.LEGACY),
                    createDatabase(FormatVersion.LATEST)
                )
            } else {
                // 生成单格式
                listOf(createDatabase(formatVersion))
            }
            tempDbFiles.addAll(dbFiles)

            // 构建规范化后的媒体清单，确保名称安全唯一
            val mediaList: List<Pair<String, ByteArray>> = buildNormalizedMediaList()

            // 创建 ZIP 文件
            FileOutputStream(outputPath).use { fos ->
                ZipOutputStream(fos).use { zos ->
                    // 添加数据库文件
                    dbFiles.forEach { dbFile ->
                        val dbName = when {
                            dbFile.name.contains("anki21b") && !dbFile.name.endsWith(".zstd") -> "collection.anki21b"
                            dbFile.name.contains("anki21b.zstd") -> "collection.anki21b"
                            dbFile.name.contains("anki21") -> "collection.anki21"
                            else -> "collection.anki2"
                        }
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
                    val metaData = createMetaData()
                    zos.write(metaData)
                    zos.closeEntry()

                    // 添加媒体映射文件
                    zos.putNextEntry(ZipEntry("media"))
                    val mediaBytes = if (formatVersion == FormatVersion.LATEST) {
                        // LATEST: Protobuf(MediaEntries) + Zstd 压缩
                        val entriesBytes = buildMediaEntriesProtobuf(mediaList)
                        ZstdNative().compress(entriesBytes, 0)
                    } else {
                        // 旧格式：JSON（未压缩）
                        createLegacyMediaJson(mediaList).toByteArray()
                    }
                    zos.write(mediaBytes)
                    zos.closeEntry()

                    // 添加媒体文件（使用编号命名）
                    mediaList.forEachIndexed { index, pair ->
                        val (_, data) = pair
                        zos.putNextEntry(ZipEntry(index.toString()))
                        val toWrite = if (formatVersion == FormatVersion.LATEST) {
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
    private fun createDatabase(version: FormatVersion): File {
        return databaseCreator.createDatabase(
            format = version.apkgFormat,
            notes = notes,
            cards = cards,
            decks = decks,
            models = models,
            mediaFiles = mediaFiles
        )
    }




    // 基于当前 formatVersion 需要，构建安全、唯一的媒体清单（name,data）
    private fun buildNormalizedMediaList(): List<Pair<String, ByteArray>> {
        if (mediaFiles.isEmpty()) return emptyList()
        val normalized = mutableListOf<Pair<String, ByteArray>>()
        val used = mutableSetOf<String>()
        mediaFiles.forEach { (origName, data) ->
            var name = if (formatVersion.schemaVersion >= 18) normalizeFilename(origName) else origName
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

    private fun createLegacyMediaJson(mediaList: List<Pair<String, ByteArray>>): String {
        val map = mediaList.mapIndexed { index, (name, _) -> index.toString() to name }.toMap()
        return Json.encodeToString(map)
    }

    // 构建 LATEST 所需的 Protobuf(MediaEntries) 并用 Zstd 压缩
    private fun buildMediaEntriesProtobuf(mediaList: List<Pair<String, ByteArray>>): ByteArray {
        val out = java.io.ByteArrayOutputStream()
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
        val out = java.io.ByteArrayOutputStream()
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

    private fun sha1Bytes(data: ByteArray): ByteArray {
        val md = MessageDigest.getInstance("SHA-1")
        return md.digest(data)
    }

    // Protobuf varint 编码（无符号）
    private fun writeVarint(out: java.io.ByteArrayOutputStream, value: Long) {
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

    /**
     * 创建 meta 文件数据（Anki 23.10+ 要求）
     * meta 文件包含包版本信息，使用正确的 protobuf 编码
     */
    private fun createMetaData(): ByteArray {
        // 对于Anki 24.11，meta文件应该使用正确的 protobuf 编码
        val versionValue = when (formatVersion) {
            FormatVersion.LEGACY -> 1      // LEGACY_1 → collection.anki2
            FormatVersion.TRANSITIONAL -> 2 // LEGACY_2 → collection.anki21
            FormatVersion.LATEST -> 3       // LATEST   → collection.anki21b
        }
        
        // 正确的 protobuf 编码：字段1 (version)，wire type 0 (varint)
        val fieldTag: Byte = 0x08
        val versionBytes = encodeVarint(versionValue.toLong())
        return byteArrayOf(fieldTag) + versionBytes
    }
    
    /**
     * 编码 varint 值
     */
    private fun encodeVarint(value: Long): ByteArray {
        val result = mutableListOf<Byte>()
        var v = value
        
        while (v >= 0x80) {
            result.add(((v and 0x7F) or 0x80).toByte())
            v = v ushr 7
        }
        result.add(v.toByte())
        
        return result.toByteArray()
    }
}
