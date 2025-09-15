package fsrs

import kotlinx.serialization.json.*
import java.io.*
import java.sql.DriverManager
import java.util.zip.ZipFile

/**
 * APKG 解析器
 * 用于解析 Anki 包格式文件，兼容 Anki 的 GitHub 源代码结构
 */
class ApkgParser {

    data class ParsedNote(
        val id: Long,
        val guid: String,
        val modelId: Long,
        val fields: List<String>,
        val tags: String,
        val modificationTime: Long,
        val updateSequenceNumber: Int
    )

    data class ParsedCard(
        val id: Long,
        val noteId: Long,
        val deckId: Long,
        val templateOrdinal: Int,
        val cardType: Int,
        val queueType: Int,
        val dueTime: Int,
        val interval: Int,
        val easeFactor: Int,
        val repetitions: Int,
        val lapses: Int,
        val remainingSteps: Int
    )

    data class ParsedDeck(
        val id: Long,
        val name: String,
        val description: String,
        val isCollapsed: Boolean,
        val isDynamic: Boolean,
        val configurationId: Long
    )

    data class ParsedModel(
        val id: Long,
        val name: String,
        val type: Int,
        val templates: List<ParsedTemplate>,
        val fields: List<ParsedField>,
        val css: String
    )

    data class ParsedTemplate(
        val name: String,
        val ordinal: Int,
        val questionFormat: String,
        val answerFormat: String
    )

    data class ParsedField(
        val name: String,
        val ordinal: Int,
        val isSticky: Boolean,
        val isRightToLeft: Boolean,
        val font: String,
        val size: Int
    )

    data class ParsedMediaFile(
        val index: Int,
        val filename: String,
        val data: ByteArray
    )

    data class ParsedApkg(
        val notes: List<ParsedNote>,
        val cards: List<ParsedCard>,
        val decks: List<ParsedDeck>,
        val models: List<ParsedModel>,
        val mediaFiles: List<ParsedMediaFile>,
        val databaseVersion: Int,
        val creationTime: Long
    )

    /**
     * 解析 APKG 文件
     */
    fun parseApkg(filePath: String): ParsedApkg {
        val zipFile = ZipFile(filePath)
        val tempDbFile = File.createTempFile("parsed_db", ".anki2")
        
        try {
            // 提取数据库文件
            val dbEntry = zipFile.entries().toList().find { it.name.endsWith(".anki2") }
                ?: throw IllegalArgumentException("No database file found in APKG")
            
            zipFile.getInputStream(dbEntry).use { input ->
                tempDbFile.outputStream().use { output ->
                    input.copyTo(output)
                }
            }

            // 解析数据库
            val url = "jdbc:sqlite:${tempDbFile.absolutePath}"
            DriverManager.getConnection(url).use { conn ->
                val notes = parseNotes(conn)
                val cards = parseCards(conn)
                val decks = parseDecks(conn)
                val models = parseModels(conn)
                val mediaFiles = parseMediaFiles(zipFile)
                val (dbVersion, creationTime) = parseCollectionInfo(conn)

                return ParsedApkg(
                    notes = notes,
                    cards = cards,
                    decks = decks,
                    models = models,
                    mediaFiles = mediaFiles,
                    databaseVersion = dbVersion,
                    creationTime = creationTime
                )
            }
        } finally {
            tempDbFile.delete()
            zipFile.close()
        }
    }

    private fun parseNotes(conn: java.sql.Connection): List<ParsedNote> {
        val notes = mutableListOf<ParsedNote>()
        
        conn.createStatement().use { stmt ->
            stmt.executeQuery("SELECT id, guid, mid, mod, usn, tags, flds FROM notes").use { rs ->
                while (rs.next()) {
                    val fieldsString = rs.getString("flds")
                    val fields = fieldsString.split("\u001f")
                    
                    notes.add(ParsedNote(
                        id = rs.getLong("id"),
                        guid = rs.getString("guid"),
                        modelId = rs.getLong("mid"),
                        fields = fields,
                        tags = rs.getString("tags"),
                        modificationTime = rs.getLong("mod"),
                        updateSequenceNumber = rs.getInt("usn")
                    ))
                }
            }
        }
        
        return notes
    }

    private fun parseCards(conn: java.sql.Connection): List<ParsedCard> {
        val cards = mutableListOf<ParsedCard>()
        
        conn.createStatement().use { stmt ->
            stmt.executeQuery("SELECT id, nid, did, ord, type, queue, due, ivl, factor, reps, lapses, left FROM cards").use { rs ->
                while (rs.next()) {
                    cards.add(ParsedCard(
                        id = rs.getLong("id"),
                        noteId = rs.getLong("nid"),
                        deckId = rs.getLong("did"),
                        templateOrdinal = rs.getInt("ord"),
                        cardType = rs.getInt("type"),
                        queueType = rs.getInt("queue"),
                        dueTime = rs.getInt("due"),
                        interval = rs.getInt("ivl"),
                        easeFactor = rs.getInt("factor"),
                        repetitions = rs.getInt("reps"),
                        lapses = rs.getInt("lapses"),
                        remainingSteps = rs.getInt("left")
                    ))
                }
            }
        }
        
        return cards
    }

    private fun parseDecks(conn: java.sql.Connection): List<ParsedDeck> {
        val decks = mutableListOf<ParsedDeck>()
        
        conn.createStatement().use { stmt ->
            stmt.executeQuery("SELECT decks FROM col WHERE id = 1").use { rs ->
                if (rs.next()) {
                    val decksJson = rs.getString("decks")
                    val decksObj = Json.parseToJsonElement(decksJson).jsonObject
                    
                    decksObj.forEach { (deckId, deckData) ->
                        val deck = deckData.jsonObject
                        decks.add(ParsedDeck(
                            id = deckId.toLong(),
                            name = deck["name"]?.jsonPrimitive?.content ?: "",
                            description = deck["desc"]?.jsonPrimitive?.content ?: "",
                            isCollapsed = deck["collapsed"]?.jsonPrimitive?.boolean ?: false,
                            isDynamic = deck["dyn"]?.jsonPrimitive?.int == 1,
                            configurationId = deck["conf"]?.jsonPrimitive?.long ?: 1L
                        ))
                    }
                }
            }
        }
        
        return decks
    }

    private fun parseModels(conn: java.sql.Connection): List<ParsedModel> {
        val models = mutableListOf<ParsedModel>()
        
        conn.createStatement().use { stmt ->
            stmt.executeQuery("SELECT models FROM col WHERE id = 1").use { rs ->
                if (rs.next()) {
                    val modelsJson = rs.getString("models")
                    val modelsObj = Json.parseToJsonElement(modelsJson).jsonObject
                    
                    modelsObj.forEach { (modelId, modelData) ->
                        val model = modelData.jsonObject
                        val templates = parseTemplates(model["tmpls"]?.jsonArray)
                        val fields = parseFields(model["flds"]?.jsonArray)
                        
                        models.add(ParsedModel(
                            id = modelId.toLong(),
                            name = model["name"]?.jsonPrimitive?.content ?: "",
                            type = model["type"]?.jsonPrimitive?.int ?: 0,
                            templates = templates,
                            fields = fields,
                            css = model["css"]?.jsonPrimitive?.content ?: ""
                        ))
                    }
                }
            }
        }
        
        return models
    }

    private fun parseTemplates(templatesArray: JsonArray?): List<ParsedTemplate> {
        return templatesArray?.mapNotNull { templateElement ->
            val template = templateElement.jsonObject
            ParsedTemplate(
                name = template["name"]?.jsonPrimitive?.content ?: "",
                ordinal = template["ord"]?.jsonPrimitive?.int ?: 0,
                questionFormat = template["qfmt"]?.jsonPrimitive?.content ?: "",
                answerFormat = template["afmt"]?.jsonPrimitive?.content ?: ""
            )
        } ?: emptyList()
    }

    private fun parseFields(fieldsArray: JsonArray?): List<ParsedField> {
        return fieldsArray?.mapNotNull { fieldElement ->
            val field = fieldElement.jsonObject
            ParsedField(
                name = field["name"]?.jsonPrimitive?.content ?: "",
                ordinal = field["ord"]?.jsonPrimitive?.int ?: 0,
                isSticky = field["sticky"]?.jsonPrimitive?.boolean ?: false,
                isRightToLeft = field["rtl"]?.jsonPrimitive?.boolean ?: false,
                font = field["font"]?.jsonPrimitive?.content ?: "Arial",
                size = field["size"]?.jsonPrimitive?.int ?: 20
            )
        } ?: emptyList()
    }

    private fun parseMediaFiles(zipFile: ZipFile): List<ParsedMediaFile> {
        val mediaFiles = mutableListOf<ParsedMediaFile>()
        
        // 解析媒体映射
        val mediaEntry = zipFile.getEntry("media")
        if (mediaEntry != null) {
            val mediaJson = zipFile.getInputStream(mediaEntry).use {
                it.readBytes().toString(Charsets.UTF_8)
            }
            
            val mediaMap = Json.parseToJsonElement(mediaJson).jsonObject
            
            // 提取媒体文件
            mediaMap.forEach { (indexStr, filenameElement) ->
                val index = indexStr.toIntOrNull()
                val filename = filenameElement.jsonPrimitive.content
                
                if (index != null) {
                    val mediaEntry = zipFile.getEntry(indexStr)
                    if (mediaEntry != null) {
                        val data = zipFile.getInputStream(mediaEntry).use {
                            it.readBytes()
                        }
                        mediaFiles.add(ParsedMediaFile(index, filename, data))
                    }
                }
            }
        }
        
        return mediaFiles
    }

    private fun parseCollectionInfo(conn: java.sql.Connection): Pair<Int, Long> {
        conn.createStatement().use { stmt ->
            stmt.executeQuery("SELECT ver, crt FROM col WHERE id = 1").use { rs ->
                if (rs.next()) {
                    val version = rs.getInt("ver")
                    val creationTime = rs.getLong("crt")
                    return Pair(version, creationTime)
                }
            }
        }
        throw IllegalArgumentException("Collection information not found")
    }

    /**
     * 快速检查 APKG 文件是否有效
     */
    fun isValidApkg(filePath: String): Boolean {
        return try {
            ZipFile(filePath).use { zipFile ->
                zipFile.entries().toList().any { it.name.endsWith(".anki2") } &&
                zipFile.getEntry("media") != null
            }
        } catch (e: Exception) {
            false
        }
    }

    /**
     * 获取 APKG 文件的基本信息
     */
    fun getApkgInfo(filePath: String): Map<String, Any> {
        val zipFile = ZipFile(filePath)
        val tempDbFile = File.createTempFile("info_db", ".anki2")
        
        try {
            val dbEntry = zipFile.entries().toList().find { it.name.endsWith(".anki2") }
                ?: return emptyMap()
            
            zipFile.getInputStream(dbEntry).use { input ->
                tempDbFile.outputStream().use { output ->
                    input.copyTo(output)
                }
            }

            val url = "jdbc:sqlite:${tempDbFile.absolutePath}"
            DriverManager.getConnection(url).use { conn ->
                val info = mutableMapOf<String, Any>()
                
                // 获取基本统计信息
                conn.createStatement().use { stmt ->
                    stmt.executeQuery("SELECT COUNT(*) FROM notes").use { rs ->
                        if (rs.next()) info["noteCount"] = rs.getInt(1)
                    }
                    
                    stmt.executeQuery("SELECT COUNT(*) FROM cards").use { rs ->
                        if (rs.next()) info["cardCount"] = rs.getInt(1)
                    }
                    
                    stmt.executeQuery("SELECT COUNT(*) FROM (SELECT json_each.value FROM col, json_each(col.decks))").use { rs ->
                        if (rs.next()) info["deckCount"] = rs.getInt(1)
                    }
                    
                    stmt.executeQuery("SELECT ver, crt FROM col WHERE id = 1").use { rs ->
                        if (rs.next()) {
                            info["databaseVersion"] = rs.getInt("ver")
                            info["creationTime"] = rs.getLong("crt")
                        }
                    }
                }
                
                return info
            }
        } finally {
            tempDbFile.delete()
            zipFile.close()
        }
    }
}