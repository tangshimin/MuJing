package fsrs

import kotlinx.serialization.json.*
import java.util.zip.ZipEntry
import java.util.zip.ZipFile

/**
 * 媒体文件解析器 - 负责解析 APKG 中的媒体文件
 */
internal class ApkgMediaParser {

    fun parseMediaFiles(zipFile: ZipFile): List<ApkgParser.ParsedMediaFile> {
        val mediaFiles = mutableListOf<ApkgParser.ParsedMediaFile>()
        
        try {
            // 解析媒体映射
            val mediaEntry = zipFile.getEntry("media")
            if (mediaEntry != null) {
                val mediaJson = zipFile.getInputStream(mediaEntry).use {
                    it.readBytes().toString(Charsets.UTF_8)
                }
                
                val mediaMap = Json.parseToJsonElement(mediaJson).jsonObject
                
                // 提取媒体文件
                mediaMap.forEach { (indexStr, filenameElement) ->
                    val index = indexStr.toIntOrNull()
                    val filename = filenameElement.jsonPrimitive.content
                    
                    if (index != null) {
                        val mediaEntry = zipFile.getEntry(indexStr)
                        if (mediaEntry != null) {
                            val data = zipFile.getInputStream(mediaEntry).use {
                                it.readBytes()
                            }
                            mediaFiles.add(ApkgParser.ParsedMediaFile(index, filename, data))
                        }
                    }
                }
            }
        } catch (e: Exception) {
            throw ApkgParseException("Failed to parse media files: ${e.message}", e)
        }
        
        return mediaFiles
    }

    fun getMediaInfo(zipFile: ZipFile): Map<String, Any> {
        val info = mutableMapOf<String, Any>()
        
        try {
            val mediaEntry = zipFile.getEntry("media")
            if (mediaEntry != null) {
                val mediaJson = zipFile.getInputStream(mediaEntry).use {
                    it.readBytes().toString(Charsets.UTF_8)
                }
                
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