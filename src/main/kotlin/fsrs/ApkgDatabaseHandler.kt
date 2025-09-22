package fsrs

import fsrs.zstd.ZstdNative
import java.io.File
import java.sql.DriverManager
import java.sql.SQLException
import java.util.zip.ZipFile

/**
 * APKG 数据库处理器
 * 负责处理 APKG 文件中的数据库提取和连接管理
 */
internal class ApkgDatabaseHandler {

    /**
     * 从 APKG 文件中提取并准备数据库连接
     */
    fun prepareDatabaseConnection(zipFile: ZipFile, format: ApkgFormat): DatabaseConnection {
        val tempDbFile = File.createTempFile("parsed_db", ".${format.databaseFileName.substringAfterLast('.')}")
        
        try {
            // 提取数据库文件
            val dbEntry = zipFile.getEntry(format.databaseFileName)
                ?: throw ApkgParseException("Database file ${format.databaseFileName} not found in APKG")
            
            zipFile.getInputStream(dbEntry).use { input ->
                tempDbFile.outputStream().use { output ->
                    if (format.useZstdCompression) {
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

            // 创建数据库连接
            val url = "jdbc:sqlite:${tempDbFile.absolutePath}"
            val conn = DriverManager.getConnection(url)
            
            return DatabaseConnection(conn, tempDbFile, format)
        } catch (e: Exception) {
            tempDbFile.delete()
            throw ApkgParseException("Failed to prepare database connection: ${e.message}", e)
        }
    }

    /**
     * 检测数据库架构版本
     */
    fun detectSchemaVersion(conn: java.sql.Connection): SchemaVersion {
        val userVersion = detectUserSchemaVersion(conn)
        val dbVersion = detectDatabaseSchemaVersion(conn)
        
        // 使用有效的架构版本（优先使用 user_version，如果为0则使用数据库版本）
        val effectiveVersion = if (userVersion > 0) userVersion else dbVersion
        
        return SchemaVersion(userVersion, dbVersion, effectiveVersion)
    }

    private fun detectUserSchemaVersion(conn: java.sql.Connection): Int {
        try {
            conn.createStatement().use { stmt ->
                stmt.executeQuery("PRAGMA user_version").use { rs ->
                    if (rs.next()) {
                        return rs.getInt(1)
                    }
                }
            }
        } catch (e: SQLException) {
            throw ApkgParseException("Failed to detect user schema version: ${e.message}", e)
        }
        throw ApkgParseException("Cannot detect user schema version")
    }

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
        throw ApkgParseException("Cannot detect database schema version")
    }
}

/**
 * 数据库连接包装类
 */
data class DatabaseConnection(
    val connection: java.sql.Connection,
    val tempFile: File,
    val format: ApkgFormat
) {
    fun close() {
        connection.close()
        tempFile.delete()
    }
}

/**
 * 架构版本信息
 */
data class SchemaVersion(
    val userVersion: Int,
    val databaseVersion: Int,
    val effectiveVersion: Int
)