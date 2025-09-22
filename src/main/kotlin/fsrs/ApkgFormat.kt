package fsrs

/**
 * APKG 格式版本枚举
 * 用于统一管理 APKG 文件的格式版本信息
 */
enum class ApkgFormat {
    LEGACY {          // collection.anki2 (Anki 2.1.x 之前)
        override val schemaVersion = 11
        override val databaseVersion = 11
        override val useZstdCompression = false
        override val databaseFileName = "collection.anki2"
    },
    TRANSITIONAL {    // collection.anki21 (Anki 2.1.x)
        override val schemaVersion = 11
        override val databaseVersion = 11
        override val useZstdCompression = false
        override val databaseFileName = "collection.anki21"
    },
    LATEST {          // collection.anki21b (Anki 23.10+)
        override val schemaVersion = 18
        override val databaseVersion = 11
        override val useZstdCompression = true
        override val databaseFileName = "collection.anki21b"
    };

    abstract val schemaVersion: Int
    abstract val databaseVersion: Int
    abstract val useZstdCompression: Boolean
    abstract val databaseFileName: String

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