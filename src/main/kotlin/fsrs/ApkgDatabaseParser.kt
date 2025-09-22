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
                            updateSequenceNumber = rs.getInt("usn"),
                            checksum = if (schemaVersion >= 18) rs.getLong("csum") else null,
                            flags = if (schemaVersion >= 18) rs.getInt("flags") else null,
                            data = if (schemaVersion >= 18) rs.getString("data") else null
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
        
        // 动态检测可用的列
        val availableColumns = getAvailableColumns(conn, "cards")
        
        // 构建查询，只包含实际存在的列
        val baseColumns = listOf("id", "nid", "did", "ord", "type", "queue", "due", "ivl", "factor", "reps", "lapses", "left")
        val v18Columns = listOf("mod", "usn", "odue", "odid", "flags", "data")
        val fsrsColumns = listOf("fsrsState", "fsrsDifficulty", "fsrsStability", "fsrsDue")
        
        val selectedColumns = mutableListOf<String>().apply {
            addAll(baseColumns)
            if (schemaVersion >= 18) {
                addAll(v18Columns.filter { it in availableColumns })
                addAll(fsrsColumns.filter { it in availableColumns })
            }
        }
        
        val query = "SELECT ${selectedColumns.joinToString(", ")} FROM cards"
        
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
                            remainingSteps = rs.getInt("left"),
                            modificationTime = if ("mod" in selectedColumns) rs.getLong("mod") else null,
                            updateSequenceNumber = if ("usn" in selectedColumns) rs.getInt("usn") else null,
                            originalDueTime = if ("odue" in selectedColumns) rs.getInt("odue") else null,
                            originalDeckId = if ("odid" in selectedColumns) rs.getInt("odid") else null,
                            flags = if ("flags" in selectedColumns) rs.getInt("flags") else null,
                            data = if ("data" in selectedColumns) rs.getString("data") else null,
                            fsrsState = if ("fsrsState" in selectedColumns) rs.getString("fsrsState") else null,
                            fsrsDifficulty = if ("fsrsDifficulty" in selectedColumns) rs.getDouble("fsrsDifficulty") else null,
                            fsrsStability = if ("fsrsStability" in selectedColumns) rs.getDouble("fsrsStability") else null,
                            fsrsDue = if ("fsrsDue" in selectedColumns) rs.getString("fsrsDue") else null
                        ))
                    }
                }
            }
        } catch (e: SQLException) {
            throw ApkgParseException("Failed to parse cards: ${e.message}", e)
        }
        
        return cards
    }
    
    /**
     * 获取表中可用的列
     */
    private fun getAvailableColumns(conn: Connection, tableName: String): Set<String> {
        val columns = mutableSetOf<String>()
        try {
            conn.createStatement().use { stmt ->
                stmt.executeQuery("PRAGMA table_info($tableName)").use { rs ->
                    while (rs.next()) {
                        columns.add(rs.getString("name"))
                    }
                }
            }
        } catch (e: SQLException) {
            // 如果无法获取列信息，返回空集合
            e.printStackTrace()
            return emptySet()
        }
        return columns
    }

    fun parseDecks(conn: Connection): List<ApkgParser.ParsedDeck> {
        val decks = mutableListOf<ApkgParser.ParsedDeck>()
        
        try {
            conn.createStatement().use { stmt ->
                stmt.executeQuery("SELECT decks FROM col WHERE id = 1").use { rs ->
                    if (rs.next()) {
                        val decksJson = rs.getString("decks")
                        
                        // 检查 decksJson 是否为空或无效
                        if (decksJson.isBlank() || decksJson == "{}" || decksJson == "null") {
                            return emptyList()
                        }
                        
                        try {
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
                                    configurationId = deck["conf"]?.jsonPrimitive?.long ?: 1L,
                                    modificationTime = deck["mod"]?.jsonPrimitive?.long,
                                    updateSequenceNumber = deck["usn"]?.jsonPrimitive?.int,
                                    reviewLimit = if (schemaVersion >= 18) deck["reviewLimit"]?.jsonPrimitive?.int else null,
                                    newLimit = if (schemaVersion >= 18) deck["newLimit"]?.jsonPrimitive?.int else null
                                ))
                            }
                        } catch (e: Exception) {
                            e.printStackTrace()
                            // 如果 JSON 解析失败，返回空列表而不是抛出异常
                            return emptyList()
                        }
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            // 对于 decks 解析失败，返回空列表而不是抛出异常
            return emptyList()
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
                        
                        // 检查 modelsJson 是否为空或无效
                        if (modelsJson.isBlank() || modelsJson == "{}" || modelsJson == "null") {
                            return emptyList()
                        }
                        
                        try {
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
                                    css = model["css"]?.jsonPrimitive?.content ?: "",
                                    modificationTime = model["mod"]?.jsonPrimitive?.long,
                                    updateSequenceNumber = model["usn"]?.jsonPrimitive?.int
                                ))
                            }
                        } catch (e: Exception) {
                            e.printStackTrace()
                            // 如果 JSON 解析失败，返回空列表而不是抛出异常
                            return emptyList()
                        }
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            // 对于 models 解析失败，返回空列表而不是抛出异常
            return emptyList()
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
                answerFormat = template["afmt"]?.jsonPrimitive?.content ?: "",
                deckId = template["did"]?.let { 
                    if (it is JsonNull) null else it.jsonPrimitive.longOrNull 
                },
                browserQuestionFormat = template["bqfmt"]?.jsonPrimitive?.content ?: "",
                browserAnswerFormat = template["bafmt"]?.jsonPrimitive?.content ?: ""
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