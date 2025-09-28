package fsrs.apkg

/**
 * APKG 格式版本枚举
 * 支持所有格式版本
 */
enum class ApkgFormat(val version: Version) {
    LEGACY(Version.LEGACY1),          // collection.anki2 (Anki 2.1.x 之前)
    TRANSITIONAL(Version.LEGACY2),    // collection.anki21 (Anki 2.1.x)
    LATEST(Version.LATEST);           // collection.anki21b (Anki 23.10+)

    val schemaVersion: Int get() = version.schemaVersion
    val databaseVersion: Int get() = version.databaseVersion
    val useZstdCompression: Boolean get() = version.useZstdCompression
    val databaseFileName: String get() = version.databaseFileName

    companion object {
        /**
         * 根据数据库文件名检测格式版本
         */
        fun fromDatabaseFileName(fileName: String): ApkgFormat {
            return when {
                fileName.contains("collection.anki21b") -> LATEST
                fileName.contains("collection.anki21") -> TRANSITIONAL
                fileName.contains("collection.anki2") -> LEGACY
                else -> throw IllegalArgumentException("Unsupported database file: $fileName")
            }
        }

        /**
         * 检测 ZIP 文件中的格式版本
         */
        fun detectFromZipEntries(entries: List<String>): ApkgFormat {
            return when {
                entries.contains("collection.anki21b") -> LATEST
                entries.contains("collection.anki21") -> TRANSITIONAL
                entries.contains("collection.anki2") -> LEGACY
                else -> throw IllegalArgumentException("No supported database format found in APKG")
            }
        }
    }
}

/**
 * APKG 版本信息数据类
 * 分离版本信息和格式逻辑
 */
data class Version(
    val schemaVersion: Int,
    val databaseVersion: Int,
    val useZstdCompression: Boolean,
    val databaseFileName: String
) {
    companion object {
        val LEGACY1 = Version(
            schemaVersion = 11,
            databaseVersion = 11,
            useZstdCompression = false,
            databaseFileName = "collection.anki2"
        )
        
        val LEGACY2 = Version(
            schemaVersion = 11,
            databaseVersion = 11,
            useZstdCompression = false,
            databaseFileName = "collection.anki21"
        )
        
        val LATEST = Version(
            schemaVersion = 18,
            databaseVersion = 11,
            useZstdCompression = true,
            databaseFileName = "collection.anki21b"
        )
    }
}