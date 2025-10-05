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
import kotlinx.serialization.json.*
import java.io.File
import java.sql.Connection
import java.sql.DriverManager
import java.sql.PreparedStatement
import java.sql.Statement
import java.time.Instant

/**
 * APKG 数据库创建器
 * 负责创建不同格式的 SQLite 数据库
 */
internal class ApkgDatabaseCreator {

    /**
     * 创建数据库文件
     */
    fun createDatabase(
        format: ApkgFormat,
        notes: List<Note>,
        cards: List<Card>,
        decks: Map<Long, Deck>,
        models: Map<Long, Model>,
        mediaFiles: Map<String, ByteArray>
    ): File {
        val dbFile = File.createTempFile("collection", ".${format.databaseFileName.substringAfterLast('.')}")
        
        createDatabaseContent(dbFile, format, notes, cards, decks, models, mediaFiles)
        
        if (format.useZstdCompression) {
            return compressDatabaseWithZstd(dbFile)
        }
        return dbFile
    }

    private fun createDatabaseContent(
        dbFile: File,
        format: ApkgFormat,
        notes: List<Note>,
        cards: List<Card>,
        decks: Map<Long, Deck>,
        models: Map<Long, Model>,
        mediaFiles: Map<String, ByteArray>
    ) {
        val url = "jdbc:sqlite:${dbFile.absolutePath}"
        DriverManager.getConnection(url).use { conn ->
            createDatabaseStructure(conn, format)
            insertData(conn, format, notes, cards, decks, models, mediaFiles)
        }
    }

    private fun createDatabaseStructure(conn: Connection, format: ApkgFormat) {
        conn.createStatement().use { stmt ->
            stmt.execute("PRAGMA user_version = ${format.schemaVersion}")
            
            // 创建表结构
            createColTable(stmt, format)
            createNotesTable(stmt)
            createCardsTable(stmt, format)
            createGravesTable(stmt)
            createRevlogTable(stmt, format)
            createAdditionalTables(stmt, format)
        }
    }

    private fun createColTable(stmt: Statement, format: ApkgFormat) {
        if (format.schemaVersion >= 18) {
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
    }

    private fun createNotesTable(stmt: Statement) {
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
    }

    private fun createCardsTable(stmt: Statement, format: ApkgFormat) {
        if (format.schemaVersion >= 18) {
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
                    fsrsState TEXT,
                    fsrsDifficulty REAL,
                    fsrsStability REAL,
                    fsrsDue TEXT
                )
            """)
        } else {
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
    }

    private fun createGravesTable(stmt: Statement) {
        stmt.execute("""
            CREATE TABLE graves (usn INTEGER NOT NULL, oid INTEGER NOT NULL, type INTEGER NOT NULL, PRIMARY KEY (oid, type)) WITHOUT ROWID
        """)
    }

    private fun createRevlogTable(stmt: Statement, format: ApkgFormat) {
        if (format.schemaVersion >= 18) {
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
                    fsrsRating INTEGER,
                    fsrsReviewTime INTEGER,
                    fsrsState TEXT
                )
            """)
        } else {
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
    }

    private fun createAdditionalTables(stmt: Statement, format: ApkgFormat) {
        if (format.schemaVersion >= 18) {
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

    private fun insertData(
        conn: Connection,
        format: ApkgFormat,
        notes: List<Note>,
        cards: List<Card>,
        decks: Map<Long, Deck>,
        models: Map<Long, Model>,
        mediaFiles: Map<String, ByteArray>
    ) {
        val now = Instant.now().epochSecond
        
        insertColData(conn, format, decks, models, now)
        insertNotesData(conn, notes, now)
        insertCardsData(conn, format, cards, now)
        insertMediaMetaData(conn, format, mediaFiles, now)
    }

    private fun insertColData(
        conn: Connection,
        format: ApkgFormat,
        decks: Map<Long, Deck>,
        models: Map<Long, Model>,
        now: Long
    ) {
        val colConfig = createColConfig(decks)
        val modelsJson = createModelsJson(models)
        val decksJson = createDecksJson(decks)
        val dconfJson = createDconfJson()

        if (format.schemaVersion >= 18) {
            conn.prepareStatement("INSERT INTO col VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)").use { stmt ->
                setColDataV18(stmt, format, colConfig, modelsJson, decksJson, dconfJson, now)
                stmt.executeUpdate()
            }
        } else {
            conn.prepareStatement("INSERT INTO col VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)").use { stmt ->
                setColDataLegacy(stmt, format, colConfig, modelsJson, decksJson, dconfJson, now)
                stmt.executeUpdate()
            }
        }
    }

    private fun createColConfig(decks: Map<Long, Deck>): JsonObject {
        return JsonObject(mapOf(
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
            "curModel" to JsonPrimitive(1),
            "collapseTime" to JsonPrimitive(1200)
        ))
    }

    private fun createModelsJson(models: Map<Long, Model>): JsonObject {
        return JsonObject(models.mapKeys { it.key.toString() }.mapValues { (_, model) ->
            JsonObject(mapOf(
                "id" to JsonPrimitive(model.id),
                "name" to JsonPrimitive(model.name),
                "type" to JsonPrimitive(model.type),
                "mod" to JsonPrimitive(model.modificationTime),
                "usn" to JsonPrimitive(model.updateSequenceNumber),
                "sortf" to JsonPrimitive(model.sortField),
                "did" to (model.deckId?.let { JsonPrimitive(it) } ?: JsonNull),
                "tmpls" to JsonArray(model.templates.map { tmpl ->
                    JsonObject(mapOf(
                        "name" to JsonPrimitive(tmpl.name),
                        "ord" to JsonPrimitive(tmpl.ordinal),
                        "qfmt" to JsonPrimitive(tmpl.questionFormat),
                        "afmt" to JsonPrimitive(tmpl.answerFormat),
                        "did" to (tmpl.deckId?.let { JsonPrimitive(it) } ?: JsonNull),
                        "bqfmt" to JsonPrimitive(tmpl.browserQuestionFormat),
                        "bafmt" to JsonPrimitive(tmpl.browserAnswerFormat)
                    ))
                }),
                "flds" to JsonArray(model.fields.map { fld ->
                    JsonObject(mapOf(
                        "name" to JsonPrimitive(fld.name),
                        "ord" to JsonPrimitive(fld.ordinal),
                        "sticky" to JsonPrimitive(fld.sticky),
                        "rtl" to JsonPrimitive(fld.rightToLeft),
                        "font" to JsonPrimitive(fld.font),
                        "size" to JsonPrimitive(fld.size)
                    ))
                }),
                "css" to JsonPrimitive(model.css)
            ))
        })
    }

    private fun createDecksJson(decks: Map<Long, Deck>): JsonObject {
        return JsonObject(decks.mapKeys { it.key.toString() }.mapValues { (_, deck) ->
            JsonObject(mapOf(
                "id" to JsonPrimitive(deck.id),
                "mod" to JsonPrimitive(deck.modificationTime),
                "name" to JsonPrimitive(deck.name),
                "usn" to JsonPrimitive(deck.updateSequenceNumber),
                "lrnToday" to JsonArray(deck.learnToday.map { JsonPrimitive(it) }),
                "revToday" to JsonArray(deck.reviewToday.map { JsonPrimitive(it) }),
                "newToday" to JsonArray(deck.newToday.map { JsonPrimitive(it) }),
                "timeToday" to JsonArray(deck.timeToday.map { JsonPrimitive(it) }),
                "collapsed" to JsonPrimitive(deck.collapsed),
                "browserCollapsed" to JsonPrimitive(deck.browserCollapsed),
                "desc" to JsonPrimitive(deck.description),
                "dyn" to JsonPrimitive(deck.isDynamic),
                "conf" to JsonPrimitive(deck.configurationId),
                "extendNew" to JsonPrimitive(deck.extendNew),
                "extendRev" to JsonPrimitive(deck.extendRev),
                "reviewLimit" to (deck.reviewLimit?.let { JsonPrimitive(it) } ?: JsonNull),
                "newLimit" to (deck.newLimit?.let { JsonPrimitive(it) } ?: JsonNull),
                "reviewLimitToday" to (deck.reviewLimitToday?.let { JsonPrimitive(it) } ?: JsonNull),
                "newLimitToday" to (deck.newLimitToday?.let { JsonPrimitive(it) } ?: JsonNull)
            ))
        })
    }

    private fun createDconfJson(): String {
        return """{
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
    }

    private fun setColDataV18(
        stmt: PreparedStatement,
        format: ApkgFormat,
        colConfig: JsonObject,
        modelsJson: JsonObject,
        decksJson: JsonObject,
        dconfJson: String,
        now: Long
    ) {
        stmt.setInt(1, 1)
        stmt.setLong(2, now)
        stmt.setLong(3, now)
        stmt.setLong(4, now)
        stmt.setInt(5, format.databaseVersion)
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
    }

    private fun setColDataLegacy(
        stmt: PreparedStatement,
        format: ApkgFormat,
        colConfig: JsonObject,
        modelsJson: JsonObject,
        decksJson: JsonObject,
        dconfJson: String,
        now: Long
    ) {
        stmt.setInt(1, 1)
        stmt.setLong(2, now)
        stmt.setLong(3, now)
        stmt.setLong(4, now)
        stmt.setInt(5, format.databaseVersion)
        stmt.setInt(6, 0)
        stmt.setInt(7, 0)
        stmt.setLong(8, now)
        stmt.setString(9, colConfig.toString())
        stmt.setString(10, modelsJson.toString())
        stmt.setString(11, decksJson.toString())
        stmt.setString(12, dconfJson)
        stmt.setString(13, "{}")
    }

    private fun insertNotesData(conn: Connection, notes: List<Note>, now: Long) {
        conn.prepareStatement("INSERT INTO notes VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)").use { stmt ->
            notes.forEach { note ->
                stmt.setLong(1, note.id)
                stmt.setString(2, note.guid)
                stmt.setLong(3, note.modelId)
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
    }

    private fun insertCardsData(conn: Connection, format: ApkgFormat, cards: List<Card>, now: Long) {
        if (format.schemaVersion >= 18) {
            conn.prepareStatement("INSERT INTO cards VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)").use { stmt ->
                cards.forEach { card ->
                    setCardDataV18(stmt, card, now)
                    stmt.addBatch()
                }
                stmt.executeBatch()
            }
        } else {
            conn.prepareStatement("INSERT INTO cards VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)").use { stmt ->
                cards.forEach { card ->
                    setCardDataLegacy(stmt, card, now)
                    stmt.addBatch()
                }
                stmt.executeBatch()
            }
        }
    }

    private fun setCardDataV18(stmt: PreparedStatement, card: Card, now: Long) {
        stmt.setLong(1, card.id)
        stmt.setLong(2, card.noteId)
        stmt.setLong(3, card.deckId)
        stmt.setInt(4, card.templateOrdinal)
        stmt.setLong(5, now)
        stmt.setInt(6, -1)
        stmt.setInt(7, card.cardType)
        stmt.setInt(8, card.queueType)
        stmt.setInt(9, card.dueTime)
        stmt.setInt(10, card.interval)
        stmt.setInt(11, card.easeFactor)
        stmt.setInt(12, card.repetitions)
        stmt.setInt(13, card.lapses)
        stmt.setInt(14, card.remainingSteps)
        stmt.setInt(15, 0)
        stmt.setInt(16, 0)
        stmt.setInt(17, 0)
        stmt.setString(18, "")
        stmt.setString(19, "")
        stmt.setDouble(20, 0.0)
        stmt.setDouble(21, 0.0)
        stmt.setString(22, "")
    }

    private fun setCardDataLegacy(stmt: PreparedStatement, card: Card, now: Long) {
        stmt.setLong(1, card.id)
        stmt.setLong(2, card.noteId)
        stmt.setLong(3, card.deckId)
        stmt.setInt(4, card.templateOrdinal)
        stmt.setLong(5, now)
        stmt.setInt(6, -1)
        stmt.setInt(7, card.cardType)
        stmt.setInt(8, card.queueType)
        stmt.setInt(9, card.dueTime)
        stmt.setInt(10, card.interval)
        stmt.setInt(11, card.easeFactor)
        stmt.setInt(12, card.repetitions)
        stmt.setInt(13, card.lapses)
        stmt.setInt(14, card.remainingSteps)
        stmt.setInt(15, 0)
        stmt.setInt(16, 0)
        stmt.setInt(17, 0)
        stmt.setString(18, "")
    }

    private fun insertMediaMetaData(conn: Connection, format: ApkgFormat, mediaFiles: Map<String, ByteArray>, now: Long) {
        if (format.schemaVersion >= 18) {
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

    // 使用 zstd 压缩 SQLite 数据库内容
    private fun compressDatabaseWithZstd(dbFile: File): File {
        val compressedFile = File.createTempFile("collection", ".${ApkgFormat.LATEST.databaseFileName}.zstd")
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
}