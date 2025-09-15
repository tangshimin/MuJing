package fsrs

import kotlinx.serialization.encodeToString
import java.io.*
import java.sql.DriverManager
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import kotlinx.serialization.json.*
import java.time.Instant

/**
 * APKG 创建器
 * 用于创建 Anki 包格式文件
 */
class ApkgCreator {

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
     * 创建 APKG 文件
     */
    fun createApkg(outputPath: String) {
        val tempDbFile = File.createTempFile("collection", ".anki2")
        try {
            // 创建 SQLite 数据库
            createDatabase(tempDbFile)

            // 创建 ZIP 文件
            FileOutputStream(outputPath).use { fos ->
                ZipOutputStream(fos).use { zos ->
                    // 添加数据库文件
                    zos.putNextEntry(ZipEntry("collection.anki2"))
                    tempDbFile.inputStream().use { it.copyTo(zos) }
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
            tempDbFile.delete()
        }
    }

    private fun createDatabase(dbFile: File) {
        val url = "jdbc:sqlite:${dbFile.absolutePath}"
        DriverManager.getConnection(url).use { conn ->
            // 创建表结构
            conn.createStatement().use { stmt ->
                // 集合表
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
                        sfld INTEGER NOT NULL,
                        csum INTEGER NOT NULL,
                        flags INTEGER NOT NULL,
                        data TEXT NOT NULL
                    )
                """)

                // 卡片表
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

                // 删除日志表
                stmt.execute("CREATE TABLE graves (usn INTEGER NOT NULL, oid INTEGER NOT NULL, type INTEGER NOT NULL, PRIMARY KEY (oid, type)) WITHOUT ROWID")

                // 复习日志表
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

            // 插入数据
            insertData(conn)
        }
    }

    private fun insertData(conn: java.sql.Connection) {
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

        conn.prepareStatement("INSERT INTO col VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)").use { stmt ->
            stmt.setInt(1, 1)
            stmt.setLong(2, now)
            stmt.setLong(3, now)
            stmt.setLong(4, now)
            stmt.setInt(5, 11) // 版本号 11 (适用于 Anki 2.1.x)
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

    private fun createMediaJson(): String {
        val mediaMap = mediaFiles.keys.mapIndexed { index, filename ->
            index.toString() to filename
        }.toMap()
        return Json.encodeToString(mediaMap)
    }
}
