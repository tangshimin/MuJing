package fsrs


import fsrs.zstd.ZstdNative
import kotlinx.serialization.json.*
import java.util.zip.ZipFile

/**
 * 媒体文件解析器 - 负责解析 APKG 中的媒体文件
 */
internal class ApkgMediaParser {

    fun parseMediaFiles(zipFile: ZipFile, databaseFormat: String): List<ApkgParser.ParsedMediaFile> {
        val mediaFiles = mutableListOf<ApkgParser.ParsedMediaFile>()
        
        try {
            // 解析媒体映射
            val mediaEntry = zipFile.getEntry("media")
            if (mediaEntry != null) {
                val mediaData = zipFile.getInputStream(mediaEntry).use {
                    it.readBytes()
                }
                
                if (databaseFormat == "collection.anki21b") {
                    // 新格式：Protobuf + Zstd 压缩
                    val decompressedData = ZstdNative().decompress(mediaData)
                    val mediaEntries = parseProtobufMediaEntries(decompressedData)
                    
                    // 提取媒体文件
                    mediaEntries.forEach { (index, filename, size, sha1) ->
                        val mediaFileEntry = zipFile.getEntry(index.toString())
                        if (mediaFileEntry != null) {
                            val compressedData = zipFile.getInputStream(mediaFileEntry).use {
                                it.readBytes()
                            }
                            val data = ZstdNative().decompress(compressedData)
                            mediaFiles.add(ApkgParser.ParsedMediaFile(index, filename, data, size, sha1))
                        }
                    }
                } else {
                    // 旧格式：JSON
                    val mediaJson = mediaData.toString(Charsets.UTF_8)
                    val mediaMap = Json.parseToJsonElement(mediaJson).jsonObject
                    
                    // 提取媒体文件
                    mediaMap.forEach { (indexStr, filenameElement) ->
                        val index = indexStr.toIntOrNull()
                        val filename = filenameElement.jsonPrimitive.content
                        
                        if (index != null) {
                            val mediaFileEntry = zipFile.getEntry(indexStr)
                            if (mediaFileEntry != null) {
                                val data = zipFile.getInputStream(mediaFileEntry).use {
                                    it.readBytes()
                                }
                                mediaFiles.add(ApkgParser.ParsedMediaFile(index, filename, data))
                            }
                        }
                    }
                }
            }
        } catch (e: Exception) {
            throw ApkgParseException("Failed to parse media files: ${e.message}", e)
        }
        
        return mediaFiles
    }

    /**
     * 解析 Protobuf 格式的媒体条目
     */
    private fun parseProtobufMediaEntries(data: ByteArray): List<Quadruple<Int, String, Int, ByteArray>> {
        val entries = mutableListOf<Quadruple<Int, String, Int, ByteArray>>()
        var offset = 0
        
        while (offset < data.size) {
            // 解析字段标签
            val (fieldNumber, wireType, bytesRead) = parseProtobufTag(data, offset)
            offset += bytesRead
            
            if (fieldNumber == 1 && wireType == 2) { // MediaEntry (length-delimited)
                val (length, bytesRead) = parseVarint(data, offset)
                offset += bytesRead
                
                val entryData = data.copyOfRange(offset, offset + length.toInt())
                offset += length.toInt()
                
                val (index, filename, size, sha1) = parseMediaEntry(entryData, entries.size)
                entries.add(Quadruple(index, filename, size, sha1))
            }
        }
        
        return entries
    }

    /**
     * 解析单个 MediaEntry
     */
    private fun parseMediaEntry(data: ByteArray, currentIndex: Int = 0): Quadruple<Int, String, Int, ByteArray> {
        var offset = 0
        var filename = ""
        var size = 0
        var sha1 = byteArrayOf()
        
        while (offset < data.size) {
            val (fieldNumber, wireType, bytesRead) = parseProtobufTag(data, offset)
            offset += bytesRead
            
            when (fieldNumber) {
                1 -> { // name (string)
                    if (wireType == 2) {
                        val (length, bytesRead) = parseVarint(data, offset)
                        offset += bytesRead
                        filename = String(data, offset, length.toInt(), Charsets.UTF_8)
                        offset += length.toInt()
                    }
                }
                2 -> { // size (uint32)
                    if (wireType == 0) {
                        val (value, bytesRead) = parseVarint(data, offset)
                        offset += bytesRead
                        size = value.toInt()
                    }
                }
                3 -> { // sha1 (bytes)
                    if (wireType == 2) {
                        val (length, bytesRead) = parseVarint(data, offset)
                        offset += bytesRead
                        sha1 = data.copyOfRange(offset, offset + length.toInt())
                        offset += length.toInt()
                    }
                }
            }
        }
        
        // 使用传入的索引（因为 Protobuf 中没有显式的索引字段）
        val index = currentIndex + 1
        
        return Quadruple(index, filename, size, sha1)
    }

    /**
     * 解析 Protobuf 标签
     */
    private fun parseProtobufTag(data: ByteArray, offset: Int): Triple<Int, Int, Int> {
        var pos = offset
        var value = 0
        var shift = 0
        
        do {
            val b = data[pos++].toInt() and 0xFF
            value = value or ((b and 0x7F) shl shift)
            shift += 7
        } while ((b and 0x80) != 0)
        
        val fieldNumber = value shr 3
        val wireType = value and 0x07
        
        return Triple(fieldNumber, wireType, pos - offset)
    }

    /**
     * 解析 Varint
     */
    private fun parseVarint(data: ByteArray, offset: Int): Pair<Long, Int> {
        var pos = offset
        var value: Long = 0
        var shift = 0
        
        do {
            val b = data[pos++].toLong() and 0xFF
            value = value or ((b and 0x7F) shl shift)
            shift += 7
        } while ((b and 0x80) != 0L)
        
        return Pair(value, pos - offset)
    }

    /**
     * 四元组数据类
     */
    private data class Quadruple<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)

    fun getMediaInfo(zipFile: ZipFile, databaseFormat: String): Map<String, Any> {
        val info = mutableMapOf<String, Any>()
        
        try {
            val mediaEntry = zipFile.getEntry("media")
            if (mediaEntry != null) {
                val mediaData = zipFile.getInputStream(mediaEntry).use {
                    it.readBytes()
                }
                
                if (databaseFormat == "collection.anki21b") {
                    // 新格式：Protobuf + Zstd 压缩
                    val decompressedData = ZstdNative().decompress(mediaData)
                    val mediaEntries = parseProtobufMediaEntries(decompressedData)
                    info["mediaFileCount"] = mediaEntries.size
                    
                    // 统计媒体文件类型
                    val mediaTypes = mutableMapOf<String, Int>()
                    mediaEntries.forEach { (_, filename, _, _) ->
                        val fileType = getMediaType(filename)
                        mediaTypes[fileType] = mediaTypes.getOrDefault(fileType, 0) + 1
                    }
                    info["mediaTypes"] = mediaTypes
                    info["format"] = "protobuf+zstd"
                } else {
                    // 旧格式：JSON
                    val mediaJson = mediaData.toString(Charsets.UTF_8)
                    val mediaMap = Json.parseToJsonElement(mediaJson).jsonObject
                    info["mediaFileCount"] = mediaMap.size
                    
                    // 统计媒体文件类型
                    val mediaTypes = mutableMapOf<String, Int>()
                    mediaMap.forEach { (_, filenameElement) ->
                        val filename = filenameElement.jsonPrimitive.content
                        val fileType = getMediaType(filename)
                        mediaTypes[fileType] = mediaTypes.getOrDefault(fileType, 0) + 1
                    }
                    info["mediaTypes"] = mediaTypes
                    info["format"] = "json"
                }
            }
        } catch (e: Exception) {
            // 对于信息获取，不抛出异常，只记录错误
            info["mediaError"] = e.message ?: "Unknown error"
        }
        
        return info
    }

    private fun getMediaType(filename: String): String {
        return when {
            filename.endsWith(".mp3") || filename.endsWith(".wav") || filename.endsWith(".ogg") -> "audio"
            filename.endsWith(".jpg") || filename.endsWith(".png") || filename.endsWith(".gif") -> "image"
            filename.endsWith(".mp4") || filename.endsWith(".webm") || filename.endsWith(".mkv") || filename.endsWith(".avi") -> "video"
            else -> "other"
        }
    }
}