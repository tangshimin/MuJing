package fsrs.apkg

import java.util.zip.ZipFile

/**
 * APKG 解析器
 * 支持所有格式版本
 */
class ApkgParser {

    private val databaseHandler = ApkgDatabaseHandler()
    private val mediaParser = ApkgMediaParser()
    
    /**
     * 解析上下文，遵循 Anki 的 Context 模式
     */
    private data class ParseContext(
        val zipFile: ZipFile,
        val format: ApkgFormat,
        val meta: ApkgMeta?
    )

    // 解析结果使用 ApkgCreator 的数据类
    data class ParsedApkg(
        val notes: List<Note>,
        val cards: List<Card>,
        val decks: List<Deck>,
        val models: List<Model>,
        val mediaFiles: List<MediaFile>,
        val databaseVersion: Int,
        val creationTime: Long,
        val format: ApkgFormat,
        val schemaVersion: Int
    )

    /**
     * 检测 APKG 文件中的格式和元数据
     */
    private fun detectFormatAndMeta(zipFile: ZipFile): ParseContext {
        val entries = zipFile.entries().toList().map { it.name }
        val format = ApkgFormat.detectFromZipEntries(entries)
        val meta = try {
            zipFile.getInputStream(zipFile.getEntry("meta")).use { stream ->
                ApkgMeta.fromInputStream(stream)
            }
        } catch (e: Exception) {
            null // 旧格式可能没有 meta 文件
        }
        return ParseContext(zipFile, format, meta)
    }


    /**
     * 解析 APKG 文件
     */
    fun parseApkg(filePath: String): ParsedApkg {
        val zipFile = ZipFile(filePath)
        val context = detectFormatAndMeta(zipFile)
        
        val dbConnection = databaseHandler.prepareDatabaseConnection(context.zipFile, context.format)
        
        try {
            val schemaVersion = databaseHandler.detectSchemaVersion(dbConnection.connection)
            val databaseParser = ApkgDatabaseParser(schemaVersion.effectiveVersion)
            
            // 遵循 Anki 的数据收集模式
            val notes = databaseParser.parseNotes(dbConnection.connection)
            val cards = databaseParser.parseCards(dbConnection.connection)
            val decks = databaseParser.parseDecks(dbConnection.connection)
            val models = databaseParser.parseModels(dbConnection.connection)
            val mediaFiles = mediaParser.parseMediaFiles(context.zipFile, context.format.databaseFileName)
            val (dbVersion, creationTime) = databaseParser.parseCollectionInfo(dbConnection.connection)

            return ParsedApkg(
                notes = notes,
                cards = cards,
                decks = decks,
                models = models,
                mediaFiles = mediaFiles,
                databaseVersion = dbVersion,
                creationTime = creationTime,
                format = context.format,
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
                    
                    // 遵循 Anki 的格式验证逻辑
                    when (format) {
                        ApkgFormat.LATEST -> {
                            val hasMeta = zipFile.getEntry("meta") != null
                            hasMedia && hasMeta
                        }
                        else -> hasMedia
                    }
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
            val context = detectFormatAndMeta(zipFile)
            val dbConnection = databaseHandler.prepareDatabaseConnection(context.zipFile, context.format)
            
            try {
                val info = mutableMapOf<String, Any>()
                
                try {
                    val schemaVersion = databaseHandler.detectSchemaVersion(dbConnection.connection)
                    val databaseParser = ApkgDatabaseParser(schemaVersion.effectiveVersion)
                    
                    // 添加格式和元数据信息
                    info["format"] = context.format.name
                    info["schemaVersion"] = schemaVersion.effectiveVersion
                    info["databaseFormat"] = context.format.databaseFileName
                    context.meta?.let { meta ->
                        info["metaVersion"] = meta.version
                    }
                    
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
                    info.putAll(mediaParser.getMediaInfo(context.zipFile, context.format.databaseFileName))
                    
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
                val context = detectFormatAndMeta(zipFile)
                val entries = zipFile.entries().toList().map { it.name }
                
                mapOf(
                    "format" to context.format.name,
                    "databaseFormat" to context.format.databaseFileName,
                    "hasMetaFile" to entries.contains("meta"),
                    "hasMediaFile" to entries.contains("media"),
                    "metaVersion" to (context.meta?.version ?: "N/A").toString(),
                    "mediaFileCount" to entries.count { it.matches(Regex("\\d+")) },
                    "totalEntries" to entries.size
                )
            }
        } catch (e: Exception) {
            mapOf("error" to (e.message ?: "Failed to get format info"))
        }
    }
}