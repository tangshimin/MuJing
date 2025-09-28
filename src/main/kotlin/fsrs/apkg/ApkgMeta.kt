package fsrs.apkg

import java.io.InputStream

/**
 * APKG 元数据类
 * 处理 meta 文件中的版本信息
 */
data class ApkgMeta(
    val version: Int
) {
    
    fun toByteArray(): ByteArray {
        val versionBytes = encodeVarint(version.toLong())
        return byteArrayOf(0x08) + versionBytes
    }
    companion object {
        /**
         * 从输入流解析 meta 文件
         */
        fun fromInputStream(input: InputStream): ApkgMeta {
            val bytes = input.readBytes()
            // 简单的 protobuf 解析：第一个字节是字段标签，后续是 varint 编码的版本号
            if (bytes.isNotEmpty() && bytes[0] == 0x08.toByte()) {
                val version = decodeVarint(bytes, 1)
                return ApkgMeta(version.toInt())
            }
            throw IllegalArgumentException("Invalid meta file format")
        }
        
        /**
         * 解码 varint 值
         */
        private fun decodeVarint(bytes: ByteArray, startIndex: Int): Long {
            var result = 0L
            var shift = 0
            var index = startIndex
            
            while (index < bytes.size) {
                val b = bytes[index].toInt() and 0xFF
                result = result or ((b and 0x7F).toLong() shl shift)
                if ((b and 0x80) == 0) {
                    return result
                }
                shift += 7
                index++
            }
            throw IllegalArgumentException("Invalid varint encoding")
        }
        
        /**
         * 创建新格式的 meta 数据
         */
        fun createLatest(): ByteArray {
            val versionBytes = encodeVarint(3L) // LATEST 版本
            return byteArrayOf(0x08) + versionBytes
        }
        
        /**
         * 创建过渡格式的 meta 数据
         */
        fun createTransitional(): ByteArray {
            val versionBytes = encodeVarint(2L) // LEGACY2 版本
            return byteArrayOf(0x08) + versionBytes
        }
        
        /**
         * 创建旧格式的 meta 数据
         */
        fun createLegacy(): ByteArray {
            val versionBytes = encodeVarint(1L) // LEGACY1 版本
            return byteArrayOf(0x08) + versionBytes
        }
        
        /**
         * 编码 varint 值
         */
        private fun encodeVarint(value: Long): ByteArray {
            val result = mutableListOf<Byte>()
            var v = value
            
            while (v >= 0x80) {
                result.add(((v and 0x7F) or 0x80).toByte())
                v = v ushr 7
            }
            result.add(v.toByte())
            
            return result.toByteArray()
        }
    }
}