package fsrs


import fsrs.zstd.ZstdNative
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.*
import java.io.File
import java.io.FileOutputStream
import java.security.MessageDigest
import java.sql.DriverManager
import java.time.Instant
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import java.util.zip.CRC32

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

    // 创建 SQLite 数据库文件（根据版本决定是否 zstd 压缩）
    private fun createDatabase(version: FormatVersion): File {
        val suffix = when (version) {
            FormatVersion.LEGACY -> "anki2"
            FormatVersion.TRANSITIONAL -> "anki21"
            FormatVersion.LATEST -> "anki21b"
        }
        val dbFile = File.createTempFile("collection", ".$suffix")
        createDatabaseContent(dbFile, version)
        if (version.useZstdCompression) {
            return compressDatabaseWithZstd(dbFile)
        }
        return dbFile
    }

    // 使用 zstd 压缩 SQLite 数据库内容（输出 .anki21b.zstd）
    private fun compressDatabaseWithZstd(dbFile: File): File {
        val compressedFile = File.createTempFile("collection", ".anki21b.zstd")
        dbFile.inputStream().use { input ->
            compressedFile.outputStream().use { output ->
                val originalData = input.readBytes()
                val compressedData = compressWithZstdJni(originalData)
                output.write(compressedData)
            }
        }
        dbFile.delete()
        return compressedFile
    }

    private fun compressWithZstdJni(data: ByteArray): ByteArray {
        return ZstdNative().compress(data, 0)
    }

    // 初始化数据库结构并插入基础数据
    private fun createDatabaseContent(dbFile: File, version: FormatVersion) {
        val url = "jdbc:sqlite:${dbFile.absolutePath}"
        DriverManager.getConnection(url).use { conn ->
            conn.createStatement().use { stmt ->
                stmt.execute("PRAGMA user_version = ${version.schemaVersion}")
                if (version.schemaVersion >= 18) {
                    stmt.execute(
                        """
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
                        """
                    )
                } else {
                    stmt.execute(
                        """
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
                        """
                    )
                }

                stmt.execute(
                    """
                    CREATE TABLE notes (
                        id INTEGER PRIMARY KEY,
                        guid TEXT NOT NULL,
                        mid INTEGER NOT NULL,
                        mod INTEGER NOT NULL,
                        usn INTEGER NOT NULL,
                        tags TEXT NOT NULL,
                        flds TEXT NOT NULL,
                        sfld INTEGER NOT NULL,
                        csum INTEGER NOT NULL,
                        flags INTEGER NOT NULL,
                        data TEXT NOT NULL
                    )
                    """
                )

                if (version.schemaVersion >= 18) {
                    stmt.execute(
                        """
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
                            fsrsState TEXT,
                            fsrsDifficulty REAL,
                            fsrsStability REAL,
                            fsrsDue TEXT
                        )
                        """
                    )
                } else {
                    stmt.execute(
                        """
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
                        """
                    )
                }

                stmt.execute("CREATE TABLE graves (usn INTEGER NOT NULL, oid INTEGER NOT NULL, type INTEGER NOT NULL, PRIMARY KEY (oid, type)) WITHOUT ROWID")

                if (version.schemaVersion >= 18) {
                    stmt.execute(
                        """
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
                            fsrsRating INTEGER,
                            fsrsReviewTime INTEGER,
                            fsrsState TEXT
                        )
                        """
                    )
                } else {
                    stmt.execute(
                        """
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
                        """
                    )
                }

                if (version.schemaVersion >= 18) {
                    stmt.execute(
                        """
                        CREATE TABLE mediaMeta (
                            dir TEXT NOT NULL,
                            fname TEXT NOT NULL,
                            csum TEXT NOT NULL,
                            mtime INTEGER NOT NULL,
                            isNew BOOLEAN NOT NULL,
                            PRIMARY KEY (dir, fname)
                        )
                        """
                    )
                    stmt.execute(
                        """
                        CREATE TABLE fsrsWeights (
                            id INTEGER PRIMARY KEY,
                            weights TEXT NOT NULL,
                            mod INTEGER NOT NULL
                        )
                        """
                    )
                    stmt.execute(
                        """
                        CREATE TABLE fsrsParams (
                            id INTEGER PRIMARY KEY,
                            params TEXT NOT NULL,
                            mod INTEGER NOT NULL
                        )
                        """
                    )
                }
            }
            insertData(conn, version)
        }
    }

    private fun insertData(conn: java.sql.Connection, version: FormatVersion) {
        val now = Instant.now().epochSecond
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
                stmt.setString(14, "[]")
                stmt.setString(15, "[]")
                stmt.setDouble(16, 0.9)
                stmt.setString(17, "")
                stmt.setString(18, "[1.0,1.0,1.0,1.0,1.0,1.0,1.0]")
                stmt.setBoolean(19, false)
                stmt.setDouble(20, 0.0)
                stmt.setDouble(21, 0.0)
                stmt.setInt(22, 0)
                stmt.setInt(23, 0)
                stmt.setBoolean(24, true)
                stmt.setDouble(25, 0.9)
                stmt.setString(26, "")
                stmt.executeUpdate()
            }
        } else {
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

        conn.prepareStatement("INSERT INTO notes VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)").use { stmt ->
            notes.forEach { note ->
                stmt.setLong(1, note.id)
                stmt.setString(2, note.guid)
                stmt.setLong(3, note.mid)
                stmt.setLong(4, now)
                stmt.setInt(5, -1)
                stmt.setString(6, note.tags)
                val fieldsString = note.fields.joinToString("\u001f")
                stmt.setString(7, fieldsString)
                stmt.setInt(8, (note.fields.firstOrNull() ?: "").hashCode() and 0x7FFFFFFF)
                stmt.setLong(9, fieldsString.hashCode().toLong() and 0x7FFFFFFF)
                stmt.setInt(10, 0)
                stmt.setString(11, "")
                stmt.addBatch()
            }
            stmt.executeBatch()
        }

        if (version.schemaVersion >= 18) {
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
                    stmt.setString(19, "")
                    stmt.setDouble(20, 0.0)
                    stmt.setDouble(21, 0.0)
                    stmt.setString(22, "")
                    stmt.addBatch()
                }
                stmt.executeBatch()
            }
        } else {
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

        if (version.schemaVersion >= 18) {
            conn.prepareStatement("INSERT INTO mediaMeta VALUES (?, ?, ?, ?, ?)").use { stmt ->
                mediaFiles.keys.forEach { filename ->
                    stmt.setString(1, "")
                    stmt.setString(2, filename)
                    stmt.setString(3, "")
                    stmt.setLong(4, now)
                    stmt.setBoolean(5, true)
                    stmt.addBatch()
                }
                stmt.executeBatch()
            }
        }
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
