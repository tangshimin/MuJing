package fsrs

import fsrs.zstd.ZstdNative
import java.io.File
import java.sql.DriverManager
import java.sql.SQLException
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
     * 检测 APKG 文件中的数据库格式（优先新格式）
     */
    private fun detectDatabaseFormat(zipFile: ZipFile): String {
        val entries = zipFile.entries().toList().map { it.name }
        
        // 优先检测新格式，然后过渡格式，最后旧格式
        return when {
            entries.contains("collection.anki21b") -> "collection.anki21b"
            entries.contains("collection.anki21") -> "collection.anki21"
            entries.contains("collection.anki2") -> "collection.anki2"
            else -> throw IllegalArgumentException("No supported database format found in APKG")
        }
    }

    /**
     * 检测数据库架构版本
     */
    private fun detectDatabaseSchemaVersion(conn: java.sql.Connection): Int {
        try {
            conn.createStatement().use { stmt ->
                stmt.executeQuery("SELECT ver FROM col WHERE id = 1").use { rs ->
                    if (rs.next()) {
                        return rs.getInt("ver")
                    }
                }
            }
        } catch (e: SQLException) {
            throw ApkgParseException("Failed to detect database schema version: ${e.message}", e)
        }
        throw ApkgParseException("无法检测数据库架构版本")
    }

    /**
     * 解析 APKG 文件
     */
    fun parseApkg(filePath: String): ParsedApkg {
        val zipFile = ZipFile(filePath)
        val dbFormat = detectDatabaseFormat(zipFile)
        val tempDbFile = File.createTempFile("parsed_db", ".${dbFormat.substringAfterLast('.')}")
        
        try {
            // 提取数据库文件
            val dbEntry = zipFile.getEntry(dbFormat)
                ?: throw IllegalArgumentException("Database file $dbFormat not found in APKG")
            
            zipFile.getInputStream(dbEntry).use { input ->
                tempDbFile.outputStream().use { output ->
                    if (dbFormat == "collection.anki21b") {
                        // 新格式使用 ZSTD 压缩，需要解压
                        val compressedData = input.readBytes()
                        val decompressedData = ZstdNative().decompress(compressedData)
                        output.write(decompressedData)
                    } else {
                        // 旧格式直接复制
                        input.copyTo(output)
                    }
                }
            }

            // 解析数据库
            val url = "jdbc:sqlite:${tempDbFile.absolutePath}"
            DriverManager.getConnection(url).use { conn ->
                // 检测架构版本并处理新格式的特殊逻辑
                val schemaVersion = detectDatabaseSchemaVersion(conn)
                
                val databaseParser = ApkgDatabaseParser(schemaVersion)
                val mediaParser = ApkgMediaParser()
                
                val notes = databaseParser.parseNotes(conn)
                val cards = databaseParser.parseCards(conn)
                val decks = databaseParser.parseDecks(conn)
                val models = databaseParser.parseModels(conn)
                val mediaFiles = mediaParser.parseMediaFiles(zipFile, dbFormat)
                val (dbVersion, creationTime) = databaseParser.parseCollectionInfo(conn)

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


    /**
     * 快速检查 APKG 文件是否有效
     */
    fun isValidApkg(filePath: String): Boolean {
        return try {
            ZipFile(filePath).use { zipFile ->
                val entries = zipFile.entries().toList().map { it.name }
                (entries.contains("collection.anki21b") || 
                 entries.contains("collection.anki21") || 
                 entries.contains("collection.anki2")) &&
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
        var tempDbFile: File? = null
        
        try {
            val dbFormat = detectDatabaseFormat(zipFile)
            tempDbFile = File.createTempFile("info_db", ".${dbFormat.substringAfterLast('.')}")
            
            val dbEntry = zipFile.getEntry(dbFormat)
                ?: return emptyMap()
            
            zipFile.getInputStream(dbEntry).use { input ->
                tempDbFile.outputStream().use { output ->
                    input.copyTo(output)
                }
            }

            val url = "jdbc:sqlite:${tempDbFile.absolutePath}"
            DriverManager.getConnection(url).use { conn ->
                val info = mutableMapOf<String, Any>()
                
                try {
                    val schemaVersion = detectDatabaseSchemaVersion(conn)
                    val databaseParser = ApkgDatabaseParser(schemaVersion)
                    val mediaParser = ApkgMediaParser()
                    
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
                    }
                    
                    val (dbVersion, creationTime) = databaseParser.parseCollectionInfo(conn)
                    info["databaseVersion"] = dbVersion
                    info["creationTime"] = creationTime
                    
                    // 添加媒体文件信息
                    info.putAll(mediaParser.getMediaInfo(zipFile, dbFormat))
                    
                } catch (e: Exception) {
                    // 对于信息获取，不抛出异常，只记录错误
                    info["error"] = e.message ?: "Unknown error"
                }
                
                return info
            }
        } catch (e: Exception) {
            return mapOf("error" to (e.message ?: "Failed to get APKG info"))
        } finally {
            tempDbFile?.delete()
            zipFile.close()
        }
    }
}