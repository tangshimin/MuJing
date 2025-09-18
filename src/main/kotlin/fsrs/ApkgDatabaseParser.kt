package fsrs

import kotlinx.serialization.json.*
import java.sql.Connection
import java.sql.SQLException

/**
 * 数据库解析器 - 负责解析 SQLite 数据库中的各种实体
 */
internal class ApkgDatabaseParser(private val schemaVersion: Int) {

    fun parseNotes(conn: Connection): List<ApkgParser.ParsedNote> {
        val notes = mutableListOf<ApkgParser.ParsedNote>()
        
        val query = if (schemaVersion >= 18) {
            "SELECT id, guid, mid, mod, usn, tags, flds, sfld, csum, flags, data FROM notes"
        } else {
            "SELECT id, guid, mid, mod, usn, tags, flds FROM notes"
        }
        
        try {
            conn.createStatement().use { stmt ->
                stmt.executeQuery(query).use { rs ->
                    while (rs.next()) {
                        val fieldsString = rs.getString("flds")
                        val fields = fieldsString.split("\u001f")
                        
                        notes.add(ApkgParser.ParsedNote(
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
        } catch (e: SQLException) {
            throw ApkgParseException("Failed to parse notes: ${e.message}", e)
        }
        
        return notes
    }

    fun parseCards(conn: Connection): List<ApkgParser.ParsedCard> {
        val cards = mutableListOf<ApkgParser.ParsedCard>()
        
        val query = if (schemaVersion >= 18) {
            "SELECT id, nid, did, ord, mod, usn, type, queue, due, ivl, factor, reps, lapses, left, odue, odid, flags, data FROM cards"
        } else {
            "SELECT id, nid, did, ord, type, queue, due, ivl, factor, reps, lapses, left FROM cards"
        }
        
        try {
            conn.createStatement().use { stmt ->
                stmt.executeQuery(query).use { rs ->
                    while (rs.next()) {
                        cards.add(ApkgParser.ParsedCard(
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
        } catch (e: SQLException) {
            throw ApkgParseException("Failed to parse cards: ${e.message}", e)
        }
        
        return cards
    }

    fun parseDecks(conn: Connection): List<ApkgParser.ParsedDeck> {
        val decks = mutableListOf<ApkgParser.ParsedDeck>()
        
        try {
            conn.createStatement().use { stmt ->
                stmt.executeQuery("SELECT decks FROM col WHERE id = 1").use { rs ->
                    if (rs.next()) {
                        val decksJson = rs.getString("decks")
                        val decksObj = Json.parseToJsonElement(decksJson).jsonObject
                        
                        decksObj.forEach { (deckId, deckData) ->
                            val deck = deckData.jsonObject
                            
                            if (schemaVersion >= 18) {
                                deck["reviewLimit"]?.jsonPrimitive?.int
                                deck["newLimit"]?.jsonPrimitive?.int
                                deck["reviewLimitToday"]?.jsonPrimitive?.int
                                deck["newLimitToday"]?.jsonPrimitive?.int
                                deck["browserCollapsed"]?.jsonPrimitive?.boolean
                            }
                            
                            decks.add(ApkgParser.ParsedDeck(
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
        } catch (e: Exception) {
            throw ApkgParseException("Failed to parse decks: ${e.message}", e)
        }
        
        return decks
    }

    fun parseModels(conn: Connection): List<ApkgParser.ParsedModel> {
        val models = mutableListOf<ApkgParser.ParsedModel>()
        
        try {
            conn.createStatement().use { stmt ->
                stmt.executeQuery("SELECT models FROM col WHERE id = 1").use { rs ->
                    if (rs.next()) {
                        val modelsJson = rs.getString("models")
                        val modelsObj = Json.parseToJsonElement(modelsJson).jsonObject
                        
                        modelsObj.forEach { (modelId, modelData) ->
                            val model = modelData.jsonObject
                            val templates = parseTemplates(model["tmpls"]?.jsonArray)
                            val fields = parseFields(model["flds"]?.jsonArray)
                            
                            if (schemaVersion >= 18) {
                                model["latexPre"]?.jsonPrimitive?.content
                                model["latexPost"]?.jsonPrimitive?.content
                                model["latexsvg"]?.jsonPrimitive?.boolean
                            }
                            
                            models.add(ApkgParser.ParsedModel(
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
        } catch (e: Exception) {
            throw ApkgParseException("Failed to parse models: ${e.message}", e)
        }
        
        return models
    }

    fun parseCollectionInfo(conn: Connection): Pair<Int, Long> {
        try {
            conn.createStatement().use { stmt ->
                stmt.executeQuery("SELECT ver, crt FROM col WHERE id = 1").use { rs ->
                    if (rs.next()) {
                        val version = rs.getInt("ver")
                        val creationTime = rs.getLong("crt")
                        return Pair(version, creationTime)
                    }
                }
            }
        } catch (e: SQLException) {
            throw ApkgParseException("Failed to parse collection info: ${e.message}", e)
        }
        throw ApkgParseException("Collection information not found")
    }

    private fun parseTemplates(templatesArray: JsonArray?): List<ApkgParser.ParsedTemplate> {
        return templatesArray?.mapNotNull { templateElement ->
            val template = templateElement.jsonObject
            ApkgParser.ParsedTemplate(
                name = template["name"]?.jsonPrimitive?.content ?: "",
                ordinal = template["ord"]?.jsonPrimitive?.int ?: 0,
                questionFormat = template["qfmt"]?.jsonPrimitive?.content ?: "",
                answerFormat = template["afmt"]?.jsonPrimitive?.content ?: ""
            )
        } ?: emptyList()
    }

    private fun parseFields(fieldsArray: JsonArray?): List<ApkgParser.ParsedField> {
        return fieldsArray?.mapNotNull { fieldElement ->
            val field = fieldElement.jsonObject
            ApkgParser.ParsedField(
                name = field["name"]?.jsonPrimitive?.content ?: "",
                ordinal = field["ord"]?.jsonPrimitive?.int ?: 0,
                isSticky = field["sticky"]?.jsonPrimitive?.boolean ?: false,
                isRightToLeft = field["rtl"]?.jsonPrimitive?.boolean ?: false,
                font = field["font"]?.jsonPrimitive?.content ?: "Arial",
                size = field["size"]?.jsonPrimitive?.int ?: 20
            )
        } ?: emptyList()
    }
}

/**
 * APKG 解析异常
 */
class ApkgParseException(message: String, cause: Throwable? = null) : RuntimeException(message, cause)