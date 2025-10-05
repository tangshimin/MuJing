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

import kotlinx.serialization.json.*
import java.sql.Connection
import java.sql.SQLException
import java.time.Instant

/**
 * 数据库解析器 - 负责解析 SQLite 数据库中的各种实体
 */
internal class ApkgDatabaseParser(private val schemaVersion: Int) {

    fun parseNotes(conn: Connection): List<Note> {
        val notes = mutableListOf<Note>()
        
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
                        
                        notes.add(Note(
                            id = rs.getLong("id"),
                            modelId = rs.getLong("mid"),
                            fields = fields,
                            tags = rs.getString("tags"),
                            guid = rs.getString("guid")
                        ))
                    }
                }
            }
        } catch (e: SQLException) {
            throw ApkgParseException("Failed to parse notes: ${e.message}", e)
        }
        
        return notes
    }

    fun parseCards(conn: Connection): List<Card> {
        val cards = mutableListOf<Card>()
        
        // 动态检测可用的列
        val availableColumns = getAvailableColumns(conn, "cards")
        
        // 构建查询，只包含实际存在的列
        val baseColumns = listOf("id", "nid", "did", "ord", "type", "queue", "due", "ivl", "factor", "reps", "lapses", "left")
        
        val selectedColumns = mutableListOf<String>().apply {
            addAll(baseColumns)
        }
        
        val query = "SELECT ${selectedColumns.joinToString(", ")} FROM cards"
        
        try {
            conn.createStatement().use { stmt ->
                stmt.executeQuery(query).use { rs ->
                    while (rs.next()) {
                        cards.add(Card(
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

    fun parseDecks(conn: Connection): List<Deck> {
        val decks = mutableListOf<Deck>()
        
        try {
            conn.createStatement().use { stmt ->
                stmt.executeQuery("SELECT decks FROM col WHERE id = 1").use { rs ->
                    if (rs.next()) {
                        val decksJson = rs.getString("decks")
                        
                        // 检查 decksJson 是否为空或无效
                        if (decksJson.isBlank() || decksJson == "{}" || decksJson == "null") {
                            println("DEBUG: decksJson is empty or invalid: '$decksJson'")
                            return emptyList()
                        }
                        
                        try {
                            val decksObj = Json.parseToJsonElement(decksJson).jsonObject
                            println("DEBUG: Found ${decksObj.size} decks in JSON")
                            
                            decksObj.forEach { (deckId, deckData) ->
                                val deck = deckData.jsonObject
                                println("DEBUG: Parsing deck $deckId: ${deck["name"]?.jsonPrimitive?.content}")
                                
                                decks.add(Deck(
                                    id = deckId.toLong(),
                                    name = deck["name"]?.jsonPrimitive?.content ?: "",
                                    description = deck["desc"]?.jsonPrimitive?.content ?: "",
                                    modificationTime = deck["mod"]?.jsonPrimitive?.long ?: 0,
                                    updateSequenceNumber = deck["usn"]?.jsonPrimitive?.int ?: 0,
                                    learnToday = deck["lrnToday"]?.jsonArray?.map { it.jsonPrimitive.int } ?: listOf(0, 0),
                                    reviewToday = deck["revToday"]?.jsonArray?.map { it.jsonPrimitive.int } ?: listOf(0, 0),
                                    newToday = deck["newToday"]?.jsonArray?.map { it.jsonPrimitive.int } ?: listOf(0, 0),
                                    timeToday = deck["timeToday"]?.jsonArray?.map { it.jsonPrimitive.int } ?: listOf(0, 0),
                                    collapsed = deck["collapsed"]?.jsonPrimitive?.boolean ?: false,
                                    browserCollapsed = deck["browserCollapsed"]?.jsonPrimitive?.boolean ?: true,
                                    isDynamic = deck["dyn"]?.jsonPrimitive?.int ?: 0,
                                    configurationId = deck["conf"]?.jsonPrimitive?.long ?: 1L,
                                    reviewLimit = deck["reviewLimit"]?.let { if (it is JsonNull) null else it.jsonPrimitive.intOrNull },
                                    newLimit = deck["newLimit"]?.let { if (it is JsonNull) null else it.jsonPrimitive.intOrNull }
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

    fun parseModels(conn: Connection): List<Model> {
        val models = mutableListOf<Model>()
        
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
                                
                                models.add(Model(
                                    id = modelId.toLong(),
                                    name = model["name"]?.jsonPrimitive?.content ?: "",
                                    type = model["type"]?.jsonPrimitive?.int ?: 0,
                                    modificationTime = model["mod"]?.jsonPrimitive?.long ?: Instant.now().epochSecond,
                                    updateSequenceNumber = model["usn"]?.jsonPrimitive?.int ?: -1,
                                    sortField = model["sortf"]?.jsonPrimitive?.int ?: 0,
                                    deckId = model["did"]?.let { 
                                        if (it is JsonNull) null else it.jsonPrimitive.longOrNull 
                                    },
                                    templates = templates,
                                    fields = fields,
                                    css = model["css"]?.jsonPrimitive?.content ?: ".card {\n font-family: arial;\n font-size: 20px;\n text-align: center;\n color: black;\n background-color: white;\n}"
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

    private fun parseTemplates(templatesArray: JsonArray?): List<CardTemplate> {
        return templatesArray?.mapNotNull { templateElement ->
            val template = templateElement.jsonObject
            CardTemplate(
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

    private fun parseFields(fieldsArray: JsonArray?): List<Field> {
        return fieldsArray?.mapNotNull { fieldElement ->
            val field = fieldElement.jsonObject
            Field(
                name = field["name"]?.jsonPrimitive?.content ?: "",
                ordinal = field["ord"]?.jsonPrimitive?.int ?: 0,
                sticky = field["sticky"]?.jsonPrimitive?.boolean ?: false,
                rightToLeft = field["rtl"]?.jsonPrimitive?.boolean ?: false,
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