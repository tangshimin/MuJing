package fsrs

import fsrs.zstd.ZstdHelper
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.*
import java.io.File
import java.io.FileOutputStream
import java.sql.DriverManager
import java.time.Instant
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

/**
 * APKG 创建器
 * 用于创建 Anki 包格式文件
 * 支持多种格式：collection.anki2 (旧格式), collection.anki21 (过渡格式), collection.anki21b (新格式)
 */
class ApkgCreator {

    /**
     * APKG 格式版本
     */
    enum class FormatVersion {
        LEGACY {          // collection.anki2 (Anki 2.1.x 之前)
            override val schemaVersion = 11
            override val databaseVersion = 11
            override val useZstdCompression = false
        },
        TRANSITIONAL {    // collection.anki21 (Anki 2.1.x)
            override val schemaVersion = 11
            override val databaseVersion = 11
            override val useZstdCompression = false
        },
        LATEST {          // collection.anki21b (Anki 23.10+)
            override val schemaVersion = 18
            override val databaseVersion = 11
            override val useZstdCompression = true  // 启用 Zstd 压缩，使用 Square 库
        };
        
        abstract val schemaVersion: Int
        abstract val databaseVersion: Int
        abstract val useZstdCompression: Boolean
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
        val mod: Long = Instant.now().epochSecond,
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
                        zos.putNextEntry(ZipEntry(dbName))
                        dbFile.inputStream().use { it.copyTo(zos) }
                        zos.closeEntry()
                    }

                    // 添加 meta 文件（Anki 23.10+ 要求）
                    zos.putNextEntry(ZipEntry("meta"))
                    val metaData = createMetaData()
                    zos.write(metaData)
                    zos.closeEntry()

                    // 添加媒体映射文件
                    zos.putNextEntry(ZipEntry("media"))
                    val mediaJson = createMediaJson()
                    zos.write(mediaJson.toByteArray())
                    zos.closeEntry()

                    // 添加媒体文件（使用编号命名）
                    mediaFiles.keys.forEachIndexed { index, filename ->
                        val data = mediaFiles[filename]!!
                        zos.putNextEntry(ZipEntry(index.toString()))
                        zos.write(data)
                        zos.closeEntry()
                    }
                }
            }
        } finally {
            tempDbFiles.forEach { it.delete() }
        }
    }

    private fun createDatabase(version: FormatVersion): File {
        val suffix = when (version) {
            FormatVersion.LEGACY -> "anki2"
            FormatVersion.TRANSITIONAL -> "anki21"
            FormatVersion.LATEST -> "anki21b"
        }
        val dbFile = File.createTempFile("collection", ".$suffix")
        createDatabaseContent(dbFile, version)
        // 记录未压缩大小
        val originalSize = dbFile.length()
        // 对新格式应用 Zstd 压缩
        println("🔧 数据库压缩检查: 格式=$suffix, 需要压缩=${version.useZstdCompression}")
        if (version.useZstdCompression) {
            println("🔧 对 $suffix 格式应用 Zstd 压缩")
            val compressedFile = compressDatabaseWithZstd(dbFile)
            println("✅ Zstd 压缩完成: ${originalSize} -> ${compressedFile.length()} 字节")
            return compressedFile
        }
        println("🔧 $suffix 格式不使用压缩，返回原始文件")
        return dbFile
    }
    
    /**
     * 使用 Zstd 压缩数据库文件
     */
    private fun compressDatabaseWithZstd(dbFile: File): File {
        val compressedFile = File.createTempFile("collection", ".anki21b.zstd")
        dbFile.inputStream().use { input ->
            compressedFile.outputStream().use { output ->
                val originalData = input.readBytes()
                println("🔧 原始数据库大小: ${originalData.size} 字节")
                val compressedData = compressWithZstdJni(originalData)
                println("🔧 压缩后大小: ${compressedData.size} 字节, 压缩率: ${String.format("%.1f%%", compressedData.size.toDouble() / originalData.size * 100)}")
                if (compressedData.size >= 4) {
                    val magicBytes = compressedData.copyOfRange(0, 4)
                    val magic = (magicBytes[0].toLong() and 0xFF) shl 24 or ((magicBytes[1].toLong() and 0xFF) shl 16) or ((magicBytes[2].toLong() and 0xFF) shl 8) or (magicBytes[3].toLong() and 0xFF)
                    println("🔧 Zstd 魔术字节: 0x${magic.toString(16).uppercase()}, 期望: 0x28B52FFD")
                    println("🔧 Zstd 压缩检测: ${magic == 0x28B52FFDL}")
                    val hexBytes = magicBytes.joinToString(" ") { "%02X".format(it.toInt() and 0xFF) }
                    println("🔧 实际字节: $hexBytes")
                }
                output.write(compressedData)
            }
        }
        // 删除原始未压缩文件
        dbFile.delete()
        return compressedFile
    }
    
    /**
     * 使用 Rust zstd JNI 桥接进行压缩（确保与 Anki 完全兼容）
     */
    private fun compressWithZstdJni(data: ByteArray): ByteArray {
        try {
            println("🔧 使用 Rust zstd JNI 压缩 (级别 0)")
            println("🔧 Zstd 版本: ${ZstdHelper.getVersion()}")
            
            // 使用 Rust zstd JNI 桥接进行压缩
            val compressedData = ZstdHelper.compress(data, 0)
            
            // 验证压缩结果
            if (compressedData.isEmpty()) {
                throw RuntimeException("Zstd compression failed: empty result")
            }
            
            // 验证Zstd魔术字节
            if (compressedData.size >= 4) {
                val magic = (compressedData[0].toLong() and 0xFF) shl 24 or
                           ((compressedData[1].toLong() and 0xFF) shl 16) or
                           ((compressedData[2].toLong() and 0xFF) shl 8) or
                           (compressedData[3].toLong() and 0xFF)
                
                if (magic != 0x28B52FFDL) {
                    throw RuntimeException("Invalid Zstd magic bytes: 0x${magic.toString(16)}")
                }
            }
            
            return compressedData
        } catch (e: Exception) {
            throw RuntimeException("Zstd compression failed: ${e.message}", e)
        }
    }

    private fun createDatabaseContent(dbFile: File, version: FormatVersion) {
        val url = "jdbc:sqlite:${dbFile.absolutePath}"
        DriverManager.getConnection(url).use { conn ->
            // 创建表结构
            conn.createStatement().use { stmt ->
                // 集合表 - 根据版本使用不同的架构
                if (version.schemaVersion >= 18) {
                    // V18+ 架构 (Anki 23.10+)
                    stmt.execute("""
                        CREATE TABLE col (
                            id INTEGER PRIMARY KEY,
                            crt INTEGER NOT NULL,
                            mod INTEGER NOT NULL,
                            scm INTEGER NOT NULL,
                            ver INTEGER NOT NULL,
                            dty INTEGER NOT NULL,
                            usn INTEGER NOT NULL,
                            ls INTEGER NOT NULL,
                            conf TEXT NOT NULL,
                            models TEXT NOT NULL,
                            decks TEXT NOT NULL,
                            dconf TEXT NOT NULL,
                            tags TEXT NOT NULL,
                            -- V18 新增字段
                            fsrsWeights TEXT,
                            fsrsParams5 TEXT,
                            desiredRetention REAL,
                            ignoreRevlogsBeforeDate TEXT,
                            easyDaysPercentages TEXT,
                            stopTimerOnAnswer BOOLEAN,
                            secondsToShowQuestion REAL,
                            secondsToShowAnswer REAL,
                            questionAction INTEGER,
                            answerAction INTEGER,
                            waitForAudio BOOLEAN,
                            sm2Retention REAL,
                            weightSearch TEXT
                        )
                    """)
                } else {
                    // V11 架构 (Anki 2.1.x)
                    stmt.execute("""
                        CREATE TABLE col (
                            id INTEGER PRIMARY KEY,
                            crt INTEGER NOT NULL,
                            mod INTEGER NOT NULL,
                            scm INTEGER NOT NULL,
                            ver INTEGER NOT NULL,
                            dty INTEGER NOT NULL,
                            usn INTEGER NOT NULL,
                            ls INTEGER NOT NULL,
                            conf TEXT NOT NULL,
                            models TEXT NOT NULL,
                            decks TEXT NOT NULL,
                            dconf TEXT NOT NULL,
                            tags TEXT NOT NULL
                        )
                    """)
                }

                // 笔记表
                stmt.execute("""
                    CREATE TABLE notes (
                        id INTEGER PRIMARY KEY,
                        guid TEXT NOT NULL,
                        mid INTEGER NOT NULL,
                        mod INTEGER NOT NULL,
                        usn INTEGER NOT NULL,
                        tags TEXT NOT NULL,
                        flds TEXT NOT NULL,
                        sfld TEXT NOT NULL,
                        csum INTEGER NOT NULL,
                        flags INTEGER NOT NULL,
                        data TEXT NOT NULL
                    )
                """)

                // 卡片表
                if (version.schemaVersion >= 18) {
                    // V18+ 架构
                    stmt.execute("""
                        CREATE TABLE cards (
                            id INTEGER PRIMARY KEY,
                            nid INTEGER NOT NULL,
                            did INTEGER NOT NULL,
                            ord INTEGER NOT NULL,
                            mod INTEGER NOT NULL,
                            usn INTEGER NOT NULL,
                            type INTEGER NOT NULL,
                            queue INTEGER NOT NULL,
                            due INTEGER NOT NULL,
                            ivl INTEGER NOT NULL,
                            factor INTEGER NOT NULL,
                            reps INTEGER NOT NULL,
                            lapses INTEGER NOT NULL,
                            left INTEGER NOT NULL,
                            odue INTEGER NOT NULL,
                            odid INTEGER NOT NULL,
                            flags INTEGER NOT NULL,
                            data TEXT NOT NULL,
                            -- V18 新增字段
                            fsrsState TEXT,
                            fsrsDifficulty REAL,
                            fsrsStability REAL,
                            fsrsDue TEXT
                        )
                    """)
                } else {
                    // V11 架构
                    stmt.execute("""
                        CREATE TABLE cards (
                            id INTEGER PRIMARY KEY,
                            nid INTEGER NOT NULL,
                            did INTEGER NOT NULL,
                            ord INTEGER NOT NULL,
                            mod INTEGER NOT NULL,
                            usn INTEGER NOT NULL,
                            type INTEGER NOT NULL,
                            queue INTEGER NOT NULL,
                            due INTEGER NOT NULL,
                            ivl INTEGER NOT NULL,
                            factor INTEGER NOT NULL,
                            reps INTEGER NOT NULL,
                            lapses INTEGER NOT NULL,
                            left INTEGER NOT NULL,
                            odue INTEGER NOT NULL,
                            odid INTEGER NOT NULL,
                            flags INTEGER NOT NULL,
                            data TEXT NOT NULL
                        )
                    """)
                }

                // 删除日志表
                stmt.execute("CREATE TABLE graves (usn INTEGER NOT NULL, oid INTEGER NOT NULL, type INTEGER NOT NULL, PRIMARY KEY (oid, type)) WITHOUT ROWID")

                // 复习日志表
                if (version.schemaVersion >= 18) {
                    // V18+ 架构
                    stmt.execute("""
                        CREATE TABLE revlog (
                            id INTEGER PRIMARY KEY,
                            cid INTEGER NOT NULL,
                            usn INTEGER NOT NULL,
                            ease INTEGER NOT NULL,
                            ivl INTEGER NOT NULL,
                            lastIvl INTEGER NOT NULL,
                            factor INTEGER NOT NULL,
                            time INTEGER NOT NULL,
                            type INTEGER NOT NULL,
                            -- V18 新增字段
                            fsrsRating INTEGER,
                            fsrsReviewTime INTEGER,
                            fsrsState TEXT
                        )
                    """)
                } else {
                    // V11 架构
                    stmt.execute("""
                        CREATE TABLE revlog (
                            id INTEGER PRIMARY KEY,
                            cid INTEGER NOT NULL,
                            usn INTEGER NOT NULL,
                            ease INTEGER NOT NULL,
                            ivl INTEGER NOT NULL,
                            lastIvl INTEGER NOT NULL,
                            factor INTEGER NOT NULL,
                            time INTEGER NOT NULL,
                            type INTEGER NOT NULL
                        )
                    """)
                }

                // V18+ 新增表：媒体元数据
                if (version.schemaVersion >= 18) {
                    stmt.execute("""
                        CREATE TABLE mediaMeta (
                            dir TEXT NOT NULL,
                            fname TEXT NOT NULL,
                            csum TEXT NOT NULL,
                            mtime INTEGER NOT NULL,
                            isNew BOOLEAN NOT NULL,
                            PRIMARY KEY (dir, fname)
                        )
                    """)
                    
                    // V18+ 新增表：FSRS 权重和参数
                    stmt.execute("""
                        CREATE TABLE fsrsWeights (
                            id INTEGER PRIMARY KEY,
                            weights TEXT NOT NULL,
                            mod INTEGER NOT NULL
                        )
                    """)
                    
                    stmt.execute("""
                        CREATE TABLE fsrsParams (
                            id INTEGER PRIMARY KEY,
                            params TEXT NOT NULL,
                            mod INTEGER NOT NULL
                        )
                    """)
                }
            }

            // 插入数据
            insertData(conn, version)
        }
    }

    private fun insertData(conn: java.sql.Connection, version: FormatVersion) {
        val now = Instant.now().epochSecond

        // 插入集合配置
        val colConfig = JsonObject(mapOf(
            "nextPos" to JsonPrimitive(1),
            "estTimes" to JsonPrimitive(true),
            "activeDecks" to JsonArray(decks.keys.map { JsonPrimitive(it) }),
            "sortType" to JsonPrimitive("noteFld"),
            "timeLim" to JsonPrimitive(0),
            "sortBackwards" to JsonPrimitive(false),
            "addToCur" to JsonPrimitive(true),
            "curDeck" to JsonPrimitive(decks.keys.firstOrNull() ?: 1),
            "newBury" to JsonPrimitive(true),
            "newSpread" to JsonPrimitive(0),
            "dueCounts" to JsonPrimitive(true),
            "curModel" to JsonPrimitive(models.keys.firstOrNull() ?: 1),
            "collapseTime" to JsonPrimitive(1200)
        ))

        val modelsJson = JsonObject(models.mapKeys { it.key.toString() }.mapValues { (_, model) ->
            JsonObject(mapOf(
                "id" to JsonPrimitive(model.id),
                "name" to JsonPrimitive(model.name),
                "type" to JsonPrimitive(model.type),
                "mod" to JsonPrimitive(model.mod),
                "usn" to JsonPrimitive(model.usn),
                "sortf" to JsonPrimitive(model.sortf),
                "did" to (model.did?.let { JsonPrimitive(it) } ?: JsonNull),
                "tmpls" to JsonArray(model.tmpls.map { tmpl ->
                    JsonObject(mapOf(
                        "name" to JsonPrimitive(tmpl.name),
                        "ord" to JsonPrimitive(tmpl.ord),
                        "qfmt" to JsonPrimitive(tmpl.qfmt),
                        "afmt" to JsonPrimitive(tmpl.afmt),
                        "did" to (tmpl.did?.let { JsonPrimitive(it) } ?: JsonNull),
                        "bqfmt" to JsonPrimitive(tmpl.bqfmt),
                        "bafmt" to JsonPrimitive(tmpl.bafmt)
                    ))
                }),
                "flds" to JsonArray(model.flds.map { fld ->
                    JsonObject(mapOf(
                        "name" to JsonPrimitive(fld.name),
                        "ord" to JsonPrimitive(fld.ord),
                        "sticky" to JsonPrimitive(fld.sticky),
                        "rtl" to JsonPrimitive(fld.rtl),
                        "font" to JsonPrimitive(fld.font),
                        "size" to JsonPrimitive(fld.size)
                    ))
                }),
                "css" to JsonPrimitive(model.css)
            ))
        })

        val decksJson = JsonObject(decks.mapKeys { it.key.toString() }.mapValues { (_, deck) ->
            JsonObject(mapOf(
                "id" to JsonPrimitive(deck.id),
                "mod" to JsonPrimitive(deck.mod),
                "name" to JsonPrimitive(deck.name),
                "usn" to JsonPrimitive(deck.usn),
                "lrnToday" to JsonArray(deck.lrnToday.map { JsonPrimitive(it) }),
                "revToday" to JsonArray(deck.revToday.map { JsonPrimitive(it) }),
                "newToday" to JsonArray(deck.newToday.map { JsonPrimitive(it) }),
                "timeToday" to JsonArray(deck.timeToday.map { JsonPrimitive(it) }),
                "collapsed" to JsonPrimitive(deck.collapsed),
                "browserCollapsed" to JsonPrimitive(deck.browserCollapsed),
                "desc" to JsonPrimitive(deck.desc),
                "dyn" to JsonPrimitive(deck.dyn),
                "conf" to JsonPrimitive(deck.conf),
                "extendNew" to JsonPrimitive(deck.extendNew),
                "extendRev" to JsonPrimitive(deck.extendRev),
                "reviewLimit" to (deck.reviewLimit?.let { JsonPrimitive(it) } ?: JsonNull),
                "newLimit" to (deck.newLimit?.let { JsonPrimitive(it) } ?: JsonNull),
                "reviewLimitToday" to (deck.reviewLimitToday?.let { JsonPrimitive(it) } ?: JsonNull),
                "newLimitToday" to (deck.newLimitToday?.let { JsonPrimitive(it) } ?: JsonNull)
            ))
        })

        val dconfJson = """{
            "1": {
                "id": 1,
                "mod": 0,
                "name": "Default",
                "usn": 0,
                "maxTaken": 60,
                "autoplay": true,
                "timer": 0,
                "replayq": true,
                "new": {
                    "bury": false,
                    "delays": [1.0, 10.0],
                    "initialFactor": 2500,
                    "ints": [1, 4, 0],
                    "order": 1,
                    "perDay": 20
                },
                "rev": {
                    "bury": false,
                    "ease4": 1.3,
                    "ivlFct": 1.0,
                    "maxIvl": 36500,
                    "perDay": 200,
                    "hardFactor": 1.2
                },
                "lapse": {
                    "delays": [10.0],
                    "leechAction": 1,
                    "leechFails": 8,
                    "minInt": 1,
                    "mult": 0.0
                },
                "dyn": false,
                "newMix": 0,
                "newPerDayMinimum": 0,
                "interdayLearningMix": 0,
                "reviewOrder": 0,
                "newSortOrder": 0,
                "newGatherPriority": 0,
                "buryInterdayLearning": false,
                "fsrsWeights": [],
                "fsrsParams5": [],
                "desiredRetention": 0.9,
                "ignoreRevlogsBeforeDate": "",
                "easyDaysPercentages": [1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0],
                "stopTimerOnAnswer": false,
                "secondsToShowQuestion": 0.0,
                "secondsToShowAnswer": 0.0,
                "questionAction": 0,
                "answerAction": 0,
                "waitForAudio": true,
                "sm2Retention": 0.9,
                "weightSearch": ""
            }
        }""".trimIndent()

        if (version.schemaVersion >= 18) {
            // V18+ 架构有更多字段
            conn.prepareStatement("INSERT INTO col VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)").use { stmt ->
                stmt.setInt(1, 1)
                stmt.setLong(2, now)
                stmt.setLong(3, now)
                stmt.setLong(4, now)
                stmt.setInt(5, version.databaseVersion)
                stmt.setInt(6, 0)
                stmt.setInt(7, 0)
                stmt.setLong(8, now)
                stmt.setString(9, colConfig.toString())
                stmt.setString(10, modelsJson.toString())
                stmt.setString(11, decksJson.toString())
                stmt.setString(12, dconfJson)
                stmt.setString(13, "{}")
                // V18 新增字段
                stmt.setString(14, "[]")  // fsrsWeights
                stmt.setString(15, "[]")  // fsrsParams5
                stmt.setDouble(16, 0.9)    // desiredRetention
                stmt.setString(17, "")     // ignoreRevlogsBeforeDate
                stmt.setString(18, "[1.0,1.0,1.0,1.0,1.0,1.0,1.0]")  // easyDaysPercentages
                stmt.setBoolean(19, false) // stopTimerOnAnswer
                stmt.setDouble(20, 0.0)    // secondsToShowQuestion
                stmt.setDouble(21, 0.0)    // secondsToShowAnswer
                stmt.setInt(22, 0)         // questionAction
                stmt.setInt(23, 0)         // answerAction
                stmt.setBoolean(24, true)  // waitForAudio
                stmt.setDouble(25, 0.9)    // sm2Retention
                stmt.setString(26, "")     // weightSearch
                stmt.executeUpdate()
            }
        } else {
            // V11 架构
            conn.prepareStatement("INSERT INTO col VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)").use { stmt ->
                stmt.setInt(1, 1)
                stmt.setLong(2, now)
                stmt.setLong(3, now)
                stmt.setLong(4, now)
                stmt.setInt(5, version.databaseVersion)
                stmt.setInt(6, 0)
                stmt.setInt(7, 0)
                stmt.setLong(8, now)
                stmt.setString(9, colConfig.toString())
                stmt.setString(10, modelsJson.toString())
                stmt.setString(11, decksJson.toString())
                stmt.setString(12, dconfJson)
                stmt.setString(13, "{}")
                stmt.executeUpdate()
            }
        }

        // 插入笔记
        conn.prepareStatement("INSERT INTO notes VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)").use { stmt ->
            notes.forEach { note ->
                stmt.setLong(1, note.id)
                stmt.setString(2, note.guid)
                stmt.setLong(3, note.mid)
                stmt.setLong(4, now)
                stmt.setInt(5, -1)
                stmt.setString(6, note.tags)
                // 确保字段分隔符正确
                val fieldsString = note.fields.joinToString("\u001f")
                stmt.setString(7, fieldsString)
                stmt.setInt(8, (note.fields.firstOrNull() ?: "").hashCode() and 0x7FFFFFFF)
                stmt.setLong(9, fieldsString.hashCode().toLong() and 0x7FFFFFFF) // 确保为正数
                stmt.setInt(10, 0)
                stmt.setString(11, "")
                stmt.addBatch()
            }
            stmt.executeBatch()
        }

        // 插入卡片
        if (version.schemaVersion >= 18) {
            // V18+ 架构有 FSRS 字段
            conn.prepareStatement("INSERT INTO cards VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)").use { stmt ->
                cards.forEach { card ->
                    stmt.setLong(1, card.id)
                    stmt.setLong(2, card.nid)
                    stmt.setLong(3, card.did)
                    stmt.setInt(4, card.ord)
                    stmt.setLong(5, now)
                    stmt.setInt(6, -1)
                    stmt.setInt(7, card.type)
                    stmt.setInt(8, card.queue)
                    stmt.setInt(9, card.due)
                    stmt.setInt(10, card.ivl)
                    stmt.setInt(11, card.factor)
                    stmt.setInt(12, card.reps)
                    stmt.setInt(13, card.lapses)
                    stmt.setInt(14, card.left)
                    stmt.setInt(15, 0)
                    stmt.setInt(16, 0)
                    stmt.setInt(17, 0)
                    stmt.setString(18, "")
                    // V18 新增 FSRS 字段
                    stmt.setString(19, "")  // fsrsState
                    stmt.setDouble(20, 0.0) // fsrsDifficulty
                    stmt.setDouble(21, 0.0) // fsrsStability
                    stmt.setString(22, "")  // fsrsDue
                    stmt.addBatch()
                }
                stmt.executeBatch()
            }
        } else {
            // V11 架构
            conn.prepareStatement("INSERT INTO cards VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)").use { stmt ->
                cards.forEach { card ->
                    stmt.setLong(1, card.id)
                    stmt.setLong(2, card.nid)
                    stmt.setLong(3, card.did)
                    stmt.setInt(4, card.ord)
                    stmt.setLong(5, now)
                    stmt.setInt(6, -1)
                    stmt.setInt(7, card.type)
                    stmt.setInt(8, card.queue)
                    stmt.setInt(9, card.due)
                    stmt.setInt(10, card.ivl)
                    stmt.setInt(11, card.factor)
                    stmt.setInt(12, card.reps)
                    stmt.setInt(13, card.lapses)
                    stmt.setInt(14, card.left)
                    stmt.setInt(15, 0)
                    stmt.setInt(16, 0)
                    stmt.setInt(17, 0)
                    stmt.setString(18, "")
                    stmt.addBatch()
                }
                stmt.executeBatch()
            }
        }

        // V18+ 插入媒体元数据
        if (version.schemaVersion >= 18) {
            conn.prepareStatement("INSERT INTO mediaMeta VALUES (?, ?, ?, ?, ?)").use { stmt ->
                mediaFiles.keys.forEach { filename ->
                    stmt.setString(1, "")  // dir
                    stmt.setString(2, filename)
                    stmt.setString(3, "")  // csum (需要计算实际校验和)
                    stmt.setLong(4, now)
                    stmt.setBoolean(5, true)  // isNew
                    stmt.addBatch()
                }
                stmt.executeBatch()
            }
        }
    }

    private fun createMediaJson(): String {
        // V18+ (LATEST) 使用新的数组格式: [{"id":0,"name":"file"}, ...]
        return if (formatVersion.schemaVersion >= 18) {
            buildJsonArray {
                mediaFiles.keys.forEachIndexed { index, filename ->
                    add(buildJsonObject {
                        put("id", index)
                        put("name", filename)
                    })
                }
            }.toString()
        } else {
            // 旧格式: {"0":"file"}
            val mediaMap = mediaFiles.keys.mapIndexed { index, filename ->
                index.toString() to filename
            }.toMap()
            Json.encodeToString(mediaMap)
        }
    }
    
    /**
     * 创建 meta 文件数据（Anki 23.10+ 要求）
     * meta 文件包含包版本信息，使用正确的 protobuf 编码
     */
    private fun createMetaData(): ByteArray {
        // 对于Anki 24.11，meta文件应该使用正确的 protobuf 编码
        val versionValue = when (formatVersion) {
            FormatVersion.LEGACY -> 1      // VERSION_LEGACY_1
            FormatVersion.TRANSITIONAL -> 2 // VERSION_LEGACY_2
            FormatVersion.LATEST -> 3       // VERSION_LATEST
        }
        
        // 正确的 protobuf 编码：字段1 (version)，wire type 0 (varint)
        // 协议：message PackageMetadata { Version version = 1; }
        // Version enum: UNKNOWN=0, LEGACY_1=1, LEGACY_2=2, LATEST=3
        
        // 编码字段编号和类型: (field_number << 3) | wire_type
        // field_number = 1, wire_type = 0 (varint) → 0x08
        val fieldTag: Byte = 0x08
        
        // 编码 varint 值
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
