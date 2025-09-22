package fsrs

import java.util.zip.ZipFile

/**
 * APKG 解析器
 * 用于解析 Anki 包格式文件，支持所有格式版本：
 * - Legacy (collection.anki2): Anki 2.1.x 之前
 * - Transitional (collection.anki21): Anki 2.1.x
 * - Latest (collection.anki21b): Anki 23.10+ (V18, Zstd压缩)
 */
class ApkgParser {

    private val databaseHandler = ApkgDatabaseHandler()

    data class ParsedNote(
        val id: Long,
        val guid: String,
        val modelId: Long,
        val fields: List<String>,
        val tags: String,
        val modificationTime: Long,
        val updateSequenceNumber: Int,
        val checksum: Long? = null,
        val flags: Int? = null,
        val data: String? = null
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
        val remainingSteps: Int,
        val modificationTime: Long? = null,
        val updateSequenceNumber: Int? = null,
        val originalDueTime: Int? = null,
        val originalDeckId: Int? = null,
        val flags: Int? = null,
        val data: String? = null,
        val fsrsState: String? = null,
        val fsrsDifficulty: Double? = null,
        val fsrsStability: Double? = null,
        val fsrsDue: String? = null
    )

    data class ParsedDeck(
        val id: Long,
        val name: String,
        val description: String,
        val isCollapsed: Boolean,
        val isDynamic: Boolean,
        val configurationId: Long,
        val modificationTime: Long? = null,
        val updateSequenceNumber: Int? = null,
        val reviewLimit: Int? = null,
        val newLimit: Int? = null
    )

    data class ParsedModel(
        val id: Long,
        val name: String,
        val type: Int,
        val templates: List<ParsedTemplate>,
        val fields: List<ParsedField>,
        val css: String,
        val modificationTime: Long? = null,
        val updateSequenceNumber: Int? = null
    )

    data class ParsedTemplate(
        val name: String,
        val ordinal: Int,
        val questionFormat: String,
        val answerFormat: String,
        val deckId: Long? = null,
        val browserQuestionFormat: String = "",
        val browserAnswerFormat: String = ""
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
        val data: ByteArray,
        val size: Int? = null,
        val sha1: ByteArray? = null
    )

    data class ParsedApkg(
        val notes: List<ParsedNote>,
        val cards: List<ParsedCard>,
        val decks: List<ParsedDeck>,
        val models: List<ParsedModel>,
        val mediaFiles: List<ParsedMediaFile>,
        val databaseVersion: Int,
        val creationTime: Long,
        val format: ApkgFormat,
        val schemaVersion: Int
    )

    /**
     * 检测 APKG 文件中的数据库格式
     */
    private fun detectFormat(zipFile: ZipFile): ApkgFormat {
        val entries = zipFile.entries().toList().map { it.name }
        return ApkgFormat.detectFromZipEntries(entries)
    }


    /**
     * 解析 APKG 文件
     */
    fun parseApkg(filePath: String): ParsedApkg {
        val zipFile = ZipFile(filePath)
        val format = detectFormat(zipFile)
        
        val dbConnection = databaseHandler.prepareDatabaseConnection(zipFile, format)
        
        try {
            val schemaVersion = databaseHandler.detectSchemaVersion(dbConnection.connection)
            val databaseParser = ApkgDatabaseParser(schemaVersion.effectiveVersion)
            val mediaParser = ApkgMediaParser()
            
            val notes = databaseParser.parseNotes(dbConnection.connection)
            val cards = databaseParser.parseCards(dbConnection.connection)
            val decks = databaseParser.parseDecks(dbConnection.connection)
            val models = databaseParser.parseModels(dbConnection.connection)
            val mediaFiles = mediaParser.parseMediaFiles(zipFile, format.databaseFileName)
            val (dbVersion, creationTime) = databaseParser.parseCollectionInfo(dbConnection.connection)

            return ParsedApkg(
                notes = notes,
                cards = cards,
                decks = decks,
                models = models,
                mediaFiles = mediaFiles,
                databaseVersion = dbVersion,
                creationTime = creationTime,
                format = format,
                schemaVersion = schemaVersion.effectiveVersion
            )
        } finally {
            dbConnection.close()
            zipFile.close()
        }
    }


    /**
     * 快速检查 APKG 文件是否有效
     */
    fun isValidApkg(filePath: String): Boolean {
        return try {
            ZipFile(filePath).use { zipFile ->
                val entries = zipFile.entries().toList().map { it.name }
                
                try {
                    val format = ApkgFormat.detectFromZipEntries(entries)
                    val hasMedia = zipFile.getEntry("media") != null
                    
                    // 检查新格式的必需文件
                    if (format == ApkgFormat.LATEST) {
                        val hasMeta = zipFile.getEntry("meta") != null
                        return hasMedia && hasMeta
                    }
                    
                    return hasMedia
                } catch (e: IllegalArgumentException) {
                    false
                }
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
        
        try {
            val format = detectFormat(zipFile)
            val dbConnection = databaseHandler.prepareDatabaseConnection(zipFile, format)
            
            try {
                val info = mutableMapOf<String, Any>()
                
                try {
                    val schemaVersion = databaseHandler.detectSchemaVersion(dbConnection.connection)
                    val databaseParser = ApkgDatabaseParser(schemaVersion.effectiveVersion)
                    val mediaParser = ApkgMediaParser()
                    
                    // 添加格式信息
                    info["format"] = format.name
                    info["schemaVersion"] = schemaVersion.effectiveVersion
                    info["databaseFormat"] = format.databaseFileName
                    
                    // 获取基本统计信息
                    dbConnection.connection.createStatement().use { stmt ->
                        stmt.executeQuery("SELECT COUNT(*) FROM notes").use { rs ->
                            if (rs.next()) info["noteCount"] = rs.getInt(1)
                        }
                        
                        stmt.executeQuery("SELECT COUNT(*) FROM cards").use { rs ->
                            if (rs.next()) info["cardCount"] = rs.getInt(1)
                        }
                        
                        stmt.executeQuery("SELECT COUNT(*) FROM (SELECT json_each.value FROM col, json_each(col.decks))").use { rs ->
                            if (rs.next()) info["deckCount"] = rs.getInt(1)
                        }
                    }
                    
                    val (dbVersion, creationTime) = databaseParser.parseCollectionInfo(dbConnection.connection)
                    info["databaseVersion"] = dbVersion
                    info["creationTime"] = creationTime
                    
                    // 添加媒体文件信息
                    info.putAll(mediaParser.getMediaInfo(zipFile, format.databaseFileName))
                    
                } catch (e: Exception) {
                    // 对于信息获取，不抛出异常，只记录错误
                    info["error"] = e.message ?: "Unknown error"
                }
                
                return info
            } finally {
                dbConnection.close()
            }
        } catch (e: Exception) {
            return mapOf("error" to (e.message ?: "Failed to get APKG info"))
        } finally {
            zipFile.close()
        }
    }

    /**
     * 获取 APKG 文件的格式版本信息
     */
    fun getFormatInfo(filePath: String): Map<String, Any> {
        return try {
            ZipFile(filePath).use { zipFile ->
                val format = detectFormat(zipFile)
                val entries = zipFile.entries().toList().map { it.name }
                
                mapOf(
                    "format" to format.name,
                    "databaseFormat" to format.databaseFileName,
                    "hasMetaFile" to entries.contains("meta"),
                    "hasMediaFile" to entries.contains("media"),
                    "mediaFileCount" to entries.count { it.matches(Regex("\\d+")) },
                    "totalEntries" to entries.size
                )
            }
        } catch (e: Exception) {
            mapOf("error" to (e.message ?: "Failed to get format info"))
        }
    }
}